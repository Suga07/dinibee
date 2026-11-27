"use client";

import { PayBillTransactions } from "@/components/paybill/pay-bill-transactions";
import { PayBillOffers } from "@/components/paybill/pay-bill-offers";
import { getStoredToken } from "@/lib/bookeat/api";
import { PrivacyPolicyView } from "@/components/bookeat/privacy-policy-view";


import { useEffect, useMemo, useRef, useState } from "react";
import {
  Activity,
  BadgePercent,
  BarChart3,
  Bell,
  Building2,
  CalendarCheck2,
  CheckCircle2,
  ChefHat,
  ChevronDown,
  CircleDollarSign,
  CreditCard,
  FileCheck2,
  FileClock,
  FileText,
  Gift,
  HandCoins,
  Headphones,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquare,
  Percent,
  QrCode,
  ReceiptIndianRupee,
  Search,
  Settings,
  ShieldAlert,
  ShieldCheck,
  ShoppingBag,
  Star,
  Store,
  TicketCheck,
  TicketPercent,
  UtensilsCrossed,
  UserCog,
  Users,
  WalletCards,
  X,
  type LucideIcon,
} from "lucide-react";
import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
} from "@/lib/bookeat/router";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { Toaster } from "@/components/ui/sonner";
import { clearAdminProfile, profileInitials, useAdminProfile } from "@/lib/admin-profile";
import {
  AuthPage,
  AdminUsersPage,
  CatalogListPage,
  CommissionPage,
  LiveOperationsPage,
  RolesPage,
  SettingsPage,
  VerificationPage,
} from "@/components/bookeat/pages";
import {
  PlatformDashboardPage,
  ReservationsManagementPage,
  RestaurantsManagementPage,
  ServiceManagementPage,
} from "@/components/bookeat/ecosystem/core-pages";
import {
  DiniTicketManagementPage,
  FoodOrdersManagementPage,
  TakeawayManagementPage,
} from "@/components/bookeat/ecosystem/operations-pages";
import {
  CorporateInvoicesPage,
  PaymentsManagementPage,
  SettlementsManagementPage,
  WalletManagementPage,
} from "@/components/bookeat/ecosystem/finance-pages";
import {
  EcosystemAnalyticsPage,
  ReviewsManagementPage,
  SupportCenterPage,
} from "@/components/bookeat/ecosystem/community-pages";
import { MembershipPlansPage } from "@/components/bookeat/membership-plans-page";
import {
  NotificationsManagementPage,
  RefundsManagementPage,
} from "@/components/bookeat/ecosystem/platform-pages";
import {
  CustomersV17,
  DiniTicketV17,
  RestaurantOnboardingV17,
  RestaurantsV17,
  ReservationsV17,
  TakeawayV17,
  TransactionsV17,
} from "@/components/bookeat/ecosystem/v17-pages";

type NavItem = { label: string; path: string; icon: LucideIcon };

