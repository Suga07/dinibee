package com.bookeat.review.service;

import com.bookeat.commerce.service.CommerceAccess;
import com.bookeat.commerce.service.IdempotencyService;
import com.bookeat.common.audit.AuditService;
import com.bookeat.common.response.PageResponse;
import com.bookeat.exception.ApiException;
import com.bookeat.exception.ErrorCode;
import com.bookeat.order.enums.FoodOrderStatus;
import com.bookeat.order.repository.FoodOrderRepository;
import com.bookeat.reservation.enums.ReservationStatus;
import com.bookeat.reservation.repository.ReservationRepository;
import com.bookeat.restaurant.repository.RestaurantRepository;
import com.bookeat.review.dto.ReviewRequests;
import com.bookeat.review.dto.ReviewResponse;
import com.bookeat.review.entity.Review;
import com.bookeat.review.enums.ReviewStatus;
import com.bookeat.review.mapper.ReviewMapper;
import com.bookeat.review.repository.ReviewRepository;
import com.bookeat.wallet.dto.WalletRequests;
import com.bookeat.wallet.enums.RewardRuleType;
import com.bookeat.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviews;
    private final ReviewMapper mapper;
    private final ReservationRepository reservations;
    private final FoodOrderRepository orders;
    private final CommerceAccess access;
    private final IdempotencyService idempotency;
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final WalletService walletService;
    private final PlatformTransactionManager transactionManager;

    private record ReviewMutationResult(UUID reviewId, UUID restaurantId) {}

    /**
     * Persists the review in its own committed transaction first so neither rating-summary
     * refresh nor optional reward claiming can mark the review transaction rollback-only
     * or abort PostgreSQL and turn a valid submission into HTTP 500.
     */
    @Override
    public ReviewResponse create(UUID actor, String key, ReviewRequests.Create q) {
        var writeTx = new TransactionTemplate(transactionManager);
        writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        ReviewMutationResult result = writeTx.execute(status -> {
            UUID[] resolvedRestaurant = new UUID[1];
            UUID reviewId = idempotency.execute(actor, "review-create", key, q, () -> {
                UUID c = access.customer(actor);
                if ((q.reservationId() == null) == (q.foodOrderId() == null)) {
                    throw new ApiException(ErrorCode.VALIDATION_FAILED);
                }
                UUID rid = q.reservationId() != null
                        ? reservations.restaurantId(q.reservationId())
                                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND))
                        : orders.restaurantId(q.foodOrderId())
                                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
                resolvedRestaurant[0] = rid;
                access.lock(rid);

                if (q.reservationId() != null) {
                    reservations.findByIdAndCustomerId(q.reservationId(), c)
                            .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                            .orElseThrow(() -> new ApiException(ErrorCode.REVIEW_INELIGIBLE));
                    var existing = reviews.findByCustomerIdAndReservationId(c, q.reservationId());
                    if (existing.isPresent()) {
                        var r = existing.get();
                        applyUpdates(r, q);
                        reviews.saveAndFlush(r);
                        return r.getId();
                    }
                } else {
                    orders.findById(q.foodOrderId())
                            .filter(o -> o.getCustomerId().equals(c) && o.getStatus() == FoodOrderStatus.COMPLETED)
                            .orElseThrow(() -> new ApiException(ErrorCode.REVIEW_INELIGIBLE));
                    var existing = reviews.findByCustomerIdAndFoodOrderId(c, q.foodOrderId());
                    if (existing.isPresent()) {
                        var r = existing.get();
                        applyUpdates(r, q);
                        reviews.saveAndFlush(r);
                        return r.getId();
                    }
                }

                var r = new Review();
                r.setCustomerId(c);
                r.setRestaurantId(rid);
                r.setReservationId(q.reservationId());
                r.setFoodOrderId(q.foodOrderId());
                r.setRating(q.rating());
                r.setFoodRating(q.foodRating());
                r.setServiceRating(q.serviceRating());
                r.setAmbienceRating(q.ambienceRating());
                r.setComment(q.comment());
                r.setStatus(ReviewStatus.VISIBLE);
                reviews.saveAndFlush(r);
                audit.record(actor, "REVIEW_CREATED", "Review", r.getId(), null);
                return r.getId();
            });

            UUID rid = resolvedRestaurant[0];
            if (rid == null) {
                rid = reviews.findById(reviewId).map(Review::getRestaurantId).orElse(null);
            }
            return new ReviewMutationResult(reviewId, rid);
        });

        UUID id = result.reviewId();
        if (result.restaurantId() != null) {
            refreshIsolated(result.restaurantId());
        }
        claimReviewRewardIfEligible(actor, id);

        return mapper.view(reviews.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Override
    public ReviewResponse update(UUID actor, UUID id, ReviewRequests.Update q) {
        var writeTx = new TransactionTemplate(transactionManager);
        writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        UUID restaurantId = writeTx.execute(status -> {
            var r = lock(id);
            if (!r.getCustomerId().equals(access.customer(actor)) || r.getStatus() == ReviewStatus.DELETED) {
                throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            access.version(r.getVersion(), q.version());
            r.setRating(q.rating());
            r.setFoodRating(q.foodRating());
            r.setServiceRating(q.serviceRating());
            r.setAmbienceRating(q.ambienceRating());
            r.setComment(q.comment());
            reviews.flush();
            return r.getRestaurantId();
        });

        if (restaurantId != null) {
            refreshIsolated(restaurantId);
        }
        // Idempotent: awards review points once if this visible review never received points earlier.
        claimReviewRewardIfEligible(actor, id);

        return mapper.view(reviews.findById(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Override
    @Transactional
    public void delete(UUID actor, UUID id, ReviewRequests.Action q) {
        var r = lock(id);
        if (!r.getCustomerId().equals(access.customer(actor))) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        access.version(r.getVersion(), q.version());
        r.setStatus(ReviewStatus.DELETED);
        r.setComment(null);
        r.setOwnerReply(null);
        reviews.flush();
        refresh(r.getRestaurantId());
        audit.record(actor, "REVIEW_DELETED", "Review", id, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> own(UUID actor, int page, int size) {
        return PageResponse.from(
                reviews.findByCustomerIdAndStatusNot(access.customer(actor), ReviewStatus.DELETED, page(page, size))
                        .map(mapper::view));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> publicReviews(UUID rid, int page, int size) {
        var venue = restaurantRepository.findById(rid)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        if (access.enabled(venue).isEmpty()) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return PageResponse.from(
                reviews.findByRestaurantIdAndStatus(rid, ReviewStatus.VISIBLE, page(page, size))
                        .map(mapper::view));
    }

    @Override
    @Transactional
    public ReviewResponse reply(UUID actor, UUID rid, UUID id, ReviewRequests.Reply q) {
        access.lockOwner(actor, rid);
        var r = reviews.findById(id)
                .filter(x -> x.getRestaurantId().equals(rid) && x.getStatus() == ReviewStatus.VISIBLE)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        access.version(r.getVersion(), q.version());
        r.setOwnerReply(q.reply());
        reviews.flush();
        audit.record(actor, "REVIEW_REPLIED", "Review", id, null);
        return mapper.view(r);
    }

    @Override
    @Transactional
    public ReviewResponse moderate(UUID actor, UUID id, ReviewRequests.Action q, boolean hide) {
        var r = lock(id);
        access.version(r.getVersion(), q.version());
        if (r.getStatus() == ReviewStatus.DELETED) {
            throw new ApiException(ErrorCode.REVIEW_INELIGIBLE);
        }
        r.setStatus(hide ? ReviewStatus.HIDDEN : ReviewStatus.VISIBLE);
        r.setModerationReason(q.reason());
        reviews.flush();
        refresh(r.getRestaurantId());
        audit.record(actor, hide ? "REVIEW_HIDDEN" : "REVIEW_RESTORED", "Review", id, null);
        return mapper.view(r);
    }

    private void claimReviewRewardIfEligible(UUID actor, UUID reviewId) {
        try {
            var ensureRuleTx = new TransactionTemplate(transactionManager);
            ensureRuleTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            Integer activeReviewRules = ensureRuleTx.execute(status -> {
                Integer anyRuleCount = jdbc.queryForObject(
                        "select count(*) from reward_rules where rule_type='REVIEW'",
                        Integer.class);
                if (anyRuleCount == null || anyRuleCount == 0) {
                    jdbc.update(
                            "insert into reward_rules(id,created_at,updated_at,version,rule_type,minimum_spend,points,conversion_value,active,effective_from,effective_until) " +
                            "values(gen_random_uuid(),CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,'REVIEW',0,10,0,true,CURRENT_TIMESTAMP - interval '1 minute',null)");
                }
                return jdbc.queryForObject(
                        "select count(*) from reward_rules where rule_type='REVIEW' and active=true and effective_from <= CURRENT_TIMESTAMP and (effective_until is null or effective_until > CURRENT_TIMESTAMP)",
                        Integer.class);
            });

            if (activeReviewRules != null && activeReviewRules > 0) {
                walletService.claim(
                        actor,
                        "review-reward-" + reviewId,
                        new WalletRequests.Claim(RewardRuleType.REVIEW, reviewId));
            }
        } catch (Exception ex) {
            log.warn("Non-fatal review reward claim failure for review={}: {}", reviewId, ex.getMessage());
        }
    }

    private void applyUpdates(Review r, ReviewRequests.Create q) {
        r.setRating(q.rating());
        if (q.foodRating() != null) r.setFoodRating(q.foodRating());
        if (q.serviceRating() != null) r.setServiceRating(q.serviceRating());
        if (q.ambienceRating() != null) r.setAmbienceRating(q.ambienceRating());
        if (q.comment() != null) r.setComment(q.comment());
        r.setStatus(ReviewStatus.VISIBLE);
    }

    private Review lock(UUID id) {
        UUID rid = jdbc.query("select restaurant_id from reviews where id=?", (r, n) -> r.getObject(1, UUID.class), id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        access.lock(rid);
        return reviews.findById(id).orElseThrow();
    }

    private Pageable page(int p, int n) {
        return PageRequest.of(p, n, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private void refreshIsolated(UUID rid) {
        try {
            var refreshTx = new TransactionTemplate(transactionManager);
            refreshTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            refreshTx.executeWithoutResult(status -> refresh(rid));
        } catch (Exception ex) {
            log.warn("Non-fatal rating summary refresh failure for restaurant={}: {}", rid, ex.getMessage());
        }
    }

    private void refresh(UUID rid) {
        try {
            jdbc.update(
                    "insert into restaurant_rating_summaries(restaurant_id,average_rating,review_count) " +
                    "select cast(? as uuid),coalesce(round(avg(rating)::numeric,2),0),count(*) " +
                    "from reviews where restaurant_id=cast(? as uuid) and status='VISIBLE' " +
                    "on conflict(restaurant_id) do update set average_rating=excluded.average_rating,review_count=excluded.review_count",
                    rid.toString(), rid.toString());
        } catch (Exception ex) {
            log.warn("Rating summary refresh SQL failed for restaurant={}: {}", rid, ex.getMessage());
        }
    }
}
