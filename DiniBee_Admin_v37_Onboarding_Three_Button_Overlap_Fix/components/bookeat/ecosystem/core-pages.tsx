"use client";

import { useMemo, useRef, useState } from "react";
import {
  Activity,
  BadgeCheck,
  BadgeIndianRupee,
  Building2,
  CalendarCheck2,
  CheckCircle2,
  Coffee,
  CreditCard,
  FileCheck2,
  FileClock,
  IndianRupee,
  MapPin,
  RefreshCw,
  ShieldAlert,
  ShoppingBag,
  Store,
  TicketCheck,
  Users,
  WalletCards,
} from "lucide-react";
import { useNavigate } from "@/lib/bookeat/router";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { toast } from "sonner";

import { DataTable, EntityCell, PageHeader, StatCard, StatusBadge, type Column, type DataRow } from "@/components/bookeat/shared";
import { ConfirmActionDialog, DataSourceNotice, DetailGrid, DetailSection, formatCount, formatMoney, ServicePills } from "@/components/bookeat/ecosystem/shared";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useApiResource } from "@/hooks/use-api-resource";
import { dashboardService, reservationService, restaurantService } from "@/lib/bookeat/services";
import type { BookingType, BusinessService, DashboardSnapshot, ReservationRecord, RestaurantApprovalStatus, RestaurantRecord } from "@/lib/bookeat/types";

const emptyDashboard: DashboardSnapshot = {
  restaurants: { total: 0, pending: 0, approved: 0 },
  customers: 0,
  reservations: { today: 0, regular: 0, group: 0, corporate: 0 },
  dining: { active: 0, checkedIn: 0, dining: 0, occupiedTables: 0 },
  takeaway: { total: 0, onTheWay: 0 },
  diniTicket: { orders: 0, waiting: 0 },
  finance: { revenue: 0, commission: 0, pendingSettlements: 0 },
  support: { customerOpen: 0, ownerOpen: 0 },
  wallet: { issued: 0, redeemed: 0 },
  recentActivity: [],
  serviceVolume: [],
  revenueTrend: [],
};