const navigation: Array<{ label?: string; items: NavItem[] }> = [
  {
    items: [
      { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
      { label: "Customers", path: "/customers", icon: Users },
    ],
  },
  {
    label: "Restaurants",
    items: [
      { label: "Restaurant Onboarding", path: "/restaurant-onboarding", icon: FileCheck2 },
      { label: "Restaurants", path: "/restaurants", icon: Store },
    ],
  },
  {
    label: "Reservations",
    items: [{ label: "Table Bookings", path: "/reservations", icon: CalendarCheck2 }],
  },
  {
    label: "Operations",
    items: [
      { label: "Takeaway Orders", path: "/takeaway-orders", icon: ShoppingBag },
      { label: "DiniTicket Orders", path: "/diniticket", icon: TicketCheck },
    ],
  },
  {
    label: "Payments",
    items: [
      { label: "Transactions", path: "/transactions", icon: CreditCard },
      { label: "Restaurant Bill Payments", path: "/pay-bill-transactions", icon: ReceiptIndianRupee },
      { label: "Refunds", path: "/refunds", icon: ReceiptIndianRupee },
      { label: "Commission", path: "/commission", icon: Percent },
      { label: "Settlements", path: "/settlements", icon: HandCoins },
      { label: "Corporate Invoices", path: "/corporate-invoices", icon: FileText },
    ],
  },
  {
    label: "DiniBee Wallet",
    items: [
      { label: "Wallet Overview", path: "/wallet", icon: WalletCards },
      { label: "Reward Rules", path: "/wallet/rules", icon: Gift },
      { label: "Transactions", path: "/wallet/transactions", icon: CircleDollarSign },
      { label: "Redemptions", path: "/wallet/redemptions", icon: TicketPercent },
      { label: "Adjustments", path: "/wallet/adjustments", icon: Settings },
    ],
  },
  {
    label: "Community",
    items: [
      { label: "Reviews & Ratings", path: "/reviews", icon: Star },
      { label: "Notifications", path: "/notifications", icon: Bell },
    ],
  },
  {
    label: "Help & Support",
    items: [
      { label: "Help & Support", path: "/support-tickets", icon: Headphones },
    ],
  },
  {
    label: "Marketing",
    items: [
      { label: "Platform Offers", path: "/platform-offers", icon: BadgePercent },
      { label: "Pay Bill Cashback & Bank Offers", path: "/pay-bill-offers", icon: Gift },
      { label: "Coupons", path: "/coupons", icon: TicketPercent },
      { label: "Membership Plans", path: "/membership-plans", icon: Gift },
    ],
  },
  { label: "Reports", items: [{ label: "Analytics & Reports", path: "/analytics", icon: BarChart3 }] },
  {
    label: "Legal & Policy",
    items: [
      { label: "Privacy Policy", path: "/privacy-policy", icon: FileText },
    ],
  },
  {
    label: "System",
    items: [
      { label: "Admin Users", path: "/admin-users", icon: UserCog },
      { label: "Roles & Permissions", path: "/roles", icon: ShieldCheck },
      { label: "Settings", path: "/settings", icon: Settings },
    ],
  },
];

const flatNavigation = navigation.flatMap((group) => group.items);

function Brand() {
  return (
    <div className="bookeat-brand dinibee-brand">
      <span className="brand-symbol dinibee-mark"><img src="/dinibee-mark.png" alt="DiniBee" /></span>
      <span className="brand-wordmark"><strong>Dini<span>Bee</span></strong><small>ADMIN PANEL</small></span>
    </div>
  );
}

function AdminSidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const profile = useAdminProfile();

  return (
    <Sidebar collapsible="icon" className="bookeat-sidebar">
      <SidebarHeader className="brand-header"><Brand /></SidebarHeader>
      <SidebarContent className="sidebar-scroll scrollbar-thin">
        {navigation.map((group, index) => (
          <SidebarGroup key={group.label ?? `main-${index}`} className="bookeat-nav-group">
            {group.label ? <SidebarGroupLabel>{group.label}</SidebarGroupLabel> : null}
            <SidebarGroupContent>
              <SidebarMenu>
                {group.items.map((item) => {
                  const active = location.pathname === item.path;
                  const Icon = item.icon;
                  return (
                    <SidebarMenuItem key={item.path}>
                      <SidebarMenuButton
                        tooltip={item.label}
                        isActive={active}
                        className="bookeat-nav-item"
                        onClick={() => navigate(item.path)}
                      >
                        <Icon />
                        <span>{item.label}</span>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  );
                })}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>
      <SidebarFooter className="sidebar-profile-wrap">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="sidebar-profile">
              <span className="admin-avatar">
                {profile.photoUrl ? <img src={profile.photoUrl} alt="" /> : profileInitials(profile.name)}<i />
              </span>
              <span><strong>{profile.name}</strong><small>{profile.role}</small></span>
              <ChevronDown />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent side="right" align="end" className="profile-dropdown">
            <DropdownMenuLabel>Admin account</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate("/settings")}><Settings />Account settings</DropdownMenuItem>
            <DropdownMenuItem onClick={() => { clearAdminProfile(); navigate("/login"); }} className="danger-menu-item"><LogOut />Sign out</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}

function GlobalSearch() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);

  useEffect(() => {
    function keydown(event: KeyboardEvent) {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        inputRef.current?.focus();
        setOpen(true);
      }
      if (event.key === "Escape") setOpen(false);
    }
    window.addEventListener("keydown", keydown);
    return () => window.removeEventListener("keydown", keydown);
  }, []);

  const results = useMemo(() => {
    if (!query.trim()) return flatNavigation.slice(0, 5);
    return flatNavigation.filter((item) => item.label.toLowerCase().includes(query.toLowerCase())).slice(0, 7);
  }, [query]);

  function go(path: string) {
    navigate(path);
    setQuery("");
    setOpen(false);
  }

  return (
    <div className="global-search">
      <Search />
      <Input ref={inputRef} value={query} onChange={(event) => { setQuery(event.target.value); setOpen(true); }} onFocus={() => setOpen(true)} placeholder="Search pages, restaurants, bookings…" aria-label="Search the admin panel" />
      <kbd>Ctrl K</kbd>
      {open ? (
        <div className="search-results">
          <div><span>Quick navigation</span><button onClick={() => setOpen(false)} aria-label="Close search"><X /></button></div>
          {results.length ? results.map((item) => { const Icon = item.icon; return <button key={item.path} onMouseDown={(event) => event.preventDefault()} onClick={() => go(item.path)}><span><Icon /></span><strong>{item.label}</strong><small>Open page</small></button>; }) : <p>No matching page found.</p>}
        </div>
      ) : null}
    </div>
  );
}

