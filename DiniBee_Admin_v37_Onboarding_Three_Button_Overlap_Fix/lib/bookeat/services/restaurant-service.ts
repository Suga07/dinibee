import { normalizeRestaurantDetail } from "./restaurant-detail.mjs";
import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection, runMutation, unwrapResource } from "@/lib/bookeat/services/data-source";
import type { BusinessService, RestaurantApprovalStatus, RestaurantRecord } from "@/lib/bookeat/types";

export function normalizeBusinessService(value: string): BusinessService {
  return value === "QUEUEBEE" ? "DINITICKET" : value as BusinessService;
}

export const restaurantService = {
  list: (status?: RestaurantApprovalStatus) =>
    readCollection(
      () => {
        if (status === "PENDING") {
          return apiClient.get<RestaurantRecord[]>("/admin/restaurants/pending");
        }
        return apiClient.get<RestaurantRecord[]>(`/admin/restaurants${status ? `?approvalStatus=${status}` : ""}`);
      },
      () => demoStore.restaurants.filter((item) => !status || item.approvalStatus === status),
    ),

  /**
   * Fetch the full Detail DTO for a single restaurant.
   * The list endpoint only returns Summary (name, status, owner, phone, email).
   * This endpoint returns location, business info, settlement profile, etc.
   */
  getDetail: async (id: string): Promise<any> => {
    const res = await apiClient.get<any>(`/admin/restaurants/${id}`);
    return normalizeRestaurantDetail(unwrapResource(res));
  },

  updateApproval: (id: string, status: string, note: string, version: number = 0, requestedServices: BusinessService[] = []) =>
    runMutation(
      async () => {
        if (status === "APPROVED") {
          // 1. Move to UNDER_REVIEW if it's not already
          let currentVersion = version;
          try {
            const reviewRes = await apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/under-review`, { version: currentVersion });
            if (reviewRes.data?.version) currentVersion = reviewRes.data.version;
          } catch (e: any) {
             // It might already be UNDER_REVIEW, ignore the 409
             if (e?.response?.status !== 409) throw e;
          }

          // 2. Verify Settlement Profile
          try {
             const settlementRes = await apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/settlement-profile/verification`, { 
               version: currentVersion, 
               verified: true,
               reason: "Auto-verified for approval"
             });
             if (settlementRes.data?.version) currentVersion = settlementRes.data.version;
          } catch (e) {
             console.error("Settlement verification failed", e);
          }

          // 3. Verify Documents
          try {
            const docsRes = await apiClient.get<any[]>(`/admin/restaurants/${id}/documents`);
            const documents = docsRes.data || [];
            for (const doc of documents) {
              const vRes = await apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/documents/${doc.id}/verification`, { 
                version: currentVersion, 
                verified: true 
              });
              if (vRes.data?.version) currentVersion = vRes.data.version;
            }
          } catch (e) {
            console.error("Document verification failed", e);
          }

          // 4. Finally Approve
          const latestRes = await apiClient.get<any>(`/admin/restaurants/${id}`);
          const actualVersion = latestRes.data?.version ?? currentVersion;
          const mappedServices = requestedServices.map(s => s === "QUEUEBEE" ? "DINITICKET" : s);
          return apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/approve`, {
            version: actualVersion,
            services: mappedServices,
            reason: note || "Approved by Admin",
          });
        }
        if (status === "REJECTED") {
          return apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/reject`, {
            version,
            reason: note || "Application rejected by Admin",
          });
        }
        return apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/under-review`, {
          version,
        });
      },
      () => {
        const restaurant = demoStore.restaurants.find((item) => item.id === id);
        if (!restaurant) throw new Error("Restaurant not found");
        restaurant.approvalStatus = status as any;
        restaurant.approvalHistory.unshift({ status: status as any, date: "19 Sep 2026", admin: "Vazh Muni", note });
        return restaurant;
      },
    ),

  updateServices: async (id: string, services: BusinessService[], version: number = 0) =>
    runMutation(
      async () => {
        const validServices: BusinessService[] = ["RESTAURANT", "TAKEAWAY", "DINITICKET"];
        let lastResult: any = null;
        for (const s of validServices) {
          const enabled = services.includes(s);
          lastResult = await apiClient.patch<RestaurantRecord>(`/admin/restaurants/${id}/services/${s}`, {
            version,
            enabled,
            reason: "Service access updated by admin",
          });
        }
        return lastResult;
      },
      () => {
        const restaurant = demoStore.restaurants.find((item) => item.id === id);
        if (!restaurant) throw new Error("Restaurant not found");
        restaurant.services = services.map(normalizeBusinessService);
        return restaurant;
      },
    ),

  setSuspended: (id: string, suspended: boolean, reason: string, version: number = 0) =>
    runMutation(
      () => {
        if (suspended) {
          return apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/suspend`, {
            version,
            reason: reason || "Suspended by platform administrator",
          });
        }
        return apiClient.put<RestaurantRecord>(`/admin/restaurants/${id}/reactivate`, {
          version,
          reason: reason || "Reactivated by platform administrator",
        });
      },
      () => {
        const restaurant = demoStore.restaurants.find((item) => item.id === id);
        if (!restaurant) throw new Error("Restaurant not found");
        restaurant.suspended = suspended;
        return restaurant;
      },
    ),
};
