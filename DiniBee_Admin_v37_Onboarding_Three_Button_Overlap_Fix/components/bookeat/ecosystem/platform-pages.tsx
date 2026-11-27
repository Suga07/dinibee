"use client";

import { useState, type FormEvent } from "react";
import {
  Bell,
  BellRing,
  CalendarClock,
  CheckCircle2,
  ChefHat,
  CreditCard,
  FileClock,
  Headphones,
  RefreshCw,
  Send,
  ShieldCheck,
  Sparkles,
  Users,
  WalletCards,
  XCircle,
} from "lucide-react";
import { useNavigate } from "@/lib/bookeat/router";
import { toast } from "sonner";

import { DataTable, EntityCell, PageHeader, StatCard, StatusBadge, type DataRow } from "@/components/bookeat/shared";
import { ConfirmActionDialog, DataSourceNotice, DetailGrid, DetailSection, formatCount, formatMoney } from "@/components/bookeat/ecosystem/shared";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Textarea } from "@/components/ui/textarea";
import { useApiResource } from "@/hooks/use-api-resource";
import { notificationService, refundService } from "@/lib/bookeat/services";
import type { NotificationAudience, NotificationRecord, NotificationType, RefundRecord } from "@/lib/bookeat/types";

export function RefundsManagementPage() {
  const navigate = useNavigate();
  const { data, loading, error, reload } = useApiResource(() => refundService.list(), [], [] as RefundRecord[]);
  const [selected, setSelected] = useState<RefundRecord | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const rows: DataRow[] = data.map((item) => ({
    id: item.id,
    refund: item.refundId,
    transaction: item.originalTransactionId,
    reference: item.relatedEntityId,
    ticket: item.relatedTicketId ?? "—",
    customer: item.customerName,
    restaurant: item.restaurantName,
    service: item.serviceType,
    amount: formatMoney(item.refundAmount),
    reason: item.reason,
    requested: item.requestedAt,
    status: item.status,
  }));

  async function submitRefund() {
    if (!selected) return;
    try {
      const updated = await refundService.submit(selected.id);
      setSelected(updated);
      setConfirmOpen(false);
      toast.success("Refund submitted for backend processing", { description: "Completion still requires a verified payment-gateway response." });
      await reload();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Unable to submit refund");
    }
  }

  return <div className="page-stack">
    <PageHeader eyebrow="Payment exception management" title="Refunds" description="Review support-linked refund requests and submit approved cases to the backend payment workflow." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <section className="stats-grid four-up">
      <StatCard label="Refund requests" value={formatCount(data.length)} note="Current result set" icon={CreditCard} />
      <StatCard label="Awaiting review" value={formatCount(data.filter((item) => item.status === "REQUESTED").length)} note="Admin action required" icon={FileClock} tone="gold" />
      <StatCard label="Processing" value={formatCount(data.filter((item) => item.status === "PROCESSING").length)} note="Waiting for gateway" icon={RefreshCw} tone="blue" />
      <StatCard label="Completed value" value={formatMoney(data.filter((item) => item.status === "COMPLETED").reduce((sum, item) => sum + item.refundAmount, 0))} note="Backend-confirmed refunds" icon={CheckCircle2} tone="green" />
    </section>
    <DataTable rows={rows} columns={[
      { key: "refund", label: "Refund ID", className: "cell-mono" },
      { key: "transaction", label: "Original transaction", className: "cell-mono" },
      { key: "reference", label: "Order / booking" },
      { key: "ticket", label: "Support ticket" },
      { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.service).replaceAll("_", " ")} /> },
      { key: "restaurant", label: "Restaurant" },
      { key: "amount", label: "Refund amount", className: "cell-money" },
      { key: "reason", label: "Reason" },
      { key: "requested", label: "Requested" },
      { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search refund, transaction, ticket, customer or restaurant…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open refund" emptyTitle="No refund requests found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><CreditCard /></span><SheetTitle>{selected?.refundId}</SheetTitle><SheetDescription>Refund review · {selected?.relatedEntityId}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><DetailSection title="Refund request"><DetailGrid fields={[
      { label: "Original transaction", value: selected.originalTransactionId }, { label: "Related order / booking", value: selected.relatedEntityId }, { label: "Related support ticket", value: selected.relatedTicketId ?? "Not linked" }, { label: "Customer", value: selected.customerName }, { label: "Restaurant", value: selected.restaurantName }, { label: "Service", value: selected.serviceType.replaceAll("_", " ") }, { label: "Original amount", value: formatMoney(selected.originalAmount) }, { label: "Refund amount", value: formatMoney(selected.refundAmount) }, { label: "Status", value: <StatusBadge status={selected.status} /> }, { label: "Requested", value: selected.requestedAt }, { label: "Processed", value: selected.processedAt ?? "Not completed" }, { label: "Gateway reference", value: selected.gatewayReference ?? "Awaiting backend" },
    ]} /></DetailSection><DetailSection title="Reason"><p className="detail-copy">{selected.reason}</p></DetailSection><div className="monitor-note"><ShieldCheck /><span><strong>Gateway-confirmed status only</strong><small>The frontend submits the request but never marks it completed. A verified backend callback must update payment, refund and support records.</small></span></div><div className="sheet-button-row"><Button variant="outline" onClick={() => navigate("/payments")}>Open payments</Button>{selected.relatedTicketId ? <Button variant="outline" onClick={() => navigate("/support-tickets")}><Headphones />Open support</Button> : null}{selected.status === "REQUESTED" ? <Button className="gold-button" onClick={() => setConfirmOpen(true)}>Submit refund</Button> : null}</div></div> : null}</SheetContent></Sheet>
    <ConfirmActionDialog open={confirmOpen} onOpenChange={setConfirmOpen} title="Submit this refund?" description="The request will be sent to the backend refund API. It remains processing until the payment gateway confirms the result." confirmLabel="Submit to backend" destructive onConfirm={submitRefund} />
  </div>;
}

