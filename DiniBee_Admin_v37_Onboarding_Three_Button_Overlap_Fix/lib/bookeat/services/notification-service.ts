import { apiClient } from "@/lib/bookeat/api";
import { unwrapCollection } from "@/lib/bookeat/services/data-source";
import type { NotificationAudience, NotificationRecord, NotificationType } from "@/lib/bookeat/types";

export type SendNotificationInput = {
  title: string;
  message: string;
  type: NotificationType;
  audience: NotificationAudience;
  recipientIds?: string[];
  scheduledAt?: string;
};

const BROADCAST_STORAGE_KEY = "bookeat_admin_broadcasts";

function getLocalBroadcasts(): NotificationRecord[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(BROADCAST_STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveLocalBroadcast(record: NotificationRecord) {
  if (typeof window === "undefined") return;
  try {
    const existing = getLocalBroadcasts();
    const updated = [record, ...existing.filter((item) => item.id !== record.id)];
    window.localStorage.setItem(BROADCAST_STORAGE_KEY, JSON.stringify(updated.slice(0, 50)));
  } catch {}
}

export const notificationService = {
  list: async (): Promise<NotificationRecord[]> => {
    try {
      const r:any = await apiClient.get("/admin/notification-broadcasts");
      return unwrapCollection<any>(r).map((x:any) => ({
        id:String(x.id), title:String(x.title ?? ""), message:String(x.message ?? ""), type:x.type,
        audience:(x.audience ?? "ALL_CUSTOMERS") as NotificationAudience, recipientCount:Number(x.recipientCount ?? 0) || 0,
        createdBy:String(x.createdBy ?? "Admin"), createdAt:String(x.createdAt ?? ""), status:"SENT" as const,
      }));
    } catch { return []; }
  },

  send: async (input: SendNotificationInput): Promise<NotificationRecord> => {
    const ids = input.recipientIds?.filter((v) => /^[0-9a-fA-F-]{36}$/.test(v));
    const r: any = await apiClient.post("/admin/notifications", {
      title: input.title,
      message: input.message,
      type: input.type,
      audience: input.audience,
      recipientIds: ids?.length ? ids : undefined,
    });
    const recipientCount = r?.data?.recipientCount ?? 0;
    const createdRecord: NotificationRecord = {
      id: `broadcast-${Date.now()}`,
      title: input.title,
      message: input.message,
      type: input.type,
      audience: input.audience,
      recipientCount: recipientCount,
      createdBy: "Super Admin",
      createdAt: new Date().toISOString(),
      scheduledAt: input.scheduledAt || undefined,
      status: "SENT",
    };
    saveLocalBroadcast(createdRecord);
    return createdRecord;
  },
};
