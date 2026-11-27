"use client";

import { useState, type FormEvent } from "react";
import {
  CheckCircle2,
  CreditCard,
  Download,
  FileClock,
  FileText,
  Gift,
  IndianRupee,
  Landmark,
  Plus,
  PlusCircle,
  ReceiptIndianRupee,
  RefreshCw,
  ShieldCheck,
  TicketPercent,
  Users,
  WalletCards,
} from "lucide-react";
import { useNavigate } from "@/lib/bookeat/router";
import { getStoredToken } from "@/lib/bookeat/api";
import { PayBillTransactions } from "@/components/paybill/pay-bill-transactions";
import { PayBillOffers } from "@/components/paybill/pay-bill-offers";
import { toast } from "sonner";

import { DataTable, EntityCell, PageHeader, StatCard, StatusBadge, type DataRow } from "@/components/bookeat/shared";
import { ConfirmActionDialog, DataSourceNotice, DetailGrid, DetailSection, formatCount, formatMoney } from "@/components/bookeat/ecosystem/shared";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { useApiResource } from "@/hooks/use-api-resource";
import { invoiceService, paymentService, walletService } from "@/lib/bookeat/services";
import type { CorporateInvoiceRecord, CustomerWallet, PaymentRecord, RewardRules, SettlementRecord, WalletOverview, WalletTransaction } from "@/lib/bookeat/types";

export function PaymentsManagementPage() {
  const [showPayBills, setShowPayBills] = useState(false);
  const [showPayOffers, setShowPayOffers] = useState(false);
  const { data, loading, error, reload } = useApiResource(() => paymentService.listTransactions(), [], [] as PaymentRecord[]);
  const [selected, setSelected] = useState<PaymentRecord | null>(null);
  const gross = data.reduce((sum, item) => sum + item.grossAmount, 0);
  const rewards = data.reduce((sum, item) => sum + item.rewardDiscount, 0);
  const gatewayPaid = data.reduce((sum, item) => sum + item.gatewayPaidAmount, 0);
  const rows: DataRow[] = data.map((item) => ({ id: item.id, transaction: item.transactionId, reference: item.referenceId, customer: item.customerName, restaurant: item.restaurantName, type: item.paymentType, gross: formatMoney(item.grossAmount), reward: formatMoney(item.rewardDiscount), gateway: formatMoney(item.gatewayPaidAmount), method: item.paymentMethod, date: item.date, status: item.status }));
  return <div className="page-stack">
    <PageHeader eyebrow="Payment management" title="Transactions" description="Track Dine-In, Takeaway, DiniTicket, Group and Corporate payments with reward discounts separated from gateway money." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <div className="flex flex-wrap items-center gap-3">
      <Button onClick={() => setShowPayBills(!showPayBills)}>{showPayBills ? 'Hide Pay Bills' : 'Pay Bill Transactions'}</Button>
      <Button variant="outline" onClick={() => setShowPayOffers(!showPayOffers)}>{showPayOffers ? 'Hide Bill Offers' : 'Bill Cashback & Offers'}</Button>
    </div>
    {showPayBills && <PayBillTransactions token={getStoredToken() || ''} />}
    {showPayOffers && <PayBillOffers token={getStoredToken() || ''} />}
    <section className="stats-grid four-up">
      <StatCard label="Gross bill value" value={formatMoney(gross)} note="Before reward discount" icon={IndianRupee} />
      <StatCard label="Reward discount" value={formatMoney(rewards)} note="DiniBee points redeemed" icon={Gift} tone="purple" />
      <StatCard label="Gateway paid" value={formatMoney(gatewayPaid)} note="Actual online/cash payment" icon={CreditCard} tone="green" />
      <StatCard label="Failed / pending" value={formatCount(data.filter((item) => ["FAILED", "PENDING"].includes(item.status)).length)} note="Requires monitoring" icon={FileClock} tone="red" />
    </section>
    {data.length > 0 ? <section className="panel-card payment-equation"><div><span><small>Live gross bill</small><strong>{formatMoney(gross)}</strong></span><i>−</i><span className="purple"><small>Reward discount</small><strong>{formatMoney(rewards)}</strong></span><i>=</i><span className="green"><small>Gateway payment</small><strong>{formatMoney(gatewayPaid)}</strong></span></div><p><ShieldCheck />Values above are calculated only from live backend payment records.</p></section> : null}
    <DataTable rows={rows} columns={[
      { key: "transaction", label: "Transaction ID", className: "cell-mono" }, { key: "reference", label: "Order / booking", className: "cell-mono" }, { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.type).replaceAll("_", " ")} /> }, { key: "restaurant", label: "Restaurant" }, { key: "gross", label: "Gross amount", className: "cell-money" }, { key: "reward", label: "Reward discount", className: "cell-money" }, { key: "gateway", label: "Gateway paid", className: "cell-money" }, { key: "method", label: "Method" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> }, { key: "date", label: "Date" },
    ]} searchPlaceholder="Search transaction, reference, customer or restaurant…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open transaction" emptyTitle="No transactions found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><CreditCard /></span><SheetTitle>{selected?.transactionId}</SheetTitle><SheetDescription>{selected?.paymentType.replaceAll("_", " ")} · {selected?.referenceId}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><DetailSection title="Payment breakdown"><DetailGrid fields={[
      { label: "Customer", value: selected.customerName }, { label: "Restaurant", value: selected.restaurantName }, { label: "Payment type", value: selected.paymentType.replaceAll("_", " ") }, { label: "Payment method", value: selected.paymentMethod }, { label: "Gross amount", value: formatMoney(selected.grossAmount) }, { label: "Reward discount", value: formatMoney(selected.rewardDiscount) }, { label: "Gateway paid", value: formatMoney(selected.gatewayPaidAmount) }, { label: "Status", value: <StatusBadge status={selected.status} /> }, { label: "Date", value: selected.date },
    ]} /></DetailSection><div className="monitor-note"><ShieldCheck /><span><strong>Backend payment truth</strong><small>Only a verified payment-gateway callback may mark this transaction PAID. The frontend never creates payment success.</small></span></div></div> : null}</SheetContent></Sheet>
  </div>;
}