const initialNotification = {
  title: "",
  message: "",
  type: "PROMOTION" as NotificationType,
  audience: "ALL_CUSTOMERS" as NotificationAudience,
  recipients: "",
  scheduledAt: "",
};

const notificationTemplates = [
  {
    name: "Weekend 20% Off",
    title: "Weekend Feast - Flat 20% Off!",
    message: "Reserve your table today at any participating restaurant and get 20% off your food bill. Use code DINIBEE20.",
    type: "PROMOTION" as NotificationType,
    audience: "ALL_CUSTOMERS" as NotificationAudience,
  },
  {
    name: "Takeaway Ready",
    title: "Your Takeaway Order is Ready!",
    message: "Your takeaway order is freshly prepared and packaged. Please show your token at the pickup counter.",
    type: "TAKEAWAY" as NotificationType,
    audience: "ALL_CUSTOMERS" as NotificationAudience,
  },
  {
    name: "Booking Confirmed",
    title: "Table Reservation Confirmed!",
    message: "Your table reservation has been confirmed by the restaurant host. We look forward to hosting you.",
    type: "BOOKING" as NotificationType,
    audience: "ALL_CUSTOMERS" as NotificationAudience,
  },
  {
    name: "Restaurant Notice",
    title: "Peak Hours Live Table Sync",
    message: "Please ensure all floor tables and reservations are synchronized with the DiniBee live waitlist queue.",
    type: "SYSTEM_NOTICE" as NotificationType,
    audience: "ALL_RESTAURANTS" as NotificationAudience,
  },
];

