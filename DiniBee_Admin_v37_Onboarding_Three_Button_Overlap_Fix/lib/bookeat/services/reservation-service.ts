import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection } from "@/lib/bookeat/services/data-source";
import type { BookingType, ReservationRecord } from "@/lib/bookeat/types";

export const reservationService = {
  list: (type?: BookingType) =>
    readCollection(
      () => apiClient.get<ReservationRecord[]>(`/admin/reservations${type ? `?bookingType=${type}` : ""}`),
      () => demoStore.reservations.filter((item) => !type || item.bookingType === type),
    ),
  history: async (id: string) => {
    const response = await apiClient.get<any>(`/admin/reservations/${id}/history`);
    const payload: any = response as any;
    return payload?.data?.data ?? payload?.data ?? payload ?? [];
  },
};

