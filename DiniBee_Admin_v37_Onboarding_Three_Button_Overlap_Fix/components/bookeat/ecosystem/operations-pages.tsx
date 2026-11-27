"use client";

import { useMemo, useState } from "react";
import {
  CalendarCheck2,
  ChefHat,
  Clock3,
  CreditCard,
  Navigation,
  PackageCheck,
  RefreshCw,
  ShoppingBag,
  TicketCheck,
  Timer,
  Users,
} from "lucide-react";
import { useNavigate } from "@/lib/bookeat/router";

import { DataTable, EntityCell, PageHeader, StatCard, StatusBadge, type Column, type DataRow } from "@/components/bookeat/shared";
import { DataSourceNotice, DetailGrid, DetailSection, formatCount, formatMoney } from "@/components/bookeat/ecosystem/shared";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useApiResource } from "@/hooks/use-api-resource";
import { diniTicketService, orderService, takeawayService } from "@/lib/bookeat/services";
import type { DiniTicketRecord, DiniTicketStatus, FoodOrderRecord, TakeawayRecord } from "@/lib/bookeat/types";

export function FoodOrdersManagementPage() {
  const { data, loading, error, reload } = useApiResource(() => orderService.listFoodOrders(), [], [] as FoodOrderRecord[]);
  const [selected, setSelected] = useState<FoodOrderRecord | null>(null);
  const rows: DataRow[] = data.map((item) => ({ id: item.id, order: item.orderNumber, booking: item.bookingNumber, customer: item.customerName, restaurant: item.restaurantName, table: item.tableId, bookingType: item.bookingType, amount: formatMoney(item.amount), payment: item.paymentStatus, created: item.createdAt, status: item.orderStatus }));
  const columns: Column<DataRow>[] = [
    { key: "order", label: "Order ID", className: "cell-mono" }, { key: "booking", label: "Booking ID", className: "cell-mono" },
    { key: "customer", label: "Customer / guest", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.bookingType)} /> },
    { key: "restaurant", label: "Restaurant" }, { key: "table", label: "Table" }, { key: "bookingType", label: "Booking type", render: (value) => <StatusBadge status={String(value)} /> },
    { key: "amount", label: "Amount", className: "cell-money" }, { key: "status", label: "Order status", render: (value) => <StatusBadge status={String(value)} /> },
    { key: "payment", label: "Payment", render: (value) => <StatusBadge status={String(value)} /> }, { key: "created", label: "Created" },
  ];
  return <div className="page-stack">
    <PageHeader eyebrow="Dine-in monitoring" title="Food orders" description="Monitor regular, group and corporate dine-in orders while restaurants control preparation and service." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <section className="stats-grid four-up">
      <StatCard label="Active orders" value={formatCount(data.filter((item) => !["COMPLETED", "CANCELLED"].includes(item.orderStatus)).length)} note="New through served" icon={ChefHat} />
      <StatCard label="Preparing" value={formatCount(data.filter((item) => item.orderStatus === "PREPARING").length)} note="Restaurant kitchen queue" icon={Clock3} tone="purple" />
      <StatCard label="Group orders" value={formatCount(data.filter((item) => item.bookingType === "GROUP").length)} note="Individual cart breakdown" icon={Users} tone="blue" />
      <StatCard label="Corporate orders" value={formatCount(data.filter((item) => item.bookingType === "CORPORATE").length)} note="Budget and invoice linked" icon={CalendarCheck2} tone="green" />
    </section>
    <DataTable rows={rows} columns={columns} searchPlaceholder="Search order, booking, customer or restaurant…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open food order" emptyTitle="No food orders found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><ChefHat /></span><SheetTitle>{selected?.orderNumber}</SheetTitle><SheetDescription>Dine-in food order · {selected?.bookingNumber}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body">
      <DetailSection title="Order details"><DetailGrid fields={[
        { label: "Booking", value: selected.bookingNumber }, { label: "Customer / group", value: selected.customerName }, { label: "Restaurant", value: selected.restaurantName }, { label: "Table", value: selected.tableId }, { label: "Booking type", value: <StatusBadge status={selected.bookingType} /> }, { label: "Created", value: selected.createdAt }, { label: "Order status", value: <StatusBadge status={selected.orderStatus} /> }, { label: "Payment", value: <StatusBadge status={selected.paymentStatus} /> }, { label: "Combined amount", value: formatMoney(selected.amount) },
      ]} /></DetailSection>
      {selected.participantOrders?.length ? <DetailSection title="Individual participant orders" description="These remain separate carts and combine only for owner/admin visibility."><div className="participant-list">{selected.participantOrders.map((order) => <div key={order.name}><span><strong>{order.name}</strong><small>{order.items} items</small></span><b>{formatMoney(order.amount)}</b></div>)}</div></DetailSection> : null}
      <div className="monitor-note"><ChefHat /><span><strong>Restaurant-controlled state</strong><small>NEW → ACCEPTED → PREPARING → READY → SERVED → COMPLETED is updated by the Restaurant Owner App.</small></span></div>
    </div> : null}</SheetContent></Sheet>
  </div>;
}

