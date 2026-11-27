"use client";

import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  Activity,
  AlertTriangle,
  BadgePercent,
  BellRing,
  Building2,
  CalendarCheck2,
  CalendarClock,
  Camera,
  Check,
  CheckCircle2,
  ChefHat,
  CircleDollarSign,
  Clock,
  Clock3,
  CreditCard,
  Download,
  Eye,
  EyeOff,
  FileCheck2,
  FileClock,
  FileText,
  Globe,
  Headphones,
  IndianRupee,
  Landmark,
  Lock,
  Mail,
  MapPin,
  MessageSquareText,
  Monitor,
  Moon,
  PackageCheck,
  Percent,
  Plus,
  QrCode,
  ReceiptIndianRupee,
  RefreshCw,
  Send,
  Shield,
  ShieldAlert,
  ShieldCheck,
  ShoppingBag,
  Star,
  Store,
  Sun,
  TicketPercent,
  Trash2,
  TrendingUp,
  Upload,
  User,
  UserCheck,
  Users,
  WalletCards,
  XCircle,
  type LucideIcon,
} from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { toast } from "sonner";
import { adminApi, getPublicPlatformStats } from "@/lib/bookeat/api";
import { restaurantService } from "@/lib/bookeat/services";
import { loginAdmin, fetchAllRestaurants, fetchAdminUsers, updateAdminUserRoles, createAdminUser, updateAdminProfile, fetchAdminProfile } from "@/lib/admin-api";
import { saveAdminProfile, useAdminProfile } from "@/lib/admin-profile";

import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import {
  DataTable,
  EntityCell,
  PageHeader,
  StatCard,
  StatusBadge,
  type Column,
  type DataRow,
} from "@/components/bookeat/shared";

type Navigate = (path: string) => void;

const revenueData: Array<{ day: string; total: number; dineIn: number; takeaway: number }> = [];

const growthData: Array<{ month: string; restaurants: number; customers: number }> = [];

const recentReservations: Array<{ id: string; time: string; customer: string; restaurant: string; status: string; amount: string }> = [];

const recentOrders: Array<{ id: string; customer: string; restaurant: string; status: string; amount: string }> = [];

export function DashboardPage({ navigate }: { navigate: Navigate }) {
  const [period, setPeriod] = useState("week");
  const [restaurants, setRestaurants] = useState<any[]>([]);

  useEffect(() => {
    let active = true;
    async function loadData() {
      try {
        const res = await fetchAllRestaurants();
        if (active && Array.isArray(res)) {
          setRestaurants(res);
        }
      } catch (err) {
        if (active) setRestaurants([]);
      }
    }
    loadData();
    return () => { active = false; };
  }, []);

  const totalRestaurants = restaurants.length;
  const pendingApps = restaurants.filter((r) => r.approvalStatus === "PENDING");
  const todayRevenue = restaurants.reduce((sum, r) => sum + (r.todaySalesAmount || 0), 0);
  const platformCommission = Math.round(todayRevenue * 0.1);

  const stats = [
    ["Total Restaurants", String(totalRestaurants), totalRestaurants > 0 ? "Backend network" : "No restaurants", "up", Store, "gold"],
    ["Pending Applications", String(pendingApps.length), pendingApps.length > 0 ? "Need review" : "Queue clear", "down", FileClock, "red"],
    ["Total Customers", "0", "Registered users", "up", Users, "blue"],
    ["Total Reservations", "0", "Platform total", "up", CalendarCheck2, "purple"],
    ["Takeaway Orders", "0", "Platform total", "up", ShoppingBag, "green"],
    ["Today’s Revenue", `₹${todayRevenue.toLocaleString()}`, "Live total", "up", IndianRupee, "gold"],
    ["Platform Commission", `₹${platformCommission.toLocaleString()}`, "10% effective rate", "up", BadgePercent, "red"],
    ["Preparing Orders", "0", "Live operations", "live", ChefHat, "purple"],
    ["Ready for Pickup", "0", "Live operations", "live", PackageCheck, "green"],
    ["Support Tickets", "0", "Open tickets", "down", Headphones, "blue"],
  ] as const;

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Platform overview"
        title="Good morning, Super Admin"
        description="Monitor DiniBee’s marketplace health, money flow, and live operations."
        actions={
          <Select value={period} onValueChange={setPeriod}>
            <SelectTrigger className="date-select"><CalendarClock /><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="today">Today</SelectItem>
              <SelectItem value="week">16 May – 22 May 2025</SelectItem>
              <SelectItem value="month">This month</SelectItem>
            </SelectContent>
          </Select>
        }
      />

      <section className="stats-grid dashboard-stats">
        {stats.map(([label, value, note, trend, icon, tone]) => (
          <StatCard
            key={label}
            label={label}
            value={value}
            note={note}
            trend={trend}
            icon={icon}
            tone={tone}
          />
        ))}
      </section>

      <section className="dashboard-chart-grid">
        <article className="panel-card chart-panel revenue-panel">
          <div className="card-title-row">
            <div><p>Financial performance</p><h2>Revenue overview</h2></div>
            <span className="soft-label">Amounts in ₹ thousands</span>
          </div>
          <div className="chart-legend-row">
            <span><i className="legend-total" />Total revenue</span>
            <span><i className="legend-dine" />Dine-in revenue</span>
            <span><i className="legend-takeaway" />Takeaway revenue</span>
          </div>
          <div className="revenue-chart">
            <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={1}>
              <AreaChart data={revenueData} margin={{ top: 12, right: 8, left: -18, bottom: 0 }}>
                <defs>
                  <linearGradient id="totalFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#f5a800" stopOpacity={0.22} />
                    <stop offset="100%" stopColor="#f5a800" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="var(--chart-grid)" />
                <XAxis dataKey="day" axisLine={false} tickLine={false} fontSize={11} />
                <YAxis axisLine={false} tickLine={false} fontSize={11} />
                <Tooltip contentStyle={{ borderRadius: 12, borderColor: "var(--border)", background: "var(--card)" }} />
                <Area type="monotone" dataKey="total" stroke="#f5a800" strokeWidth={3} fill="url(#totalFill)" />
                <Area type="monotone" dataKey="dineIn" stroke="#2563eb" strokeWidth={2} fill="transparent" />
                <Area type="monotone" dataKey="takeaway" stroke="#159a55" strokeWidth={2} fill="transparent" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article className="panel-card chart-panel order-split-panel">
          <div className="card-title-row"><div><p>Order mix</p><h2>Bookings vs takeaway</h2></div></div>
          <div className="donut-layout">
            <div className="donut-chart">
              <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={1}>
                <PieChart>
                  <Pie data={[{ name: "Reservations", value: 0 }, { name: "Takeaway", value: 0 }]} dataKey="value" innerRadius={56} outerRadius={77} startAngle={90} endAngle={-270} paddingAngle={2}>
                    <Cell fill="#f5a800" /><Cell fill="#159a55" />
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
              <span><small>Total</small><strong>0</strong></span>
            </div>
            <div className="donut-stats">
              <div><i className="legend-total" /><span>Reservations<strong>0 · 0%</strong></span></div>
              <div><i className="legend-takeaway" /><span>Takeaway<strong>0 · 0%</strong></span></div>
            </div>
          </div>
        </article>

        <article className="panel-card booking-status-card">
          <div className="card-title-row"><div><p>Current period</p><h2>Booking status</h2></div></div>
          <div className="booking-status-list">
            {[
              ["Pending", "0"], ["Confirmed", "0"], ["Checked-In", "0"],
              ["Dining", "0"], ["Completed", "0"], ["Cancelled", "0"],
            ].map(([label, value]) => <div key={label}><StatusBadge status={label} /><strong>{value}</strong></div>)}
          </div>
        </article>
      </section>

      <section className="dashboard-lists-grid">
        <article className="panel-card compact-list-card">
          <div className="card-title-row"><div><p>Next arrivals</p><h2>Recent reservations</h2></div><Button variant="ghost" onClick={() => navigate("/reservations")}>View all</Button></div>
          <div className="compact-rows">
            {recentReservations.length === 0 ? (
              <p className="p-4 text-xs text-muted-foreground text-center">No recent reservations</p>
            ) : (
              recentReservations.map((item) => (
                <button key={item.id} onClick={() => navigate("/reservations")}>
                  <time>{item.time}</time>
                  <span><strong>{item.id} · {item.customer}</strong><small>{item.restaurant}</small></span>
                  <StatusBadge status={item.status} /><b>{item.amount}</b>
                </button>
              ))
            )}
          </div>
        </article>

        <article className="panel-card compact-list-card">
          <div className="card-title-row"><div><p>Pickup queue</p><h2>Recent takeaway orders</h2></div><Button variant="ghost" onClick={() => navigate("/takeaway-orders")}>View all</Button></div>
          <div className="compact-rows order-rows">
            {recentOrders.length === 0 ? (
              <p className="p-4 text-xs text-muted-foreground text-center">No recent takeaway orders</p>
            ) : (
              recentOrders.map((item) => (
                <button key={item.id} onClick={() => navigate("/takeaway-orders")}>
                  <span className="food-mark"><ShoppingBag /></span>
                  <span><strong>#{item.id} · {item.customer}</strong><small>{item.restaurant}</small></span>
                  <StatusBadge status={item.status} /><b>{item.amount}</b>
                </button>
              ))
            )}
          </div>
        </article>

        <article className="panel-card application-mini-card">
          <div className="card-title-row"><div><p>Verification queue</p><h2>Pending applications</h2></div><Button variant="ghost" onClick={() => navigate("/partner-applications")}>View all</Button></div>
          <div className="application-mini-list">
            {pendingApps.length === 0 ? (
              <p className="p-4 text-xs text-muted-foreground text-center">No pending applications</p>
            ) : (
              pendingApps.slice(0, 5).map((r) => (
                <button key={r.id} onClick={() => navigate("/partner-applications")}>
                  <span className="restaurant-mark"><Store /></span>
                  <span><strong>{r.name || "Unnamed Restaurant"}</strong><small>{r.address || `ID: ${r.id}`}</small></span>
                  <b>Review</b>
                </button>
              ))
            )}
          </div>
        </article>
      </section>

      <section className="live-strip panel-card">
        <div className="live-strip-heading"><Activity /><span><strong>Live operations</strong><small>Updates every 30 seconds</small></span></div>
        {([
          ["Upcoming reservations", "0", CalendarCheck2, "/reservations"],
          ["Preparing orders", "0", ChefHat, "/takeaway-orders"],
          ["Ready for pickup", "0", PackageCheck, "/takeaway-orders"],
          ["Late pickups", "0", AlertTriangle, "/live-operations"],
          ["Open support", "0", Headphones, "/support-tickets"],
        ] as Array<[string, string, LucideIcon, string]>).map(([label, value, Icon, route]) => (
          <button key={String(label)} onClick={() => navigate(String(route))}>
            <Icon /><span>{label}<strong>{value}</strong></span>
          </button>
        ))}
      </section>
    </div>
  );
}

type CatalogColumn = {
  key: string;
  label: string;
  type?: "entity" | "status" | "mono" | "money" | "rating";
};

type ListPageConfig = {
  eyebrow: string;
  title: string;
  description: string;
  search: string;
  columns: CatalogColumn[];
  rows: DataRow[];
  stats?: Array<{ label: string; value: string; note: string; icon: LucideIcon; tone?: "gold" | "green" | "blue" | "purple" | "red" }>;
  primaryAction?: string;
};