export function PlatformDashboardPage() {
  const { data, loading, error, reload } = useApiResource(
    () => dashboardService.getOverview(),
    [],
    emptyDashboard,
  );

  const safeData = useMemo(() => {
    const raw: any = data || emptyDashboard;
    const isFlat = raw.totalRestaurants !== undefined || raw.pendingRestaurants !== undefined;
    return {
      restaurants: {
        total: isFlat ? (raw.totalRestaurants ?? 0) : (raw.restaurants?.total ?? 0),
        pending: isFlat ? (raw.pendingRestaurants ?? 0) : (raw.restaurants?.pending ?? 0),
        approved: isFlat ? (raw.approvedRestaurants ?? 0) : (raw.restaurants?.approved ?? 0),
      },
      customers: isFlat ? (raw.totalCustomers ?? 0) : (raw.customers ?? 0),
      reservations: {
        today: isFlat ? (raw.todayReservations ?? 0) : (raw.reservations?.today ?? 0),
        regular: raw.reservations?.regular ?? 0,
        group: raw.reservations?.group ?? 0,
        corporate: raw.reservations?.corporate ?? 0,
      },
      dining: {
        active: raw.dining?.active ?? 0,
        checkedIn: raw.dining?.checkedIn ?? 0,
        dining: raw.dining?.dining ?? 0,
        occupiedTables: raw.dining?.occupiedTables ?? 0,
      },
      takeaway: {
        total: isFlat ? (raw.todayOrders ?? 0) : (raw.takeaway?.total ?? 0),
        onTheWay: raw.takeaway?.onTheWay ?? 0,
      },
      diniTicket: {
        orders: isFlat ? (raw.activeDiniTickets ?? 0) : (raw.diniTicket?.orders ?? 0),
        waiting: raw.diniTicket?.waiting ?? 0,
      },
      finance: {
        revenue: isFlat ? (raw.totalRevenue ?? 0) : (raw.finance?.revenue ?? 0),
        commission: isFlat ? (raw.platformCommission ?? 0) : (raw.finance?.commission ?? 0),
        pendingSettlements: isFlat ? (raw.pendingSettlements ?? 0) : (raw.finance?.pendingSettlements ?? 0),
      },
      support: {
        customerOpen: isFlat ? (raw.openSupportTickets ?? 0) : (raw.support?.customerOpen ?? 0),
        ownerOpen: raw.support?.ownerOpen ?? 0,
      },
      wallet: {
        issued: raw.wallet?.issued ?? 0,
        redeemed: raw.wallet?.redeemed ?? 0,
      },
      recentActivity: (raw.recentActivity || []) as DashboardSnapshot["recentActivity"],
      serviceVolume: (raw.serviceVolume || []) as DashboardSnapshot["serviceVolume"],
      revenueTrend: (raw.revenueTrend || raw.restaurantGrowth || []) as DashboardSnapshot["revenueTrend"],
    };
  }, [data]);

  const headlineStats = [
    { label: "Total restaurants", value: safeData.restaurants.total, note: "Across all approval states", icon: Store, tone: "gold" as const },
    { label: "Pending restaurants", value: safeData.restaurants.pending, note: "Waiting for verification", icon: FileClock, tone: "red" as const },
    { label: "Approved restaurants", value: safeData.restaurants.approved, note: "Platform partners", icon: BadgeCheck, tone: "green" as const },
    { label: "Total customers", value: safeData.customers, note: "Wallet-enabled accounts shown", icon: Users, tone: "blue" as const },
    { label: "Today’s reservations", value: safeData.reservations.today, note: "All booking types", icon: CalendarCheck2, tone: "purple" as const },
  ];

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="DiniBee platform control"
        title="Operations overview"
        description="Monitor the customer app, restaurant-owner workflows, money movement, rewards and support from one place."
        actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()} disabled={loading}><RefreshCw />Refresh</Button></>}
      />
      {error ? <div className="page-error panel-card"><ShieldAlert /><span><strong>Unable to load dashboard</strong><small>{error}</small></span><Button variant="outline" onClick={() => void reload()}>Retry</Button></div> : null}

      <section className="stats-grid dashboard-stats">
        {headlineStats.map((stat) => <StatCard key={stat.label} label={stat.label} value={loading ? "—" : formatCount(stat.value)} note={stat.note} icon={stat.icon} tone={stat.tone} />)}
      </section>

      <section className="ecosystem-summary-grid">
        <article className="panel-card ecosystem-metric-group">
          <div className="card-title-row"><div><p>Reservation mix</p><h2>Today’s bookings</h2></div><CalendarCheck2 /></div>
          <div className="mini-metric-grid">
            <div><span>Regular</span><strong>{formatCount(safeData.reservations.regular)}</strong></div>
            <div><span>Group</span><strong>{formatCount(safeData.reservations.group)}</strong></div>
            <div><span>Corporate</span><strong>{formatCount(safeData.reservations.corporate)}</strong></div>
          </div>
        </article>
        <article className="panel-card ecosystem-metric-group">
          <div className="card-title-row"><div><p>Dine-in lifecycle</p><h2>Active restaurant floor</h2></div><Coffee /></div>
          <div className="mini-metric-grid four">
            <div><span>Active dine-in</span><strong>{formatCount(safeData.dining.active)}</strong></div>
            <div><span>Checked in</span><strong>{formatCount(safeData.dining.checkedIn)}</strong></div>
            <div><span>Dining</span><strong>{formatCount(safeData.dining.dining)}</strong></div>
            <div><span>Occupied tables</span><strong>{formatCount(safeData.dining.occupiedTables)}</strong></div>
          </div>
        </article>
        <article className="panel-card ecosystem-metric-group">
          <div className="card-title-row"><div><p>Pickup channels</p><h2>Takeaway & DiniTicket</h2></div><ShoppingBag /></div>
          <div className="mini-metric-grid four">
            <div><span>Takeaway</span><strong>{formatCount(safeData.takeaway.total)}</strong></div>
            <div><span>On-the-Way</span><strong>{formatCount(safeData.takeaway.onTheWay)}</strong></div>
            <div><span>DiniTicket orders</span><strong>{formatCount(safeData.diniTicket.orders)}</strong></div>
            <div><span>Waiting tokens</span><strong>{formatCount(safeData.diniTicket.waiting)}</strong></div>
          </div>
        </article>
      </section>

      <section className="stats-grid dashboard-stats ecosystem-finance-stats">
        <StatCard label="Today’s revenue" value={formatMoney(safeData.finance.revenue)} note="Gross paid amount" icon={IndianRupee} tone="gold" />
        <StatCard label="Platform commission" value={formatMoney(safeData.finance.commission)} note="DiniBee revenue" icon={BadgeIndianRupee} tone="green" />
        <StatCard label="Pending settlements" value={formatMoney(safeData.finance.pendingSettlements)} note="Restaurant payable" icon={WalletCards} tone="blue" />
        <StatCard label="Customer support" value={formatCount(safeData.support.customerOpen)} note="Open or in progress" icon={Users} tone="purple" />
        <StatCard label="Owner support" value={formatCount(safeData.support.ownerOpen)} note="Open or in progress" icon={Store} tone="red" />
        <StatCard label="Points issued" value={formatCount(safeData.wallet.issued)} note="Today’s wallet credits" icon={TicketCheck} tone="green" />
        <StatCard label="Points redeemed" value={formatCount(safeData.wallet.redeemed)} note="Today’s bill discounts" icon={CreditCard} tone="purple" />
      </section>

      <section className="ecosystem-chart-grid">
        <article className="panel-card chart-panel">
          <div className="card-title-row"><div><p>Money flow</p><h2>Gross revenue vs commission</h2></div><span className="soft-label">₹ by day</span></div>
          <div className="ecosystem-chart">
            <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={1}>
              <LineChart data={safeData.revenueTrend} margin={{ top: 12, right: 10, left: 2, bottom: 0 }}>
                <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="var(--chart-grid)" />
                <XAxis dataKey="day" axisLine={false} tickLine={false} fontSize={11} />
                <YAxis axisLine={false} tickLine={false} fontSize={11} />
                <Tooltip formatter={(value) => formatMoney(Number(value))} />
                <Legend />
                <Line type="monotone" dataKey="gross" name="Gross paid" stroke="#f5a800" strokeWidth={3} dot={{ r: 3 }} />
                <Line type="monotone" dataKey="commission" name="Commission" stroke="#159a55" strokeWidth={2} dot={{ r: 2 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </article>
        <article className="panel-card chart-panel">
          <div className="card-title-row"><div><p>Service mix</p><h2>Orders by workflow</h2></div></div>
          <div className="ecosystem-chart">
            <ResponsiveContainer width="100%" height="100%" minWidth={0} minHeight={1}>
              <BarChart data={safeData.serviceVolume} margin={{ top: 12, right: 8, left: -18, bottom: 0 }}>
                <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="var(--chart-grid)" />
                <XAxis dataKey="name" axisLine={false} tickLine={false} fontSize={10} />
                <YAxis axisLine={false} tickLine={false} fontSize={11} />
                <Tooltip />
                <Bar dataKey="value" name="Records" fill="#f5a800" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>
      </section>

      <section className="panel-card recent-activity-panel">
        <div className="card-title-row"><div><p>Customer → owner → admin</p><h2>Recent activity</h2></div><Activity /></div>
        <div className="ecosystem-activity-list">
          {safeData.recentActivity.map((item: DashboardSnapshot["recentActivity"][number]) => <div key={item.id}><span className="activity-mark"><Activity /></span><span><strong>{item.title}</strong><small>{item.detail}</small></span><StatusBadge status={item.status} /><time>{item.time}</time></div>)}
        </div>
      </section>
    </div>
  );
}

