import { apiClient } from "@/lib/bookeat/api";
import { unwrapCollection, readCollection } from "@/lib/bookeat/services/data-source";
import type { PaymentRecord, SettlementRecord } from "@/lib/bookeat/types";
function payment(x:any): PaymentRecord { return {
 id:String(x?.id??""), transactionId:String(x?.transactionId??x?.gatewayPaymentId??"—"), referenceId:String(x?.referenceId??"—"),
 customerName:String(x?.customerName??"—"), restaurantName:String(x?.restaurantName??"—"), paymentType:(x?.paymentType??"DINE_IN") as PaymentRecord["paymentType"],
 grossAmount:Number(x?.grossAmount??x?.amount??0)||0, rewardDiscount:Number(x?.rewardDiscount??x?.discountAmount??0)||0, gatewayPaidAmount:Number(x?.gatewayPaidAmount??x?.amount??0)||0,
 paymentMethod:String(x?.paymentMethod??x?.gateway??"—"), status:(x?.status??"PENDING") as PaymentRecord["status"], date:String(x?.date??x?.createdAt??"—") }; }
export const paymentService = {
 listTransactions: async()=>{try{const r:any=await apiClient.get("/admin/finance/payments");return unwrapCollection<any>(r).map(payment)}catch{return []}},
 listSettlements: () => readCollection(() => apiClient.get<SettlementRecord[]>("/admin/finance/settlements")),
};
