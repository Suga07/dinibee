package com.bookeat.review.dto;
import jakarta.validation.constraints.*;import java.util.UUID;
public final class ReviewRequests {
 private ReviewRequests(){}
 public record Create(UUID reservationId,UUID foodOrderId,@Min(1) @Max(5) int rating,@Min(1) @Max(5) Integer foodRating,@Min(1) @Max(5) Integer serviceRating,@Min(1) @Max(5) Integer ambienceRating,@Size(max=3000) String comment){}
 public record Update(@NotNull @PositiveOrZero Long version,@Min(1) @Max(5) int rating,@Min(1) @Max(5) Integer foodRating,@Min(1) @Max(5) Integer serviceRating,@Min(1) @Max(5) Integer ambienceRating,@Size(max=3000) String comment){}
 public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason){}
 public record Reply(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=2000) String reply){}
}