type RestaurantView = "all" | "pending" | "approved" | "rejected";
type RestaurantAction = "under_review" | "approve" | "reject" | "suspend" | "reactivate" | null;

function restaurantStatusForView(view: RestaurantView): RestaurantApprovalStatus | undefined {
  if (view === "pending") return "PENDING";
  if (view === "approved") return "APPROVED";
  if (view === "rejected") return "REJECTED";
  return undefined;
}

export function RestaurantsManagementPage({ view = "all" }: { view?: RestaurantView }) {
  const status = restaurantStatusForView(view);
  const { data, loading, error, reload } = useApiResource(() => restaurantService.list(status), [status], [] as RestaurantRecord[]);
  const [selected, setSelected] = useState<RestaurantRecord | null>(null);
  const [detailData, setDetailData] = useState<any>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const detailRequest = useRef(0);
  const [action, setAction] = useState<RestaurantAction>(null);
  const rows: DataRow[] = data.map((item: any) => {
    const rawServices = item.requestedServices || item.enabledServices || item.services || [];
    const servicesList = Array.isArray(rawServices)
      ? rawServices.map((service: string) => service === "DINITICKET" ? "DiniTicket" : service).join(", ")
      : "";
    const rawStatus: string = item.suspended ? "SUSPENDED" : (item.approvalStatus || "DRAFT");
    const displayStatus = rawStatus === "DRAFT" ? "SUBMITTED" : rawStatus;
    return {
      id: item.id,
      restaurant: item.restaurantName || "Unnamed Restaurant",
      restaurantSubtitle: item.id,
      owner: item.ownerName || "—",
      phone: item.phone || "—",
      email: item.email || "—",
      location: item.location || item.city || "—",
      services: servicesList || "None",
      created: item.createdAt ? new Date(item.createdAt).toLocaleDateString() : "—",
      status: displayStatus,
    };
  });
  const columns: Column<DataRow>[] = [
    { key: "restaurant", label: "Restaurant", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.restaurantSubtitle)} /> },
    { key: "owner", label: "Owner" }, { key: "phone", label: "Phone" }, { key: "email", label: "Email" },
    { key: "location", label: "Location" }, { key: "services", label: "Business services" }, { key: "created", label: "Created" },
    { key: "status", label: "Approval status", render: (value) => <StatusBadge status={String(value)} /> },
  ];
  const titles: Record<RestaurantView, string> = { all: "All restaurants", pending: "Pending approval", approved: "Approved restaurants", rejected: "Rejected restaurants" };

  async function completeAction() {
    if (!selected || !action || detailLoading || detailError || !detailData) return;
    try {
      // IMPORTANT: Use detailData.version (from GET /admin/restaurants/{id}) because
      // the Summary DTO returned by the list endpoint does NOT include the version field.
      // Sending version=0 (the summary fallback) always causes a 409 Conflict because
      // the restaurant entity is updated many times during onboarding.
      const version = (detailData?.version ?? selected.version ?? (selected as any).v ?? 0) as number;
      if (action === "under_review") await restaurantService.updateApproval(selected.id, "UNDER_REVIEW", "Marked as under review by admin", version);
      if (action === "approve") await restaurantService.updateApproval(selected.id, "APPROVED", "Approved after admin verification", version, detailData?.requestedServices ?? []);
      if (action === "reject") await restaurantService.updateApproval(selected.id, "REJECTED", "Rejected after compliance review", version);
      if (action === "suspend") await restaurantService.setSuspended(selected.id, true, "Suspended by platform administrator", version);
      if (action === "reactivate") await restaurantService.setSuspended(selected.id, false, "Reactivated by platform administrator", version);
      toast.success(
        action === "under_review"
          ? "Application set to Under Review"
          : action === "approve"
          ? "Restaurant approved"
          : action === "reject"
          ? "Restaurant rejected"
          : action === "suspend"
          ? "Restaurant suspended"
          : "Restaurant reactivated"
      );
      setAction(null);
      setSelected(null);
      setDetailData(null);
      await reload();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Unable to update restaurant");
    }
  }


  return (
    <div className="page-stack">
      <PageHeader eyebrow="Restaurant management" title={titles[view]} description="Review restaurant identity, owner information, compliance, service access and approval history." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
      <section className="stats-grid four-up">
        <StatCard label="Total" value={formatCount(data.length)} note="Current result set" icon={Store} />
        <StatCard label="Pending" value={formatCount(data.filter((item: any) => ["SUBMITTED", "UNDER_REVIEW", "PENDING"].includes(item.approvalStatus)).length)} note="Awaiting decision" icon={FileClock} tone="gold" />
        <StatCard label="Approved" value={formatCount(data.filter((item: any) => item.approvalStatus === "APPROVED").length)} note="Eligible for services" icon={CheckCircle2} tone="green" />
        <StatCard label="Suspended" value={formatCount(data.filter((item: any) => item.suspended || item.status === "SUSPENDED" || item.approvalStatus === "SUSPENDED").length)} note="Platform restriction" icon={ShieldAlert} tone="red" />
      </section>
      <DataTable rows={rows} columns={columns} searchPlaceholder="Search restaurant, owner, phone, email or location…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={async (row) => {
        const summary = data.find((item) => item.id === row.id) ?? null;
        setSelected(summary);
        setDetailData(null);
        if (summary?.id) {
          const request = ++detailRequest.current;
          setDetailLoading(true);
          setDetailError(null);
          try {
            const detail = await restaurantService.getDetail(String(summary.id));
            if (request === detailRequest.current) setDetailData(detail);
          } catch (caught) {
            if (request === detailRequest.current) setDetailError(caught instanceof Error ? caught.message : "Unable to load restaurant details");
          } finally {
            if (request === detailRequest.current) setDetailLoading(false);
          }
        }
      }} actionLabel="Open restaurant" emptyTitle="No restaurants found" />

      <Sheet open={Boolean(selected)} onOpenChange={(open) => { if (!open) { setSelected(null); detailRequest.current++; } }}>
        <SheetContent className="ecosystem-sheet restaurant-detail-sheet">
          <SheetHeader><span className="sheet-mark"><Store /></span><SheetTitle>{selected?.restaurantName}</SheetTitle><SheetDescription>{selected?.id} · Complete platform verification record</SheetDescription></SheetHeader>
          {selected ? <div className="ecosystem-sheet-body">
            {detailError ? <p role="alert">{detailError}. Close and reopen this restaurant to retry.</p> : null}
            <div className="detail-identity"><EntityCell title={detailData?.ownerName || selected.ownerName || "—"} subtitle="Restaurant owner" /><StatusBadge status={selected.suspended ? "SUSPENDED" : ((detailData?.approvalStatus ?? selected.approvalStatus) === "DRAFT" ? "SUBMITTED" : (detailData?.approvalStatus ?? selected.approvalStatus))} /></div>

            <DetailSection title="Restaurant & owner information"><DetailGrid fields={[
              { label: "Restaurant", value: detailData?.restaurantName || selected.restaurantName || "—" },
              { label: "Owner", value: detailData?.ownerName || selected.ownerName || "—" },
              { label: "Phone", value: detailData?.phone || selected.phone || "—" },
              { label: "Email", value: detailData?.email || selected.email || "—" },
              { label: "Location", value: detailData ? [detailData.location?.addressLine1, detailData.location?.city, detailData.location?.state, detailData.location?.postalCode].filter(Boolean).join(", ") || "—" : (selected as any).city || "—" },
              { label: "Created", value: selected.createdAt || "—" },
            ]} /></DetailSection>
            <DetailSection title="Business information"><DetailGrid fields={[
              { label: "Legal business", value: detailLoading ? "Loading…" : (detailData?.businessInfo?.businessName || "—") },
              { label: "Registration number", value: detailData?.businessInfo?.businessRegistrationNumber || "—" },
              { label: "GST", value: detailLoading ? "Loading…" : (detailData?.businessInfo?.gstNumber || "—") },
              { label: "FSSAI", value: detailLoading ? "Loading…" : (detailData?.businessInfo?.fssaiNumber || "—") },
              { label: "Operating hours", value: detailLoading ? "Loading…" : (detailData?.operatingHours?.hours?.length ? `${detailData.operatingHours.hours.filter((h: any) => !h.closed).length} days / week` : "—") },
              { label: "Payment account", value: detailLoading ? "Loading…" : (detailData?.settlementProfile ? `${detailData.settlementProfile.bankName} — ${detailData.settlementProfile.maskedAccountNumber || detailData.settlementProfile.accountHolderName || "—"}` : "—") },
              { label: "Business services", value: <ServicePills services={detailData?.services?.map((s: any) => s.serviceType) ?? selected.requestedServices ?? (selected as any).enabledServices ?? []} /> },
            ]} /></DetailSection>
            <DetailSection title="Documents" description="Files remain owned by the restaurant application."><div className="document-status-list">{(detailData?.documents || selected.documents || []).map((document: any) => <div key={document.id ?? document.name}><FileCheck2 /><span>{document.name}</span><StatusBadge status={document.status} /></div>)}</div></DetailSection>
            <DetailSection title="Approval history"><div className="approval-history">{(selected.approvalHistory || []).map((item, index) => <div key={`${item.date}-${index}`}><span><StatusBadge status={item.status} /><strong>{item.note}</strong><small>{item.admin}</small></span><time>{item.date}</time></div>)}</div></DetailSection>
            <div className="monitor-note"><ShieldAlert /><span><strong>Platform oversight only</strong><small>Tables, menu, staff, arrivals and daily restaurant operations remain in the Restaurant Owner App.</small></span></div>
            <div className="sheet-button-row restaurant-actions flex flex-wrap gap-2 pt-2">
              {["SUBMITTED", "PENDING"].includes(selected.approvalStatus as string) ? (
                <>
                  <Button variant="outline" className="border-amber-500/50 text-amber-400 hover:bg-amber-500/10" onClick={() => setAction("under_review")}>Mark Under Review</Button>
                  <Button variant="outline" className="danger-outline" onClick={() => setAction("reject")}>Reject Application</Button>
                  <Button className="gold-button" onClick={() => setAction("approve")}>Approve Restaurant</Button>
                </>
              ) : null}
              {(selected.approvalStatus as string) === "UNDER_REVIEW" ? (
                <>
                  <Button variant="outline" className="danger-outline" onClick={() => setAction("reject")}>Reject Application</Button>
                  <Button className="gold-button" onClick={() => setAction("approve")}>Approve Restaurant</Button>
                </>
              ) : null}
              {selected.approvalStatus === "APPROVED" && !selected.suspended ? (
                <Button variant="outline" className="danger-outline" onClick={() => setAction("suspend")}>Suspend Restaurant</Button>
              ) : null}
              {selected.suspended || (selected.approvalStatus as string) === "SUSPENDED" ? (
                <Button className="gold-button" onClick={() => setAction("reactivate")}>Reactivate Restaurant</Button>
              ) : null}
            </div>
          </div> : null}
        </SheetContent>
      </Sheet>
      <ConfirmActionDialog
        open={Boolean(action)}
        onOpenChange={(open) => !open && setAction(null)}
        title={
          action === "under_review"
            ? "Mark application under review?"
            : action === "approve"
            ? "Approve restaurant application?"
            : action === "reject"
            ? "Reject restaurant application?"
            : action === "suspend"
            ? "Suspend restaurant?"
            : "Reactivate restaurant?"
        }
        description={
          action === "approve"
            ? "The owner will receive account access to the approved business services."
            : action === "under_review"
            ? "The onboarding application status will be updated to UNDER_REVIEW."
            : "This sensitive administration action will be saved in PostgreSQL audit log."
        }
        confirmLabel={
          action === "under_review"
            ? "Mark Under Review"
            : action === "approve"
            ? "Approve"
            : action === "reject"
            ? "Reject"
            : action === "suspend"
            ? "Suspend"
            : "Reactivate"
        }
        destructive={action === "reject" || action === "suspend"}
        onConfirm={completeAction}
      />
    </div>
  );
}