export function NotificationsManagementPage() {
  const { data, loading, error, reload } = useApiResource(() => notificationService.list(), [], [] as NotificationRecord[]);
  const [selected, setSelected] = useState<NotificationRecord | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [form, setForm] = useState(initialNotification);

  const rows: DataRow[] = data.map((item) => ({
    id: item.id,
    title: item.title,
    message: item.message,
    type: item.type,
    audience: item.audience,
    recipients: item.recipientCount ?? "Calculated by backend",
    createdBy: item.createdBy,
    created: item.createdAt,
    schedule: item.scheduledAt ?? "Immediate",
    status: item.status,
  }));

  const selectedAudience = form.audience.startsWith("SELECTED_");

  function applyTemplate(tmpl: typeof notificationTemplates[number]) {
    setForm((current) => ({
      ...current,
      title: tmpl.title,
      message: tmpl.message,
      type: tmpl.type,
      audience: tmpl.audience,
    }));
    toast.info(`Applied "${tmpl.name}" template`);
  }

  function reviewSend(event: FormEvent) {
    event.preventDefault();
    if (!form.title.trim() || !form.message.trim() || (selectedAudience && !form.recipients.trim())) {
      toast.error("Complete the title, message and selected recipients");
      return;
    }
    setConfirmOpen(true);
  }

  async function sendNotification() {
    setIsSubmitting(true);
    try {
      const res = await notificationService.send({
        title: form.title.trim(),
        message: form.message.trim(),
        type: form.type,
        audience: form.audience,
        recipientIds: selectedAudience ? form.recipients.split(",").map((value) => value.trim()).filter(Boolean) : undefined,
        scheduledAt: form.scheduledAt || undefined,
      });
      setConfirmOpen(false);
      setForm(initialNotification);
      toast.success("Notification broadcasted successfully!", {
        description: `Delivered live via backend engine to ${form.audience.replaceAll("_", " ").toLowerCase()}.`,
      });
      await reload();
    } catch (caught) {
      toast.error(caught instanceof Error ? caught.message : "Unable to broadcast notification");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        eyebrow="Live Communication Engine"
        title="Notifications"
        description="Broadcast instant in-app alerts, promotions, table updates and order notifications to customer apps and restaurant owners."
        actions={
          <>
            <DataSourceNotice />
            <Button variant="outline" onClick={() => void reload()}>
              <RefreshCw />Refresh
            </Button>
          </>
        }
      />

      <section className="stats-grid four-up">
        <StatCard label="Total Broadcasts" value={formatCount(data.length)} note="Live database records" icon={Bell} />
        <StatCard label="Customer Audiences" value={formatCount(data.filter((item) => item.audience.includes("CUSTOMERS")).length)} note="All or targeted customers" icon={Users} tone="blue" />
        <StatCard label="Restaurant Audiences" value={formatCount(data.filter((item) => item.audience.includes("RESTAURANTS")).length)} note="All or targeted owners" icon={WalletCards} tone="purple" />
        <StatCard label="Live Engine Status" value="Active" note="Spring Boot broadcast service" icon={CheckCircle2} tone="green" />
      </section>

      {/* Live Composer and Interactive Phone Preview */}
      <section className="notification-layout">
        <form className="panel-card notification-composer" onSubmit={reviewSend}>
          <div className="card-title-row">
            <div>
              <p>Broadcast composer</p>
              <h2>Create notification</h2>
            </div>
            <span className="notification-icon"><BellRing /></span>
          </div>

          <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", alignItems: "center" }}>
            <span style={{ fontSize: "11px", color: "var(--muted-foreground)", display: "flex", alignItems: "center", gap: "4px" }}>
              <Sparkles style={{ width: "13px", height: "13px", color: "var(--gold)" }} /> Quick templates:
            </span>
            {notificationTemplates.map((tmpl) => (
              <button
                key={tmpl.name}
                type="button"
                onClick={() => applyTemplate(tmpl)}
                style={{
                  fontSize: "10px",
                  padding: "3px 8px",
                  borderRadius: "6px",
                  border: "1px solid var(--border)",
                  background: "var(--muted)",
                  cursor: "pointer",
                }}
              >
                {tmpl.name}
              </button>
            ))}
          </div>

          <div className="form-two-col">
            <label>
              Audience
              <Select value={form.audience} onValueChange={(audience) => setForm((current) => ({ ...current, audience: audience as NotificationAudience }))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL_CUSTOMERS">All customers (Live App)</SelectItem>
                  <SelectItem value="SELECTED_CUSTOMERS">Selected customers</SelectItem>
                  <SelectItem value="ALL_RESTAURANTS">All restaurant owners</SelectItem>
                  <SelectItem value="SELECTED_RESTAURANTS">Selected restaurants</SelectItem>
                </SelectContent>
              </Select>
            </label>

            <label>
              Notification type
              <Select value={form.type} onValueChange={(type) => setForm((current) => ({ ...current, type: type as NotificationType }))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {["PROMOTION", "BOOKING", "TAKEAWAY", "PAYMENT", "REFUND", "WALLET_POINTS", "SUPPORT_TICKET", "RESTAURANT_APPROVAL", "SYSTEM_NOTICE"].map((type) => (
                    <SelectItem key={type} value={type}>{type.replaceAll("_", " ")}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </label>
          </div>

          {selectedAudience ? (
            <label>
              Recipient UUIDs
              <Input
                value={form.recipients}
                onChange={(event) => setForm((current) => ({ ...current, recipients: event.target.value }))}
                placeholder="UUID1, UUID2 (e.g. d1f0efef-6c1c-43bb-bcdc-a4bf57a35278)"
              />
            </label>
          ) : null}

          <label>
            Title
            <Input
              required
              maxLength={90}
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              placeholder="e.g. Special Weekend Feast - Flat 20% Off!"
            />
            <small>{form.title.length}/90 characters</small>
          </label>

          <label>
            Message
            <Textarea
              required
              maxLength={500}
              value={form.message}
              onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))}
              placeholder="Write the notification message that will appear on customer devices..."
            />
            <small>{form.message.length}/500 characters</small>
          </label>

          <Button className="gold-button composer-submit" type="submit" disabled={isSubmitting}>
            <Send />
            {isSubmitting ? "Broadcasting…" : "Send notification to live apps"}
          </Button>
        </form>

        <aside className="panel-card notification-preview">
          <div className="card-title-row">
            <div>
              <p>Live device preview</p>
              <h2>Customer phone preview</h2>
            </div>
          </div>

          <div className="phone-preview">
            <div className="phone-status">
              <span>9:41</span>
              <i />
            </div>
            <div className="phone-brand">
              <span className="mini-brand"><ChefHat /></span>
              <strong>Dini<span>Bee</span></strong>
            </div>

            <div className="push-preview">
              <span className="mini-brand"><ChefHat /></span>
              <div>
                <strong>{form.title || "Your notification title"}</strong>
                <p>{form.message || "Your notification message preview will appear here in real-time as you compose."}</p>
                <small>now • {form.type.replaceAll("_", " ")}</small>
              </div>
            </div>

            <div style={{ marginTop: "16px", padding: "10px", borderRadius: "10px", background: "white", border: "1px solid var(--border)", fontSize: "9px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}>
                <span style={{ color: "var(--muted-foreground)" }}>Target Audience</span>
                <strong>{form.audience.replaceAll("_", " ")}</strong>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--muted-foreground)" }}>Delivery Mode</span>
                <strong style={{ color: "#1c7645" }}>Instant In-App Broadcast</strong>
              </div>
            </div>
          </div>
        </aside>
      </section>

      {/* Broadcast History Table */}
      <DataTable
        rows={rows}
        columns={[
          { key: "title", label: "Notification", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.message)} /> },
          { key: "type", label: "Type", render: (value) => <StatusBadge status={String(value)} /> },
          { key: "audience", label: "Audience" },
          { key: "recipients", label: "Recipients" },
          { key: "createdBy", label: "Created by" },
          { key: "created", label: "Created" },
          { key: "schedule", label: "Delivery" },
          { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
        ]}
        searchPlaceholder="Search notification title, message, type or audience…"
        loading={loading}
        error={error}
        onRetry={reload}
        onRefresh={reload}
        onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)}
        actionLabel="Open notification"
        emptyTitle="No notifications found"
      />

      <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}>
        <SheetContent className="ecosystem-sheet">
          <SheetHeader>
            <span className="sheet-mark"><Bell /></span>
            <SheetTitle>{selected?.title}</SheetTitle>
            <SheetDescription>{selected?.type.replaceAll("_", " ")} notification</SheetDescription>
          </SheetHeader>
          {selected ? (
            <div className="ecosystem-sheet-body">
              <DetailSection title="Delivery details">
                <DetailGrid
                  fields={[
                    { label: "Audience", value: selected.audience.replaceAll("_", " ") },
                    { label: "Recipient count", value: selected.recipientCount?.toLocaleString("en-IN") ?? "Live broadcast" },
                    { label: "Status", value: <StatusBadge status={selected.status} /> },
                    { label: "Created by", value: selected.createdBy },
                    { label: "Created", value: selected.createdAt },
                    { label: "Scheduled", value: selected.scheduledAt ?? "Immediate" },
                  ]}
                />
              </DetailSection>
              <DetailSection title="Message">
                <p className="detail-copy">{selected.message}</p>
              </DetailSection>
              {selected.status === "FAILED" ? (
                <div className="page-error">
                  <XCircle />
                  <span>
                    <strong>Delivery failed</strong>
                    <small>Retry must be requested through the notification backend.</small>
                  </span>
                </div>
              ) : null}
            </div>
          ) : null}
        </SheetContent>
      </Sheet>

      <ConfirmActionDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Broadcast this notification live?"
        description={`Target Audience: ${form.audience.replaceAll("_", " ")}. This will immediately push the notification to user devices through the Spring Boot live notification system.`}
        confirmLabel="Broadcast now"
        onConfirm={sendNotification}
      />
    </div>
  );
}

