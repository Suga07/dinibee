import { apiClient, downloadApiFile } from "@/lib/bookeat/api";
import { readResource } from "@/lib/bookeat/services/data-source";
import type { DashboardSnapshot } from "@/lib/bookeat/types";
export type ReportFilters={period:"TODAY"|"WEEK"|"MONTH"|"CUSTOM";restaurantId?:string;serviceType?:string;bookingType?:string;from?:string;to?:string};
function ymd(d:Date){return d.toISOString().slice(0,10)}
function dates(f:ReportFilters){const now=new Date(); const to=f.to||ymd(now); if(f.from)return {fromDate:f.from,toDate:to}; const d=new Date(now); if(f.period==='WEEK')d.setDate(d.getDate()-6); else if(f.period==='MONTH')d.setDate(d.getDate()-29); return {fromDate:f.period==='TODAY'?to:ymd(d),toDate:to};}
export const reportService={
  getOverview:(_filters:ReportFilters)=>readResource(()=>apiClient.get<DashboardSnapshot>("/admin/dashboard")),
  exportPdf:async(reportType:string,filters:ReportFilters)=>{const q=new URLSearchParams(dates(filters)); if(filters.serviceType&&filters.serviceType!=='ALL')q.set('serviceType',filters.serviceType); await downloadApiFile(`/admin/reports/${reportType}/export/pdf?${q}`,`bookeat-${reportType.toLowerCase()}-report.pdf`);}
};