export function ServiceManagementPage() {
  const { data, loading, error, reload } = useApiResource(() => restaurantService.list("APPROVED"), [], [] as RestaurantRecord[]);
  const [selected, setSelected] = useState<RestaurantRecord | null>(null);
  const [detailData, setDetailData] = useState<any>(null);
  const [services, setServices] = useState<BusinessService[]>([]);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const rows: DataRow[] = data.map((item: any) => {
    const rawServices = item.services || item.enabledServices || [];
    const servicesList = Array.isArray(rawServices)
      ? rawServices.map((service: string) => service === "DINITICKET" ? "DiniTicket" : service).join(", ")
      : "";
    return {
      id: item.id,
      restaurant: item.restaurantName || "Unnamed Restaurant",
      restaurantSubtitle: item.id,
      owner: item.ownerName || "—",
      location: item.location || item.city || "—",
      services: servicesList || "None",
      status: item.suspended ? "SUSPENDED" : "APPROVED",
    };
  });

  async function openRestaurant(row: DataRow) {
    const restaurant = data.find((item) => item.id === row.id) ?? null;
    setSelected(restaurant);
    setDetailData(null);
    const rawServices = restaurant?.services || (restaurant as any)?.enabledServices || [];
    setServices(Array.isArray(rawServices) ? rawServices : []);
    // Fetch full detail to get the real optimistic-lock version (Summary DTO has no version).
    if (restaurant?.id) {
      try {
        const detail = await restaurantService.getDetail(String(restaurant.id));
        setDetailData(detail);
      } catch (caught) {
        toast.error(caught instanceof Error ? caught.message : "Unable to load restaurant");
      }
    }
  }

  function toggle(service: BusinessService, checked: boolean) {
    setServices((current) => checked ? Array.from(new Set([...current, service])) : current.filter((item) => item !== service));
  }

  async function save() {
    if (!selected) return;
    if (!services.length) {
      toast.error("Select at least one business service");
      return;
    }
    try {
      // Use detailData.version (real DB version) — Summary DTO has no version field.
      const version = (detailData?.version ?? selected.version ?? (selected as any).v ?? 0) as number;
      await restaurantService.updateServices(selected.id, services, version);
      toast.success("Business services updated", { description: "The owner app will show only the enabled modules." });
      setConfirmOpen(false);
      setSelected(null);
      setDetailData(null);
      await reload();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Unable to update services");
    }
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Restaurant access" title="Business service management" description="Control whether an approved restaurant can use Restaurant, Takeaway and DiniTicket modules." actions={<DataSourceNotice />} />
    <section className="service-policy panel-card"><ShieldAlert /><span><strong>DiniTicket compatibility</strong><small>The frontend always displays DiniTicket. A legacy backend value of QUEUEBEE is mapped to DINITICKET in the service layer.</small></span></section>
    <DataTable rows={rows} columns={[
      { key: "restaurant", label: "Restaurant", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.restaurantSubtitle)} /> }, { key: "owner", label: "Owner" }, { key: "location", label: "Location" }, { key: "services", label: "Enabled services" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search restaurant or enabled service…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => void openRestaurant(row)} actionLabel="Update services" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => { if (!open) { setSelected(null); setDetailData(null); } }}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><Building2 /></span><SheetTitle>Update business services</SheetTitle><SheetDescription>{selected?.restaurantName} · Changes affect owner-app module access</SheetDescription></SheetHeader><div className="ecosystem-sheet-body">
      <div className="service-choice-list">{(["RESTAURANT", "TAKEAWAY", "DINITICKET"] as BusinessService[]).map((service) => <label key={service}><Checkbox checked={services.includes(service)} onCheckedChange={(checked) => toggle(service, Boolean(checked))} /><span><strong>{service === "DINITICKET" ? "DiniTicket" : service.charAt(0) + service.slice(1).toLowerCase()}</strong><small>{service === "RESTAURANT" ? "Tables, reservations and dine-in" : service === "TAKEAWAY" ? "Regular and On-the-Way takeaway" : "Token queue and pickup orders"}</small></span></label>)}</div>
      <div className="monitor-note"><CheckCircle2 /><span><strong>Owner app filtering</strong><small>After backend confirmation, the owner sees common pages plus only these selected service modules.</small></span></div>
      <div className="sheet-button-row"><Button variant="outline" onClick={() => setSelected(null)}>Cancel</Button><Button className="gold-button" onClick={() => setConfirmOpen(true)}>Save service access</Button></div>
    </div></SheetContent></Sheet>
    <ConfirmActionDialog open={confirmOpen} onOpenChange={setConfirmOpen} title="Change restaurant service access?" description="This changes which business modules the restaurant owner can access and creates an audit event." confirmLabel="Confirm services" onConfirm={save} />
  </div>;
}