type TakeawayView = "all" | "regular" | "on-the-way";

export function TakeawayManagementPage({ view = "all" }: { view?: TakeawayView }) {
  const navigate = useNavigate();
  const type = view === "regular" ? "REGULAR" : view === "on-the-way" ? "ON_THE_WAY" : undefined;
  const { data, loading, error, reload } = useApiResource(() => takeawayService.list(type), [type], [] as TakeawayRecord[]);
  const [selected, setSelected] = useState<TakeawayRecord | null>(null);
  const rows: DataRow[] = useMemo(() => data.map((item) => ({ id: item.id, order: item.orderNumber, customer: item.customerName, restaurant: item.restaurantName, type: item.type, journey: item.journeyStatus, eta: item.eta ?? "—", amount: formatMoney(item.amount), payment: item.paymentStatus, refund: item.refundStatus, created: item.createdAt, status: item.status })), [data]);
  return <div className="page-stack">
    <PageHeader eyebrow="Pickup operations" title="Takeaway orders" description="Monitor regular pickup and On-the-Way orders, including customer journey status and ETA." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <Tabs value={view} onValueChange={(value) => navigate(value === "all" ? "/takeaway-orders" : `/takeaway-orders/${value}`)} className="page-tabs"><TabsList><TabsTrigger value="all">All</TabsTrigger><TabsTrigger value="regular">Regular</TabsTrigger><TabsTrigger value="on-the-way">On-the-Way</TabsTrigger></TabsList></Tabs>
    <section className="stats-grid four-up">
      <StatCard label="Orders" value={formatCount(data.length)} note="Current selection" icon={ShoppingBag} />
      <StatCard label="On-the-Way" value={formatCount(data.filter((item) => item.type === "ON_THE_WAY").length)} note="Journey-aware pickup" icon={Navigation} tone="blue" />
      <StatCard label="Ready" value={formatCount(data.filter((item) => item.status === "READY").length)} note="Waiting for customer" icon={PackageCheck} tone="green" />
      <StatCard label="Refunded" value={formatCount(data.filter((item) => item.refundStatus === "REFUNDED").length)} note="Completed refund flow" icon={CreditCard} tone="red" />
    </section>
    <DataTable rows={rows} columns={[
      { key: "order", label: "Order ID", className: "cell-mono" }, { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.type ?? "REGULAR").replaceAll("_", " ")} /> }, { key: "restaurant", label: "Restaurant" },
      { key: "journey", label: "Journey", render: (value) => <StatusBadge status={String(value)} /> }, { key: "eta", label: "ETA" }, { key: "amount", label: "Amount", className: "cell-money" },
      { key: "payment", label: "Payment", render: (value) => <StatusBadge status={String(value)} /> }, { key: "refund", label: "Refund", render: (value) => <StatusBadge status={String(value)} /> }, { key: "status", label: "Order status", render: (value) => <StatusBadge status={String(value)} /> }, { key: "created", label: "Created" },
    ]} searchPlaceholder="Search takeaway order, customer or restaurant…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open takeaway" emptyTitle="No takeaway orders found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><ShoppingBag /></span><SheetTitle>{selected?.orderNumber}</SheetTitle><SheetDescription>{(selected?.type ?? "REGULAR").replaceAll("_", " ")} takeaway order</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><DetailSection title="Order and customer journey"><DetailGrid fields={[
      { label: "Customer", value: selected.customerName }, { label: "Restaurant", value: selected.restaurantName }, { label: "Type", value: (selected.type ?? "REGULAR").replaceAll("_", " ") }, { label: "Journey", value: <StatusBadge status={selected.journeyStatus} /> }, { label: "ETA", value: selected.eta ?? "Not provided" }, { label: "Order status", value: <StatusBadge status={selected.status} /> }, { label: "Payment", value: <StatusBadge status={selected.paymentStatus} /> }, { label: "Refund", value: <StatusBadge status={selected.refundStatus} /> }, { label: "Amount", value: formatMoney(selected.amount) }, { label: "Created", value: selected.createdAt },
    ]} /></DetailSection><div className="journey-strip">{["NOT_STARTED", "LEAVING_NOW", "ON_THE_WAY", "NEARBY", "ARRIVED"].map((step) => <span key={step} className={step === selected.journeyStatus ? "active" : ""}><i />{step.replaceAll("_", " ")}</span>)}</div><div className="monitor-note"><Navigation /><span><strong>ETA-aware preparation</strong><small>The owner app receives journey updates and controls preparation. The admin monitors exceptions, payments and refunds.</small></span></div></div> : null}</SheetContent></Sheet>
  </div>;
}

