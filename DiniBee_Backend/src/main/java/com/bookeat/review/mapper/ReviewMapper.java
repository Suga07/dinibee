package com.bookeat.review.mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.bookeat.review.entity.Review;
import com.bookeat.review.dto.ReviewResponse;
import java.util.UUID;

@Component
public class ReviewMapper {
    private final JdbcTemplate jdbc;

    @Autowired
    public ReviewMapper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ReviewMapper() {
        this.jdbc = null;
    }

    public ReviewResponse view(Review r) {
        return view(r, resolveCustomerName(r.getCustomerId()));
    }

    public ReviewResponse view(Review r, String customerName) {
        return new ReviewResponse(
            r.getId(),
            r.getVersion(),
            r.getRestaurantId(),
            r.getReservationId(),
            r.getFoodOrderId(),
            customerName,
            r.getRating(),
            r.getFoodRating(),
            r.getServiceRating(),
            r.getAmbienceRating(),
            r.getComment(),
            r.getOwnerReply(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }

    private String resolveCustomerName(UUID customerId) {
        if (jdbc == null || customerId == null) {
            return "Verified Diner";
        }
        try {
            var rows = jdbc.query(
                "select trim(concat_ws(' ', u.first_name, u.last_name)) from customers c join users u on u.id = c.user_id where c.id = ?",
                (rs, rowNum) -> rs.getString(1),
                customerId
            );
            if (!rows.isEmpty() && rows.get(0) != null && !rows.get(0).isBlank()) {
                return rows.get(0).trim();
            }
        } catch (Exception ignored) {
        }
        return "Verified Diner";
    }
}
