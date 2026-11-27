export type AdminRole =
  | "SUPER_ADMIN"
  | "RESTAURANT_VERIFIER"
  | "FINANCE_ADMIN"
  | "SUPPORT_ADMIN"
  | "OPERATIONS_ADMIN";

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ApiError = {
  status: number;
  message: string;
  fieldErrors?: Record<string, string>;
  payload?: unknown;
};

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "https://api.dinibee.tech/api/v1";

const TOKEN_STORAGE_KEY = "bookeat_admin_token";

/* =========================================================
   TOKEN STORAGE
   ========================================================= */

function cleanToken(token: string | null): string | null {
  if (!token) return null;

  const cleaned = token
    .trim()
    .replace(/^Bearer\s+/i, "")
    .replace(/^"|"$/g, "")
    .trim();

  return cleaned.length > 0 ? cleaned : null;
}

export function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;

  return cleanToken(
    window.localStorage.getItem(TOKEN_STORAGE_KEY) ||
    window.localStorage.getItem("adminToken")
  );
}

export function setStoredToken(token: string) {
  if (typeof window === "undefined") return;

  const cleaned = cleanToken(token);

  if (!cleaned) {
    throw new Error("Cannot store an empty admin token.");
  }

  window.localStorage.setItem(
    TOKEN_STORAGE_KEY,
    cleaned
  );

  // Remove old legacy key so a stale token cannot be reused.
  window.localStorage.removeItem("adminToken");
}

export function clearStoredToken() {
  if (typeof window === "undefined") return;

  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  window.localStorage.removeItem("adminToken");
}

/* =========================================================
   REQUEST
   ========================================================= */

async function request<T>(
  path: string,
  init: RequestInit = {}
): Promise<T> {
  const token = getStoredToken();

  const headers = new Headers(init.headers);

  headers.set("Accept", "application/json");

  if (init.body && !headers.has("Content-Type")) {
    headers.set(
      "Content-Type",
      "application/json"
    );
  }

  if (token) {
    headers.set(
      "Authorization",
      `Bearer ${token}`
    );
  }

  const response = await fetch(
    `${API_BASE_URL}${path}`,
    {
      ...init,
      headers,
    }
  );

  let payload: any = null;

  if (response.status !== 204) {
    payload = await response
      .json()
      .catch(() => null);
  }

  if (!response.ok) {
    if (
      response.status === 401 &&
      typeof window !== "undefined"
    ) {
      clearStoredToken();
      window.localStorage.removeItem('adminUser');
      window.localStorage.removeItem('bookeat_admin_profile');

      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }

      window.dispatchEvent(
        new CustomEvent("bookeat:unauthorized")
      );
    }

    throw {
      status: response.status,

      message:
        payload?.message ??
        payload?.error ??
        `Request failed with status ${response.status}.`,

      fieldErrors:
        payload?.fieldErrors ??
        payload?.errors,

      payload,
    } satisfies ApiError;
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return payload as T;
}

/* =========================================================
   HTTP CLIENT
   ========================================================= */

export const apiClient = {
  get: <T>(path: string) =>
    request<T>(path),

  post: <T>(
    path: string,
    body?: unknown
  ) =>
    request<T>(path, {
      method: "POST",
      body:
        body === undefined
          ? undefined
          : JSON.stringify(body),
    }),

  postWithHeaders: <T>(path: string, body?: unknown, headers?: Record<string, string>) =>
    request<T>(path, {
      method: "POST",
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    }),

  put: <T>(
    path: string,
    body?: unknown
  ) =>
    request<T>(path, {
      method: "PUT",
      body:
        body === undefined
          ? undefined
          : JSON.stringify(body),
    }),

  patch: <T>(
    path: string,
    body?: unknown
  ) =>
    request<T>(path, {
      method: "PATCH",
      body:
        body === undefined
          ? undefined
          : JSON.stringify(body),
    }),

  delete: <T>(path: string) =>
    request<T>(path, {
      method: "DELETE",
    }),
};

