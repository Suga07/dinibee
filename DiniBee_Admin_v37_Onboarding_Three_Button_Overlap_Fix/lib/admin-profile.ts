import { useEffect, useState } from "react";
import { fetchCurrentAdminUser } from "./admin-api";

export type AdminProfile = {
  name: string;
  email: string;
  photoUrl: string | null;
  role: string;
};

const PROFILE_KEY = "bookeat_admin_profile";
const PROFILE_EVENT = "bookeat:profile-updated";

const defaultProfile: AdminProfile = {
  name: "Super Admin",
  email: "admin@bookeat.com",
  photoUrl: null,
  role: "SUPER_ADMIN",
};

function readProfile(): AdminProfile {
  if (typeof window === "undefined") return defaultProfile;
  try {
    const saved = JSON.parse(localStorage.getItem(PROFILE_KEY) || "null") as Partial<AdminProfile> | null;
    const legacy = JSON.parse(localStorage.getItem("adminUser") || "null") as { name?: string; email?: string; role?: string } | null;
    if (saved && (saved.name || saved.email || saved.role)) {
      return {
        name: saved.name || defaultProfile.name,
        email: saved.email || defaultProfile.email,
        photoUrl: saved.photoUrl || null,
        role: saved.role || defaultProfile.role,
      };
    }
    if (legacy && (legacy.name || legacy.email || legacy.role)) {
      return {
        name: legacy.name || defaultProfile.name,
        email: legacy.email || defaultProfile.email,
        photoUrl: null,
        role: legacy.role || defaultProfile.role,
      };
    }
    return defaultProfile;
  } catch {
    return defaultProfile;
  }
}

export function saveAdminProfile(update: Partial<AdminProfile>) {
  if (typeof window === "undefined") return;
  const profile = { ...readProfile(), ...update };
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
  window.dispatchEvent(new Event(PROFILE_EVENT));
}

export function clearAdminProfile() {
  if (typeof window === "undefined") return;
  localStorage.removeItem(PROFILE_KEY);
  localStorage.removeItem("adminUser");
  localStorage.removeItem("adminToken");
  localStorage.removeItem("bookeat_admin_token");
  window.dispatchEvent(new Event(PROFILE_EVENT));
}

export function useAdminProfile() {
  const [profile, setProfile] = useState<AdminProfile>(defaultProfile);

  useEffect(() => {
    const refresh = () => setProfile(readProfile());
    refresh();

    if (typeof window !== "undefined" && (localStorage.getItem("adminToken") || localStorage.getItem("bookeat_admin_token"))) {
      fetchCurrentAdminUser().then((user) => {
        if (user) {
          saveAdminProfile(user);
        }
      });
    }

    window.addEventListener(PROFILE_EVENT, refresh);
    window.addEventListener("storage", refresh);
    return () => {
      window.removeEventListener(PROFILE_EVENT, refresh);
      window.removeEventListener("storage", refresh);
    };
  }, []);

  return profile;
}

export function profileInitials(name: string) {
  const initials = name.trim().split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join("");
  return initials.toUpperCase() || "SA";
}