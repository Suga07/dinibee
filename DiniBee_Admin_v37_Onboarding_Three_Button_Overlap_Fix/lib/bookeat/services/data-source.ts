import type { PageResponse } from "@/lib/bookeat/api";

export const isDemoDataEnabled = false;

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

export function unwrapCollection<T>(payload: any): T[] {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  if (payload.data) {
    if (Array.isArray(payload.data)) return payload.data;
    if (payload.data.content && Array.isArray(payload.data.content)) return payload.data.content;
  }
  return payload.content || [];
}

export async function readCollection<T>(
  request: () => Promise<T[] | PageResponse<T>>,
  _demoValue?: () => T[],
): Promise<T[]> {
  try {
    const res = await request();
    return unwrapCollection(res);
  } catch (err) {
    console.warn("API collection request failed or empty backend:", err);
    return [];
  }
}

function zeroObject<T>(obj: T): T {
  if (obj === null || obj === undefined) return obj;
  if (typeof obj === "number") return 0 as unknown as T;
  if (typeof obj === "boolean") return false as unknown as T;
  if (typeof obj === "string") return "" as unknown as T;
  if (Array.isArray(obj)) return [] as unknown as T;
  if (typeof obj === "object") {
    const res: Record<string, any> = {};
    for (const key of Object.keys(obj as object)) {
      res[key] = zeroObject((obj as Record<string, any>)[key]);
    }
    return res as T;
  }
  return obj;
}

export function unwrapResource<T>(payload: any): T {
  if (!payload) return {} as T;
  if (payload.data !== undefined) return payload.data as T;
  return payload as T;
}

export async function readResource<T>(
  request: () => Promise<T>,
  demoValue?: () => T,
): Promise<T> {
  try {
    const res = await request();
    return unwrapResource<T>(res);
  } catch (err) {
    if (demoValue) {
      try {
        const val = demoValue();
        if (isDemoDataEnabled) return val;
        return zeroObject(val);
      } catch {}
    }
    return {} as T;
  }
}

export async function runMutation<T>(
  request: () => Promise<T>,
  demoMutation?: () => T,
): Promise<T> {
  try {
    return await request();
  } catch (err) {
    if (demoMutation && isDemoDataEnabled) return demoMutation();
    throw err;
  }
}