/* =========================================================
   ADMIN API
   ========================================================= */

export const adminApi = {
  auth: {
    login: (
      email: string,
      password: string
    ) =>
      apiClient.post<{
        token: string;
        role: AdminRole;
      }>(
        "/admin/auth/login",
        {
          email,
          password,
        }
      ),

    forgotPassword: (email: string) =>
      apiClient.post<void>(
        "/admin/auth/forgot-password",
        { email }
      ),

    resetPassword: (
      token: string,
      password: string
    ) =>
      apiClient.post<void>(
        "/admin/auth/reset-password",
        {
          token,
          password,
        }
      ),
  },

  dashboard: () =>
    apiClient.get("/admin/dashboard"),

  partnerApplications: (
    query = ""
  ) =>
    apiClient.get(
      `/admin/restaurants/applications${query}`
    ),

  reviewApplication: (
    id: string,
    payload: unknown
  ) =>
    apiClient.patch(
      `/admin/restaurants/applications/${id}/review`,
      payload
    ),

  restaurants: (query = "") =>
    apiClient.get(
      `/admin/restaurants${query}`
    ),

  reservations: (query = "") =>
    apiClient.get(
      `/admin/reservations${query}`
    ),

  takeawayOrders: (query = "") =>
    apiClient.get(
      `/admin/takeaway-orders${query}`
    ),

  customers: (query = "") =>
    apiClient.get(
      `/admin/customers${query}`
    ),

  reviews: (query = "") =>
    apiClient.get(
      `/admin/reviews${query}`
    ),

  supportTickets: (query = "") =>
    apiClient.get(
      `/admin/support/tickets${query}`
    ),

  payments: (query = "") =>
    apiClient.get(
      `/admin/finance/payments${query}`
    ),

  payouts: (query = "") =>
    apiClient.get(
      `/admin/finance/payouts${query}`
    ),

  refunds: (query = "") =>
    apiClient.get(
      `/admin/finance/refunds${query}`
    ),

  commission: () =>
    apiClient.get(
      "/admin/finance/commission"
    ),

  offers: (query = "") =>
    apiClient.get(
      `/admin/offers${query}`
    ),

  createOffer: (payload: any) =>
    apiClient.post(
      "/admin/offers",
      payload
    ),

  coupons: (query = "") =>
    apiClient.get(
      `/admin/coupons${query}`
    ),

  createCoupon: (payload: any) =>
    apiClient.post(
      "/admin/coupons",
      payload
    ),

  disablePromotion: (id: string, reason = "Disabled from admin portal") =>
    apiClient.put(
      `/admin/promotions/${id}/disable`,
      { version: 0, reason }
    ),

  notifications: (query = "") =>
    apiClient.get(
      `/admin/notifications${query}`
    ),

  reports: (query = "") =>
    apiClient.get(
      `/admin/reports${query}`
    ),

  admins: (query = "") =>
    apiClient.get(
      `/admin/users${query}`
    ),

  roles: () =>
    apiClient.get("/admin/roles"),

  settings: () =>
    apiClient.get("/admin/settings"),
};
export async function downloadApiFile(path: string, filename: string): Promise<void> {
  const token = getStoredToken();
  const response = await fetch(`${API_BASE_URL}${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!response.ok) throw new Error(`Download failed (${response.status})`);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = filename; document.body.appendChild(a); a.click(); a.remove();
  URL.revokeObjectURL(url);
}

export async function getPublicPlatformStats(): Promise<{restaurantPartners:number;dailyOrders:number}> {
  const response = await fetch(`${API_BASE_URL}/public/platform-stats`, { headers: { Accept: 'application/json' } });
  if (!response.ok) return { restaurantPartners: 0, dailyOrders: 0 };
  const payload:any = await response.json(); const d = payload?.data ?? payload ?? {};
  return { restaurantPartners: Number(d.restaurantPartners ?? 0), dailyOrders: Number(d.dailyOrders ?? 0) };
}
