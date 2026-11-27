import { apiClient } from "@/lib/bookeat/api";
import type { DashboardSnapshot } from "@/lib/bookeat/types";

const n=(v:any)=>Number(v??0);
export const dashboardService={
 async getOverview():Promise<DashboardSnapshot>{
  const raw:any=await apiClient.get("/admin/dashboard"); const d=raw?.data??raw??{};
  return {
   restaurants:{total:n(d.totalRestaurants),pending:n(d.pendingRestaurants),approved:n(d.approvedRestaurants)}, customers:n(d.totalCustomers),
   reservations:{today:n(d.todayReservations),regular:n(d.todayRegularReservations),group:n(d.todayGroupReservations),corporate:n(d.todayCorporateReservations)},
   dining:{active:n(d.activeDining),checkedIn:n(d.checkedInReservations),dining:n(d.diningReservations),occupiedTables:n(d.occupiedTables)},
   takeaway:{total:n(d.activeTakeaways),onTheWay:n(d.onTheWayTakeaways)}, diniTicket:{orders:n(d.activeDiniTickets),waiting:n(d.waitingDiniTickets)},
   finance:{revenue:n(d.totalRevenue),commission:n(d.platformCommission),pendingSettlements:n(d.pendingSettlements)},
   support:{customerOpen:n(d.openCustomerSupportTickets),ownerOpen:n(d.openOwnerSupportTickets)}, wallet:{issued:n(d.pointsIssued),redeemed:n(d.pointsRedeemed)},
   recentActivity:Array.isArray(d.recentActivity)?d.recentActivity:[], serviceVolume:Array.isArray(d.serviceVolume)?d.serviceVolume:[],
   revenueTrend:Array.isArray(d.revenueTrend)?d.revenueTrend.map((x:any)=>({day:String(x.day??''),gross:n(x.net_sales??x.gross),commission:n(x.commission)})):[]
  };
 }
};
