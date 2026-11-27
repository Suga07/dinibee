export interface AdminRestaurant {
  id: number;
  name: string;
  address: string;
  phone: string;
  ownerName: string;
  ownerEmail: string;
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  services: string[];
  todayBookingsCount: number;
  todaySalesAmount: number;
  platformCommission: number;
}

export interface PlatformAnalytics {
  totalGrossSales: number;
  platformCommission: number;
  totalBookingsToday: number;
  activeRestaurantsCount: number;
  activeSeatedTables: number;
  activeTakeaways: number;
  activeDiniTickets: number;
}

export interface AdminLoginResponse {
  token: string;
  email: string;
  role: string;
  name: string;
}

export interface AdminUserRecord {
  version: number;
  user: {
    id: string;
    firstName: string;
    lastName: string;
    email: string;
    status: string;
    roles?: string[];
    permissions?: string[];
    createdAt?: string;
  };
}

const API_ROOT = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'https://api.dinibee.tech/api/v1';
const BASE_URL = `${API_ROOT}/admin`;
const AUTH_URL = `${API_ROOT}/auth`;

export async function loginAdmin(email: string, password: string): Promise<AdminLoginResponse> {
  const res = await fetch(`${AUTH_URL}/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    if (res.status === 401 || res.status === 400 || res.status === 403) {
      throw new Error('Invalid email or password. Access denied.');
    }
    const errText = await res.text();
    throw new Error(`Authentication failed (${res.status}): ${errText}`);
  }

  const payload = (await res.json()) as Record<string, any>;
  const data = payload.data || payload;
  const token = (data.accessToken || data.token || data.jwtToken || payload.accessToken || payload.token || '') as string;
  const user = data.user || payload.user;
  const firstRole = user?.roles?.[0];
  const role = (typeof firstRole === "string" ? firstRole : firstRole?.code || data.role || payload.role || 'SUPER_ADMIN') as string;

  if (!token) {
    throw new Error('Authentication succeeded but server returned an empty access token.');
  }

  const name = [user?.firstName, user?.lastName].filter(Boolean).join(" ").trim() || email;

  return {
    token,
    email: user?.email || email,
    role,
    name,
  };
}

function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('adminToken') || localStorage.getItem('bookeat_admin_token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }
  return headers;
}

export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const headers = getAuthHeaders();
  options.headers = { ...headers, ...(options.headers || {}) };

  const res = await fetch(url, options);

  if (res.status === 401 && typeof window !== 'undefined') {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('bookeat_admin_token');
    localStorage.removeItem('adminUser');
    localStorage.removeItem('bookeat_admin_profile');
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login';
    }
  }

  return res;
}

export async function fetchCurrentAdminUser(): Promise<{ name: string; email: string; role: string } | null> {
  try {
    const res = await fetchWithAuth(`${AUTH_URL}/me`);
    if (!res.ok) return null;
    const payload = (await res.json()) as any;
    const user = payload.data || payload;
    if (!user || !user.email) return null;
    const firstRole = user.roles?.[0];
    const role = (typeof firstRole === 'string' ? firstRole : firstRole?.code || 'SUPER_ADMIN') as string;
    const name = [user.firstName, user.lastName].filter(Boolean).join(" ").trim() || user.email;
    return { name, email: user.email, role };
  } catch {
    return null;
  }
}

export async function fetchAllRestaurants(): Promise<AdminRestaurant[]> {
  const res = await fetchWithAuth(`${BASE_URL}/restaurants`);
  if (!res.ok) {
    if (res.status === 401 || res.status === 403) return [];
    throw new Error(`Failed to fetch restaurants: ${res.status}`);
  }
  const payload = (await res.json()) as any;
  const data = payload.data || payload;
  const rows:any[] = Array.isArray(data) ? data : (data.content || []);
  return rows.map((r:any)=>({ ...r, name:r.restaurantName ?? r.name, address:r.addressLine1 ?? r.city ?? r.address ?? '', ownerEmail:r.email ?? r.ownerEmail, services:r.enabledServices ?? r.requestedServices ?? r.services ?? [], suspended:r.status === 'SUSPENDED' })) as any;
}

export async function fetchAdminUsers(page = 0, size = 20): Promise<{
  content: AdminUserRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}> {
  const res = await fetchWithAuth(`${BASE_URL}/users?page=${page}&size=${size}`);
  if (!res.ok) throw new Error(`Failed to fetch administrators: ${res.status}`);
  const payload = (await res.json()) as any;
  const data = payload.data || payload;
  return {
    content: Array.isArray(data.content) ? data.content : [],
    page: data.page ?? page,
    size: data.size ?? size,
    totalElements: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 0,
  };
}

export async function updateAdminUserRoles(
  id: string,
  version: number,
  roles: string[],
  reason = 'Updated from admin users page',
) {
  const res = await fetchWithAuth(`${BASE_URL}/users/${id}/roles`, {
    method: 'PUT',
    body: JSON.stringify({ version, roles, reason }),
  });
  if (!res.ok) {
    const errorJson = (await res.json().catch(() => null)) as any;
    throw new Error(errorJson?.message || `Failed to update administrator roles (${res.status})`);
  }
  const payload = (await res.json()) as any;
  return payload.data || payload;
}

export async function updateApprovalStatus(
  restaurantId: number,
  status: 'APPROVED' | 'REJECTED'
): Promise<AdminRestaurant> {
  const res = await fetchWithAuth(`${BASE_URL}/restaurants/${restaurantId}/approval`, {
    method: 'PUT',
    body: JSON.stringify({ approvalStatus: status }),
  });

  if (!res.ok) {
    throw new Error(`Failed to update approval status for restaurant ${restaurantId}: ${res.status}`);
  }

  const payload = (await res.json()) as any;
  return payload.data || payload;
}

export async function fetchPlatformAnalytics(): Promise<PlatformAnalytics> {
  const res = await fetchWithAuth(`${BASE_URL}/dashboard/analytics`);
  if (!res.ok) {
    throw new Error(`Failed to fetch platform analytics: ${res.status}`);
  }
  const payload = (await res.json()) as any;
  return payload.data || payload;
}

export interface CreateAdminUserParams {
  name: string;
  email: string;
  password: string;
  role: string;
}

export async function createAdminUser(params: CreateAdminUserParams): Promise<any> {
  const nameParts = params.name.trim().split(' ');
  const firstName = nameParts[0] || params.name.trim();
  const lastName = nameParts.slice(1).join(' ') || firstName;

  const roleCodeMapping: Record<string, string> = {
    SUPER_ADMIN: 'SUPER_ADMIN',
    RESTAURANT_VERIFIER: 'RESTAURANT_VERIFICATION_ADMIN',
    FINANCE_ADMIN: 'FINANCE_ADMIN',
    SUPPORT_ADMIN: 'SUPPORT_ADMIN',
    OPERATIONS_ADMIN: 'OPERATIONS_ADMIN',
  };

  const backendRole = roleCodeMapping[params.role] || params.role || 'SUPER_ADMIN';

  const res = await fetchWithAuth(`${BASE_URL}/users`, {
    method: 'POST',
    body: JSON.stringify({
      firstName,
      lastName,
      email: params.email.trim(),
      password: params.password,
      roles: [backendRole],
    }),
  });

  if (!res.ok) {
    const errorJson = (await res.json().catch(() => null)) as any;
    const message = errorJson?.message || `Failed to create admin user (${res.status})`;
    throw new Error(message);
  }

  const payload = (await res.json()) as any;
  return payload.data || payload;
}

export async function fetchAdminProfile(): Promise<any> {
  const res = await fetchWithAuth(`${BASE_URL}/profile`);
  if (!res.ok) {
    throw new Error(`Failed to fetch admin profile: ${res.status}`);
  }
  const payload = (await res.json()) as any;
  return payload.data || payload;
}

export async function updateAdminProfile(params: { name: string; email: string; password?: string }): Promise<any> {
  const nameParts = params.name.trim().split(' ');
  const firstName = nameParts[0] || params.name.trim();
  const lastName = nameParts.slice(1).join(' ') || firstName;

  const bodyObj: Record<string, any> = {
    firstName,
    lastName,
    email: params.email.trim(),
  };

  if (params.password && params.password.trim() !== '') {
    bodyObj.password = params.password.trim();
  }

  const res = await fetchWithAuth(`${BASE_URL}/profile`, {
    method: 'PUT',
    body: JSON.stringify(bodyObj),
  });

  if (!res.ok) {
    const errorJson = (await res.json().catch(() => null)) as any;
    throw new Error(errorJson?.message || `Failed to update profile (${res.status})`);
  }

  if (typeof window !== 'undefined') {
    const current = JSON.parse(localStorage.getItem('adminUser') || '{}');
    localStorage.setItem('adminUser', JSON.stringify({ ...current, name: params.name, email: params.email }));
  }

  const payload = (await res.json()) as any;
  return payload.data || payload;
}