function Topbar() {
  const navigate = useNavigate();
  const profile = useAdminProfile();
  return (
    <header className="bookeat-topbar">
      <div className="topbar-left"><SidebarTrigger className="sidebar-trigger"><Menu /></SidebarTrigger><GlobalSearch /></div>
      <div className="topbar-actions">
        <Button variant="ghost" size="icon" className="header-icon-button" aria-label="Messages" onClick={() => navigate("/support-tickets")}><MessageSquare /></Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild><Button variant="ghost" size="icon" className="header-icon-button" aria-label="Notifications"><Bell /></Button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="notification-dropdown">
            <DropdownMenuLabel className="notification-title"><span><strong>Notifications</strong><small>Operational shortcuts</small></span><button onClick={() => navigate("/notifications")}>View all</button></DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate("/restaurants/pending")}><span className="dropdown-event-icon gold"><Store /></span><span><strong>Partner approvals</strong><small>Review pending restaurant applications</small></span></DropdownMenuItem>
            <DropdownMenuItem onClick={() => navigate("/payments")}><span className="dropdown-event-icon red"><CreditCard /></span><span><strong>Payment monitoring</strong><small>Review all transaction states</small></span></DropdownMenuItem>
            <DropdownMenuItem onClick={() => navigate("/support-tickets")}><span className="dropdown-event-icon blue"><Headphones /></span><span><strong>Support center</strong><small>Open the unified ticket queue</small></span></DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
        <span className="topbar-divider" />
        <DropdownMenu>
          <DropdownMenuTrigger asChild><button className="topbar-profile"><span className="admin-avatar">{profile.photoUrl ? <img src={profile.photoUrl} alt="" /> : profileInitials(profile.name)}<i /></span><span><strong>{profile.name}</strong><small>{profile.role}</small></span><ChevronDown /></button></DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="profile-dropdown">
            <DropdownMenuLabel>{profile.name}<br /><small>{profile.email}</small></DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => navigate("/settings")}><Settings />Settings</DropdownMenuItem>
            <DropdownMenuItem onClick={() => navigate("/roles")}><ShieldCheck />Role & permissions</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => { clearAdminProfile(); navigate("/login"); }} className="danger-menu-item"><LogOut />Sign out</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}

function NotFoundPage() {
  const navigate = useNavigate();
  return <div className="not-found panel-card"><span><Search /></span><h1>Page not found</h1><p>The admin page you requested does not exist or your role cannot access it.</p><Button className="gold-button" onClick={() => navigate("/dashboard")}>Return to dashboard</Button></div>;
}

