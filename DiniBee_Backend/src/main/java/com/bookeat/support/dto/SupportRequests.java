package com.bookeat.support.dto;
import jakarta.validation.constraints.*;import java.util.UUID;import com.bookeat.support.enums.*;
public final class SupportRequests {
 private SupportRequests(){}
 public record Create(@NotBlank @Size(max=80) String category,@NotBlank @Size(max=160) String subject,@NotBlank @Size(max=5000) String description,@NotNull SupportPriority priority){}
 public record Message(@NotBlank @Size(max=5000) String message){}
 public record Action(@NotNull @PositiveOrZero Long version,@NotBlank @Size(max=1000) String reason){}
 public record Assign(@NotNull @PositiveOrZero Long version,@NotNull UUID adminId,@NotBlank @Size(max=1000) String reason){}
 public record Status(@NotNull @PositiveOrZero Long version,@NotNull SupportStatus status,@NotBlank @Size(max=1000) String reason){}
}
