import { apiClient } from "@/lib/bookeat/api";
import { unwrapCollection } from "@/lib/bookeat/services/data-source";
import type { DiniTicketRecord, DiniTicketStatus } from "@/lib/bookeat/types";

function normalize(x:any): DiniTicketRecord {
  return {
    id:String(x?.id??""), tokenNumber:String(x?.tokenNumber??x?.ticketNumber??"—"), customerName:String(x?.customerName??"—"),
    restaurantName:String(x?.restaurantName??"—"), orderNumber:String(x?.orderNumber??"—"), amount:Number(x?.amount??0)||0,
    paymentStatus:(x?.paymentStatus??"UNPAID") as DiniTicketRecord["paymentStatus"], queueStatus:(x?.queueStatus??x?.status??"WAITING") as DiniTicketStatus,
    arrivalStatus:(x?.arrivalStatus??"NOT_STARTED") as DiniTicketRecord["arrivalStatus"], createdAt:String(x?.createdAt??new Date().toISOString()),
    waitMinutes:Number(x?.waitMinutes??0)||0, customerId:x?.customerId?String(x.customerId):undefined, customerUserId:x?.customerUserId?String(x.customerUserId):undefined,
    customerPhone:x?.customerPhone?String(x.customerPhone):undefined, customerEmail:x?.customerEmail?String(x.customerEmail):undefined,
    restaurantId:x?.restaurantId?String(x.restaurantId):undefined, restaurantLogoUrl:x?.restaurantLogoUrl?String(x.restaurantLogoUrl):undefined,
    restaurantCoverImageUrl:x?.restaurantCoverImageUrl?String(x.restaurantCoverImageUrl):undefined, restaurantLocation:x?.restaurantLocation?String(x.restaurantLocation):undefined,
    foodOrderId:x?.foodOrderId?String(x.foodOrderId):undefined, paymentId:x?.paymentId?String(x.paymentId):undefined,
    foodImageUrl:x?.foodImageUrl?String(x.foodImageUrl):undefined, itemsSummary:x?.itemsSummary?String(x.itemsSummary):undefined,
  };
}
export const diniTicketService = {
  list: async (statuses?: DiniTicketStatus[]) => {
    try { const r:any=await apiClient.get(`/admin/diniticket/orders${statuses?.length?`?statuses=${statuses.join(",")}`:""}`); return unwrapCollection<any>(r).map(normalize); } catch { return []; }
  },
};