export function SettlementsManagementPage() {
  const { data, loading, error, reload } = useApiResource(() => paymentService.listSettlements(), [], [] as SettlementRecord[]);
  const [selected, setSelected] = useState<SettlementRecord | null>(null);
  const pending = data.filter((item) => item.status !== "PAID").reduce((sum, item) => sum + item.netSettlement, 0);
  const rows: DataRow[] = data.map((item) => ({ id: item.id, settlement: item.settlementId, restaurant: item.restaurantName, period: item.period, gross: formatMoney(item.grossAmount), commission: formatMoney(item.commission), gatewayFee: formatMoney(item.gatewayFee), refund: formatMoney(item.refundAdjustment), net: formatMoney(item.netSettlement), created: item.createdDate, paid: item.paidDate ?? "—", status: item.status }));
  return <div className="page-stack">
    <PageHeader eyebrow="Restaurant finance" title="Settlements" description="Review the full settlement calculation, status, creation date and paid date for every restaurant." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <section className="stats-grid four-up"><StatCard label="Settlements" value={formatCount(data.length)} note="Current cycle records" icon={WalletCards} /><StatCard label="Pending payable" value={formatMoney(pending)} note="Not yet marked paid" icon={FileClock} tone="gold" /><StatCard label="Paid" value={formatCount(data.filter((item) => item.status === "PAID").length)} note="Completed transfers" icon={CheckCircle2} tone="green" /><StatCard label="Failed / on hold" value={formatCount(data.filter((item) => ["FAILED", "ON_HOLD"].includes(item.status)).length)} note="Requires finance review" icon={Landmark} tone="red" /></section>
    <DataTable rows={rows} columns={[
      { key: "settlement", label: "Settlement ID", className: "cell-mono" }, { key: "restaurant", label: "Restaurant", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.period)} /> }, { key: "gross", label: "Gross", className: "cell-money" }, { key: "commission", label: "Commission" }, { key: "gatewayFee", label: "Gateway fee" }, { key: "refund", label: "Refund adjustment" }, { key: "net", label: "Net settlement", className: "cell-money" }, { key: "created", label: "Created" }, { key: "paid", label: "Paid date" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search settlement or restaurant…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open settlement" emptyTitle="No settlements found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><WalletCards /></span><SheetTitle>{selected?.settlementId}</SheetTitle><SheetDescription>{selected?.restaurantName} · {selected?.period}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><DetailSection title="Settlement calculation"><DetailGrid fields={[
      { label: "Gross customer payment", value: formatMoney(selected.grossAmount) }, { label: "Platform commission", value: `− ${formatMoney(selected.commission)}` }, { label: "Gateway fee", value: `− ${formatMoney(selected.gatewayFee)}` }, { label: "Refund adjustment", value: `− ${formatMoney(selected.refundAdjustment)}` }, { label: "Final restaurant settlement", value: formatMoney(selected.netSettlement) }, { label: "Status", value: <StatusBadge status={selected.status} /> }, { label: "Created", value: selected.createdDate }, { label: "Paid", value: selected.paidDate ?? "Not paid" },
    ]} /></DetailSection></div> : null}</SheetContent></Sheet>
  </div>;
}

