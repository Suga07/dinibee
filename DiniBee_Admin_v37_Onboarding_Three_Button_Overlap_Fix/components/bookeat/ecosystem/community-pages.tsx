"use client";

import { useMemo, useRef, useState, type FormEvent } from "react";
import {
  BarChart3,
  CalendarCheck2,
  CheckCircle2,
  Clock3,
  CreditCard,
  Download,
  FileText,
  Headphones,
  Image as ImageIcon,
  Paperclip,
  RefreshCw,
  Send,
  ShieldAlert,
  ShoppingBag,
  Star,
  Store,
  TicketCheck,
  Users,
  WalletCards,
  XCircle,
} from "lucide-react";
import { useNavigate } from "@/lib/bookeat/router";
import { toast } from "sonner";

import { DataTable, EntityCell, PageHeader, StatCard, StatusBadge, type DataRow } from "@/components/bookeat/shared";
import { ConfirmActionDialog, DataSourceNotice, DetailGrid, DetailSection, formatCount, formatMoney } from "@/components/bookeat/ecosystem/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { useApiResource } from "@/hooks/use-api-resource";
import { reportService, reviewService, supportService } from "@/lib/bookeat/services";
import type { DashboardSnapshot, ReviewRecord, SupportStatus, SupportTicketRecord, SupportUserType } from "@/lib/bookeat/types";