type DiniTicketView = "live" | "orders" | "completed" | "exceptions";

function statusesForDiniTicket(view: DiniTicketView): DiniTicketStatus[] | undefined {
  if (view === "live") return ["WAITING", "PREPARING", "READY", "CALLED"];
  if (view === "completed") return ["COLLECTED", "COMPLETED"];
  if (view === "exceptions") return ["CANCELLED", "NO_SHOW"];
  return undefined;
}

export function DiniTicketManagementPage({ view = "live" }: { view?: DiniTicketView }) {
  const navigate = useNavigate();
  const statuses = useMemo(() => statusesForDiniTicket(view), [view]);
  const { data, loading, error, reload } = useApiResource(() => diniTicketService.list(statuses), [view], [] as DiniTicketRecord[]);
  const [selected, setSelected] = useState<DiniTicketRecord | null>(null);
  const currentToken = data.find((item) => item.queueStatus === "CALLED")?.tokenNumber ?? "—";
  const averageWait = data.length ? Math.round(data.reduce((sum, item) => sum + item.waitMinutes, 0) / data.length) : 0;
  const rows: DataRow[] = data.map((item) => ({ id: item.id, token: item.tokenNumber, customer: item.customerName, restaurant: item.restaurantName, order: item.orderNumber, amount: formatMoney(item.amount), payment: item.paymentStatus, arrival: item.arrivalStatus, wait: `${item.waitMinutes} min`, created: item.createdAt, status: item.queueStatus }));
  return <div className="page-stack">
    <PageHeader eyebrow="DiniTicket queue management" title="DiniTicket" description="Monitor token queues, customer arrival and pickup orders across enabled restaurants." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <Tabs value={view} onValueChange={(value) => navigate(`/diniticket/${value}`)} className="page-tabs"><TabsList><TabsTrigger value="live">Live Queue</TabsTrigger><TabsTrigger value="orders">Orders</TabsTrigger><TabsTrigger value="completed">Completed</TabsTrigger><TabsTrigger value="exceptions">No Show / Cancelled</TabsTrigger></TabsList></Tabs>
    <section className="diniticket-hero-grid">
      <article className="panel-card current-token-card"><span><TicketCheck /></span><div><small>Current token</small><strong>{currentToken}</strong><em>Called for collection</em></div></article>
      <StatCard label="Waiting tokens" value={formatCount(data.filter((item) => item.queueStatus === "WAITING").length)} note="Queue not started" icon={Users} />
      <StatCard label="Average wait" value={`${averageWait} min`} note="Current result set" icon={Timer} tone="blue" />
      <StatCard label="Ready tokens" value={formatCount(data.filter((item) => item.queueStatus === "READY").length)} note="Food is ready" icon={PackageCheck} tone="green" />
      <StatCard label="Completed" value={formatCount(data.filter((item) => ["COLLECTED", "COMPLETED"].includes(item.queueStatus)).length)} note="Collected orders" icon={TicketCheck} tone="purple" />
    </section>
    <DataTable rows={rows} columns={[
      { key: "token", label: "Token", className: "cell-mono" }, { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.order)} /> }, { key: "restaurant", label: "Restaurant" }, { key: "order", label: "Order" }, { key: "amount", label: "Amount", className: "cell-money" },
      { key: "payment", label: "Payment", render: (value) => <StatusBadge status={String(value)} /> }, { key: "arrival", label: "Arrival", render: (value) => <StatusBadge status={String(value)} /> }, { key: "wait", label: "Wait" }, { key: "created", label: "Created" }, { key: "status", label: "Queue status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search token, customer, restaurant or order…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open DiniTicket" emptyTitle="No DiniTicket orders found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><TicketCheck /></span><SheetTitle>Token {selected?.tokenNumber}</SheetTitle><SheetDescription>{selected?.restaurantName} · {selected?.orderNumber}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><DetailSection title="Queue order"><DetailGrid fields={[
      { label: "Customer", value: selected.customerName }, { label: "Restaurant", value: selected.restaurantName }, { label: "Order", value: selected.orderNumber }, { label: "Token", value: selected.tokenNumber }, { label: "Queue status", value: <StatusBadge status={selected.queueStatus} /> }, { label: "Arrival", value: <StatusBadge status={selected.arrivalStatus} /> }, { label: "Payment", value: <StatusBadge status={selected.paymentStatus} /> }, { label: "Amount", value: formatMoney(selected.amount) }, { label: "Wait time", value: `${selected.waitMinutes} minutes` }, { label: "Created", value: selected.createdAt },
    ]} /></DetailSection><div className="monitor-note"><TicketCheck /><span><strong>Owner-managed queue</strong><small>WAITING → PREPARING → READY → CALLED → COLLECTED → COMPLETED is controlled by the restaurant.</small></span></div></div> : null}</SheetContent></Sheet>
  </div>;
}
