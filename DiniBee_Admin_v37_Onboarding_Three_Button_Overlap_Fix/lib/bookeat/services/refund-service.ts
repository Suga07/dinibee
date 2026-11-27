import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection, runMutation } from "@/lib/bookeat/services/data-source";
import type { RefundRecord } from "@/lib/bookeat/types";

export const refundService = {
  list: () =>
    readCollection(
      () => apiClient.get<RefundRecord[]>("/admin/finance/refunds"),
      () => demoStore.refunds,
    ),
  submit: (refundId: string) =>
    runMutation(
      () => apiClient.post<RefundRecord>(`/admin/finance/refunds/${refundId}/submit`),
      () => {
        const refund = demoStore.refunds.find((item) => item.id === refundId);
        if (!refund) throw new Error("Refund request not found");
        refund.status = "PROCESSING";
        return refund;
      },
    ),
};