export function ReviewsManagementPage() {
  const { data, loading, error, reload } = useApiResource(() => reviewService.list(), [], [] as ReviewRecord[]);
  const [selected, setSelected] = useState<ReviewRecord | null>(null);
  const [moderation, setModeration] = useState<"HIDDEN" | "REMOVED" | null>(null);
  const rows: DataRow[] = data.map((item) => ({ id: item.id, customer: item.customerName, restaurant: item.restaurantName, reference: item.referenceId, rating: item.restaurantRating, food: item.foodQuality, service: item.service, ambience: item.ambience, value: item.valueForMoney, review: item.reviewText, images: item.imageCount, created: item.createdAt, status: item.status }));

  async function moderate() {
    if (!selected || !moderation) return;
    try {
      await reviewService.moderate(selected.id, moderation, "Admin moderation after content review");
      toast.success(moderation === "HIDDEN" ? "Review hidden" : "Review removed");
      setModeration(null); setSelected(null); await reload();
    } catch (caught) { toast.error(caught instanceof Error ? caught.message : "Unable to moderate review"); }
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Trust & safety" title="Reviews & ratings" description="Inspect restaurant, food, service, ambience and value ratings; moderate only when platform policy requires it." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <section className="stats-grid four-up"><StatCard label="Reviews" value={formatCount(data.length)} note="Current result set" icon={Star} /><StatCard label="Average rating" value={data.length ? (data.reduce((sum, item) => sum + item.restaurantRating, 0) / data.length).toFixed(1) : "0.0"} note="Restaurant rating" icon={Star} tone="green" /><StatCard label="Reported" value={formatCount(data.filter((item) => item.status === "REPORTED").length)} note="Needs moderation review" icon={ShieldAlert} tone="red" /><StatCard label="With images" value={formatCount(data.filter((item) => item.imageCount > 0).length)} note="Customer uploads" icon={ImageIcon} tone="blue" /></section>
    <DataTable rows={rows} columns={[
      { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.reference)} /> }, { key: "restaurant", label: "Restaurant" }, { key: "rating", label: "Restaurant", render: (value) => <span className="rating-cell"><Star />{value}</span> }, { key: "food", label: "Food" }, { key: "service", label: "Service" }, { key: "ambience", label: "Ambience" }, { key: "value", label: "Value" }, { key: "review", label: "Review text" }, { key: "images", label: "Images" }, { key: "created", label: "Created" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search customer, restaurant, reference or review…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Review content" emptyTitle="No reviews found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><Star /></span><SheetTitle>Review {selected?.id}</SheetTitle><SheetDescription>{selected?.restaurantName} · {selected?.referenceId}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><div className="detail-identity"><EntityCell title={selected.customerName} subtitle="Verified customer" /><StatusBadge status={selected.status} /></div><DetailSection title="Rating breakdown"><div className="rating-breakdown">{[
      ["Restaurant", selected.restaurantRating], ["Food quality", selected.foodQuality], ["Service", selected.service], ["Ambience", selected.ambience], ["Value for money", selected.valueForMoney],
    ].map(([label, value]) => <div key={String(label)}><span>{label}</span><strong><Star />{value}/5</strong></div>)}</div></DetailSection><DetailSection title="Customer review"><blockquote>{selected.reviewText}</blockquote><small className="image-count"><ImageIcon />{selected.imageCount} customer image{selected.imageCount === 1 ? "" : "s"}</small></DetailSection><div className="monitor-note"><ShieldAlert /><span><strong>Moderation policy</strong><small>Do not rewrite legitimate customer reviews. Hide or remove only for policy, safety or privacy reasons.</small></span></div><div className="sheet-button-row"><Button variant="outline" className="danger-outline" onClick={() => setModeration("REMOVED")}>Remove</Button><Button variant="outline" onClick={() => setModeration("HIDDEN")}>Hide review</Button></div></div> : null}</SheetContent></Sheet>
    <ConfirmActionDialog open={Boolean(moderation)} onOpenChange={(open) => !open && setModeration(null)} title={moderation === "REMOVED" ? "Remove this review?" : "Hide this review?"} description="This moderation action must be auditable and should be used only after checking the reported content." confirmLabel={moderation === "REMOVED" ? "Remove review" : "Hide review"} destructive onConfirm={moderate} />
  </div>;
}

type SupportView = "all" | "customers" | "owners" | "open" | "in-progress" | "resolved" | "closed";

function supportFilter(view: SupportView): { userType?: SupportUserType; status?: SupportStatus } {
  if (view === "customers") return { userType: "CUSTOMER" };
  if (view === "owners") return { userType: "OWNER" };
  if (view === "open") return { status: "OPEN" };
  if (view === "in-progress") return { status: "IN_PROGRESS" };
  if (view === "resolved") return { status: "RESOLVED" };
  if (view === "closed") return { status: "CLOSED" };
  return {};
}

export function SupportCenterPage({ view = "all" }: { view?: SupportView }) {
  const navigate = useNavigate();
  const filter = useMemo(() => supportFilter(view), [view]);
  const { data, loading, error, reload } = useApiResource(() => supportService.list(filter.userType, filter.status), [view], [] as SupportTicketRecord[]);
  const [selected, setSelected] = useState<SupportTicketRecord | null>(null);
  const [message, setMessage] = useState("");
  const [attachmentName, setAttachmentName] = useState("");
  const [closeConfirm, setCloseConfirm] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const rows: DataRow[] = data.map((item) => ({ id: item.id, ticket: item.ticketNumber, user: item.userName, userSubtitle: item.userType, userType: item.userType, restaurant: item.restaurantName ?? "—", category: item.category, reference: item.relatedEntityId ?? "—", lastMessage: item.lastMessage, created: item.createdAt, updated: item.updatedAt, assigned: item.assignedAdminName, status: item.status }));

  async function sendMessage(event: FormEvent) {
    event.preventDefault();
    if (!selected || !message.trim()) return;
    try {
      await supportService.sendMessage(selected.id, message.trim());
      setMessage(""); setAttachmentName("");
      setSelected(await supportService.get(selected.id));
      toast.success("Reply sent");
      await reload();
    } catch (caught) { toast.error(caught instanceof Error ? caught.message : "Unable to send reply"); }
  }

  async function changeStatus(status: SupportStatus) {
    if (!selected) return;
    try {
      const updated = await supportService.updateStatus(selected.id, status, selected.version ?? 0);
      setSelected(updated); setCloseConfirm(false); toast.success(`Ticket marked ${status.replaceAll("_", " ").toLowerCase()}`); await reload();
    } catch (caught) { toast.error(caught instanceof Error ? caught.message : "Unable to update ticket"); }
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Unified support center" title="Help & support" description="Receive direct chat sessions from customers and restaurant owners in one audited support workflow." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <Tabs value={view} onValueChange={(value) => navigate(value === "all" ? "/support-tickets" : `/support/${value}`)} className="page-tabs"><TabsList><TabsTrigger value="all">All</TabsTrigger><TabsTrigger value="customers">Customers</TabsTrigger><TabsTrigger value="owners">Restaurant Owners</TabsTrigger><TabsTrigger value="open">Open</TabsTrigger><TabsTrigger value="in-progress">In Progress</TabsTrigger><TabsTrigger value="resolved">Resolved</TabsTrigger><TabsTrigger value="closed">Closed</TabsTrigger></TabsList></Tabs>
    <section className="stats-grid four-up"><StatCard label="Tickets" value={formatCount(data.length)} note="Current selection" icon={Headphones} /><StatCard label="Customer tickets" value={formatCount(data.filter((item) => item.userType === "CUSTOMER").length)} note="Customer App chats" icon={Users} tone="blue" /><StatCard label="Owner tickets" value={formatCount(data.filter((item) => item.userType === "OWNER").length)} note="Restaurant Owner App chats" icon={Store} tone="purple" /><StatCard label="Open / in progress" value={formatCount(data.filter((item) => ["OPEN", "IN_PROGRESS"].includes(item.status)).length)} note="Needs admin attention" icon={Clock3} tone="red" /></section>
    <DataTable rows={rows} columns={[
      { key: "ticket", label: "Ticket ID", className: "cell-mono" }, { key: "user", label: "User", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.userType).replace("OWNER", "Restaurant owner")} /> }, { key: "restaurant", label: "Restaurant" }, { key: "category", label: "Issue category" }, { key: "reference", label: "Related booking / order" }, { key: "lastMessage", label: "Last message" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> }, { key: "created", label: "Created" }, { key: "updated", label: "Updated" }, { key: "assigned", label: "Assigned admin" },
    ]} searchPlaceholder="Search ticket, user, restaurant, category or reference…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={async (row) => { try { setSelected(await supportService.get(String(row.id))); } catch (e) { toast.error(e instanceof Error ? e.message : "Unable to open support chat"); } }} actionLabel="Open support chat" emptyTitle="No support tickets found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet support-center-sheet"><SheetHeader><span className="sheet-mark"><Headphones /></span><SheetTitle>{selected?.ticketNumber}</SheetTitle><SheetDescription>{selected?.userType} · {selected?.category}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body support-body"><div className="ticket-context"><EntityCell title={selected.userName} subtitle={selected.restaurantName ?? selected.userType} /><StatusBadge status={selected.status} /></div><DetailGrid fields={[
      { label: "Issue category", value: selected.category }, { label: "Related record", value: selected.relatedEntityId ?? "Not linked" }, { label: "Assigned admin", value: selected.assignedAdminName }, { label: "Updated", value: selected.updatedAt },
    ]} /><div className="support-conversation">{(selected.messages || []).map((item) => <div key={item.id} className={`support-message ${(item.senderType || "").toLowerCase()}`}><span>{String(item.senderName || "User").split(" ").filter(Boolean).map((part) => part[0]).slice(0, 2).join("").toUpperCase()}</span><div><strong>{item.senderName || "User"}<small>{item.senderType} · {item.createdAt}</small></strong><p>{item.message}</p>{item.attachmentName ? <em><Paperclip />{item.attachmentName}</em> : null}</div></div>)}</div><form className="support-reply" onSubmit={sendMessage}><Textarea value={message} onChange={(event) => setMessage(event.target.value)} placeholder="Reply directly to the customer or restaurant owner…" /><input ref={fileInput} type="file" hidden onChange={(event) => setAttachmentName(event.target.files?.[0]?.name ?? "")} /><div><Button type="button" variant="outline" onClick={() => fileInput.current?.click()}><Paperclip />{attachmentName || "Attach file"}</Button><Button className="gold-button" type="submit"><Send />Send</Button></div></form><div className="support-status-actions">{selected.status === "OPEN" ? <Button variant="outline" onClick={() => void changeStatus("IN_PROGRESS")}>Mark in progress</Button> : null}{selected.status !== "RESOLVED" && selected.status !== "CLOSED" ? <Button variant="outline" onClick={() => void changeStatus("RESOLVED")}><CheckCircle2 />Resolve</Button> : null}{selected.status !== "CLOSED" ? <Button className="danger-button" onClick={() => setCloseConfirm(true)}><XCircle />Close</Button> : <Button variant="outline" onClick={() => void changeStatus("OPEN")}>Reopen</Button>}</div></div> : null}</SheetContent></Sheet>
    <ConfirmActionDialog open={closeConfirm} onOpenChange={setCloseConfirm} title="Close support ticket?" description="The complete chat history will be retained. A new customer or owner message may reopen the ticket." confirmLabel="Close ticket" destructive onConfirm={() => changeStatus("CLOSED")} />
  </div>;
}

