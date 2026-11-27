import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection, runMutation } from "@/lib/bookeat/services/data-source";
import type { ReviewRecord } from "@/lib/bookeat/types";

export const reviewService = {
  list: () => readCollection(() => apiClient.get<ReviewRecord[]>("/admin/reviews"), () => demoStore.reviews),
  moderate: (id: string, status: ReviewRecord["status"], reason: string) => runMutation(
    () => apiClient.patch<ReviewRecord>(`/admin/reviews/${id}/moderation`, { status, reason }),
    () => {
      const review = demoStore.reviews.find((item) => item.id === id);
      if (!review) throw new Error("Review not found");
      review.status = status;
      return review;
    },
  ),
};