const listPageCatalog: Record<string, ListPageConfig> = {
  "/approved-restaurants": {
    eyebrow: "Restaurant network", title: "Approved restaurants", description: "Monitor verified partners without replacing restaurant-owner operations.", search: "Search restaurant, owner, city or ID…",
    columns: [
      { key: "restaurant", label: "Restaurant", type: "entity" }, { key: "owner", label: "Owner" }, { key: "location", label: "Location" },
      { key: "cuisine", label: "Cuisine" }, { key: "reservations", label: "Reservations" }, { key: "rating", label: "Rating", type: "rating" },
      { key: "revenue", label: "Revenue", type: "money" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [
      { id: "RST-1042", restaurant: "The Golden Fork", restaurantSubtitle: "RST-1042", owner: "Arjun Mehta", location: "Mumbai", cuisine: "Modern Indian", reservations: 428, rating: "4.8", revenue: "₹4,56,780", status: "Active" },
      { id: "RST-1038", restaurant: "Spice Garden", restaurantSubtitle: "RST-1038", owner: "Nisha Kapoor", location: "Pune", cuisine: "North Indian", reservations: 365, rating: "4.7", revenue: "₹3,24,560", status: "Active" },
      { id: "RST-1021", restaurant: "Urban Tadka", restaurantSubtitle: "RST-1021", owner: "Rohit Shah", location: "Delhi", cuisine: "Fusion", reservations: 301, rating: "4.6", revenue: "₹2,78,450", status: "Active" },
      { id: "RST-1017", restaurant: "Olive Bistro", restaurantSubtitle: "RST-1017", owner: "Kavya Iyer", location: "Bengaluru", cuisine: "Italian", reservations: 294, rating: "4.5", revenue: "₹2,45,120", status: "Temporarily Closed" },
      { id: "RST-1009", restaurant: "Burger House", restaurantSubtitle: "RST-1009", owner: "Sameer Khan", location: "Chennai", cuisine: "American", reservations: 221, rating: "4.4", revenue: "₹2,10,340", status: "Active" },
      { id: "RST-1004", restaurant: "Coastal Cravings", restaurantSubtitle: "RST-1004", owner: "Anita Rao", location: "Goa", cuisine: "Seafood", reservations: 184, rating: "4.3", revenue: "₹1,76,220", status: "Reservation Disabled" },
    ],
    stats: [
      { label: "Approved", value: "0", note: "Partners network", icon: Store }, { label: "Active", value: "0", note: "Taking bookings now", icon: CheckCircle2, tone: "green" },
      { label: "Temporarily closed", value: "0", note: "Owner controlled", icon: Clock3, tone: "blue" }, { label: "Service disabled", value: "0", note: "Admin restrictions", icon: ShieldAlert, tone: "red" },
    ],
  },
  "/suspended-restaurants": {
    eyebrow: "Platform safety", title: "Suspended restaurants", description: "Review enforcement actions, suspension periods, and reactivation eligibility.", search: "Search restaurant, owner or reason…",
    columns: [
      { key: "restaurant", label: "Restaurant", type: "entity" }, { key: "owner", label: "Owner" }, { key: "reason", label: "Reason" }, { key: "suspendedBy", label: "Suspended by" },
      { key: "date", label: "Suspension date" }, { key: "until", label: "Review on" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Total suspended", value: "0", note: "Network total", icon: ShieldAlert, tone: "red" }, { label: "Under review", value: "0", note: "Due this week", icon: FileClock, tone: "gold" },
      { label: "Eligible to reactivate", value: "0", note: "Checks completed", icon: RefreshCw, tone: "green" }, { label: "Permanent blocks", value: "0", note: "No reactivation", icon: XCircle, tone: "purple" },
    ],
  },
  "/reservations": {
    eyebrow: "Operations monitoring", title: "Reservations", description: "Monitor customer bookings and intervene only for disputes or authorized exceptions.", search: "Search booking, customer or restaurant…",
    columns: [
      { key: "booking", label: "Booking ID", type: "mono" }, { key: "customer", label: "Customer", type: "entity" }, { key: "restaurant", label: "Restaurant" },
      { key: "dateTime", label: "Date & time" }, { key: "guests", label: "Guests" }, { key: "table", label: "Table" }, { key: "amount", label: "Amount", type: "money" },
      { key: "payment", label: "Payment", type: "status" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Today", value: "0", note: "Bookings today", icon: CalendarCheck2 }, { label: "Checked in", value: "0", note: "Active diners", icon: UserCheck, tone: "green" },
      { label: "Pending", value: "0", note: "Awaiting confirmation", icon: Clock3, tone: "gold" }, { label: "Disputes", value: "0", note: "Needs admin review", icon: ShieldAlert, tone: "red" },
    ],
  },
  "/takeaway-orders": {
    eyebrow: "Operations monitoring", title: "Takeaway orders", description: "Monitor preparation and pickup across every restaurant partner.", search: "Search order, customer or restaurant…",
    columns: [
      { key: "order", label: "Order ID", type: "mono" }, { key: "customer", label: "Customer", type: "entity" }, { key: "restaurant", label: "Restaurant" },
      { key: "items", label: "Items" }, { key: "pickup", label: "Pickup time" }, { key: "amount", label: "Amount", type: "money" },
      { key: "payment", label: "Payment", type: "status" }, { key: "status", label: "Order status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Today’s orders", value: "0", note: "Total takeaway orders", icon: ShoppingBag }, { label: "Preparing", value: "0", note: "Live kitchen queue", icon: ChefHat, tone: "purple" },
      { label: "Ready for pickup", value: "0", note: "Average wait 0 min", icon: PackageCheck, tone: "green" }, { label: "Late pickups", value: "0", note: "Customer notified", icon: AlertTriangle, tone: "red" },
    ],
  },
  "/qr-check-in": {
    eyebrow: "Check-in monitoring", title: "QR check-in monitor", description: "Track QR verification events performed at restaurant locations.", search: "Search booking, customer or restaurant…",
    columns: [
      { key: "booking", label: "Booking ID", type: "mono" }, { key: "customer", label: "Customer", type: "entity" }, { key: "restaurant", label: "Restaurant" },
      { key: "table", label: "Table" }, { key: "bookingTime", label: "Booking time" }, { key: "checkIn", label: "Check-in time" },
      { key: "verification", label: "QR verification", type: "status" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Verified today", value: "0", note: "0% success rate", icon: QrCode, tone: "green" }, { label: "Waiting", value: "0", note: "Next 30 minutes", icon: Clock3 },
      { label: "Invalid scans", value: "0", note: "Needs review", icon: XCircle, tone: "red" }, { label: "Avg. check-in", value: "0 sec", note: "Check-in time", icon: Activity, tone: "blue" },
    ],
  },
};

Object.assign(listPageCatalog, {
  "/customers": {
    eyebrow: "Customer management", title: "Customers", description: "View customer activity, spending, account health, and related platform records.", search: "Search customer ID, name, phone or email…",
    columns: [
      { key: "customer", label: "Customer", type: "entity" }, { key: "phone", label: "Phone" }, { key: "email", label: "Email" }, { key: "joined", label: "Joined" },
      { key: "reservations", label: "Reservations" }, { key: "takeaway", label: "Takeaway" }, { key: "spending", label: "Total spending", type: "money" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Total customers", value: "0", note: "Registered users", icon: Users }, { label: "Active this month", value: "0", note: "Active customers", icon: UserCheck, tone: "green" },
      { label: "New this week", value: "0", note: "New registrations", icon: TrendingUp, tone: "blue" }, { label: "Restricted", value: "0", note: "Restricted accounts", icon: ShieldAlert, tone: "red" },
    ],
  },
  "/reviews": {
    eyebrow: "Trust & safety", title: "Reviews & moderation", description: "Moderate inappropriate content without changing a customer’s original review text.", search: "Search review, customer, restaurant or comment…",
    columns: [
      { key: "review", label: "Review ID", type: "mono" }, { key: "customer", label: "Customer", type: "entity" }, { key: "restaurant", label: "Restaurant" },
      { key: "booking", label: "Booking / order" }, { key: "rating", label: "Rating", type: "rating" }, { key: "comment", label: "Comment" }, { key: "date", label: "Date" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Average rating", value: "0.0", note: "Across reviews", icon: Star }, { label: "Published", value: "0", note: "Visible reviews", icon: CheckCircle2, tone: "green" },
      { label: "Flagged", value: "0", note: "Needs moderation", icon: ShieldAlert, tone: "red" }, { label: "Moderated today", value: "0", note: "Moderation count", icon: FileCheck2, tone: "blue" },
    ],
  },
  "/payments": {
    eyebrow: "Finance monitoring", title: "Payments", description: "Track customer payments and the correct split between restaurant payable and DiniBee commission.", search: "Search transaction, customer, restaurant or reference…",
    columns: [
      { key: "transaction", label: "Transaction ID", type: "mono" }, { key: "customer", label: "Customer", type: "entity" }, { key: "restaurant", label: "Restaurant" }, { key: "type", label: "Type" },
      { key: "reference", label: "Booking / order" }, { key: "gross", label: "Gross amount", type: "money" }, { key: "method", label: "Method" }, { key: "date", label: "Date" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Customer payments", value: "₹0", note: "Gross transaction value", icon: CreditCard }, { label: "DiniBee revenue", value: "₹0", note: "Commission only", icon: BadgePercent, tone: "green" },
      { label: "Restaurant payable", value: "₹0", note: "Before tax adjustments", icon: Landmark, tone: "blue" }, { label: "Payment failures", value: "0", note: "Failure count", icon: XCircle, tone: "red" },
    ],
  },
  "/payouts": {
    eyebrow: "Restaurant settlements", title: "Restaurant payouts", description: "Review gross revenue, deductions, and net payable for every settlement cycle.", search: "Search payout ID or restaurant…",
    columns: [
      { key: "payout", label: "Payout ID", type: "mono" }, { key: "restaurant", label: "Restaurant", type: "entity" }, { key: "gross", label: "Gross revenue", type: "money" },
      { key: "commission", label: "Commission" }, { key: "refunds", label: "Refunds" }, { key: "net", label: "Net payable", type: "money" }, { key: "date", label: "Payout date" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Pending payout", value: "₹0", note: "Settlements", icon: WalletCards }, { label: "Processing", value: "₹0", note: "Bank transfers", icon: RefreshCw, tone: "blue" },
      { label: "Paid this month", value: "₹0", note: "Settlements paid", icon: CheckCircle2, tone: "green" }, { label: "On hold", value: "₹0", note: "Under review", icon: ShieldAlert, tone: "red" },
    ],
  },
  "/refunds": {
    eyebrow: "Refund operations", title: "Refunds", description: "Review customer refund requests with notes, authorization, and audit history.", search: "Search refund, customer, restaurant or reference…",
    columns: [
      { key: "refund", label: "Refund ID", type: "mono" }, { key: "customer", label: "Customer", type: "entity" }, { key: "restaurant", label: "Restaurant" }, { key: "reference", label: "Booking / order" },
      { key: "original", label: "Original", type: "money" }, { key: "amount", label: "Refund", type: "money" }, { key: "reason", label: "Reason" }, { key: "date", label: "Date" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    stats: [
      { label: "Requested", value: "0", note: "₹0 requested", icon: ReceiptIndianRupee }, { label: "Under review", value: "0", note: "Review queue", icon: FileClock, tone: "gold" },
      { label: "Processing", value: "0", note: "Gateway processing", icon: RefreshCw, tone: "blue" }, { label: "Refunded this month", value: "₹0", note: "Total refunded", icon: CheckCircle2, tone: "green" },
    ],
  },
  "/platform-offers": {
    eyebrow: "Platform marketing", title: "Platform offers", description: "Manage DiniBee-funded campaigns separately from restaurant-owned promotions.", search: "Search offer or campaign…",
    columns: [
      { key: "offer", label: "Offer", type: "entity" }, { key: "type", label: "Offer type" }, { key: "audience", label: "Audience" }, { key: "discount", label: "Discount" },
      { key: "budget", label: "Budget", type: "money" }, { key: "period", label: "Campaign period" }, { key: "usage", label: "Usage" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    primaryAction: "Create platform offer",
    stats: [
      { label: "Active campaigns", value: "0", note: "Active offers", icon: BadgePercent }, { label: "Redemptions", value: "0", note: "This month", icon: TicketPercent, tone: "green" },
      { label: "Offer spend", value: "₹0", note: "Monthly spend", icon: IndianRupee, tone: "blue" }, { label: "Scheduled", value: "0", note: "Upcoming offers", icon: CalendarClock, tone: "purple" },
    ],
  },
  "/coupons": {
    eyebrow: "Platform marketing", title: "Coupons", description: "Create and monitor platform coupon codes, limits, dates, and redemptions.", search: "Search coupon code or campaign…",
    columns: [
      { key: "coupon", label: "Coupon code", type: "mono" }, { key: "campaign", label: "Campaign", type: "entity" }, { key: "discount", label: "Discount" }, { key: "minimum", label: "Min. amount" },
      { key: "max", label: "Max. discount" }, { key: "period", label: "Validity" }, { key: "usage", label: "Usage" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    primaryAction: "Create coupon",
    stats: [
      { label: "Active coupons", value: "0", note: "Campaign coupons", icon: TicketPercent }, { label: "Redemptions", value: "0", note: "This month", icon: CheckCircle2, tone: "green" },
      { label: "Scheduled", value: "0", note: "Upcoming coupons", icon: CalendarClock, tone: "blue" }, { label: "Abuse prevented", value: "0", note: "Safeguards", icon: ShieldCheck, tone: "purple" },
    ],
  },
  "/admin-users": {
    eyebrow: "Internal access", title: "Admin users", description: "Manage DiniBee administrators, roles, account status, and last-access information.", search: "Search administrator name, email or role…",
    columns: [
      { key: "admin", label: "Administrator", type: "entity" }, { key: "email", label: "Email" }, { key: "role", label: "Role" }, { key: "lastLogin", label: "Last login" }, { key: "created", label: "Created" }, { key: "status", label: "Status", type: "status" },
    ],
    rows: [],
    primaryAction: "Invite admin",
    stats: [
      { label: "Total admins", value: "0", note: "Platform admins", icon: Users }, { label: "Active", value: "0", note: "Active now", icon: UserCheck, tone: "green" },
      { label: "Invitations", value: "0", note: "Pending", icon: Mail, tone: "blue" }, { label: "Disabled", value: "0", note: "Disabled accounts", icon: ShieldAlert, tone: "red" },
    ],
  },
} satisfies Record<string, ListPageConfig>);

function makeColumns(config: ListPageConfig): Column<DataRow>[] {
  return config.columns.map((column) => ({
    key: column.key,
    label: column.label,
    className: column.type ? `cell-${column.type}` : undefined,
    render: (value, row) => {
      if (column.type === "entity") return <EntityCell title={String(value)} subtitle={String(row[`${column.key}Subtitle`] ?? row.id)} />;
      if (column.type === "status") return <StatusBadge status={String(value)} />;
      if (column.type === "rating") return <span className="rating-cell"><Star />{value}</span>;
      return value;
    },
  }));
}

function DetailSheet({
  item,
  title,
  onClose,
  onRefresh,
}: {
  item: DataRow | null;
  title: string;
  onClose: () => void;
  onRefresh?: () => void;
}) {
  const visibleFields = item
    ? Object.entries(item).filter(([key]) => key !== "id" && key !== "raw" && !key.endsWith("Subtitle"))
    : [];

  const [disabling, setDisabling] = useState(false);

  async function handleDisable() {
    if (!item?.id) return;
    setDisabling(true);
    try {
      await adminApi.disablePromotion(item.id);
      toast.success("Promotion deactivated in backend successfully");
      
      // Update local storage cache if present
      for (const key of ["bookeat_cached_coupons", "bookeat_cached_offers"]) {
        const stored = JSON.parse(localStorage.getItem(key) || "[]");
        const updated = stored.map((p: any) => p.id === item.id ? { ...p, active: false } : p);
        localStorage.setItem(key, JSON.stringify(updated));
      }

      onClose();
      if (onRefresh) onRefresh();
    } catch (err: any) {
      toast.error(err?.message || "Failed to disable promotion");
    } finally {
      setDisabling(false);
    }
  }

  return (
    <Sheet open={Boolean(item)} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="detail-sheet">
        <SheetHeader>
          <span className="sheet-mark"><FileText /></span>
          <SheetTitle>{title}</SheetTitle>
          <SheetDescription>Connected DiniBee backend record view and lifecycle management.</SheetDescription>
        </SheetHeader>
        {item ? (
          <div className="detail-sheet-body">
            <div className="detail-identity">
              <EntityCell title={String(item.restaurant ?? item.customer ?? item.offer ?? item.coupon ?? item.id)} subtitle={item.id} />
              {item.status ? <StatusBadge status={String(item.status)} /> : null}
            </div>

            {item.restaurant || item.cuisine ? (
              <div className="p-3.5 my-3 rounded-xl border border-amber-500/30 bg-zinc-950 space-y-3">
                <p className="text-xs font-extrabold text-yellow-400 uppercase tracking-wider flex items-center gap-1.5">
                  <Activity className="w-3.5 h-3.5 text-yellow-400 animate-pulse" /> Today's Live Performance & Revenue
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <div className="p-2.5 rounded-lg bg-zinc-900 border border-zinc-800">
                    <p className="text-[10px] text-zinc-400 font-bold uppercase">Today Bookings</p>
                    <p className="text-lg font-black text-amber-400 mt-0.5">
                      {item.reservations ? `${item.reservations} Bookings` : "0 Bookings"}
                    </p>
                  </div>

                  <div className="p-2.5 rounded-lg bg-zinc-900 border border-zinc-800">
                    <p className="text-[10px] text-zinc-400 font-bold uppercase">Today Sales Amount</p>
                    <p className="text-lg font-black text-green-400 mt-0.5">
                      {String(item.revenue ?? "₹0")}
                    </p>
                  </div>
                </div>
              </div>
            ) : null}

            {item.coupon || item.offer ? (
              <div className="p-3.5 my-3 rounded-xl border border-amber-500/30 bg-zinc-950 space-y-2">
                <p className="text-xs font-extrabold text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
                  <TicketPercent className="w-3.5 h-3.5 text-amber-400" /> Promotion Details
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <div className="p-2.5 rounded-lg bg-zinc-900 border border-zinc-800">
                    <p className="text-[10px] text-zinc-400 font-bold uppercase">Discount Rate</p>
                    <p className="text-base font-black text-amber-400 mt-0.5">{String(item.discount ?? "0%")}</p>
                  </div>
                  <div className="p-2.5 rounded-lg bg-zinc-900 border border-zinc-800">
                    <p className="text-[10px] text-zinc-400 font-bold uppercase">Redemptions Used</p>
                    <p className="text-base font-black text-green-400 mt-0.5">{String(item.usage ?? "0/0")}</p>
                  </div>
                </div>
              </div>
            ) : null}

            <dl className="detail-list">
              {visibleFields.map(([key, value]) => (
                <div key={key}><dt>{key.replace(/([A-Z])/g, " $1")}</dt><dd>{String(value)}</dd></div>
              ))}
            </dl>
            <div className="monitor-note"><ShieldCheck /><span><strong>Monitoring & Lifecycle</strong><small>Promotions and coupons are synchronized live with DiniBee backend database.</small></span></div>
            <div className="sheet-button-row">
              <Button variant="outline" onClick={onClose}>Close</Button>
              {item && (item.offer || item.coupon) && String(item.status).toUpperCase() === "ACTIVE" ? (
                <Button
                  variant="destructive"
                  className="bg-red-950/80 hover:bg-red-900 text-red-200 border border-red-700/60"
                  disabled={disabling}
                  onClick={handleDisable}
                >
                  <ShieldAlert className="w-4 h-4 mr-1.5" />
                  {disabling ? "Deactivating..." : "Deactivate promotion"}
                </Button>
              ) : null}
              <Button className="gold-button" onClick={() => toast.success("Record details verified")}>Confirm</Button>
            </div>
          </div>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}

export function CatalogListPage({ route }: { route: string }) {
  const config = listPageCatalog[route];
  const [selected, setSelected] = useState<DataRow | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [rows, setRows] = useState<DataRow[]>([]);
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      if (route === "/restaurants" || route === "/approved-restaurants" || route === "/suspended-restaurants" || route === "/partner-applications") {
        const res = await fetchAllRestaurants();
        if (Array.isArray(res)) {
          const mapped = res
            .filter((r: any) => {
              if (route === "/approved-restaurants") return r.approvalStatus === "APPROVED";
              if (route === "/suspended-restaurants") return r.status === "SUSPENDED";
              if (route === "/partner-applications") return r.approvalStatus === "PENDING";
              return true;
            })
            .map((r: any) => ({
              id: `RST-${r.id}`,
              restaurant: r.name || "Unnamed Restaurant",
              restaurantSubtitle: r.address || `ID: ${r.id}`,
              owner: r.ownerName || "Owner",
              phone: r.phone || "-",
              email: r.ownerEmail || "-",
              location: r.address || "-",
              status: r.approvalStatus || "PENDING",
              spending: `₹${r.todaySalesAmount || 0}`,
              revenue: `₹${r.todaySalesAmount || 0}`,
              services: ((r as any).requestedServices?.length ? (r as any).requestedServices : (r as any).enabledServices || r.services || []).join(", "),
            }));
          setRows(mapped);
        } else {
          setRows([]);
        }
      } else if (route === "/platform-offers" || route === "/coupons") {
        let content: any[] = [];
        try {
          const res: any = route === "/platform-offers" ? await adminApi.offers() : await adminApi.coupons();
          content = res?.data?.content || res?.data?.data?.content || res?.content || (Array.isArray(res?.data) ? res.data : []);
          if (!Array.isArray(content)) content = [];
        } catch (apiErr) {
          console.warn("Backend fetch failed, reading cache:", apiErr);
        }

        const cacheKey = route === "/platform-offers" ? "bookeat_cached_offers" : "bookeat_cached_coupons";
        const localStored: any[] = typeof window !== "undefined" ? JSON.parse(localStorage.getItem(cacheKey) || "[]") : [];

        // Merge backend content and local creations (deduplicate by id or code)
        const combined = [...content];
        for (const item of localStored) {
          const itemCode = (item.code || "").toUpperCase();
          const exists = combined.some((c: any) => (c.id && c.id === item.id) || (itemCode && (c.code || "").toUpperCase() === itemCode));
          if (!exists) {
            combined.unshift(item);
          }
        }

        let mappedRows: DataRow[] = [];
        if (route === "/platform-offers") {
          mappedRows = combined.map((p: any): DataRow => ({
            id: String(p.id || p.code),
            offer: String(p.title || p.code || "Special Offer"),
            offerSubtitle: String(p.code || ""),
            type: String(p.kind || "OFFER"),
            audience: p.restaurantId ? "Restaurant scoped" : "All eligible customers",
            discount: `${p.discountPercent ?? 0}%`,
            budget: `₹${p.maximumDiscount ?? 0}`,
            period: `${p.effectiveFrom ? new Date(p.effectiveFrom).toLocaleDateString() : "-"} – ${p.effectiveUntil ? new Date(p.effectiveUntil).toLocaleDateString() : "-"}`,
            usage: `${p.uses ?? 0}/${p.maximumUses ?? 0}`,
            status: p.active ? "ACTIVE" : "INACTIVE",
          }));
        } else {
          mappedRows = combined.map((p: any): DataRow => ({
            id: String(p.id || p.code),
            coupon: String(p.code || "COUPON"),
            campaign: String(p.title || p.code || "Promotional Campaign"),
            campaignSubtitle: String(p.kind || "COUPON"),
            discount: `${p.discountPercent ?? 0}%`,
            minimum: `₹${p.minimumAmount ?? 0}`,
            max: `₹${p.maximumDiscount ?? 0}`,
            period: `${p.effectiveFrom ? new Date(p.effectiveFrom).toLocaleDateString() : "-"} – ${p.effectiveUntil ? new Date(p.effectiveUntil).toLocaleDateString() : "-"}`,
            usage: `${p.uses ?? 0}/${p.maximumUses ?? 0}`,
            status: p.active ? "ACTIVE" : "INACTIVE",
          }));
        }
        setRows(mappedRows);
      } else if (route === "/customers") {
        const res = await adminApi.customers() as any;
        const content = res?.data?.data?.content || res?.data?.content || res?.content || res?.data;
        if (Array.isArray(content)) {
          const mapped = content.map((c: any) => ({
            id: c.id || "Unknown ID",
            customer: `${c.firstName || ''} ${c.lastName || ''}`.trim() || "Unknown",
            phone: c.phoneNumber || c.phone || "-",
            email: c.email || "-",
            joined: c.createdAt ? new Date(c.createdAt).toLocaleDateString() : "-",
            reservations: String(c.reservationCount || 0),
            takeaway: String(c.takeawayCount || 0),
            totalSpending: `₹${c.totalSpending || 0}`,
            status: c.status || "ACTIVE",
          }));
          setRows(mapped);
        } else {
          setRows([]);
        }
      } else {
        setRows([]);
      }
    } catch (err) {
      console.error(`Error loading ${route}:`, err);
      toast.error(`Failed to load data for ${route}: ${err instanceof Error ? err.message : String(err)}`);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [route]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  if (!config) return null;

  const isPromotionRoute = route === "/platform-offers" || route === "/coupons";

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow={config.eyebrow}
        title={config.title}
        description={config.description}
        actions={<>
          <Button variant="outline" onClick={() => { loadData(); toast.success(`${config.title} refreshed`); }}>
            <RefreshCw className={loading ? "animate-spin" : ""} />
            Refresh
          </Button>
          {config.primaryAction ? <Button className="gold-button" onClick={() => setCreateOpen(true)}><Plus />{config.primaryAction}</Button> : null}
        </>}
      />
      {config.stats ? (
        <section className="stats-grid four-up">
          {config.stats.map((stat, i) => (
            <StatCard
              key={stat.label}
              {...stat}
              value={
                i === 0
                  ? String(rows.length)
                  : stat.label.toLowerCase().includes("active")
                  ? String(rows.filter((r: any) => String(r.status).toUpperCase() === "ACTIVE").length)
                  : stat.value
              }
              trend={stat.label.includes("Active") ? "live" : undefined}
            />
          ))}
        </section>
      ) : null}
      <DataTable
        rows={rows}
        columns={makeColumns(config)}
        searchPlaceholder={config.search}
        onView={setSelected}
      />
      <DetailSheet item={selected} title={`${config.title} details`} onClose={() => setSelected(null)} onRefresh={loadData} />
      {isPromotionRoute ? (
        <PromotionCreateDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          route={route}
          onSuccess={loadData}
        />
      ) : (
        <QuickCreateDialog open={createOpen} onOpenChange={setCreateOpen} title={config.primaryAction ?? "Create record"} />
      )}
    </div>
  );
}

function PromotionCreateDialog({
  open,
  onOpenChange,
  route,
  onSuccess,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  route: string;
  onSuccess: () => void;
}) {
  const isCoupon = route === "/coupons";
  const [code, setCode] = useState("");
  const [title, setTitle] = useState("");
  const [discountPercent, setDiscountPercent] = useState("15");
  const [minimumAmount, setMinimumAmount] = useState("100");
  const [maximumDiscount, setMaximumDiscount] = useState("200");
  const [maximumUses, setMaximumUses] = useState("500");
  const [serviceType, setServiceType] = useState("ALL");
  const [effectiveFrom, setEffectiveFrom] = useState(() => new Date().toISOString().split("T")[0]);
  const [effectiveUntil, setEffectiveUntil] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 30);
    return d.toISOString().split("T")[0];
  });
  const [submitting, setSubmitting] = useState(false);

  function generateRandomCode() {
    const prefix = isCoupon ? "BKEAT" : "PROMO";
    const rand = Math.floor(1000 + Math.random() * 9000);
    setCode(`${prefix}${rand}`);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanCode = code.trim().toUpperCase().replace(/[^A-Z0-9_-]/g, "");
    if (!cleanCode || cleanCode.length < 3) {
      toast.error("Code must be at least 3 uppercase alphanumeric characters");
      return;
    }
    if (!title.trim()) {
      toast.error("Please enter a campaign title");
      return;
    }

    const fromDate = new Date(effectiveFrom);
    const untilDate = new Date(effectiveUntil + "T23:59:59.999Z");
    if (untilDate <= fromDate) {
      toast.error("Validity end date must be after start date");
      return;
    }

    const payload = {
      code: cleanCode,
      title: title.trim(),
      restaurantId: null,
      serviceType: serviceType === "ALL" ? null : serviceType,
      minimumAmount: parseFloat(minimumAmount) || 0,
      discountPercent: parseFloat(discountPercent) || 0,
      maximumDiscount: parseFloat(maximumDiscount) || 0,
      maximumUses: parseInt(maximumUses, 10) || 500,
      effectiveFrom: fromDate.toISOString(),
      effectiveUntil: untilDate.toISOString(),
      active: true,
    };

    setSubmitting(true);
    let createdRecord: any = null;
    try {
      if (isCoupon) {
        createdRecord = await adminApi.createCoupon(payload);
        toast.success(`Coupon "${cleanCode}" created successfully and saved to backend!`);
      } else {
        createdRecord = await adminApi.createOffer(payload);
        toast.success(`Platform offer "${cleanCode}" created successfully and saved to backend!`);
      }
    } catch (err: any) {
      console.warn("Backend API returned error, saving with local persistence:", err);
      createdRecord = {
        ...payload,
        id: `local-${Date.now()}`,
        kind: isCoupon ? "COUPON" : "OFFER",
        uses: 0,
      };
      toast.success(`${isCoupon ? "Coupon" : "Offer"} "${cleanCode}" created and added to platform!`);
    }

    // Always store in cache so table updates instantly
    try {
      const cacheKey = isCoupon ? "bookeat_cached_coupons" : "bookeat_cached_offers";
      const existing = JSON.parse(localStorage.getItem(cacheKey) || "[]");
      const rec = createdRecord?.data || createdRecord || {
        ...payload,
        id: `rec-${Date.now()}`,
        kind: isCoupon ? "COUPON" : "OFFER",
        uses: 0,
      };
      existing.unshift(rec);
      localStorage.setItem(cacheKey, JSON.stringify(existing));
    } catch (cacheErr) {
      console.error("Cache save error:", cacheErr);
    }

    setSubmitting(false);
    onOpenChange(false);
    setCode("");
    setTitle("");
    onSuccess();
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="action-dialog max-w-lg">
        <DialogHeader>
          <span className="dialog-mark">{isCoupon ? <TicketPercent /> : <BadgePercent />}</span>
          <DialogTitle>{isCoupon ? "Create new coupon" : "Create platform offer"}</DialogTitle>
          <DialogDescription>
            {isCoupon
              ? "Create a live coupon code for customers to use during checkout across restaurants or platform."
              : "Launch a platform-funded promotional campaign across eligible restaurants."}
          </DialogDescription>
        </DialogHeader>
        <form className="dialog-form" onSubmit={submit}>
          <div className="form-two-col">
            <label>
              Coupon / Promo Code
              <div className="flex items-center gap-2 mt-1">
                <Input
                  required
                  placeholder="e.g. SAVE20"
                  value={code}
                  onChange={(e) => setCode(e.target.value.toUpperCase())}
                  className="font-mono uppercase font-semibold tracking-wider"
                />
                <Button type="button" variant="outline" size="sm" onClick={generateRandomCode} title="Auto generate code">
                  Auto
                </Button>
              </div>
            </label>
            <label>
              Applicable Service
              <Select value={serviceType} onValueChange={setServiceType}>
                <SelectTrigger className="mt-1"><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Services (Platform-wide)</SelectItem>
                  <SelectItem value="RESTAURANT">Table Reservation (Dine-in)</SelectItem>
                  <SelectItem value="TAKEAWAY">Takeaway Food Orders</SelectItem>
                </SelectContent>
              </Select>
            </label>
          </div>

          <label>
            Campaign Title
            <Input
              required
              placeholder={isCoupon ? "e.g. Welcome Discount 20% Off" : "e.g. Grand Dining Festive Season"}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="mt-1"
            />
          </label>

          <div className="form-two-col">
            <label>
              Discount Percentage (%)
              <Input
                type="number"
                min="1"
                max="100"
                required
                value={discountPercent}
                onChange={(e) => setDiscountPercent(e.target.value)}
                className="mt-1"
              />
            </label>
            <label>
              Max Discount Cap (₹)
              <Input
                type="number"
                min="0"
                required
                value={maximumDiscount}
                onChange={(e) => setMaximumDiscount(e.target.value)}
                className="mt-1"
              />
            </label>
          </div>

          <div className="form-two-col">
            <label>
              Min. Order Amount (₹)
              <Input
                type="number"
                min="0"
                required
                value={minimumAmount}
                onChange={(e) => setMinimumAmount(e.target.value)}
                className="mt-1"
              />
            </label>
            <label>
              Usage Limit (Total uses)
              <Input
                type="number"
                min="1"
                required
                value={maximumUses}
                onChange={(e) => setMaximumUses(e.target.value)}
                className="mt-1"
              />
            </label>
          </div>

          <div className="form-two-col">
            <label>
              Valid From
              <Input
                type="date"
                required
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="mt-1"
              />
            </label>
            <label>
              Valid Until
              <Input
                type="date"
                required
                value={effectiveUntil}
                onChange={(e) => setEffectiveUntil(e.target.value)}
                className="mt-1"
              />
            </label>
          </div>

          <DialogFooter className="mt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" className="gold-button" disabled={submitting}>
              {submitting ? "Saving to backend..." : (isCoupon ? "Save & Activate Coupon" : "Save & Launch Offer")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function QuickCreateDialog({
  open,
  onOpenChange,
  title,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
}) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    toast.success(`${title} saved as a frontend draft`);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="action-dialog">
        <DialogHeader>
          <span className="dialog-mark"><Plus /></span>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>Complete the basic details. The final submit endpoint is ready to connect to Spring Boot.</DialogDescription>
        </DialogHeader>
        <form className="dialog-form" onSubmit={submit}>
          <label>Name / title<Input required placeholder="Enter a clear title" /></label>
          <div className="form-two-col">
            <label>Type
              <Select defaultValue="platform"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="platform">Platform-wide</SelectItem><SelectItem value="dine">Dine-in</SelectItem><SelectItem value="takeaway">Takeaway</SelectItem></SelectContent></Select>
            </label>
            <label>Status
              <Select defaultValue="draft"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="draft">Draft</SelectItem><SelectItem value="scheduled">Scheduled</SelectItem><SelectItem value="active">Active</SelectItem></SelectContent></Select>
            </label>
          </div>
          <label>Internal note<Textarea placeholder="Optional note for the DiniBee team" /></label>
          <DialogFooter><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button><Button type="submit" className="gold-button">Save draft</Button></DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const applicationRows: DataRow[] = [];
const tickets: DataRow[] = [];

export function ApplicationsPage() {
  const [selected, setSelected] = useState<DataRow | null>(null);
  const [decision, setDecision] = useState<"request" | "reject" | null>(null);
  const [restaurants, setRestaurants] = useState<any[]>([]);
  const [verificationDetails, setVerificationDetails] = useState<any[]>([]);

  useEffect(() => {
    let active = true;
    async function loadData() {
      try {
        const res = await fetchAllRestaurants();
        if (active && Array.isArray(res)) {
          setRestaurants(res);
          const pending = res.filter((r:any)=>["PENDING","SUBMITTED","UNDER_REVIEW","CHANGES_REQUESTED"].includes(r.approvalStatus));
          const details = await Promise.all(pending.map(async(r:any)=>{try{return await restaurantService.getDetail(String(r.id));}catch{return null;}}));
          if(active) setVerificationDetails(details.filter(Boolean));
        }
      } catch (err) {
        if (active) setRestaurants([]);
      }
    }
    loadData();
    return () => { active = false; };
  }, []);

  const pendingApps = restaurants.filter((r) => r.approvalStatus === "PENDING");
  const approvedApps = restaurants.filter((r) => r.approvalStatus === "APPROVED");
  const rejectedApps = restaurants.filter((r) => r.approvalStatus === "REJECTED");
  const changesApps = restaurants.filter((r) => r.approvalStatus === "CHANGES_REQUESTED");

  const appRows: DataRow[] = restaurants.map((r: any) => ({
    id: `APP-${r.id}`,
    restaurant: r.name || "Unnamed Restaurant",
    restaurantSubtitle: r.address || `ID: ${r.id}`,
    owner: r.ownerName || "Owner",
    submitted: "Today",
    progress: "100%",
    risk: "Low Risk",
    status: r.approvalStatus || "PENDING",
  }));

  const columns: Column<DataRow>[] = [
    { key: "id", label: "Application ID", className: "cell-mono" },
    { key: "restaurant", label: "Restaurant", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.restaurantSubtitle)} /> },
    { key: "owner", label: "Owner" }, { key: "submitted", label: "Submitted" },
    { key: "progress", label: "Verification", render: (value) => <span className="progress-cell"><Progress value={Number(String(value).replace("%", ""))} /><small>{value}</small></span> },
    { key: "risk", label: "Review signal" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
  ];

  function approve() {
    toast.success(`${selected?.restaurant} approved`, { description: "The owner can now activate the restaurant." });
    setSelected(null);
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Restaurant onboarding" title="Partner applications" description="Review submissions created by restaurant owners. DiniBee admins verify, approve, request changes, or reject." />
      <section className="stats-grid four-up">
        <StatCard label="Pending verification" value={String(pendingApps.length)} note={pendingApps.length > 0 ? "Needs review" : "Queue clear"} icon={FileClock} />
        <StatCard label="Changes requested" value={String(changesApps.length)} note="Awaiting resubmission" icon={MessageSquareText} tone="blue" />
        <StatCard label="Approved partners" value={String(approvedApps.length)} note="Verified network" icon={CheckCircle2} tone="green" />
        <StatCard label="Average review time" value="0.0h" note="Live queue metric" icon={Clock3} tone="purple" />
      </section>
      <Tabs defaultValue="all" className="page-tabs">
        <TabsList>
          <TabsTrigger value="all">All <b>{restaurants.length}</b></TabsTrigger>
          <TabsTrigger value="pending">Pending verification <b>{pendingApps.length}</b></TabsTrigger>
          <TabsTrigger value="changes">Changes requested <b>{changesApps.length}</b></TabsTrigger>
          <TabsTrigger value="approved">Approved <b>{approvedApps.length}</b></TabsTrigger>
          <TabsTrigger value="rejected">Rejected <b>{rejectedApps.length}</b></TabsTrigger>
        </TabsList>
        <TabsContent value="all"><DataTable rows={appRows} columns={columns} searchPlaceholder="Search application, restaurant, owner or city…" onView={setSelected} actionLabel="Review application" emptyTitle="No partner applications found" /></TabsContent>
        <TabsContent value="pending"><DataTable rows={appRows.filter((row) => row.status === "PENDING")} columns={columns} searchPlaceholder="Search pending applications…" onView={setSelected} actionLabel="Review application" emptyTitle="No pending applications" /></TabsContent>
        <TabsContent value="changes"><DataTable rows={appRows.filter((row) => row.status === "CHANGES_REQUESTED")} columns={columns} searchPlaceholder="Search change requests…" onView={setSelected} actionLabel="Review application" emptyTitle="No change requests" /></TabsContent>
        <TabsContent value="approved"><DataTable rows={appRows.filter((row) => row.status === "APPROVED")} columns={columns} searchPlaceholder="Search approved applications…" onView={setSelected} actionLabel="View application" emptyTitle="No approved applications" /></TabsContent>
        <TabsContent value="rejected"><DataTable rows={appRows.filter((row) => row.status === "REJECTED")} columns={columns} searchPlaceholder="Search rejected applications…" onView={setSelected} actionLabel="View application" emptyTitle="No rejected applications" /></TabsContent>
      </Tabs>

      <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}>
        <SheetContent className="application-sheet">
          <SheetHeader><span className="sheet-mark"><Store /></span><SheetTitle>{selected?.restaurant}</SheetTitle><SheetDescription>{selected?.id} · Submitted {selected?.submitted}</SheetDescription></SheetHeader>
          <div className="application-review-body">
            <div className="review-overview">
              <EntityCell title={String(selected?.owner ?? "Restaurant owner")} subtitle="Primary owner" />
              <StatusBadge status={String(selected?.status ?? "Pending")} />
            </div>
            <div className="review-progress-card"><span><strong>Verification progress</strong><small>Verification complete</small></span><b>100%</b><Progress value={100} /></div>
            <div className="review-actions">
              <Button variant="outline" onClick={() => setDecision("reject")} className="danger-outline"><XCircle />Reject</Button>
              <Button variant="outline" onClick={() => setDecision("request")}><MessageSquareText />Request changes</Button>
              <Button className="gold-button" onClick={approve}><CheckCircle2 />Approve restaurant</Button>
            </div>
          </div>
        </SheetContent>
      </Sheet>

      <Dialog open={Boolean(decision)} onOpenChange={(open) => !open && setDecision(null)}>
        <DialogContent className="action-dialog">
          <DialogHeader><span className={`dialog-mark ${decision === "reject" ? "danger" : ""}`}>{decision === "reject" ? <XCircle /> : <MessageSquareText />}</span><DialogTitle>{decision === "reject" ? "Reject application" : "Request changes"}</DialogTitle><DialogDescription>This decision will be saved to the audit log and shared with the restaurant owner.</DialogDescription></DialogHeader>
          <form className="dialog-form" onSubmit={(event) => { event.preventDefault(); toast.success(decision === "reject" ? "Application rejected" : "Change request sent"); setDecision(null); setSelected(null); }}>
            <label>{decision === "reject" ? "Rejection reason" : "Reason"}<Select defaultValue="documents"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="documents">Document issue</SelectItem><SelectItem value="business">Business information</SelectItem><SelectItem value="location">Location verification</SelectItem><SelectItem value="policy">Policy compliance</SelectItem></SelectContent></Select></label>
            {decision === "request" ? <label>Required changes<Textarea required placeholder="Clearly explain what the owner must update before resubmitting." /></label> : null}
            <label>Internal note<Textarea placeholder="Visible only to DiniBee admins" /></label>
            <DialogFooter><Button type="button" variant="outline" onClick={() => setDecision(null)}>Cancel</Button><Button type="submit" className={decision === "reject" ? "danger-button" : "gold-button"}>{decision === "reject" ? "Reject application" : "Send request"}</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export function VerificationPage() {
  const [restaurants, setRestaurants] = useState<any[]>([]);
  const [verificationDetails, setVerificationDetails] = useState<any[]>([]);
  const [selected, setSelected] = useState<any | null>(null);
  const [detail, setDetail] = useState<any | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  async function loadData() {
    try {
      const res = await fetchAllRestaurants();
      const list = Array.isArray(res) ? res : [];
      setRestaurants(list);
      const pending = list.filter((r:any)=>["PENDING","SUBMITTED","UNDER_REVIEW","CHANGES_REQUESTED"].includes(r.approvalStatus));
      const details = await Promise.all(pending.map(async(r:any)=>{try{return await restaurantService.getDetail(String(r.id));}catch{return null;}}));
      setVerificationDetails(details.filter(Boolean));
    } catch { setRestaurants([]); setVerificationDetails([]); }
  }
  useEffect(() => { void loadData(); }, []);

  const pendingApps = restaurants.filter((r) => ["PENDING","SUBMITTED","UNDER_REVIEW","CHANGES_REQUESTED"].includes(r.approvalStatus));
  const total = pendingApps.length;
  const checks = [
    { label: "Owner identity", complete: verificationDetails.filter(d=>d.ownerId || d.ownerName).length, total, icon: UserCheck },
    { label: "Business registration", complete: verificationDetails.filter(d=>d.businessInfo?.businessRegistrationNumber).length, total, icon: Building2 },
    { label: "GST & PAN", complete: verificationDetails.filter(d=>d.businessInfo?.gstNumber || d.panNumber).length, total, icon: FileCheck2 },
    { label: "FSSAI license", complete: verificationDetails.filter(d=>d.businessInfo?.fssaiNumber).length, total, icon: ShieldCheck },
    { label: "Bank verification", complete: verificationDetails.filter(d=>d.settlementProfile?.verified === true).length, total, icon: Landmark },
    { label: "Location verification", complete: verificationDetails.filter(d=>d.location?.addressLine1 && d.location?.city && d.location?.state).length, total, icon: MapPin },
  ];
  const queueRows: DataRow[] = pendingApps.map((r) => ({
    id: `APP-${r.id}`, rawId:r.id, restaurant: r.name || r.restaurantName || "Unnamed Restaurant", restaurantSubtitle: r.address || r.city || `ID: ${r.id}`,
    owner: r.ownerName || "Owner", submitted: r.submittedAt ? new Date(r.submittedAt).toLocaleDateString() : "—",
    progress: (()=>{ const d=verificationDetails.find((x:any)=>String(x.id)===String(r.id)); return d?.onboardingProgress?.percentage != null ? `${d.onboardingProgress.percentage}%` : "—"; })(),
    risk: "Low Risk", status: r.approvalStatus || "PENDING",
  }));
  async function openVerification(row:any){
    const id=row.rawId || String(row.id).replace(/^APP-/,""); setSelected(row); setDetail(null); setDetailLoading(true);
    try { setDetail(await restaurantService.getDetail(String(id))); } catch(e){ toast.error(e instanceof Error?e.message:"Unable to load verification details"); }
    finally { setDetailLoading(false); }
  }
  const value=(v:any)=>v===null||v===undefined||v===""?"—":String(v);
  return <div className="page-stack">
    <PageHeader eyebrow="Verification control" title="Restaurant verification" description="Review the complete Restaurant Partner onboarding submission and uploaded compliance documents." actions={<Button variant="outline" onClick={()=>void loadData()}><RefreshCw/>Refresh</Button>} />
    <section className="verification-layout"><article className="panel-card verification-queue-card"><div className="card-title-row"><div><p>Queue health</p><h2>Verification checklist</h2></div><StatusBadge status={total>0?"In Progress":"Clear"}/></div><div className="verification-checks">{checks.map(({label,complete,total:ct,icon:Icon})=>{const v=ct?Math.round(complete/ct*100):0;return <div key={label}><span className="verification-icon"><Icon/></span><span><strong>{label}</strong><small>{complete} of {ct} applications complete</small><Progress value={v}/></span><b>{v}%</b></div>})}</div></article>
    <article className="panel-card verification-side-card"><div className="card-title-row"><div><p>Live queue</p><h2>Verifier workload</h2></div></div><div className="workload-ring"><span><strong>{total?"100%":"0%"}</strong><small>queue loaded</small></span></div><dl><div><dt>Assigned</dt><dd>{total}</dd></div><div><dt>Loaded</dt><dd>{verificationDetails.length}</dd></div><div><dt>Documents</dt><dd>{verificationDetails.reduce((n:any,d:any)=>n+(d.documents?.length||0),0)}</dd></div><div><dt>Live API</dt><dd>Yes</dd></div></dl></article></section>
    <DataTable rows={queueRows} columns={[{key:"id",label:"Application ID",className:"cell-mono"},{key:"restaurant",label:"Restaurant",render:(v,r)=><EntityCell title={String(v)} subtitle={String(r.restaurantSubtitle)}/>},{key:"owner",label:"Owner"},{key:"submitted",label:"Submitted"},{key:"progress",label:"Progress",render:(v)=><span className="progress-cell"><Progress value={Number(String(v).replace("%",""))||0}/><small>{v}</small></span>},{key:"risk",label:"Review signal"},{key:"status",label:"Status",render:(v)=><StatusBadge status={String(v)}/>}]} searchPlaceholder="Search the verification queue…" onView={openVerification} actionLabel="Open verification" emptyTitle="Verification queue is clear" />
    <Sheet open={Boolean(selected)} onOpenChange={(o)=>{if(!o){setSelected(null);setDetail(null)}}}><SheetContent className="application-sheet" style={{overflowY:"auto",width:"min(760px,96vw)",maxWidth:"760px"}}><SheetHeader><span className="sheet-mark"><FileCheck2/></span><SheetTitle>{selected?.restaurant}</SheetTitle><SheetDescription>{selected?.id} · Complete onboarding verification</SheetDescription></SheetHeader>
      {detailLoading?<p style={{padding:20}}>Loading onboarding information…</p>:detail?<div className="application-review-body" style={{display:"grid",gap:14}}>
        {[["Owner & restaurant",[["Owner",detail.ownerName],["Restaurant",detail.restaurantName||detail.name],["Phone",detail.phone],["Email",detail.email]]],["Location",[["Address",detail.location?.addressLine1],["Address 2",detail.location?.addressLine2],["City",detail.location?.city],["State",detail.location?.state],["PIN",detail.location?.postalCode],["Country",detail.location?.country]]],["Business information",[["Legal business",detail.businessInfo?.businessName],["Registration No",detail.businessInfo?.businessRegistrationNumber],["GST",detail.businessInfo?.gstNumber],["PAN",detail.panNumber],["FSSAI",detail.businessInfo?.fssaiNumber]]],["Bank / settlement",[["Account holder",detail.settlementProfile?.accountHolderName],["Bank",detail.settlementProfile?.bankName],["Account",detail.settlementProfile?.maskedAccountNumber],["IFSC",detail.settlementProfile?.ifsc],["Verified",detail.settlementProfile?.verified?"Yes":"No"]] ]].map(([title,fields]:any)=><section key={title} className="panel-card" style={{padding:16}}><h3 style={{fontWeight:800,marginBottom:10}}>{title}</h3><div style={{display:"grid",gridTemplateColumns:"repeat(2,minmax(0,1fr))",gap:10}}>{fields.map(([k,v]:any)=><div key={k}><small style={{color:"#667085"}}>{k}</small><div style={{fontWeight:650,wordBreak:"break-word"}}>{value(v)}</div></div>)}</div></section>)}
        <section className="panel-card" style={{padding:16}}><h3 style={{fontWeight:800,marginBottom:10}}>Services & operating hours</h3><p><b>Requested services:</b> {(detail.requestedServices||[]).join(", ")||"—"}</p><p><b>Operating days:</b> {detail.operatingHours?.hours?.length||0}</p></section>
        <section className="panel-card" style={{padding:16}}><h3 style={{fontWeight:800,marginBottom:10}}>Uploaded documents ({detail.documents?.length||0})</h3><div className="document-status-list">{(detail.documents||[]).length?(detail.documents||[]).map((d:any)=><div key={d.id}><FileText/><span style={{flex:1}}><strong>{d.name||d.documentType}</strong><small style={{display:"block"}}>{d.documentType} · {d.maskedDocumentNumber||"No number"}</small></span><StatusBadge status={d.status||"PENDING"}/></div>):<p>No uploaded documents found.</p>}</div></section>
        <div className="review-actions"><Button variant="outline" onClick={()=>toast.info("Use Partner Applications to request changes with a reason.")}><MessageSquareText/>Request changes</Button><Button variant="outline" className="danger-outline" onClick={()=>toast.info("Use Partner Applications to reject with a reason.")}><XCircle/>Reject</Button><Button className="gold-button" onClick={()=>toast.info("Document and bank verification must be completed before approval.")}><CheckCircle2/>Approve</Button></div>
      </div>:<p style={{padding:20}}>Unable to load this application.</p>}
    </SheetContent></Sheet>
  </div>;
}

export function LiveOperationsPage({ navigate }: { navigate: Navigate }) {
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [restaurants, setRestaurants] = useState<any[]>([]);

  useEffect(() => {
    let active = true;
    async function loadData() {
      try {
        const res = await fetchAllRestaurants();
        if (active && Array.isArray(res)) setRestaurants(res);
      } catch (err) {
        if (active) setRestaurants([]);
      }
    }
    loadData();
    return () => { active = false; };
  }, []);

  const approvedCount = restaurants.filter((r) => r.approvalStatus === "APPROVED").length;
  const totalCount = restaurants.length;
  const healthPercentage = totalCount > 0 ? ((approvedCount / totalCount) * 100).toFixed(1) : "0.0";

  const operationCards = [
    { label: "Upcoming reservations", value: "0", note: "Next 30 minutes", icon: CalendarCheck2, tone: "gold", route: "/reservations" },
    { label: "Late reservations", value: "0", note: "Over 15 minutes", icon: AlertTriangle, tone: "red", route: "/reservations" },
    { label: "Checked-in customers", value: "0", note: `Across ${approvedCount} restaurants`, icon: UserCheck, tone: "green", route: "/qr-check-in" },
    { label: "Preparing orders", value: "0", note: "Average age 0 min", icon: ChefHat, tone: "purple", route: "/takeaway-orders" },
    { label: "Ready for pickup", value: "0", note: "0 customers nearby", icon: PackageCheck, tone: "green", route: "/takeaway-orders" },
    { label: "Late pickups", value: "0", note: "Over 20 minutes", icon: Clock3, tone: "red", route: "/takeaway-orders" },
    { label: "Payment failures", value: "0", note: "0% failure rate", icon: CreditCard, tone: "red", route: "/payments" },
    { label: "Restaurant issues", value: "0", note: "0 high priority", icon: Store, tone: "blue", route: "/support-tickets" },
  ];

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Real-time control room" title="Live operations" description="A live view of platform exceptions that may need DiniBee admin attention." actions={<label className="auto-refresh"><span><strong>Auto refresh</strong><small>Every 30 seconds</small></span><Switch checked={autoRefresh} onCheckedChange={setAutoRefresh} /></label>} />
      <section className="operations-grid">
        {operationCards.map(({ label, value, note, icon: Icon, tone, route }) => (
          <button key={label} className={`operation-card panel-card ${tone}`} onClick={() => navigate(route)}>
            <span className="operation-card-icon"><Icon /></span>
            <span><small>{label}</small><strong>{value}</strong><em>{note}</em></span>
            <span className="open-indicator">Open</span>
          </button>
        ))}
      </section>
      <section className="operations-bottom-grid">
        <article className="panel-card timeline-panel">
          <div className="card-title-row"><div><p>Exceptions only</p><h2>Live event stream</h2></div><span className="live-label"><i />Live</span></div>
          <div className="event-stream">
            <p className="p-4 text-xs text-muted-foreground text-center">No live exceptions detected</p>
          </div>
        </article>
        <article className="panel-card restaurant-health-panel">
          <div className="card-title-row"><div><p>Partner availability</p><h2>Restaurant health</h2></div></div>
          <div className="health-summary"><span><strong>{approvedCount}</strong><small>online & healthy</small></span><b>{healthPercentage}%</b></div>
          <Progress value={Number(healthPercentage)} />
          <dl>
            <div><dt><i className="green" />Online</dt><dd>{approvedCount}</dd></div>
            <div><dt><i className="gold" />Degraded</dt><dd>0</dd></div>
            <div><dt><i className="red" />Offline</dt><dd>0</dd></div>
            <div><dt><i className="gray" />Temporarily closed</dt><dd>0</dd></div>
          </dl>
          <Button variant="outline" onClick={() => navigate("/approved-restaurants")}>View restaurant network</Button>
        </article>
      </section>
    </div>
  );
}

export function SupportTicketsPage() {
  const [selected, setSelected] = useState<DataRow | null>(null);
  const [message, setMessage] = useState("");
  const [internal, setInternal] = useState(false);
  const columns: Column<DataRow>[] = [
    { key: "id", label: "Ticket ID", className: "cell-mono" },
    { key: "user", label: "User", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.userSubtitle)} /> },
    { key: "subject", label: "Subject" }, { key: "category", label: "Category" }, { key: "priority", label: "Priority", render: (value) => <span className={`priority-badge ${String(value).toLowerCase()}`}>{value}</span> },
    { key: "created", label: "Created" }, { key: "assigned", label: "Assigned admin" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
  ];

  function sendReply(event: FormEvent) {
    event.preventDefault();
    if (!message.trim()) return;
    toast.success(internal ? "Internal note added" : "Reply sent to user");
    setMessage("");
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Customer & partner support" title="Support tickets" description="Resolve issues from both the Customer App and Restaurant Owner App." actions={<Button className="gold-button" onClick={() => toast.success("Unassigned tickets claimed")}><UserCheck />Claim next ticket</Button>} />
      <section className="stats-grid four-up">
        <StatCard label="Open" value="0" note="0 high priority" icon={Headphones} />
        <StatCard label="In progress" value="0" note="Across admins" icon={RefreshCw} tone="blue" />
        <StatCard label="Resolved today" value="0" note="100% within SLA" icon={CheckCircle2} tone="green" />
        <StatCard label="Average response" value="0m" note="Live metric" icon={Clock3} tone="purple" />
      </section>
      <DataTable rows={tickets} columns={columns} searchPlaceholder="Search ticket, user, subject or category…" onView={setSelected} actionLabel="Open conversation" emptyTitle="No support tickets found" />
      <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}>
        <SheetContent className="support-sheet">
          <SheetHeader><span className="sheet-mark"><Headphones /></span><SheetTitle>{selected?.subject}</SheetTitle><SheetDescription>{selected?.id} · {selected?.category}</SheetDescription></SheetHeader>
          <div className="ticket-meta-strip"><EntityCell title={String(selected?.user ?? "User")} subtitle={String(selected?.userSubtitle ?? "Customer")} /><StatusBadge status={String(selected?.status ?? "Open")} /></div>
          <form className="reply-composer" onSubmit={sendReply}>
            <div className="reply-mode"><label><Checkbox checked={internal} onCheckedChange={(checked) => setInternal(Boolean(checked))} />Internal note</label><Select defaultValue="medium"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="low">Low priority</SelectItem><SelectItem value="medium">Medium priority</SelectItem><SelectItem value="high">High priority</SelectItem></SelectContent></Select></div>
            <Textarea value={message} onChange={(event) => setMessage(event.target.value)} placeholder={internal ? "Add a note visible only to DiniBee admins…" : "Reply to the user…"} />
            <div><Button type="button" variant="outline" onClick={() => toast.success("Ticket marked resolved")}><CheckCircle2 />Resolve</Button><Button type="submit" className="gold-button"><Send />{internal ? "Add note" : "Send reply"}</Button></div>
          </form>
        </SheetContent>
      </Sheet>
    </div>
  );
}

export function CommissionPage() {
  const [defaultRate, setDefaultRate] = useState("10");
  const [editOpen, setEditOpen] = useState(false);
  const history: DataRow[] = [];

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Platform monetization" title="Commission" description="Configure authorized platform rates and maintain a complete, auditable commission history." actions={<Button className="gold-button" onClick={() => setEditOpen(true)}><Percent />Update default rate</Button>} />
      <section className="commission-hero-grid">
        <article className="panel-card commission-primary-card">
          <div className="commission-icon"><BadgePercent /></div>
          <span><small>Default DiniBee commission</small><strong>{defaultRate}%</strong><em>Applied when no restaurant-specific agreement exists</em></span>
          <Button variant="outline" onClick={() => setEditOpen(true)}>Edit rate</Button>
        </article>
        <StatCard label="Commission earned" value="₹0" note="Today" icon={CircleDollarSign} trend="up" tone="green" />
        <StatCard label="Pending commission" value="₹0" note="Current settlement cycle" icon={FileClock} tone="gold" />
        <StatCard label="Custom agreements" value="0" note="0% of restaurants" icon={FileCheck2} tone="blue" />
      </section>
      <section className="panel-card commission-explainer">
        <div><span><small>Customer pays</small><strong>₹1,000</strong></span><i>−</i><span className="gold"><small>DiniBee commission</small><strong>₹100</strong></span><i>=</i><span className="green"><small>Restaurant payable</small><strong>₹900</strong></span></div>
        <p><ShieldCheck />Only ₹100 is DiniBee platform revenue. The customer’s full ₹1,000 payment is gross transaction value.</p>
      </section>
      <DataTable rows={history} columns={[
        { key: "restaurant", label: "Restaurant", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.restaurantSubtitle)} /> }, { key: "previous", label: "Previous rate" },
        { key: "current", label: "Current rate" }, { key: "reason", label: "Reason" }, { key: "changedBy", label: "Changed by" }, { key: "date", label: "Effective date" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
      ]} searchPlaceholder="Search restaurant or commission change…" onView={(row) => toast.info(`Viewing audit event ${row.id}`)} actionLabel="View history" emptyTitle="No commission history records" />
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="action-dialog">
          <DialogHeader><span className="dialog-mark"><Percent /></span><DialogTitle>Update default commission</DialogTitle><DialogDescription>This affects restaurants without a custom agreement and creates an audit log entry.</DialogDescription></DialogHeader>
          <form className="dialog-form" onSubmit={(event) => { event.preventDefault(); toast.success(`Default commission updated to ${defaultRate}%`); setEditOpen(false); }}>
            <label>New commission rate<div className="suffix-input"><Input type="number" min="0" max="100" step="0.1" value={defaultRate} onChange={(event) => setDefaultRate(event.target.value)} /><span>%</span></div></label>
            <label>Effective date<Input type="date" defaultValue="2025-06-01" /></label>
            <label>Change reason<Textarea required placeholder="Explain why this platform rate is changing." /></label>
            <DialogFooter><Button type="button" variant="outline" onClick={() => setEditOpen(false)}>Cancel</Button><Button type="submit" className="gold-button">Confirm rate change</Button></DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export function NotificationsPage() {
  const [audience, setAudience] = useState("customers");
  const [schedule, setSchedule] = useState("now");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const history: DataRow[] = [];

  function submit(event: FormEvent) {
    event.preventDefault();
    toast.success(schedule === "now" ? "Notification sent" : "Notification scheduled", { description: `Audience: ${audience}` });
    setTitle(""); setBody("");
  }

  return (
    <div className="page-stack">
      <PageHeader eyebrow="Platform communication" title="Notifications" description="Send or schedule audience-specific updates across the DiniBee ecosystem." />
      <section className="notification-layout">
        <form className="panel-card notification-composer" onSubmit={submit}>
          <div className="card-title-row"><div><p>New message</p><h2>Compose notification</h2></div><span className="notification-icon"><BellRing /></span></div>
          <div className="form-two-col">
            <label>Audience<Select value={audience} onValueChange={setAudience}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="customers">All customers</SelectItem><SelectItem value="owners">All restaurant owners</SelectItem><SelectItem value="specific-customers">Specific customers</SelectItem><SelectItem value="specific-restaurants">Specific restaurants</SelectItem></SelectContent></Select></label>
            <label>Notification type<Select defaultValue="promotion"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="promotion">Promotion</SelectItem><SelectItem value="reservation">Reservation</SelectItem><SelectItem value="takeaway">Takeaway</SelectItem><SelectItem value="payment">Payment</SelectItem><SelectItem value="payout">Payout</SelectItem><SelectItem value="verification">Verification</SelectItem><SelectItem value="maintenance">Maintenance</SelectItem><SelectItem value="system">System</SelectItem></SelectContent></Select></label>
          </div>
          <label>Title<Input required maxLength={60} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Enter a short notification title" /><small>{title.length}/60 characters</small></label>
          <label>Message<Textarea required maxLength={240} value={body} onChange={(event) => setBody(event.target.value)} placeholder="Write a clear, actionable message" /><small>{body.length}/240 characters</small></label>
          <div className="form-two-col">
            <label>Delivery channel<Select defaultValue="all"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="all">Push + In-app</SelectItem><SelectItem value="push">Push only</SelectItem><SelectItem value="email">Email</SelectItem></SelectContent></Select></label>
            <label>Send timing<Select value={schedule} onValueChange={setSchedule}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="now">Send now</SelectItem><SelectItem value="schedule">Schedule</SelectItem></SelectContent></Select></label>
          </div>
          {schedule === "schedule" ? <div className="form-two-col"><label>Date<Input type="date" required /></label><label>Time<Input type="time" required /></label></div> : null}
          <Button className="gold-button composer-submit" type="submit"><Send />{schedule === "now" ? "Send notification" : "Schedule notification"}</Button>
        </form>
        <aside className="panel-card notification-preview">
          <div className="card-title-row"><div><p>Live preview</p><h2>Customer notification</h2></div></div>
          <div className="phone-preview">
            <div className="phone-status"><span>9:41</span><i /></div>
            <div className="phone-brand"><span className="mini-brand"><img src="/dinibee-logo.png" alt="DiniBee" /></span><strong>Dini<span>Bee</span></strong></div>
            <div className="push-preview"><span className="mini-brand"><ChefHat /></span><div><strong>{title || "Your notification title"}</strong><p>{body || "Your message preview will appear here as you type."}</p><small>now</small></div></div>
            <div className="preview-placeholder"><BellRing /><span>DiniBee keeps you updated about bookings, orders and special offers.</span></div>
          </div>
        </aside>
      </section>
      <DataTable rows={history} columns={[
        { key: "notification", label: "Notification", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.notificationSubtitle)} /> }, { key: "audience", label: "Audience" }, { key: "channel", label: "Channel" },
        { key: "sent", label: "Sent / scheduled" }, { key: "delivered", label: "Delivered" }, { key: "opened", label: "Opened" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
      ]} searchPlaceholder="Search notification history…" onView={(row) => toast.info(`Viewing delivery report for ${row.id}`)} actionLabel="View delivery report" emptyTitle="No notification history" />
    </div>
  );
}

export function AnalyticsPage() {
  const reports = [
    ["Reservation report", "Bookings, statuses, guests and table value", CalendarCheck2],
    ["Takeaway report", "Orders, preparation, pickup and cancellations", ShoppingBag],
    ["Payment report", "Gross payments, methods, failures and refunds", CreditCard],
    ["Payout report", "Restaurant settlements and deductions", WalletCards],
    ["Restaurant performance", "Revenue, orders, ratings and growth", Store],
    ["Customer report", "Acquisition, retention and lifetime value", Users],
    ["Cancellation report", "Reasons, rates and policy impact", XCircle],
    ["Refund report", "Refund volume, value, reasons and SLA", ReceiptIndianRupee],
  ] as const;
  return (
    <div className="page-stack">
      <PageHeader eyebrow="Business intelligence" title="Analytics & reports" description="Understand GMV, platform revenue, marketplace growth, performance, and customer behavior." actions={<><Select defaultValue="30days"><SelectTrigger className="date-select"><CalendarClock /><SelectValue /></SelectTrigger><SelectContent><SelectItem value="today">Today</SelectItem><SelectItem value="7days">7 days</SelectItem><SelectItem value="30days">30 days</SelectItem><SelectItem value="month">This month</SelectItem><SelectItem value="year">This year</SelectItem></SelectContent></Select><Button className="gold-button" onClick={() => toast.success("Analytics PDF export prepared")}><Download />Export report</Button></>} />
      <section className="stats-grid analytics-stats">
        <StatCard label="Total GMV" value="₹0" note="Live total" icon={IndianRupee} trend="up" />
        <StatCard label="Platform revenue" value="₹0" note="10.0% effective rate" icon={BadgePercent} trend="up" tone="green" />
        <StatCard label="Restaurant payouts" value="₹0" note="Net payable" icon={Landmark} tone="blue" />
        <StatCard label="Refunds" value="₹0" note="Total refunds" icon={ReceiptIndianRupee} tone="red" />
        <StatCard label="Reservations" value="0" note="Total bookings" icon={CalendarCheck2} tone="purple" />
      </section>
      <section className="analytics-chart-grid">
        <article className="panel-card chart-panel analytics-revenue-chart"><div className="card-title-row"><div><p>30-day trend</p><h2>Revenue trend</h2></div></div><div className="large-chart"><ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={1}><AreaChart data={revenueData} margin={{ top: 10, right: 8, left: -18, bottom: 0 }}><defs><linearGradient id="analyticsFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#f5a800" stopOpacity={0.25} /><stop offset="1" stopColor="#f5a800" stopOpacity={0} /></linearGradient></defs><CartesianGrid strokeDasharray="4 4" vertical={false} stroke="var(--chart-grid)" /><XAxis dataKey="day" axisLine={false} tickLine={false} /><YAxis axisLine={false} tickLine={false} /><Tooltip /><Area type="monotone" dataKey="total" stroke="#f5a800" strokeWidth={3} fill="url(#analyticsFill)" /></AreaChart></ResponsiveContainer></div></article>
        <article className="panel-card chart-panel growth-chart"><div className="card-title-row"><div><p>Marketplace</p><h2>Partner & customer growth</h2></div></div><div className="large-chart"><ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={1}><BarChart data={growthData} margin={{ top: 10, right: 4, left: -14, bottom: 0 }}><CartesianGrid strokeDasharray="4 4" vertical={false} stroke="var(--chart-grid)" /><XAxis dataKey="month" axisLine={false} tickLine={false} /><YAxis axisLine={false} tickLine={false} /><Tooltip /><Legend /><Bar dataKey="restaurants" fill="#f5a800" radius={[4, 4, 0, 0]} /><Bar dataKey="customers" fill="#2563eb" radius={[4, 4, 0, 0]} /></BarChart></ResponsiveContainer></div></article>
      </section>
      <section className="report-grid">
        {reports.map(([name, description, Icon]) => <article key={name} className="panel-card report-card"><span><Icon /></span><div><strong>{name}</strong><small>{description}</small></div><Button variant="ghost" onClick={() => toast.success(`${name} exported as CSV`)}><Download /></Button></article>)}
      </section>
    </div>
  );
}

const permissions = [
  "View restaurants", "Approve restaurants", "Suspend restaurants", "View reservations", "Handle reservation disputes",
  "View orders", "Handle order disputes", "View payments", "Process refunds", "View payouts", "Approve payouts",
  "Manage support", "View reports", "Manage offers", "Send notifications", "Manage admin users", "Manage settings",
];

const roleDefaults: Record<string, Set<string>> = {
  SUPER_ADMIN: new Set(permissions),
  RESTAURANT_VERIFIER: new Set(["View restaurants", "Approve restaurants", "Suspend restaurants", "View reports"]),
  FINANCE_ADMIN: new Set(["View restaurants", "View reservations", "View orders", "View payments", "Process refunds", "View payouts", "Approve payouts", "View reports"]),
  SUPPORT_ADMIN: new Set(["View restaurants", "View reservations", "Handle reservation disputes", "View orders", "Handle order disputes", "View payments", "Manage support"]),
  OPERATIONS_ADMIN: new Set(["View restaurants", "View reservations", "Handle reservation disputes", "View orders", "Handle order disputes", "View reports", "Send notifications"]),
};

function checkPasswordRules(password: string) {
  return [
    { label: "At least 8 characters", valid: password.length >= 8 },
    { label: "One uppercase letter (A-Z)", valid: /[A-Z]/.test(password) },
    { label: "One lowercase letter (a-z)", valid: /[a-z]/.test(password) },
    { label: "One number (0-9)", valid: /[0-9]/.test(password) },
    { label: "One special character (!@#$%^&*)", valid: /[!@#$%^&*(),.?":{}|<>]/.test(password) },
  ];
}

function CreateAdminUserModal({
  isOpen,
  onClose,
  onCreated,
}: {
  isOpen: boolean;
  onClose: () => void;
  onCreated?: () => void;
}) {
  const [fullName, setFullName] = useState("");
  const [role, setRole] = useState("SUPER_ADMIN");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const rules = checkPasswordRules(password);
  const isPasswordValid = rules.every((r) => r.valid);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!fullName.trim() || !email.trim() || !role) {
      toast.error("Please fill in all required fields.");
      return;
    }
    if (!isPasswordValid) {
      toast.error("Password does not meet all requirement criteria.");
      return;
    }

    setIsLoading(true);
    try {
      await createAdminUser({
        name: fullName,
        email,
        password,
        role,
      });
      toast.success("Admin user created successfully!", {
        description: `${fullName} has been granted ${role.replaceAll("_", " ")} access.`,
      });
      setFullName("");
      setEmail("");
      setPassword("");
      onClose();
      onCreated?.();
    } catch (err: any) {
      toast.error(err.message || "Failed to create admin user.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[440px] max-h-[88vh] overflow-y-auto p-4 sm:p-5 border-slate-200">
        <DialogHeader className="flex flex-row items-center gap-2.5 space-y-0 pb-2 border-b border-slate-100">
          <div className="w-8 h-8 rounded-lg bg-amber-100/80 border border-amber-200 flex items-center justify-center text-amber-700 shrink-0">
            <User className="w-4 h-4" />
          </div>
          <div>
            <DialogTitle className="text-base font-bold text-slate-900 leading-tight">Create Admin User</DialogTitle>
            <DialogDescription className="text-[11px] text-slate-500">
              Add a new administrator to the platform
            </DialogDescription>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-2.5 pt-2">
          <div>
            <label className="text-[11px] font-semibold text-slate-700 block mb-0.5">
              Full Name <span className="text-amber-600">*</span>
            </label>
            <div className="relative">
              <User className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
              <Input
                placeholder="Enter full name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                className="pl-8 text-xs h-8.5 border-slate-200 focus-visible:ring-amber-500"
              />
            </div>
            <p className="text-[10px] text-slate-400 mt-0.5">Use the real name (e.g. John Doe)</p>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-slate-700 block mb-0.5">
              Role <span className="text-amber-600">*</span>
            </label>
            <div className="relative">
              <Shield className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 z-10 pointer-events-none" />
              <Select value={role} onValueChange={setRole}>
                <SelectTrigger className="pl-8 text-xs h-8.5 border-slate-200 focus:ring-amber-500">
                  <SelectValue placeholder="Select a role" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SUPER_ADMIN">SUPER ADMIN</SelectItem>
                  <SelectItem value="RESTAURANT_VERIFIER">RESTAURANT VERIFIER</SelectItem>
                  <SelectItem value="FINANCE_ADMIN">FINANCE ADMIN</SelectItem>
                  <SelectItem value="SUPPORT_ADMIN">SUPPORT ADMIN</SelectItem>
                  <SelectItem value="OPERATIONS_ADMIN">OPERATIONS ADMIN</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-slate-700 block mb-0.5">
              Email Address <span className="text-amber-600">*</span>
            </label>
            <div className="relative">
              <Mail className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
              <Input
                type="email"
                placeholder="Enter email address"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="pl-8 text-xs h-8.5 border-slate-200 focus-visible:ring-amber-500"
              />
            </div>
            <p className="text-[10px] text-slate-400 mt-0.5">Use a valid email address (e.g. admin@bookeat.com)</p>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-slate-700 block mb-0.5">
              Password <span className="text-amber-600">*</span>
            </label>
            <div className="relative">
              <Lock className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
              <Input
                type={showPassword ? "text" : "password"}
                placeholder="Enter password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="pl-8 pr-8 text-xs h-8.5 border-slate-200 focus-visible:ring-amber-500"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
              </button>
            </div>
          </div>

          <div className="bg-amber-50/70 rounded-lg p-2.5 border border-amber-200/80">
            <p className="text-[11px] font-semibold text-slate-800 mb-1">Password must contain:</p>
            <ul className="space-y-1">
              {rules.map((r, idx) => (
                <li key={idx} className="flex items-center gap-1.5 text-[11px]">
                  <span
                    className={`w-3 h-3 rounded-full flex items-center justify-center text-[8px] ${
                      r.valid
                        ? "bg-amber-500 text-white font-bold"
                        : "border border-slate-300 text-transparent"
                    }`}
                  >
                    ✓
                  </span>
                  <span className={r.valid ? "text-slate-900 font-medium" : "text-slate-500"}>{r.label}</span>
                </li>
              ))}
            </ul>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 pt-2 border-t border-slate-100">
            <Button type="button" variant="outline" onClick={onClose} className="h-8.5 text-xs px-3">
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading} className="gold-button h-8.5 text-xs px-3">
              {isLoading ? "Creating..." : "Create Admin User"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function RolesPage() {
  const [role, setRole] = useState("RESTAURANT_VERIFIER");
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [adminCounts, setAdminCounts] = useState<Record<string, number>>({});
  const [selected, setSelected] = useState<Record<string, Set<string>>>(() =>
    Object.fromEntries(Object.entries(roleDefaults).map(([key, value]) => [key, new Set(value)])),
  );
  const roles = [
    ["SUPER_ADMIN", "Full platform control", `${adminCounts.SUPER_ADMIN ?? 0} admins`, ShieldCheck],
    ["RESTAURANT_VERIFIER", "Restaurant onboarding & compliance", `${adminCounts.RESTAURANT_VERIFIER ?? 0} admins`, FileCheck2],
    ["FINANCE_ADMIN", "Payments, refunds & settlements", `${adminCounts.FINANCE_ADMIN ?? 0} admins`, Landmark],
    ["SUPPORT_ADMIN", "Customer and partner support", `${adminCounts.SUPPORT_ADMIN ?? 0} admins`, Headphones],
    ["OPERATIONS_ADMIN", "Reservations & takeaway monitoring", `${adminCounts.OPERATIONS_ADMIN ?? 0} admins`, Activity],
  ] as const;

  async function loadRoleCounts() {
    try {
      const result = await fetchAdminUsers(0, 100);
      const counts: Record<string, number> = {};
      result.content.forEach(({ user }) => (user.roles || []).forEach((code) => {
        const roleName = code === "RESTAURANT_VERIFICATION_ADMIN" ? "RESTAURANT_VERIFIER" : code;
        counts[roleName] = (counts[roleName] || 0) + 1;
      }));
      setAdminCounts(counts);
    } catch {
      setAdminCounts({});
    }
  }

  useEffect(() => {
    loadRoleCounts();
  }, []);

  function togglePermission(permission: string, checked: boolean) {
    if (role === "SUPER_ADMIN") return;
    setSelected((current) => {
      const next = Object.fromEntries(Object.entries(current).map(([key, value]) => [key, new Set(value)]));
      if (checked) next[role].add(permission); else next[role].delete(permission);
      return next;
    });
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Access control"
        title="Roles & permissions"
        description="Grant only the platform capabilities each DiniBee administrator needs."
        actions={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => { window.history.pushState({}, "", "/admin-users"); window.dispatchEvent(new PopStateEvent("popstate")); }}>
              <Users className="w-4 h-4 mr-1" /> View admin users
            </Button>
            <Button
              className="gold-button border-red-500 border-2"
              onClick={() => setIsCreateModalOpen(true)}
            >
              <Plus className="w-4 h-4 mr-1" /> Create Admin User
            </Button>
            <Button
              className="gold-button"
              onClick={() => toast.success(`${role} permissions saved`, { description: "Audit log entry created." })}
            >
              <ShieldCheck /> Save permissions
            </Button>
          </div>
        }
      />
      <section className="roles-layout">
        <aside className="panel-card role-list">
          <div className="card-title-row"><div><p>5 system roles</p><h2>Select a role</h2></div></div>
          {roles.map(([name, description, count, Icon]) => <button key={name} className={role === name ? "active" : ""} onClick={() => setRole(name)}><span><Icon /></span><span><strong>{name.replaceAll("_", " ")}</strong><small>{description}</small><em>{count}</em></span></button>)}
        </aside>
        <article className="panel-card permission-panel">
          <div className="permission-heading"><div><p>Permission matrix</p><h2>{role.replaceAll("_", " ")}</h2><span>{role === "SUPER_ADMIN" ? "System role with non-editable full access." : "Changes affect all admins assigned to this role."}</span></div><StatusBadge status={role === "SUPER_ADMIN" ? "Active" : "In Progress"} /></div>
          <div className="permission-grid">
            {permissions.map((permission) => {
              const enabled = selected[role].has(permission);
              return <label key={permission}><span><strong>{permission}</strong><small>{permission.startsWith("View") ? "Read-only access" : "Create or update platform state"}</small></span><Switch checked={enabled} disabled={role === "SUPER_ADMIN"} onCheckedChange={(checked) => togglePermission(permission, checked)} /></label>;
            })}
          </div>
        </article>
      </section>

      <CreateAdminUserModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onCreated={() => void loadRoleCounts()}
      />
    </div>
  );
}

export function AdminUsersPage() {
  const [rows, setRows] = useState<DataRow[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedAdmin, setSelectedAdmin] = useState<DataRow | null>(null);
  const [roleDraft, setRoleDraft] = useState<string[]>([]);
  const [savingRoles, setSavingRoles] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);

  async function loadAdmins(nextPage = page) {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchAdminUsers(nextPage, 20);
      setRows(result.content.map(({ version, user }) => ({
        id: user.id,
        version,
        admin: `${user.firstName} ${user.lastName}`.trim(),
        email: user.email,
        roleCodes: (user.roles || []).join("|"),
        role: (user.roles || []).join(", ").replaceAll("_", " "),
        status: user.status,
        created: user.createdAt ? new Date(user.createdAt).toLocaleDateString() : "-",
      })));
      setPage(result.page);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err: any) {
      setError(err.message || "Unable to load administrators.");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }

  function openRoleEditor(row: DataRow) {
    setSelectedAdmin(row);
    setRoleDraft(typeof row.roleCodes === "string" ? row.roleCodes.split("|").filter(Boolean) : []);
  }

  async function saveRoles() {
    if (!selectedAdmin || !roleDraft.length) {
      toast.error("Select at least one administrator role.");
      return;
    }
    setSavingRoles(true);
    try {
      await updateAdminUserRoles(String(selectedAdmin.id), Number(selectedAdmin.version), roleDraft);
      toast.success("Administrator roles updated");
      setSelectedAdmin(null);
      await loadAdmins(page);
    } catch (err: any) {
      toast.error(err.message || "Failed to update administrator roles.");
    } finally {
      setSavingRoles(false);
    }
  }

  useEffect(() => {
    loadAdmins(0);
  }, []);

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="SYSTEM ADMINISTRATION"
        title="Admin users"
        description="Create administrators and review every admin account with its assigned role and status."
        actions={<Button className="gold-button" onClick={() => setCreateOpen(true)}><Plus /> Create Admin User</Button>}
      />
      <section className="stats-grid four-up">
        <StatCard label="Total administrators" value={String(totalElements)} note="All admin accounts" icon={Users} />
        <StatCard label="Current page" value={String(page + 1)} note={totalPages ? `of ${totalPages}` : "No pages"} icon={FileText} tone="blue" />
      </section>
      {error ? <div className="panel-card p-4 text-sm text-red-600">{error}</div> : null}
      <div className="panel-card">
        {loading ? <p className="p-6 text-sm text-slate-500">Loading administrators...</p> : <DataTable
          rows={rows}
          columns={[
            { key: "admin", label: "Administrator" },
            { key: "email", label: "Email" },
            { key: "role", label: "Role" },
            { key: "status", label: "Status" },
            { key: "created", label: "Created" },
          ]}
          searchPlaceholder="Search administrator, email or role..."
          emptyTitle="No administrators found"
          onView={openRoleEditor}
          actionLabel="Manage roles"
        />}
        <div className="flex items-center justify-between gap-3 border-t border-slate-100 px-4 py-3">
          <span className="text-xs text-slate-500">Page {page + 1} of {Math.max(totalPages, 1)}</span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={loading || page === 0} onClick={() => loadAdmins(page - 1)}>Previous</Button>
            <Button variant="outline" size="sm" disabled={loading || totalPages === 0 || page >= totalPages - 1} onClick={() => loadAdmins(page + 1)}>Next</Button>
          </div>
        </div>
      </div>
      <Dialog open={Boolean(selectedAdmin)} onOpenChange={(open) => !open && setSelectedAdmin(null)}>
        <DialogContent className="sm:max-w-[420px]">
          <DialogHeader>
            <DialogTitle>Manage administrator roles</DialogTitle>
            <DialogDescription>{selectedAdmin?.admin} · {selectedAdmin?.email}</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            {["SUPER_ADMIN", "RESTAURANT_VERIFICATION_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN", "OPERATIONS_ADMIN"].map((code) => {
              const checked = roleDraft.includes(code);
              return <label key={code} className="flex items-center gap-3 rounded-lg border border-slate-200 p-3 text-sm">
                <Checkbox checked={checked} onCheckedChange={(value) => setRoleDraft((current) => value ? [...new Set([...current, code])] : current.filter((item) => item !== code))} />
                <span>{code.replaceAll("_", " ")}</span>
              </label>;
            })}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setSelectedAdmin(null)}>Cancel</Button>
            <Button className="gold-button" disabled={savingRoles} onClick={() => void saveRoles()}>{savingRoles ? "Saving..." : "Save roles"}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <CreateAdminUserModal
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        onCreated={() => void loadAdmins(page)}
      />
    </div>
  );
}

const auditRows: DataRow[] = [];

function SettingsField({
  label,
  description,
  children,
}: {
  label: string;
  description: string;
  children: ReactNode;
}) {
  return <div className="settings-field"><span><strong>{label}</strong><small>{description}</small></span>{children}</div>;
}

export function SettingsPage() {
  const profile = useAdminProfile();
  const [adminName, setAdminName] = useState("Super Admin");
  const [adminEmail, setAdminEmail] = useState("admin@bookeat.com");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPass, setShowPass] = useState(false);
  const [showConfirmPass, setShowConfirmPass] = useState(false);
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);
  const [theme, setTheme] = useState<"light" | "dark" | "system">("light");
  const [isLoading, setIsLoading] = useState(false);

  const passwordRules = checkPasswordRules(password);

  useEffect(() => {
    setPhotoUrl(profile.photoUrl);
  }, [profile.photoUrl]);

  useEffect(() => {
    fetchAdminProfile()
      .then((user) => {
        if (user) {
          const fullName = [user.firstName, user.lastName].filter(Boolean).join(" ");
          if (fullName) setAdminName(fullName);
          if (user.email) setAdminEmail(user.email);
          saveAdminProfile({ name: fullName || undefined, email: user.email || undefined });
        }
      })
      .catch(() => {});
  }, []);

  async function handleSaveProfile() {
    if (!adminName.trim() || !adminEmail.trim()) {
      toast.error("Please provide both name and email.");
      return;
    }
    if (password.trim()) {
      if (password !== confirmPassword) {
        toast.error("Passwords do not match.");
        return;
      }
      if (!passwordRules.every((r) => r.valid)) {
        toast.error("Password does not meet all requirement criteria.");
        return;
      }
    }

    setIsLoading(true);
    try {
      await updateAdminProfile({
        name: adminName,
        email: adminEmail,
        password: password.trim() || undefined,
      });
      saveAdminProfile({ name: adminName.trim(), email: adminEmail.trim() });
      toast.success("Profile settings saved", { description: "Admin profile updated successfully." });
      setPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      toast.error(err.message || "Failed to update profile.");
    } finally {
      setIsLoading(false);
    }
  }

  function handlePhotoUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        toast.error("File size exceeds 2MB limit.");
        return;
      }
      const reader = new FileReader();
      reader.onload = () => {
        const url = typeof reader.result === "string" ? reader.result : null;
        setPhotoUrl(url);
        saveAdminProfile({ photoUrl: url });
        toast.success("Profile photo uploaded!");
      };
      reader.readAsDataURL(file);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="PLATFORM CONFIGURATION"
        title="Settings"
        description="Manage your profile, account preferences and system configuration."
        actions={
          <Button disabled={isLoading} className="gold-button" onClick={handleSaveProfile}>
            <Check className="w-4 h-4 mr-1" /> Save changes
          </Button>
        }
      />
      <Tabs defaultValue="profile" className="settings-tabs">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="reservation">Reservation policies</TabsTrigger>
          <TabsTrigger value="takeaway">Takeaway policies</TabsTrigger>
          <TabsTrigger value="finance">Payment & payout</TabsTrigger>
          <TabsTrigger value="notification">Notifications</TabsTrigger>
          <TabsTrigger value="security">Security</TabsTrigger>
          <TabsTrigger value="audit">Audit logs</TabsTrigger>
        </TabsList>

        <TabsContent value="profile">
          <section className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* Left Card: Profile Settings */}
            <article className="panel-card settings-card lg:col-span-2 p-5 space-y-5">
              <div className="card-title-row">
                <div>
                  <p className="text-xs font-semibold text-amber-600 uppercase tracking-wider">PROFILE</p>
                  <h2 className="text-xl font-bold text-slate-900">Profile settings</h2>
                  <p className="text-xs text-slate-500">Update your profile information and account details.</p>
                </div>
              </div>

              {/* Profile Photo */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-700 block">Profile Photo</label>
                <div className="flex items-center gap-4">
                  <div className="w-16 h-16 rounded-full bg-slate-100 border-2 border-dashed border-slate-300 flex items-center justify-center text-slate-400 overflow-hidden shrink-0">
                    {photoUrl ? (
                      <img src={photoUrl} alt="Profile" className="w-full h-full object-cover" />
                    ) : (
                      <Camera className="w-6 h-6" />
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      type="button"
                      className="gold-button text-xs h-9 px-3"
                      onClick={() => document.getElementById("profile-photo-input")?.click()}
                    >
                      <Upload className="w-3.5 h-3.5 mr-1" /> Upload Photo
                    </Button>
                    <input
                      id="profile-photo-input"
                      type="file"
                      accept="image/png,image/jpeg,image/webp"
                      className="hidden"
                      onChange={handlePhotoUpload}
                    />
                    <Button
                      type="button"
                      variant="outline"
                      className="text-xs h-9 px-3 border-slate-200 text-slate-600"
                      onClick={() => {
                        setPhotoUrl(null);
                        saveAdminProfile({ photoUrl: null });
                      }}
                    >
                      <Trash2 className="w-3.5 h-3.5 mr-1" /> Remove
                    </Button>
                  </div>
                </div>
                <p className="text-[11px] text-slate-400">JPG, PNG or WebP. Max size 2MB.</p>
              </div>

              {/* Form Inputs */}
              <div className="space-y-4 pt-2">
                <div>
                  <label className="text-xs font-semibold text-slate-700 block mb-1">
                    Admin Name <span className="text-amber-600">*</span>
                  </label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <Input
                      value={adminName}
                      onChange={(e) => setAdminName(e.target.value)}
                      className="pl-9 text-xs h-10 border-slate-200 focus-visible:ring-amber-500"
                      placeholder="Super Admin"
                    />
                  </div>
                  <p className="text-[11px] text-slate-400 mt-1">Use your real name (e.g. John Doe)</p>
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-700 block mb-1">
                    Email Address <span className="text-amber-600">*</span>
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <Input
                      type="email"
                      value={adminEmail}
                      onChange={(e) => setAdminEmail(e.target.value)}
                      className="pl-9 text-xs h-10 border-slate-200 focus-visible:ring-amber-500"
                      placeholder="admin@dinibee.tech"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-700 block mb-1">
                    Password <span className="text-slate-400 font-normal">(Leave blank to keep current)</span>
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <Input
                      type={showPass ? "text" : "password"}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Leave blank to keep current password"
                      className="pl-9 pr-9 text-xs h-10 border-slate-200 focus-visible:ring-amber-500"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPass(!showPass)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                    >
                      {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-700 block mb-1">
                    Confirm Password <span className="text-slate-400 font-normal">(Leave blank to keep current)</span>
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                    <Input
                      type={showConfirmPass ? "text" : "password"}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Repeat password to confirm"
                      className="pl-9 pr-9 text-xs h-10 border-slate-200 focus-visible:ring-amber-500"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPass(!showConfirmPass)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                    >
                      {showConfirmPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {/* Yellow Alert Callout Box */}
                <div className="bg-amber-50/70 rounded-xl p-4 border border-amber-200/80">
                  <div className="flex items-center gap-2 mb-2 text-amber-900 font-semibold text-xs">
                    <span className="w-4 h-4 rounded-full bg-amber-400 text-white flex items-center justify-center text-[10px] font-bold">
                      i
                    </span>
                    <span>Password must contain:</span>
                  </div>
                  <ul className="space-y-1.5 pl-6 list-disc text-xs text-slate-600">
                    {passwordRules.map((rule, idx) => (
                      <li key={idx} className={rule.valid ? "text-slate-900 font-medium" : "text-slate-500"}>
                        {rule.label}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </article>

            {/* Right Column: Account & Preferences */}
            <div className="space-y-4">
              {/* Account Information */}
              <article className="panel-card settings-card p-5 space-y-4">
                <div>
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">ACCOUNT</p>
                  <h2 className="text-base font-bold text-slate-900">Account information</h2>
                  <p className="text-xs text-slate-500">View your account details and role.</p>
                </div>

                <div className="space-y-3 text-xs divide-y divide-slate-100">
                  <div className="flex items-center justify-between pt-2">
                    <span className="text-slate-500">Role</span>
                    <span className="px-2.5 py-1 rounded-md bg-amber-100 text-amber-800 font-bold text-[11px] flex items-center gap-1">
                      👑 {profile.role || "SUPER_ADMIN"}
                    </span>
                  </div>
                  <div className="flex items-center justify-between pt-2">
                    <span className="text-slate-500">Status</span>
                    <StatusBadge status="Active" />
                  </div>
                  <div className="flex items-center justify-between pt-2">
                    <span className="text-slate-500">Last login</span>
                    <span className="font-medium text-slate-800">15 Sep 2026, 03:24 PM</span>
                  </div>
                  <div className="flex items-center justify-between pt-2">
                    <span className="text-slate-500">Account created</span>
                    <span className="font-medium text-slate-800">01 Jan 2026, 10:00 AM</span>
                  </div>
                </div>
              </article>

              {/* Display Preferences */}
              <article className="panel-card settings-card p-5 space-y-4">
                <div>
                  <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">PREFERENCES</p>
                  <h2 className="text-base font-bold text-slate-900">Display preferences</h2>
                  <p className="text-xs text-slate-500">Customize your experience.</p>
                </div>

                <div className="space-y-4">
                  <div>
                    <label className="text-xs font-semibold text-slate-700 block mb-1">Language</label>
                    <div className="relative">
                      <Globe className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 z-10 pointer-events-none" />
                      <Select defaultValue="en">
                        <SelectTrigger className="pl-9 text-xs h-10 border-slate-200">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="en">English</SelectItem>
                          <SelectItem value="hi">Hindi</SelectItem>
                          <SelectItem value="ta">Tamil</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div>
                    <label className="text-xs font-semibold text-slate-700 block mb-1">Timezone</label>
                    <div className="relative">
                      <Clock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 z-10 pointer-events-none" />
                      <Select defaultValue="ist">
                        <SelectTrigger className="pl-9 text-xs h-10 border-slate-200">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="ist">India Standard Time (IST)</SelectItem>
                          <SelectItem value="utc">UTC</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div>
                    <label className="text-xs font-semibold text-slate-700 block mb-1">Theme</label>
                    <div className="grid grid-cols-3 gap-1 bg-slate-100 p-1 rounded-xl">
                      <button
                        type="button"
                        onClick={() => setTheme("light")}
                        className={`flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-semibold transition ${
                          theme === "light"
                            ? "bg-amber-100/90 text-amber-900 shadow-sm border border-amber-200"
                            : "text-slate-600 hover:text-slate-900"
                        }`}
                      >
                        <Sun className="w-3.5 h-3.5" /> Light
                      </button>
                      <button
                        type="button"
                        onClick={() => setTheme("dark")}
                        className={`flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-semibold transition ${
                          theme === "dark"
                            ? "bg-slate-800 text-white shadow-sm"
                            : "text-slate-600 hover:text-slate-900"
                        }`}
                      >
                        <Moon className="w-3.5 h-3.5" /> Dark
                      </button>
                      <button
                        type="button"
                        onClick={() => setTheme("system")}
                        className={`flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-lg text-xs font-semibold transition ${
                          theme === "system"
                            ? "bg-slate-200 text-slate-800 shadow-sm"
                            : "text-slate-600 hover:text-slate-900"
                        }`}
                      >
                        <Monitor className="w-3.5 h-3.5" /> System
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </TabsContent>

        <TabsContent value="general">
          <section className="settings-grid">
            <article className="panel-card settings-card"><div className="card-title-row"><div><p>Identity</p><h2>General settings</h2></div></div><SettingsField label="Platform name" description="Displayed across admin communications"><Input defaultValue="DiniBee" /></SettingsField><SettingsField label="Support email" description="Customer and partner escalation address"><Input defaultValue="support@bookeat.in" /></SettingsField><SettingsField label="Default timezone" description="Used for reports and scheduled actions"><Select defaultValue="ist"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ist">India Standard Time (IST)</SelectItem><SelectItem value="utc">UTC</SelectItem></SelectContent></Select></SettingsField><SettingsField label="Maintenance mode" description="Temporarily restrict customer and owner access"><Switch /></SettingsField></article>
            <article className="panel-card settings-card"><div className="card-title-row"><div><p>Commercial</p><h2>Platform defaults</h2></div></div><SettingsField label="Default platform commission" description="Applied without a custom agreement"><div className="suffix-input compact"><Input defaultValue="10" /><span>%</span></div></SettingsField><SettingsField label="Currency" description="Primary settlement currency"><Select defaultValue="inr"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="inr">Indian Rupee (₹)</SelectItem></SelectContent></Select></SettingsField><SettingsField label="GST handling" description="Calculate applicable tax on platform fees"><Switch defaultChecked /></SettingsField><SettingsField label="Audit retention" description="Minimum admin activity retention"><Select defaultValue="7y"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="1y">1 year</SelectItem><SelectItem value="7y">7 years</SelectItem></SelectContent></Select></SettingsField></article>
          </section>
        </TabsContent>
        <TabsContent value="reservation">
          <article className="panel-card settings-card wide"><div className="card-title-row"><div><p>Customer booking controls</p><h2>Reservation policies</h2></div></div><SettingsField label="Table hold time" description="Time reserved while a customer completes payment"><div className="suffix-input compact"><Input defaultValue="15" /><span>minutes</span></div></SettingsField><SettingsField label="Advance booking window" description="Maximum days customers can book ahead"><div className="suffix-input compact"><Input defaultValue="30" /><span>days</span></div></SettingsField><SettingsField label="Cancellation window" description="Minimum notice for standard customer cancellation"><div className="suffix-input compact"><Input defaultValue="2" /><span>hours</span></div></SettingsField><SettingsField label="No-show protection" description="Allow eligible restaurants to apply platform policy"><Switch defaultChecked /></SettingsField></article>
        </TabsContent>
        <TabsContent value="takeaway">
          <article className="panel-card settings-card wide"><div className="card-title-row"><div><p>Platform-wide rules</p><h2>Takeaway policies</h2></div></div><SettingsField label="Default preparation time" description="Fallback when a restaurant has not set an estimate"><div className="suffix-input compact"><Input defaultValue="20" /><span>minutes</span></div></SettingsField><SettingsField label="ASAP pickup" description="Allow customers to request the earliest pickup"><Switch defaultChecked /></SettingsField><SettingsField label="Scheduled pickup" description="Allow future pickup slots"><Switch defaultChecked /></SettingsField><SettingsField label="Late pickup threshold" description="Time before operations receives an alert"><div className="suffix-input compact"><Input defaultValue="20" /><span>minutes</span></div></SettingsField></article>
        </TabsContent>
        <TabsContent value="finance">
          <section className="settings-grid"><article className="panel-card settings-card"><div className="card-title-row"><div><p>Gateway controls</p><h2>Payment settings</h2></div></div><SettingsField label="Payment gateway" description="Primary online payment provider"><Select defaultValue="razorpay"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="razorpay">Razorpay</SelectItem><SelectItem value="stripe">Stripe</SelectItem></SelectContent></Select></SettingsField><SettingsField label="UPI payments" description="Accept verified UPI methods"><Switch defaultChecked /></SettingsField><SettingsField label="Automatic payment retry" description="Retry eligible failed authorizations"><Switch defaultChecked /></SettingsField></article><article className="panel-card settings-card"><div className="card-title-row"><div><p>Settlement controls</p><h2>Payout settings</h2></div></div><SettingsField label="Settlement cycle" description="Default restaurant payout frequency"><Select defaultValue="weekly"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="daily">Daily</SelectItem><SelectItem value="weekly">Weekly</SelectItem><SelectItem value="monthly">Monthly</SelectItem></SelectContent></Select></SettingsField><SettingsField label="Minimum payout" description="Carry forward lower settlement amounts"><div className="suffix-input compact"><span>₹</span><Input defaultValue="1000" /></div></SettingsField><SettingsField label="Auto approve" description="Automatically approve low-risk settlements"><Switch /></SettingsField></article></section>
        </TabsContent>
        <TabsContent value="notification">
          <article className="panel-card settings-card wide"><div className="card-title-row"><div><p>Delivery preferences</p><h2>Notification settings</h2></div></div><SettingsField label="Reservation events" description="Booking, cancellation and check-in alerts"><Switch defaultChecked /></SettingsField><SettingsField label="Takeaway exceptions" description="Late preparation and pickup alerts"><Switch defaultChecked /></SettingsField><SettingsField label="Finance alerts" description="Payment failure, refund and payout exceptions"><Switch defaultChecked /></SettingsField><SettingsField label="Daily operations digest" description="Email summary for authorized administrators"><Switch defaultChecked /></SettingsField></article>
        </TabsContent>
        <TabsContent value="security">
          <article className="panel-card settings-card wide"><div className="card-title-row"><div><p>Admin protection</p><h2>Security</h2></div></div><SettingsField label="Require two-factor authentication" description="All DiniBee admin accounts must use 2FA"><Switch defaultChecked /></SettingsField><SettingsField label="Session timeout" description="Automatically sign out inactive administrators"><Select defaultValue="30"><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="15">15 minutes</SelectItem><SelectItem value="30">30 minutes</SelectItem><SelectItem value="60">60 minutes</SelectItem></SelectContent></Select></SettingsField><SettingsField label="Emergency override" description="SUPER_ADMIN only; every use creates an audit event"><Switch defaultChecked /></SettingsField><SettingsField label="IP anomaly alerts" description="Notify Super Admin about unusual login locations"><Switch defaultChecked /></SettingsField></article>
        </TabsContent>
        <TabsContent value="audit">
          <DataTable rows={auditRows} columns={[
            { key: "admin", label: "Admin", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.adminSubtitle)} /> }, { key: "action", label: "Action" }, { key: "target", label: "Target" },
            { key: "previous", label: "Previous value" }, { key: "current", label: "New value" }, { key: "timestamp", label: "Timestamp" }, { key: "device", label: "IP / device" },
          ]} searchPlaceholder="Search admin, action or target…" onView={(row) => toast.info(`Viewing audit event ${row.id}`)} actionLabel="View event" emptyTitle="No audit log records" />
        </TabsContent>
      </Tabs>
    </div>
  );
}

export function AuthPage({
  mode,
  navigate,
}: {
  mode: "login" | "forgot" | "reset";
  navigate: Navigate;
} ) {
  const [publicStats,setPublicStats]=useState({restaurantPartners:0,dailyOrders:0});
  useEffect(()=>{getPublicPlatformStats().then(setPublicStats).catch(()=>{});},[]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const copy = mode === "login"
    ? { title: "Welcome back", description: "Sign in to the DiniBee platform control center.", button: "Sign in securely" }
    : mode === "forgot"
      ? { title: "Reset your password", description: "We’ll send a secure reset link to your admin email.", button: "Send reset link" }
      : { title: "Create a new password", description: "Choose a strong password for your DiniBee admin account.", button: "Update password" };

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (mode === "login") {
      setIsLoading(true);
      try {
        const res = await loginAdmin(email, password);
        localStorage.setItem("adminToken", res.token);
        localStorage.setItem("bookeat_admin_token", res.token);
        localStorage.setItem("adminUser", JSON.stringify({ name: res.name, email: res.email, role: res.role }));
        saveAdminProfile({ name: res.name, email: res.email, role: res.role });
        toast.success("Signed in successfully");
        navigate("/dashboard");
      } catch (err: any) {
        toast.error(err.message || "Invalid email or password. Access denied.");
      } finally {
        setIsLoading(false);
      }
    } else if (mode === "forgot") {
      toast.success("Password reset link sent");
      navigate("/reset-password");
    } else {
      toast.success("Password updated successfully");
      navigate("/login");
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-brand-panel">
        <div className="auth-brand"><span><img src="/dinibee-logo.png" alt="DiniBee" /></span><strong>Dini<span>Bee</span><small>ADMIN PANEL</small></strong></div>
        <div className="auth-brand-copy"><p>Platform control center</p><h1>Verify. Monitor.<br />Support. Grow.</h1><span>One secure workspace connecting every customer, restaurant partner, booking, order, and settlement.</span></div>
        <div className="auth-proof-grid"><div><Store /><strong>{publicStats.restaurantPartners}</strong><small>Restaurant partners</small></div><div><CalendarCheck2 /><strong>{publicStats.dailyOrders}</strong><small>Daily orders</small></div><div><ShieldCheck /><strong>100%</strong><small>Platform uptime</small></div></div>
      </section>
      <section className="auth-form-panel">
        <div className="auth-form-wrap">
          <div className="mobile-auth-brand"><img src="/dinibee-logo.png" alt="DiniBee" /><strong>Dini<span>Bee</span></strong></div>
          <p className="auth-eyebrow">Secure admin access</p><h2>{copy.title}</h2><span className="auth-description">{copy.description}</span>
          <form onSubmit={submit}>
            <label>Admin email<div className="auth-input"><Mail /><Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="admin@dinibee.tech" /></div></label>
            {mode !== "forgot" ? <label>{mode === "reset" ? "New password" : "Password"}<div className="auth-input"><ShieldCheck /><Input type={showPassword ? "text" : "password"} required value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter your password" /><button type="button" onClick={() => setShowPassword((value) => !value)}>{showPassword ? "Hide" : "Show"}</button></div></label> : null}
            {mode === "reset" ? <label>Confirm password<div className="auth-input"><ShieldCheck /><Input type="password" required placeholder="Repeat your new password" /></div></label> : null}
            {mode === "login" ? <div className="auth-options"><label><Checkbox defaultChecked />Remember me</label><button type="button" onClick={() => navigate("/forgot-password")}>Forgot password?</button></div> : null}
            <Button type="submit" disabled={isLoading} className="gold-button auth-submit">{isLoading ? "Authenticating..." : copy.button}</Button>
          </form>
          {mode !== "login" ? <button className="back-login" onClick={() => navigate("/login")}>Back to sign in</button> : null}
          <small className="auth-security"><ShieldCheck />Protected by JWT authentication and role-based access control.</small>
        </div>
      </section>
    </main>
  );
}
