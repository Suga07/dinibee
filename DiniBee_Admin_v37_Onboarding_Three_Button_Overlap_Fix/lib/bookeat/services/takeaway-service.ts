import { apiClient } from "@/lib/bookeat/api";
import { unwrapCollection } from "@/lib/bookeat/services/data-source";
import type { TakeawayRecord } from "@/lib/bookeat/types";

function normalize(x: any): TakeawayRecord {
  return {
    id: String(x?.id ?? ""), orderNumber: String(x?.orderNumber ?? x?.order_number ?? "—"),
    type: (x?.type === "ON_THE_WAY" ? "ON_THE_WAY" : "REGULAR"),
    customerName: String(x?.customerName ?? "—"), restaurantName: String(x?.restaurantName ?? "—"),
    journeyStatus: (x?.journeyStatus ?? "NOT_STARTED") as TakeawayRecord["journeyStatus"],
    eta: x?.eta ?? undefined, amount: Number(x?.amount ?? x?.total ?? 0) || 0,
    paymentStatus: (x?.paymentStatus ?? "UNPAID") as TakeawayRecord["paymentStatus"],
    refundStatus: (x?.refundStatus ?? "NONE") as TakeawayRecord["refundStatus"],
    status: (x?.status ?? "PLACED") as TakeawayRecord["status"], createdAt: String(x?.createdAt ?? x?.created_at ?? "—"),
    customerId: x?.customerId ? String(x.customerId) : undefined, customerUserId: x?.customerUserId ? String(x.customerUserId) : undefined,
    customerPhone: x?.customerPhone ? String(x.customerPhone) : undefined, customerEmail: x?.customerEmail ? String(x.customerEmail) : undefined,
    restaurantId: x?.restaurantId ? String(x.restaurantId) : undefined, restaurantLogoUrl: x?.restaurantLogoUrl ? String(x.restaurantLogoUrl) : undefined,
    restaurantCoverImageUrl: x?.restaurantCoverImageUrl ? String(x.restaurantCoverImageUrl) : undefined, restaurantLocation: x?.restaurantLocation ? String(x.restaurantLocation) : undefined,
    foodOrderId: x?.foodOrderId ? String(x.foodOrderId) : undefined, paymentId: x?.paymentId ? String(x.paymentId) : undefined,
    paymentMethod: x?.paymentMethod ? String(x.paymentMethod) : undefined, foodImageUrl: x?.foodImageUrl ? String(x.foodImageUrl) : undefined,
    itemsSummary: x?.itemsSummary ? String(x.itemsSummary) : undefined,
  };
}
export const takeawayService = {
  list: async (type?: TakeawayRecord["type"]) => {
    try { const r:any=await apiClient.get(`/admin/takeaway-orders${type ? `?type=${type}` : ""}`); return unwrapCollection<any>(r).map(normalize); } catch { return []; }
  },
};
