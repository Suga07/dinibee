import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection } from "@/lib/bookeat/services/data-source";
import type { FoodOrderRecord } from "@/lib/bookeat/types";

export const orderService = {
  listFoodOrders: () =>
    readCollection(
      () => apiClient.get<FoodOrderRecord[]>("/admin/food-orders"),
      () => demoStore.foodOrders,
    ),
};