const emptyReport: DashboardSnapshot = { restaurants: { total: 0, pending: 0, approved: 0 }, customers: 0, reservations: { today: 0, regular: 0, group: 0, corporate: 0 }, dining: { active: 0, checkedIn: 0, dining: 0, occupiedTables: 0 }, takeaway: { total: 0, onTheWay: 0 }, diniTicket: { orders: 0, waiting: 0 }, finance: { revenue: 0, commission: 0, pendingSettlements: 0 }, support: { customerOpen: 0, ownerOpen: 0 }, wallet: { issued: 0, redeemed: 0 }, recentActivity: [], serviceVolume: [], revenueTrend: [] };

export function EcosystemAnalyticsPage() {
  const [period, setPeriod] = useState<"TODAY" | "WEEK" | "MONTH" | "CUSTOM">("MONTH");
  const [serviceType, setServiceType] = useState("ALL");
  const [bookingType, setBookingType] = useState("ALL");
  const { data, loading, error, reload } = useApiResource(() => reportService.getOverview({ period, ...(serviceType !== "ALL" ? { serviceType } : {}), ...(bookingType !== "ALL" ? { bookingType } : {}) }), [period, serviceType, bookingType], emptyReport);

  const safeData = useMemo(() => {
    const raw:any = data || emptyReport; const flat = raw.totalRestaurants !== undefined;
    return {
      ...emptyReport, ...raw,
      restaurants: flat ? {total:raw.totalRestaurants??0,pending:raw.pendingRestaurants??0,approved:raw.approvedRestaurants??0} : {...emptyReport.restaurants,...(raw.restaurants||{})},
      customers: flat ? (raw.totalCustomers??0) : (raw.customers??0),
      reservations: flat ? {...emptyReport.reservations,today:raw.totalReservations??raw.todayReservations??0} : {...emptyReport.reservations,...(raw.reservations||{})},
      takeaway: flat ? {...emptyReport.takeaway,total:raw.totalOrders??0} : {...emptyReport.takeaway,...(raw.takeaway||{})},
      diniTicket: flat ? {orders:raw.activeDiniTickets??0,waiting:raw.activeDiniTickets??0} : {...emptyReport.diniTicket,...(raw.diniTicket||{})},
      finance: flat ? {revenue:Number(raw.totalRevenue??0),commission:Number(raw.platformCommission??0),pendingSettlements:raw.pendingSettlements??0} : {...emptyReport.finance,...(raw.finance||{})},
      support: flat ? {customerOpen:raw.openSupportTickets??0,ownerOpen:0} : {...emptyReport.support,...(raw.support||{})},
      wallet: {...emptyReport.wallet,...(raw.wallet||{})}, dining:{...emptyReport.dining,...(raw.dining||{})}
    };
  }, [data]);

  const reports = [
    ["Restaurant registrations", "Approval, rejection and service access", Store,"RESTAURANTS"], ["Customer registrations", "Acquisition and account health", Users,"CUSTOMERS"], ["Reservation report", "Regular, Group and Corporate", CalendarCheck2,"RESERVATIONS"], ["Food order revenue", "Dine-in participant and combined orders", FileText,"ORDERS"], ["Takeaway report", "Regular and On-the-Way", ShoppingBag,"TAKEAWAY"], ["DiniTicket report", "Queue, completion and no-show", TicketCheck,"DINITICKET"], ["Payment report", "Paid, failed and reward discount", CreditCard,"PAYMENTS"], ["Settlement report", "Commission, fees and restaurant payable", WalletCards,"SETTLEMENTS"], ["Wallet report", "Issued, redeemed and expired points", TicketCheck,"WALLET"], ["Support report", "Open, resolved and response time", Headphones,"SUPPORT"], ["Review report", "Ratings and reported content", Star,"REVIEWS"],
  ] as const;
  return <div className="page-stack">
    <PageHeader eyebrow="Reports & analytics" title="DiniBee ecosystem analytics" description="Analyze every customer, restaurant, booking, order, payment, reward and support workflow." actions={<DataSourceNotice />} />
    <section className="panel-card report-filter-bar"><label>Period<Select value={period} onValueChange={(value) => setPeriod(value as typeof period)}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="TODAY">Today</SelectItem><SelectItem value="WEEK">This Week</SelectItem><SelectItem value="MONTH">This Month</SelectItem><SelectItem value="CUSTOM">Custom Range</SelectItem></SelectContent></Select></label>{period === "CUSTOM" ? <><label>From<Input type="date" /></label><label>To<Input type="date" /></label></> : null}<label>Service<Select value={serviceType} onValueChange={setServiceType}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ALL">All services</SelectItem><SelectItem value="RESTAURANT">Restaurant</SelectItem><SelectItem value="TAKEAWAY">Takeaway</SelectItem><SelectItem value="DINITICKET">DiniTicket</SelectItem></SelectContent></Select></label><label>Booking type<Select value={bookingType} onValueChange={setBookingType}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ALL">All bookings</SelectItem><SelectItem value="REGULAR">Regular</SelectItem><SelectItem value="GROUP">Group</SelectItem><SelectItem value="CORPORATE">Corporate</SelectItem></SelectContent></Select></label><Button variant="outline" onClick={() => void reload()}><RefreshCw />Apply</Button></section>
    {error ? <div className="page-error panel-card"><ShieldAlert /><span><strong>Unable to load reports</strong><small>{error}</small></span><Button variant="outline" onClick={() => void reload()}>Retry</Button></div> : null}
    <section className="stats-grid dashboard-stats"><StatCard label="Restaurants" value={loading ? "—" : formatCount(safeData.restaurants.total)} note="Registration report" icon={Store} /><StatCard label="Reservations" value={formatCount(safeData.reservations.today)} note="All booking types" icon={CalendarCheck2} tone="purple" /><StatCard label="Takeaway" value={formatCount(safeData.takeaway.total)} note={`${safeData.takeaway.onTheWay} On-the-Way`} icon={ShoppingBag} tone="blue" /><StatCard label="DiniTicket" value={formatCount(safeData.diniTicket.orders)} note={`${safeData.diniTicket.waiting} waiting`} icon={TicketCheck} tone="gold" /><StatCard label="Gross payments" value={formatMoney(safeData.finance.revenue)} note="Paid gateway amount" icon={CreditCard} tone="green" /><StatCard label="Commission" value={formatMoney(safeData.finance.commission)} note="Platform revenue" icon={BarChart3} tone="green" /><StatCard label="Wallet issued" value={formatCount(safeData.wallet.issued)} note={`${safeData.wallet.redeemed} redeemed`} icon={TicketCheck} tone="purple" /><StatCard label="Open support" value={formatCount(safeData.support.customerOpen + safeData.support.ownerOpen)} note="Customer + owner" icon={Headphones} tone="red" /></section>
    <section className="report-grid ecosystem-report-grid">{reports.map(([name, description, Icon, reportType]) => <article key={name} className="panel-card report-card"><span><Icon /></span><div><strong>{name}</strong><small>{description}</small></div><Button variant="ghost" aria-label={`Export ${name}`} onClick={async () => { try { await reportService.exportPdf(reportType,{period,serviceType,bookingType}); toast.success(`${name} PDF downloaded`); } catch(e) { toast.error(e instanceof Error ? e.message : "PDF download failed"); } }}><Download /></Button></article>)}</section>
  </div>;
}
