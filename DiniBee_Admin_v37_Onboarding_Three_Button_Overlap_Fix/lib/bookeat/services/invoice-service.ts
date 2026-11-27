import { apiClient } from "@/lib/bookeat/api";
import { demoStore } from "@/lib/bookeat/demo-data";
import { readCollection } from "@/lib/bookeat/services/data-source";
import type { CorporateInvoiceRecord } from "@/lib/bookeat/types";

export const invoiceService = {
  list: () => readCollection(() => apiClient.get<CorporateInvoiceRecord[]>("/admin/corporate-invoices"), () => demoStore.invoices),
};