type ReservationView = "all" | "regular" | "group" | "corporate";

function bookingTypeForView(view: ReservationView): BookingType | undefined {
  if (view === "regular") return "REGULAR";
  if (view === "group") return "GROUP";
  if (view === "corporate") return "CORPORATE";
  return undefined;
}

export function ReservationsManagementPage({ view = "all" }: { view?: ReservationView }) {
  const navigate = useNavigate();
  const bookingType = bookingTypeForView(view);
  const { data: apiData, loading, error, reload } = useApiResource(() => reservationService.list(bookingType), [bookingType], [] as ReservationRecord[]);
  const data = useMemo(() => bookingType ? apiData.filter((item) => String(item.bookingType).toUpperCase() === bookingType) : apiData, [apiData, bookingType]);
  const [selected, setSelected] = useState<ReservationRecord | null>(null);
  const rows: DataRow[] = useMemo(() => data.map((item) => ({
    id: item.id,
    booking: item.bookingNumber,
    customer: item.customerName,
    customerSubtitle: item.customerId,
    restaurant: item.restaurantName,
    bookingType: item.bookingType,
    date: item.date,
    time: item.time,
    guests: item.guestCount,
    table: item.tableId,
    arrival: item.arrivalStatus,
    payment: item.paymentStatus,
    amount: formatMoney(item.amount),
    status: item.status,
  })), [data]);
  const columns: Column<DataRow>[] = [
    { key: "booking", label: "Booking ID", className: "cell-mono" },
    { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.customerSubtitle)} /> },
    { key: "restaurant", label: "Restaurant" }, { key: "bookingType", label: "Booking type", render: (value) => <StatusBadge status={String(value)} /> },
    { key: "date", label: "Date" }, { key: "time", label: "Time" }, { key: "guests", label: "Guests" }, { key: "table", label: "Table" },
    { key: "arrival", label: "Arrival", render: (value) => <StatusBadge status={String(value)} /> }, { key: "payment", label: "Payment", render: (value) => <StatusBadge status={String(value)} /> },
    { key: "amount", label: "Amount", className: "cell-money" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
  ];

  return <div className="page-stack">
    <PageHeader eyebrow="Reservations monitoring" title="Reservations" description="Monitor regular, group and corporate bookings without replacing restaurant-owner operations." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <Tabs value={view} onValueChange={(value) => navigate(value === "all" ? "/reservations" : `/reservations/${value}`)} className="page-tabs"><TabsList><TabsTrigger value="all">All</TabsTrigger><TabsTrigger value="regular">Regular</TabsTrigger><TabsTrigger value="group">Group</TabsTrigger><TabsTrigger value="corporate">Corporate</TabsTrigger></TabsList></Tabs>
    <section className="stats-grid four-up">
      <StatCard label="Bookings" value={formatCount(data.length)} note="Current selection" icon={CalendarCheck2} />
      <StatCard label="Arrived" value={formatCount(data.filter((item) => item.arrivalStatus === "ARRIVED").length)} note="Owner marked arrival" icon={MapPin} tone="green" />
      <StatCard label="Currently dining" value={formatCount(data.filter((item) => item.status === "DINING").length)} note="Admin monitoring only" icon={Coffee} tone="purple" />
      <StatCard label="Payment pending" value={formatCount(data.filter((item) => item.paymentStatus === "PENDING").length)} note="Requires platform visibility" icon={CreditCard} tone="red" />
    </section>
    <DataTable rows={rows} columns={columns} searchPlaceholder="Search booking, customer, restaurant or table…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open booking" emptyTitle="No reservations found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet booking-detail-sheet"><SheetHeader><span className="sheet-mark"><CalendarCheck2 /></span><SheetTitle>{selected?.bookingNumber}</SheetTitle><SheetDescription>{selected?.bookingType} booking · Monitoring and support view</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body">
      <div className="detail-identity"><EntityCell title={selected.customerName} subtitle={selected.customerId} /><StatusBadge status={selected.status} /></div>
      <DetailSection title="Booking details"><DetailGrid fields={[
        { label: "Restaurant", value: selected.restaurantName }, { label: "Date & time", value: `${selected.date} · ${selected.time}` }, { label: "Guests", value: selected.guestCount }, { label: "Table", value: selected.tableId }, { label: "Booking type", value: <StatusBadge status={selected.bookingType} /> }, { label: "Amount", value: formatMoney(selected.amount) },
      ]} /></DetailSection>
      <DetailSection title="Arrival, dining and checkout"><DetailGrid fields={[
        { label: "Arrival", value: <StatusBadge status={selected.arrivalStatus} /> }, { label: "Booking status", value: <StatusBadge status={selected.status} /> }, { label: "Check-in", value: selected.checkInTime ?? "Not checked in" }, { label: "Checkout", value: selected.checkOutTime ?? "Not checked out" }, { label: "Dining duration", value: selected.diningDuration ?? "Not started" }, { label: "Table status", value: <StatusBadge status={selected.tableStatus} /> }, { label: "Payment", value: <StatusBadge status={selected.paymentStatus} /> }, { label: "Reward points", value: selected.rewardPoints }, { label: "Review", value: <StatusBadge status={selected.reviewStatus} /> },
      ]} /></DetailSection>
      {selected.bookingType === "GROUP" ? <DetailSection title="Group booking" description="Every guest keeps an individual cart and payment state."><DetailGrid fields={[
        { label: "Group ID", value: selected.groupId ?? "—" }, { label: "Host", value: selected.customerName }, { label: "Ordering deadline", value: selected.orderingDeadline ?? "—" }, { label: "Ordering status", value: <StatusBadge status={selected.orderingStatus ?? "OPEN"} /> }, { label: "Payment mode", value: selected.paymentMode?.replaceAll("_", " ") ?? "—" }, { label: "Combined total", value: formatMoney(selected.groupGuests?.reduce((sum, guest) => sum + guest.orderAmount, 0) ?? 0) },
      ]} /><div className="participant-list">{selected.groupGuests?.map((guest) => <div key={guest.id}><span><strong>{guest.name}</strong><small>{guest.joined ? "Joined" : "Waiting to join"} · Individual order {formatMoney(guest.orderAmount)}</small></span><StatusBadge status={guest.paymentStatus} /></div>)}</div></DetailSection> : null}
      {selected.bookingType === "CORPORATE" ? <DetailSection title="Corporate event & invoice"><DetailGrid fields={[
        { label: "Company", value: selected.companyName ?? "—" }, { label: "Corporate account", value: selected.corporateAccount ?? "—" }, { label: "Contact person", value: selected.contactPerson ?? "—" }, { label: "Event", value: selected.eventName ?? "—" }, { label: "Per-person budget", value: formatMoney(selected.perPersonBudget ?? 0) }, { label: "Total budget", value: formatMoney(selected.totalBudget ?? 0) }, { label: "Billing method", value: selected.billingMethod?.replaceAll("_", " ") ?? "—" }, { label: "Recurrence", value: selected.recurrence?.replaceAll("_", " ") ?? "—" }, { label: "Invoice terms", value: selected.invoiceTerms?.replaceAll("_", " ") ?? "—" },
      ]} /></DetailSection> : null}
      <div className="monitor-note"><ShieldAlert /><span><strong>Read and support access</strong><small>Customer arrival, check-in, dining start and table cleaning are controlled by the customer and restaurant-owner workflows. No normal admin override is provided here.</small></span></div>
      <div className="sheet-button-row"><Button variant="outline" onClick={() => setSelected(null)}>Close</Button></div>
    </div> : null}</SheetContent></Sheet>
  </div>;
}