function AdminRoutes() {
  const navigate = useNavigate();
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="/dashboard" element={<PlatformDashboardPage />} />

      <Route path="/restaurant-onboarding" element={<RestaurantOnboardingV17 />} />
      <Route path="/restaurants" element={<RestaurantsV17 />} />
      <Route path="/restaurants/pending" element={<Navigate to="/restaurant-onboarding" replace />} />
      <Route path="/restaurants/approved" element={<Navigate to="/restaurants" replace />} />
      <Route path="/restaurants/rejected" element={<Navigate to="/restaurant-onboarding" replace />} />
      <Route path="/restaurants/services" element={<Navigate to="/restaurants" replace />} />
      <Route path="/partner-applications" element={<Navigate to="/restaurant-onboarding" replace />} />
      <Route path="/approved-restaurants" element={<Navigate to="/restaurants" replace />} />
      <Route path="/suspended-restaurants" element={<Navigate to="/restaurant-onboarding" replace />} />
      <Route path="/restaurant-verification" element={<Navigate to="/restaurant-onboarding" replace />} />

      <Route path="/users" element={<Navigate to="/customers" replace />} />
      <Route path="/customers" element={<CustomersV17 />} />
      <Route path="/reservations" element={<ReservationsV17 />} />
      <Route path="/reservations/regular" element={<Navigate to="/reservations" replace />} />
      <Route path="/reservations/group" element={<Navigate to="/reservations" replace />} />
      <Route path="/reservations/corporate" element={<Navigate to="/reservations" replace />} />
      <Route path="/qr-check-in" element={<Navigate to="/reservations" replace />} />
      <Route path="/food-orders" element={<Navigate to="/takeaway-orders" replace />} />
      <Route path="/live-operations" element={<Navigate to="/dashboard" replace />} />

      <Route path="/takeaway-orders" element={<TakeawayV17 />} />
      <Route path="/takeaway-orders/regular" element={<Navigate to="/takeaway-orders" replace />} />
      <Route path="/takeaway-orders/on-the-way" element={<Navigate to="/takeaway-orders" replace />} />
      <Route path="/diniticket" element={<DiniTicketV17 />} />
      <Route path="/diniticket/live" element={<Navigate to="/diniticket" replace />} />
      <Route path="/diniticket/orders" element={<Navigate to="/diniticket" replace />} />
      <Route path="/diniticket/completed" element={<Navigate to="/diniticket" replace />} />
      <Route path="/diniticket/exceptions" element={<Navigate to="/diniticket" replace />} />

      <Route path="/payments" element={<Navigate to="/transactions" replace />} />
      <Route path="/transactions" element={<TransactionsV17 />} />
      <Route path="/pay-bill-transactions" element={<div className="space-y-5 p-4"><h1 className="text-2xl font-bold">Restaurant Bill Payments</h1><p className="text-sm text-muted-foreground">Review full and split payments, payment history, and approval status.</p><PayBillTransactions token={getStoredToken() || ""} /></div>} />
      <Route path="/pay-bill-offers" element={<div className="space-y-5 p-4"><h1 className="text-2xl font-bold">Cashback & Bank Offers</h1><PayBillOffers token={getStoredToken() || ""} /></div>} />
      <Route path="/settlements" element={<SettlementsManagementPage />} />
      <Route path="/payouts" element={<Navigate to="/settlements" replace />} />
      <Route path="/refunds" element={<RefundsManagementPage />} />
      <Route path="/commission" element={<CommissionPage />} />
      <Route path="/corporate-invoices" element={<CorporateInvoicesPage />} />
      <Route path="/wallet" element={<WalletManagementPage />} />
      <Route path="/wallet/rules" element={<WalletManagementPage view="rules" />} />
      <Route path="/wallet/transactions" element={<WalletManagementPage view="transactions" />} />
      <Route path="/wallet/redemptions" element={<WalletManagementPage view="redemptions" />} />
      <Route path="/wallet/adjustments" element={<WalletManagementPage view="adjustments" />} />

      <Route path="/reviews" element={<ReviewsManagementPage />} />
      <Route path="/support-tickets" element={<SupportCenterPage />} />
      <Route path="/support/customers" element={<Navigate to="/support-tickets" replace />} />
      <Route path="/support/owners" element={<Navigate to="/support-tickets" replace />} />
      <Route path="/support/open" element={<Navigate to="/support-tickets" replace />} />
      <Route path="/support/in-progress" element={<Navigate to="/support-tickets" replace />} />
      <Route path="/support/resolved" element={<Navigate to="/support-tickets" replace />} />
      <Route path="/support/closed" element={<Navigate to="/support-tickets" replace />} />

      <Route path="/platform-offers" element={<CatalogListPage route="/platform-offers" />} />
      <Route path="/coupons" element={<CatalogListPage route="/coupons" />} />
      <Route path="/notifications" element={<NotificationsManagementPage />} />
      <Route path="/membership-plans" element={<MembershipPlansPage />} />
      <Route path="/analytics" element={<EcosystemAnalyticsPage />} />
      <Route path="/admin-users" element={<AdminUsersPage />} />
      <Route path="/roles" element={<RolesPage />} />
      <Route path="/privacy-policy" element={<PrivacyPolicyView inAdminLayout={true} />} />
      <Route path="/privacy" element={<PrivacyPolicyView inAdminLayout={true} />} />
      <Route path="/settings" element={<SettingsPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

function RoutedApp() {
  const location = useLocation();
  const navigate = useNavigate();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const authMode = location.pathname === "/login"
    ? "login"
    : location.pathname === "/forgot-password"
      ? "forgot"
      : location.pathname === "/reset-password"
        ? "reset"
        : null;

  const isAuthenticated = mounted && typeof window !== "undefined" && Boolean(localStorage.getItem("adminToken"));

  if (!mounted) {
    if (authMode) {
      return <AuthPage mode={authMode} navigate={navigate} />;
    }
    return <AuthPage mode="login" navigate={navigate} />;
  }

  if (isAuthenticated && authMode === "login") {
    return <Navigate to="/dashboard" replace />;
  }

  if (authMode) return <AuthPage mode={authMode} navigate={navigate} />;

  const isPublicLegal = location.pathname === "/privacy-policy" || location.pathname === "/privacy";
  if (!isAuthenticated && isPublicLegal) {
    return <PrivacyPolicyView inAdminLayout={false} />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <SidebarProvider defaultOpen>
      <AdminSidebar />
      <SidebarInset className="admin-shell">
        <Topbar />
        <main className="admin-content"><AdminRoutes /></main>
      </SidebarInset>
    </SidebarProvider>
  );
}

export function BookEatAdminPanel() {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  return (
    <BrowserRouter>
      <RoutedApp />
      <Toaster position="top-right" richColors closeButton />
    </BrowserRouter>
  );
}
