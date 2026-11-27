import { apiClient } from "@/lib/bookeat/api";
import { unwrapCollection, unwrapResource } from "@/lib/bookeat/services/data-source";
import type { SupportMessage, SupportStatus, SupportTicketRecord, SupportUserType } from "@/lib/bookeat/types";

type BackendTicket = { id:string; version:number; ticketNumber:string; creatorType:SupportUserType; category:string; subject:string; description:string; priority:string; status:SupportStatus; assignedAdmin?:string|null; createdAt:string; resolvedAt?:string|null };
type BackendMessage = { id:string; fromSupport:boolean; message:string; attachmentUrl?:string|null; createdAt:string };
const key = () => `admin-support-${Date.now()}-${Math.random().toString(36).slice(2)}`;
const mapMessage = (m:BackendMessage, creator:SupportUserType):SupportMessage => ({ id:m.id, senderType:m.fromSupport ? "ADMIN" : creator, senderName:m.fromSupport ? "DiniBee Support" : (creator === "OWNER" ? "Restaurant Owner" : "Customer"), message:m.message, createdAt:m.createdAt, attachmentName:m.attachmentUrl || undefined });
const mapTicket = (t:BackendTicket, messages:SupportMessage[] = []):SupportTicketRecord => ({ id:t.id, version:t.version ?? 0, ticketNumber:t.ticketNumber, userType:t.creatorType, userName:t.creatorType === "OWNER" ? "Restaurant Owner" : "Customer", relatedEntityType:"OTHER", category:t.category, subject:t.subject, description:t.description, lastMessage:messages.at(-1)?.message || t.description || t.subject, status:t.status, assignedAdminName:t.assignedAdmin || "Unassigned", createdAt:t.createdAt, updatedAt:messages.at(-1)?.createdAt || t.createdAt, messages });

export const supportService = {
  async list(userType?:SupportUserType, status?:SupportStatus) {
    const payload:any = await apiClient.get(`/admin/support/tickets?page=0&size=100`);
    let rows = unwrapCollection<BackendTicket>(payload).map(t => mapTicket(t));
    if (userType) rows = rows.filter(x => x.userType === userType);
    if (status) rows = rows.filter(x => x.status === status);
    return rows;
  },
  async get(ticketId:string) {
    const raw:any = await apiClient.get(`/admin/support/tickets/${ticketId}`);
    const t = unwrapResource<BackendTicket>(raw);
    const msgRaw:any = await apiClient.get(`/admin/support/tickets/${ticketId}/messages?page=0&size=100`);
    const msgs = unwrapCollection<BackendMessage>(msgRaw).map(m => mapMessage(m, t.creatorType));
    return mapTicket(t, msgs);
  },
  async sendMessage(ticketId:string, message:string) {
    const raw:any = await apiClient.postWithHeaders(`/admin/support/tickets/${ticketId}/messages`, {message}, {"Idempotency-Key":key()});
    const m = unwrapResource<BackendMessage>(raw);
    return mapMessage(m, "CUSTOMER");
  },
  async updateStatus(ticketId:string, status:SupportStatus, version:number) {
    const raw:any = await apiClient.put(`/admin/support/tickets/${ticketId}/status`, {version, status, reason:"Updated from admin live support"});
    return mapTicket(unwrapResource<BackendTicket>(raw));
  },
};