type WalletView = "overview" | "rules" | "transactions" | "redemptions" | "adjustments";
const emptyWalletOverview: WalletOverview = { customersWithWallet: 0, totalPointsIssued: 0, totalPointsRedeemed: 0, outstandingPoints: 0, todayEarnedPoints: 0, todayRedeemedPoints: 0 };
const emptyRules: RewardRules = { pointsConversionRate: 1, minimumRedeemPoints: 50, maximumRedeemPercentage: 50, pointsExpiryDays: 365, enabled: true };

export function WalletManagementPage({ view = "overview" }: { view?: WalletView }) {
  const navigate = useNavigate();
  const overview = useApiResource(() => walletService.getOverview(), [], emptyWalletOverview);
  const wallets = useApiResource(() => walletService.listWallets(), [], [] as CustomerWallet[]);
  const transactions = useApiResource(() => walletService.listTransactions(), [], [] as WalletTransaction[]);
  const rules = useApiResource(() => walletService.getRewardRules(), [], emptyRules);
  const ruleForm = rules.data;
  const setRuleForm = rules.setData;
  const [adjustOpen, setAdjustOpen] = useState(false);
  const [confirmAdjustment, setConfirmAdjustment] = useState(false);
  const [customerId, setCustomerId] = useState("");
  const [adjustmentDirection, setAdjustmentDirection] = useState<"ADD" | "REMOVE">("ADD");
  const [points, setPoints] = useState("100");
  const [reason, setReason] = useState("");

  const walletRows: DataRow[] = wallets.data.map((item) => ({ id: item.customerId, customer: item.customerName, customerSubtitle: item.customerId, balance: formatCount(item.balance), redeemValue: formatMoney(item.approximateRedeemValue), earned: formatCount(item.totalEarned), redeemed: formatCount(item.totalRedeemed), status: "ACTIVE" }));
  const visibleTransactions = view === "redemptions" ? transactions.data.filter((item) => item.type === "REDEEM") : transactions.data;
  const transactionRows: DataRow[] = visibleTransactions.map((item) => ({ id: item.id, customer: item.customerName, customerSubtitle: item.customerId, type: item.type, points: item.points > 0 ? `+${item.points}` : item.points, reference: item.relatedBookingId ?? item.relatedOrderId ?? "—", reason: item.reason, admin: item.adminName ?? "System", created: item.createdAt, status: item.type === "EXPIRED" ? "EXPIRED" : "COMPLETED" }));

  async function saveRules(event: FormEvent) {
    event.preventDefault();
    try {
      await walletService.updateRewardRules(ruleForm);
      toast.success("Reward rules updated", { description: "Backend audit logging is required in live mode." });
      await rules.reload();
    } catch (caught) { toast.error(caught instanceof Error ? caught.message : "Unable to save reward rules"); }
  }

  async function applyAdjustment() {
    const parsed = Number(points);
    if (!customerId || !Number.isFinite(parsed) || parsed <= 0 || !reason.trim()) {
      toast.error("Choose a customer and enter valid points and reason");
      return;
    }
    try {
      await walletService.adjustPoints(customerId, adjustmentDirection === "ADD" ? parsed : -parsed, reason);
      toast.success("Wallet adjustment recorded");
      setConfirmAdjustment(false); setAdjustOpen(false); setReason("");
      await Promise.all([wallets.reload(), transactions.reload(), overview.reload()]);
    } catch (caught) { toast.error(caught instanceof Error ? caught.message : "Unable to adjust wallet"); }
  }

  const tabs = <Tabs value={view} onValueChange={(value) => navigate(value === "overview" ? "/wallet" : `/wallet/${value}`)} className="page-tabs"><TabsList><TabsTrigger value="overview">Wallet Overview</TabsTrigger><TabsTrigger value="rules">Reward Rules</TabsTrigger><TabsTrigger value="transactions">Transactions</TabsTrigger><TabsTrigger value="redemptions">Redemptions</TabsTrigger><TabsTrigger value="adjustments">Adjustments</TabsTrigger></TabsList></Tabs>;
  const tableError = wallets.error || transactions.error || overview.error || rules.error;

  return <div className="page-stack">
    <PageHeader eyebrow="DiniBee rewards" title="DiniBee Wallet" description="Manage customer reward-point rules, balances, redemptions and audited manual adjustments." actions={<><DataSourceNotice />{view === "adjustments" ? <Button className="gold-button" onClick={() => setAdjustOpen(true)}><Plus />Adjust points</Button> : null}</>} />
    {tabs}
    {view === "overview" ? <>
      <section className="stats-grid dashboard-stats"><StatCard label="Customers with wallet" value={formatCount(overview.data.customersWithWallet)} note="Active wallet accounts" icon={Users} /><StatCard label="Points issued" value={formatCount(overview.data.totalPointsIssued)} note="Lifetime earned" icon={PlusCircle} tone="green" /><StatCard label="Points redeemed" value={formatCount(overview.data.totalPointsRedeemed)} note="Lifetime bill discounts" icon={TicketPercent} tone="purple" /><StatCard label="Outstanding points" value={formatCount(overview.data.outstandingPoints)} note="Current customer liability" icon={WalletCards} tone="blue" /><StatCard label="Today earned" value={formatCount(overview.data.todayEarnedPoints)} note="Completed + paid only" icon={Gift} tone="green" /><StatCard label="Today redeemed" value={formatCount(overview.data.todayRedeemedPoints)} note="Applied before gateway" icon={CreditCard} tone="gold" /></section>
      <DataTable rows={walletRows} columns={[
        { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.customerSubtitle)} /> }, { key: "balance", label: "Points balance", className: "cell-money" }, { key: "redeemValue", label: "Approx. redeem value", className: "cell-money" }, { key: "earned", label: "Earned total" }, { key: "redeemed", label: "Redeemed total" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
      ]} searchPlaceholder="Search customer wallet…" loading={wallets.loading || overview.loading} error={tableError} onRetry={() => Promise.all([wallets.reload(), overview.reload()]).then(() => undefined)} onRefresh={() => Promise.all([wallets.reload(), overview.reload()]).then(() => undefined)} />
    </> : null}
    {view === "rules" ? <form className="panel-card reward-rules-form" onSubmit={saveRules}><div className="card-title-row"><div><p>Platform configuration</p><h2>Reward rules</h2></div><label className="rule-enabled"><span><strong>Rewards enabled</strong><small>Enable earning and redemption</small></span><Switch checked={ruleForm.enabled} onCheckedChange={(enabled) => setRuleForm((current) => ({ ...current, enabled }))} /></label></div><div className="reward-rule-grid">
      <label>Points conversion rate<div className="suffix-input"><Input type="number" min="0.01" step="0.01" value={ruleForm.pointsConversionRate} onChange={(event) => setRuleForm((current) => ({ ...current, pointsConversionRate: Number(event.target.value) }))} /><span>₹ / point</span></div></label>
      <label>Minimum redeem points<div className="suffix-input"><Input type="number" min="1" value={ruleForm.minimumRedeemPoints} onChange={(event) => setRuleForm((current) => ({ ...current, minimumRedeemPoints: Number(event.target.value) }))} /><span>points</span></div></label>
      <label>Maximum bill redemption<div className="suffix-input"><Input type="number" min="1" max="100" value={ruleForm.maximumRedeemPercentage} onChange={(event) => setRuleForm((current) => ({ ...current, maximumRedeemPercentage: Number(event.target.value) }))} /><span>%</span></div></label>
      <label>Points expiry<div className="suffix-input"><Input type="number" min="1" value={ruleForm.pointsExpiryDays} onChange={(event) => setRuleForm((current) => ({ ...current, pointsExpiryDays: Number(event.target.value) }))} /><span>days</span></div></label>
    </div><div className="reward-safety-note"><ShieldCheck /><span><strong>Reward award rule</strong><small>Booking/order COMPLETED + payment PAID → idempotent backend award → wallet update. Never award from frontend state.</small></span></div><Button className="gold-button" type="submit">Save reward rules</Button></form> : null}
    {view === "transactions" || view === "redemptions" || view === "adjustments" ? <DataTable rows={transactionRows} columns={[
      { key: "customer", label: "Customer", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.customerSubtitle)} /> }, { key: "type", label: "Transaction type", render: (value) => <StatusBadge status={String(value)} /> }, { key: "points", label: "Points", className: "cell-money" }, { key: "reference", label: "Related booking / order" }, { key: "reason", label: "Reason" }, { key: "admin", label: "Admin / source" }, { key: "created", label: "Created" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search wallet transaction, customer or reference…" loading={transactions.loading} error={transactions.error} onRetry={transactions.reload} onRefresh={transactions.reload} emptyTitle={view === "redemptions" ? "No redemptions found" : "No wallet transactions found"} /> : null}
    <Dialog open={adjustOpen} onOpenChange={setAdjustOpen}><DialogContent className="action-dialog"><DialogHeader><span className="dialog-mark"><WalletCards /></span><DialogTitle>Manual wallet adjustment</DialogTitle><DialogDescription>Add or remove customer points with a mandatory reason. The backend must retain the admin and timestamp.</DialogDescription></DialogHeader><div className="dialog-form"><label>Customer<Select value={customerId} onValueChange={setCustomerId}><SelectTrigger><SelectValue placeholder="Choose customer" /></SelectTrigger><SelectContent>{wallets.data.map((wallet) => <SelectItem key={wallet.customerId} value={wallet.customerId}>{wallet.customerName} · {wallet.balance} pts</SelectItem>)}</SelectContent></Select></label><div className="form-two-col"><label>Action<Select value={adjustmentDirection} onValueChange={(value) => setAdjustmentDirection(value as "ADD" | "REMOVE")}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent><SelectItem value="ADD">Add points</SelectItem><SelectItem value="REMOVE">Remove points</SelectItem></SelectContent></Select></label><label>Points<Input type="number" min="1" value={points} onChange={(event) => setPoints(event.target.value)} /></label></div><label>Reason<Textarea required value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Explain why this adjustment is required" /></label><DialogFooter><Button variant="outline" onClick={() => setAdjustOpen(false)}>Cancel</Button><Button className="gold-button" onClick={() => setConfirmAdjustment(true)}>Review adjustment</Button></DialogFooter></div></DialogContent></Dialog>
    <ConfirmActionDialog open={confirmAdjustment} onOpenChange={setConfirmAdjustment} title={`${adjustmentDirection === "ADD" ? "Add" : "Remove"} ${points || "0"} points?`} description="This sensitive wallet change will record the customer, reason, admin user and timestamp." confirmLabel="Confirm adjustment" destructive={adjustmentDirection === "REMOVE"} onConfirm={applyAdjustment} />
  </div>;
}

export function CorporateInvoicesPage() {
  const { data, loading, error, reload } = useApiResource(() => invoiceService.list(), [], [] as CorporateInvoiceRecord[]);
  const [selected, setSelected] = useState<CorporateInvoiceRecord | null>(null);
  const rows: DataRow[] = data.map((item) => ({ id: item.id, invoice: item.invoiceId, company: item.companyName, restaurant: item.restaurantName, booking: item.corporateBookingNumber, invoiceDate: item.invoiceDate, dueDate: item.dueDate, subtotal: formatMoney(item.subtotal), tax: formatMoney(item.tax), total: formatMoney(item.total), paid: formatMoney(item.paidAmount), pending: formatMoney(item.pendingAmount), status: item.status }));
  return <div className="page-stack">
    <PageHeader eyebrow="Corporate billing" title="Corporate invoices" description="Monitor company invoices, payment terms, due dates and outstanding balances." actions={<><DataSourceNotice /><Button variant="outline" onClick={() => void reload()}><RefreshCw />Refresh</Button></>} />
    <section className="stats-grid four-up"><StatCard label="Invoices" value={formatCount(data.length)} note="Current result set" icon={FileText} /><StatCard label="Total billed" value={formatMoney(data.reduce((sum, item) => sum + item.total, 0))} note="Invoice total" icon={ReceiptIndianRupee} tone="blue" /><StatCard label="Paid" value={formatMoney(data.reduce((sum, item) => sum + item.paidAmount, 0))} note="Confirmed company payments" icon={CheckCircle2} tone="green" /><StatCard label="Outstanding" value={formatMoney(data.reduce((sum, item) => sum + item.pendingAmount, 0))} note="Pending and overdue" icon={FileClock} tone="red" /></section>
    <DataTable rows={rows} columns={[
      { key: "invoice", label: "Invoice ID", className: "cell-mono" }, { key: "company", label: "Company", render: (value, row) => <EntityCell title={String(value)} subtitle={String(row.booking)} /> }, { key: "restaurant", label: "Restaurant" }, { key: "invoiceDate", label: "Invoice date" }, { key: "dueDate", label: "Due date" }, { key: "subtotal", label: "Subtotal" }, { key: "tax", label: "Tax" }, { key: "total", label: "Total", className: "cell-money" }, { key: "paid", label: "Paid" }, { key: "pending", label: "Pending", className: "cell-money" }, { key: "status", label: "Status", render: (value) => <StatusBadge status={String(value)} /> },
    ]} searchPlaceholder="Search invoice, company, restaurant or booking…" loading={loading} error={error} onRetry={reload} onRefresh={reload} onView={(row) => setSelected(data.find((item) => item.id === row.id) ?? null)} actionLabel="Open invoice" emptyTitle="No corporate invoices found" />
    <Sheet open={Boolean(selected)} onOpenChange={(open) => !open && setSelected(null)}><SheetContent className="ecosystem-sheet"><SheetHeader><span className="sheet-mark"><FileText /></span><SheetTitle>{selected?.invoiceId}</SheetTitle><SheetDescription>{selected?.companyName} · {selected?.corporateBookingNumber}</SheetDescription></SheetHeader>{selected ? <div className="ecosystem-sheet-body"><DetailSection title="Invoice details"><DetailGrid fields={[
      { label: "Company", value: selected.companyName }, { label: "Restaurant", value: selected.restaurantName }, { label: "Corporate booking", value: selected.corporateBookingNumber }, { label: "Invoice date", value: selected.invoiceDate }, { label: "Due date", value: selected.dueDate }, { label: "Subtotal", value: formatMoney(selected.subtotal) }, { label: "Tax", value: formatMoney(selected.tax) }, { label: "Total", value: formatMoney(selected.total) }, { label: "Paid amount", value: formatMoney(selected.paidAmount) }, { label: "Pending amount", value: formatMoney(selected.pendingAmount) }, { label: "Status", value: <StatusBadge status={selected.status} /> },
    ]} /></DetailSection><Button className="gold-button" onClick={() => toast.success("Invoice download requested from backend")}><Download />Download invoice</Button></div> : null}</SheetContent></Sheet>
  </div>;
}
