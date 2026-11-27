"use client";

import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import {
  CalendarCheck2,
  CheckCircle2,
  Clock3,
  CreditCard,
  FileCheck2,
  FileText,
  FileClock,
  IndianRupee,
  MapPin,
  RefreshCw,
  SlidersHorizontal,
  Search,
  ShoppingBag,
  Crown,
  Heart,
  Star,
  UserRound,
  Bell,
  Ban,
  ExternalLink,
  Package,
  Store,
  TicketCheck,
  Users,
  XCircle,
} from "lucide-react";

import { adminApi, apiClient, getStoredToken } from "@/lib/bookeat/api";
import { diniTicketService, notificationService, paymentService, reservationService, restaurantService, reviewService, supportService, takeawayService, walletService } from "@/lib/bookeat/services";
import { useApiResource } from "@/hooks/use-api-resource";
import type { DiniTicketRecord, PaymentRecord, ReservationRecord, RestaurantRecord, TakeawayRecord } from "@/lib/bookeat/types";
import { PageHeader, StatCard, StatusBadge } from "@/components/bookeat/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";

function money(value: unknown) {
  const number = Number(value ?? 0) || 0;
  return `₹${number.toLocaleString("en-IN")}`;
}

function text(value: unknown, fallback = "—") {
  const out = String(value ?? "").trim();
  return out || fallback;
}

function safeDate(value: unknown) {
  const d = new Date(String(value ?? ""));
  return Number.isNaN(d.getTime()) ? null : d;
}

function csvDownload(filename: string, rows: Array<Record<string, unknown>>) {
  if (typeof window === "undefined" || !rows.length) { toast.info("No rows to export"); return; }
  const headers = Array.from(new Set(rows.flatMap((row) => Object.keys(row))));
  const esc = (value: unknown) => `"${String(value ?? "").replaceAll('"','""')}"`;
  const csv = [headers.map(esc).join(","), ...rows.map((row) => headers.map((h) => esc(row[h])).join(","))].join("\n");
  const url = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.style.display = "none";
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 500);
  toast.success(`Exported ${rows.length} row${rows.length === 1 ? "" : "s"}`);
}

function avatarDataUri(name: string) {
  const initials = String(name || "U").split(/\s+/).filter(Boolean).slice(0,2).map((v)=>v[0]?.toUpperCase()).join("") || "U";
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96"><rect width="100%" height="100%" rx="48" fill="#eef4fb"/><text x="50%" y="54%" text-anchor="middle" dominant-baseline="middle" font-family="Arial" font-size="30" font-weight="700" fill="#23344d">${initials}</text></svg>`;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function mediaUrlFor(item: any, kind: "restaurant" | "food" = "restaurant") {
  const raw = kind === "food"
    ? item?.foodImageUrl ?? item?.imageUrl ?? item?.restaurantCoverImageUrl ?? item?.restaurantLogoUrl
    : item?.restaurantCoverImageUrl ?? item?.restaurantLogoUrl ?? item?.coverImageUrl ?? item?.logoUrl;
  return productionMediaUrl(raw);
}

async function sendRecordNotification(item: any, title: string, message: string) {
  const userId = String(item?.customerUserId ?? "");
  if (!/^[0-9a-fA-F-]{36}$/.test(userId)) { toast.error("Customer notification ID is unavailable. Deploy the updated backend first."); return; }
  await notificationService.send({ title, message, type: "SYSTEM_NOTICE", audience: "SELECTED_CUSTOMERS", recipientIds: [userId] });
  toast.success("Notification sent");
}

function unwrapArray(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== "object") return [];
  for (const key of ["content", "items", "records", "results"]) {
    if (Array.isArray(payload[key])) return payload[key];
  }
  if (payload.data !== undefined) return unwrapArray(payload.data);
  return [];
}

function productionMediaUrl(value: unknown): string {
  let url = String(value ?? "").trim();
  if (!url) return "";
  if (url.startsWith("/")) url = `https://api.dinibee.tech${url}`;
  url = url
    .replace(/^http:\/\/api\.dinibee\.tech(?::80)?/i, "https://api.dinibee.tech")
    .replace(/^http:\/\/localhost(?::\d+)?/i, "https://api.dinibee.tech")
    .replace(/^http:\/\/127\.0\.0\.1(?::\d+)?/i, "https://api.dinibee.tech")
    .replace(/^http:\/\/200\.97\.166\.143(?::\d+)?/i, "https://api.dinibee.tech");
  return url;
}

function restaurantImageCandidates(item: any): string[] {
  const values: unknown[] = [
    item?.coverImageUrl, item?.coverUrl, item?.coverImage,
    item?.logoUrl, item?.logoImageUrl, item?.logoImage,
    item?.imageUrl, item?.image, item?.restaurantImageUrl,
    item?.media?.coverImageUrl, item?.media?.logoUrl, item?.media?.imageUrl,
    item?.onboardingForm?.coverImageUrl, item?.onboardingForm?.logoUrl,
    ...(Array.isArray(item?.photos) ? item.photos.map((p: any) => p?.url ?? p?.fileUrl ?? p) : []),
    ...(Array.isArray(item?.documents) ? item.documents.filter((d: any) => ["COVER_IMAGE","LOGO"].includes(String(d?.documentType ?? "").toUpperCase())).map((d: any) => d?.fileUrl) : []),
  ];
  return Array.from(new Set(values.map(productionMediaUrl).filter(Boolean)));
}

function restaurantImage(item: any): string {
  return restaurantImageCandidates(item)[0] ?? "";
}

function normalizeRestaurant(item: any) {
  const location = item?.location && typeof item.location === "object"
    ? [item.location.addressLine1, item.location.city, item.location.state].filter(Boolean).join(", ")
    : item?.location || item?.address || item?.city;
  const services = item?.requestedServices || item?.enabledServices || item?.services || [];
  return {
    ...item,
    id: String(item?.id ?? ""),
    restaurantName: text(item?.restaurantName ?? item?.name, "Unnamed Restaurant"),
    ownerName: text(item?.ownerName ?? item?.owner?.name ?? item?.contactName, "Owner"),
    phone: text(item?.phone ?? item?.phoneNumber ?? item?.mobileNumber),
    email: text(item?.email ?? item?.ownerEmail),
    location: text(location),
    approvalStatus: text(item?.approvalStatus ?? item?.status, "SUBMITTED").toUpperCase(),
    cuisine: text(item?.cuisine ?? item?.cuisineType ?? item?.businessInfo?.cuisineType, "Various"),
    imageUrl: restaurantImage(item),
    services: Array.isArray(services) ? services : [],
    rating: Number(item?.rating ?? item?.averageRating ?? 0) || 0,
    orders: Number(item?.orderCount ?? item?.totalOrders ?? 0) || 0,
    todaySales: Number(item?.todaySalesAmount ?? item?.todaySales ?? 0) || 0,
  };
}

function normalizeCustomer(c: any) {
  const first = c?.firstName ?? c?.first_name ?? "";
  const last = c?.lastName ?? c?.last_name ?? "";
  const name = c?.fullName ?? c?.displayName ?? c?.name ?? `${first} ${last}`.trim();
  return {
    ...c, id: String(c?.id ?? c?.customerId ?? c?.userId ?? ""), name: text(name, "Customer"),
    phone: text(c?.phoneNumber ?? c?.mobileNumber ?? c?.phone), email: text(c?.email),
    location: text(c?.location ?? c?.city ?? c?.address?.city),
    joined: c?.createdAt ? new Date(c.createdAt).toLocaleDateString("en-IN") : text(c?.joinedAt),
    status: text(c?.status, "ACTIVE").toUpperCase(),
    premium: (() => {
      if (c?.premium === true || c?.isPremium === true) return true;
      const plan = String(c?.membershipPlan?.name ?? c?.membershipPlan ?? c?.planName ?? c?.customerType ?? "").toUpperCase();
      const membershipStatus = String(c?.membershipStatus ?? c?.membership?.status ?? "").toUpperCase();
      const active = c?.membershipActive === true || ["ACTIVE","PAID","CURRENT"].includes(membershipStatus);
      return active && !!plan && !["FREE","BASIC","NON_PREMIUM","NON-PREMIUM","STANDARD"].includes(plan);
    })(),
    orders: Number(c?.totalOrders ?? c?.orderCount ?? c?.ordersCount ?? 0) || 0,
    reservations: Number(c?.reservationCount ?? c?.reservationsCount ?? 0) || 0, takeaway: Number(c?.takeawayCount ?? c?.takeawayOrdersCount ?? 0) || 0,
    totalSpent: Number(c?.totalSpending ?? c?.totalSpent ?? c?.lifetimeValue ?? 0) || 0, imageUrl: productionMediaUrl(c?.profileImageUrl ?? c?.photoUrl ?? c?.avatarUrl),
  };
}

function RestaurantThumb({ item, large = false }: { item: any; large?: boolean }) {
  const candidates = restaurantImageCandidates(item);
  const src = candidates[0];
  return src ? (
    <img className={large ? "v17-restaurant-image large" : "v17-restaurant-image"} src={src} data-candidates={JSON.stringify(candidates)} data-index="0" alt={item.restaurantName || "Restaurant"}
      onError={(event) => {
        const img = event.currentTarget;
        let list: string[] = [];
        try { list = JSON.parse(img.dataset.candidates || "[]"); } catch {}
        const next = Number(img.dataset.index || "0") + 1;
        if (next < list.length) { img.dataset.index = String(next); img.src = list[next]; } else { img.style.display = "none"; const fallback = img.nextElementSibling as HTMLElement | null; if (fallback) fallback.style.display = "grid"; }
      }} />
  ) : null;
}

function RestaurantVisual({ item, large = false }: { item: any; large?: boolean }) {
  return <div className={large ? "v19-visual large" : "v19-visual"}><RestaurantThumb item={item} large={large}/><div className={large ? "v17-image-fallback large" : "v17-image-fallback"} style={{display: restaurantImageCandidates(item).length ? "none" : "grid"}}><Store /></div></div>;
}

async function fetchFullRestaurant(item: any) {
  try { return normalizeRestaurant({ ...item, ...(await restaurantService.getDetail(item.id)) }); } catch { return item; }
}

async function downloadPrivateDocument(restaurantId: string, doc: any) {
  const token = getStoredToken();
  const raw = String(doc?.fileUrl ?? doc?.downloadUrl ?? doc?.url ?? doc?.path ?? "").trim();
  const candidates = [
    raw ? productionMediaUrl(raw) : "",
    `https://api.dinibee.tech/api/v1/admin/restaurants/${restaurantId}/documents/${doc?.id}/download`,
    `https://api.dinibee.tech/api/v1/admin/restaurants/${restaurantId}/documents/${doc?.id}`,
  ].filter(Boolean);
  let lastStatus = 0;
  for (const url of candidates) {
    try {
      const response = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
      lastStatus = response.status;
      if (!response.ok) continue;
      const contentType = response.headers.get("content-type") || "";
      if (contentType.includes("application/json")) {
        const payload = await response.json().catch(() => null);
        const nested = payload?.fileUrl ?? payload?.downloadUrl ?? payload?.url ?? payload?.data?.fileUrl ?? payload?.data?.downloadUrl ?? payload?.data?.url;
        if (nested) {
          const nestedResponse = await fetch(productionMediaUrl(nested), { headers: token ? { Authorization: `Bearer ${token}` } : {} });
          if (!nestedResponse.ok) continue;
          const blob = await nestedResponse.blob();
          const objectUrl = URL.createObjectURL(blob);
          const anchor = window.document.createElement("a");
          anchor.href = objectUrl;
          anchor.download = String(doc?.originalFilename ?? doc?.name ?? `${doc?.documentType ?? "document"}.bin`);
          anchor.style.display = "none";
          window.document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();
          window.setTimeout(() => URL.revokeObjectURL(objectUrl), 2000);
          return;
        }
        continue;
      }
      const blob = await response.blob();
      const objectUrl = URL.createObjectURL(blob);
      const anchor = window.document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = String(doc?.originalFilename ?? doc?.name ?? `${doc?.documentType ?? "document"}.bin`);
      anchor.style.display = "none";
      window.document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 2000);
      return;
    } catch {}
  }
  throw new Error(`Document could not be opened${lastStatus ? ` (${lastStatus})` : ""}`);
}

function FilterInput({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  return <label className="v17-search"><Search /><Input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} /></label>;
}

export function RestaurantOnboardingV17() {
  const { data, loading, error, reload } = useApiResource(async () => {
    const list = (await restaurantService.list()).map(normalizeRestaurant);
    return Promise.all(list.map(fetchFullRestaurant));
  }, [], [] as any[]);
  const [status, setStatus] = useState("SUBMITTED");
  const [query, setQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [locationFilter, setLocationFilter] = useState("ALL");
  const [documentFilter, setDocumentFilter] = useState("ALL");
  const [sortOrder, setSortOrder] = useState<"newest"|"oldest">("newest");
  const [selected, setSelected] = useState<any | null>(null);
  const [detailTab, setDetailTab] = useState("overview");

  const pendingStatuses = ["SUBMITTED", "UNDER_REVIEW", "PENDING", "CHANGES_REQUESTED"];
  const countFor = (value: string) => value === "SUBMITTED" ? data.filter(r => pendingStatuses.includes(r.approvalStatus)).length : data.filter(r => r.approvalStatus === value).length;
  const typeOptions = useMemo(() => Array.from(new Set(data.map((r:any)=>String(r.cuisine||"").trim()).filter(Boolean))).sort(), [data]);
  const locationOptions = useMemo(() => Array.from(new Set(data.map((r:any)=>String(r.location||"").trim()).filter(Boolean))).sort(), [data]);
  const hasRequiredDocs = (r:any) => {
    const types = new Set((r.documents||[]).map((d:any)=>String(d.documentType??d.type??"").toUpperCase()));
    return ["FSSAI","PAN","BANK_PROOF"].every((v)=>types.has(v));
  };
  const filtered = useMemo(() => data.filter((r:any) => {
    const statusMatch = status === "SUBMITTED" ? pendingStatuses.includes(r.approvalStatus) : r.approvalStatus === status;
    if (!statusMatch) return false;
    if (typeFilter !== "ALL" && String(r.cuisine) !== typeFilter) return false;
    if (locationFilter !== "ALL" && String(r.location) !== locationFilter) return false;
    if (documentFilter === "COMPLETE" && !hasRequiredDocs(r)) return false;
    if (documentFilter === "INCOMPLETE" && hasRequiredDocs(r)) return false;
    const haystack = `${r.restaurantName} ${r.ownerName} ${r.phone} ${r.email} ${r.location} ${r.cuisine}`.toLowerCase();
    return haystack.includes(query.trim().toLowerCase());
  }).sort((a:any,b:any)=>{
    const av = new Date(a.createdAt??a.updatedAt??0).getTime() || 0;
    const bv = new Date(b.createdAt??b.updatedAt??0).getTime() || 0;
    return sortOrder==="newest" ? bv-av : av-bv;
  }), [data, status, query, typeFilter, locationFilter, documentFilter, sortOrder]);

  async function choose(item: any) {
    const full = await fetchFullRestaurant(item);
    try {
      const docsPayload = await apiClient.get<any>(`/admin/restaurants/${item.id}/documents`);
      const documents = unwrapArray(docsPayload);
      setSelected({ ...full, documents: documents.length ? documents : (full.documents || []) });
    } catch {
      setSelected(full);
    }
    setDetailTab("overview");
  }
  async function approve(item: any) { await restaurantService.updateApproval(item.id, "APPROVED", "Approved by DiniBee admin", item.version ?? item.v ?? 0, item.services ?? []); setSelected(null); await reload(); }
  async function reject(item: any) { await restaurantService.updateApproval(item.id, "REJECTED", "Rejected by DiniBee admin", item.version ?? item.v ?? 0, item.services ?? []); setSelected(null); await reload(); }

  const docs = selected?.documents || [];
  const normalizeDocType = (value: unknown) => String(value ?? "").trim().toUpperCase().replace(/[ -]+/g, "_");
  const docAliases: Record<string, string[]> = {
    FSSAI: ["FSSAI", "FSSAI_LICENSE", "FOOD_LICENSE"],
    PAN: ["PAN", "PAN_CARD"],
    BANK_PROOF: ["BANK_PROOF", "BANK", "BANK_STATEMENT", "CANCELLED_CHEQUE", "CANCELED_CHEQUE"],
    GST: ["GST", "GST_CERTIFICATE", "GSTIN"],
    BUSINESS_REGISTRATION: ["BUSINESS_REGISTRATION", "REGISTRATION_CERTIFICATE", "BUSINESS_CERTIFICATE"],
  };
  const docByType = (type: string) => {
    const aliases = docAliases[type] ?? [type];
    return docs.find((d: any) => aliases.includes(normalizeDocType(d.documentType ?? d.type ?? d.name)));
  };
  const docCards = [["FSSAI License","FSSAI"],["PAN Card","PAN"],["Bank Proof","BANK_PROOF"],["GST Certificate","GST"],["Business Registration","BUSINESS_REGISTRATION"]] as const;

  return <div className="v19-reference-page">
    <div className={selected ? "v19-split-layout has-detail" : "v19-split-layout"}>
      <main className="v19-main-pane">
        <div className="v19-title-row"><div><h1>Restaurant Onboarding</h1><p>Review and manage restaurant partner applications</p></div><div className="v19-view-tools"><select className="v19-native-select v19-compact-select" value={sortOrder} onChange={e=>setSortOrder(e.target.value as "newest"|"oldest")} aria-label="Sort onboarding applications"><option value="newest">Newest First</option><option value="oldest">Oldest First</option></select></div></div>
        <div className="v19-tabbar">{[["SUBMITTED","Pending"],["APPROVED","Approved"],["REJECTED","Rejected"],["SUSPENDED","Suspended"]].map(([value,label]) => <button key={value} className={status===value?"active":""} onClick={()=>setStatus(value)}>{label} <span>({countFor(value)})</span></button>)}</div>
        <div className="v19-filter-row"><select className="v19-native-select" value={typeFilter} onChange={e=>setTypeFilter(e.target.value)} aria-label="Restaurant type"><option value="ALL">All Types</option>{typeOptions.map(v=><option key={v} value={v}>{v}</option>)}</select><select className="v19-native-select" value={locationFilter} onChange={e=>setLocationFilter(e.target.value)} aria-label="Location"><option value="ALL">All Locations</option>{locationOptions.map(v=><option key={v} value={v}>{v}</option>)}</select><select className="v19-native-select" value={documentFilter} onChange={e=>setDocumentFilter(e.target.value)} aria-label="Document status"><option value="ALL">All Documents</option><option value="COMPLETE">Documents Complete</option><option value="INCOMPLETE">Documents Missing</option></select><FilterInput value={query} onChange={setQuery} placeholder="Search by name, owner, phone…"/></div>
        {error ? <div className="page-error panel-card">{error}</div> : null}
        <div className="v19-onboarding-grid">{filtered.map((r) => <article className={selected?.id===r.id?"v19-onboarding-card selected":"v19-onboarding-card"} key={r.id}>
          <div className="v19-card-image"><RestaurantVisual item={r} large/><span>{r.createdAt ? new Date(r.createdAt).toLocaleDateString("en-IN") : "Application"}</span></div>
          <div className="v19-card-copy"><h3>{r.restaurantName}</h3><p>♟ {r.ownerName}</p><p>⌕ {r.phone}</p><p>⌖ {r.location}</p><p>◉ {r.cuisine}</p><div className="v19-doc-tags">{[["FSSAI","FSSAI"],["PAN","PAN"],["Bank","BANK_PROOF"],["GST","GST"]].map(([label,type])=>{const aliases=docAliases[type]??[type];const present=(r.documents||[]).some((d:any)=>aliases.includes(normalizeDocType(d.documentType??d.type??d.name)));return <span key={type} className={present?"":"warn"}>{label} {present?"✓":"×"}</span>})}</div><div className="v19-two-actions"><Button variant="outline" onClick={()=>void choose(r)}>View</Button>{pendingStatuses.includes(r.approvalStatus)?<Button className="gold-button" onClick={()=>void approve(r)}>Approve</Button>:<Button className="gold-button" onClick={()=>void choose(r)}>Details</Button>}</div></div>
        </article>)}{!loading&&!filtered.length?<div className="v17-empty">No restaurant applications in this filter.</div>:null}</div>
      </main>
      {selected ? <aside className="v19-detail-pane">
        <div className="v37-detail-body">
        <button className="v19-close" onClick={()=>setSelected(null)}>×</button>
        <h2>Restaurant Application</h2>
        <div className="v19-hero"><RestaurantVisual item={selected} large/></div>
        <div className="v19-identity-row"><RestaurantVisual item={{...selected, coverImageUrl:selected.logoUrl, imageUrl:selected.logoUrl}}/><div><h3>{selected.restaurantName}</h3><p>{selected.location} · {selected.cuisine}</p><small>Applied on {selected.createdAt ? new Date(selected.createdAt).toLocaleString("en-IN") : "—"}</small></div><StatusBadge status={selected.approvalStatus}/></div>
        <nav className="v19-detail-tabs">{["overview","documents","photos","bank","notes"].map(tab=><button key={tab} className={detailTab===tab?"active":""} onClick={()=>setDetailTab(tab)}>{tab==="bank"?"Bank Details":tab.charAt(0).toUpperCase()+tab.slice(1)}</button>)}</nav>
        {detailTab === "overview" ? <div className="v19-detail-scroll"><h4>Basic Information</h4><dl className="v19-definition-list"><div><dt>Restaurant Name</dt><dd>{selected.restaurantName}</dd></div><div><dt>Owner Name</dt><dd>{selected.ownerName}</dd></div><div><dt>Mobile Number</dt><dd>{selected.phone}</dd></div><div><dt>Email</dt><dd>{selected.email}</dd></div><div><dt>Address</dt><dd>{selected.location}</dd></div><div><dt>Cuisine Type</dt><dd>{selected.cuisine}</dd></div><div><dt>Services</dt><dd>{(selected.services||[]).join(", ")||"—"}</dd></div><div><dt>FSSAI Number</dt><dd>{selected.businessInfo?.fssaiNumber ?? selected.fssaiNumber ?? "—"}</dd></div><div><dt>GST Number</dt><dd>{selected.businessInfo?.gstNumber ?? selected.gstNumber ?? "—"}</dd></div></dl><div className="v19-section-title"><h4>Document Verification</h4><button onClick={()=>setDetailTab("documents")}>View All</button></div><div className="v19-document-cards">{docCards.map(([label,type])=>{const d=docByType(type);return <div key={type}><FileText/><b>{label}</b><StatusBadge status={d?.verificationStatus ?? d?.status ?? (d?"PENDING":"NOT_PROVIDED")}/>{d?<button onClick={()=>downloadPrivateDocument(selected.id,d).catch(e=>toast.error(e.message))}>Download</button>:<small>Not provided</small>}</div>})}</div></div> : null}
        {detailTab === "documents" ? <div className="v19-detail-scroll"><div className="v19-section-title"><h4>Uploaded Documents</h4><span>{docs.length} files</span></div><div className="v19-document-list">{docs.map((d:any)=><div key={d.id}><FileText/><div><b>{d.originalFilename ?? d.name ?? d.documentType}</b><small>{d.documentType} · {d.maskedDocumentNumber ?? "No number"}</small></div><StatusBadge status={d.verificationStatus ?? d.status}/><Button size="sm" variant="outline" onClick={()=>downloadPrivateDocument(selected.id,d).catch(e=>toast.error(e.message))}>Download</Button></div>)}{!docs.length?<p>No uploaded documents found.</p>:null}</div></div>:null}
        {detailTab === "photos" ? <div className="v19-detail-scroll"><h4>Restaurant Photos</h4><div className="v19-photo-grid"><RestaurantVisual item={selected} large/></div></div>:null}
        {detailTab === "bank" ? <div className="v19-detail-scroll"><h4>Bank Details</h4><dl className="v19-definition-list"><div><dt>Account</dt><dd>{selected.settlementProfile?.maskedAccountNumber ?? selected.bankAccount ?? "—"}</dd></div><div><dt>Bank</dt><dd>{selected.settlementProfile?.bankName ?? "—"}</dd></div><div><dt>IFSC</dt><dd>{selected.settlementProfile?.ifscCode ?? "—"}</dd></div></dl></div>:null}
        {detailTab === "notes" ? <div className="v19-detail-scroll"><h4>Review Notes</h4><div className="v23-review-notes">{(() => { const history = selected.approvalHistory ?? selected.reviewHistory ?? selected.auditTrail ?? []; const notes = [selected.reviewNote, selected.approvalReason, selected.notes].filter(Boolean); return <>{notes.map((n:any,i:number)=><div className="v23-note-card" key={`n-${i}`}><b>Admin review</b><p>{String(n)}</p></div>)}{Array.isArray(history)&&history.length?history.map((h:any,i:number)=><div className="v23-note-card" key={`h-${i}`}><div><b>{text(h.status ?? h.action,"Review")}</b><small>{text(h.date ?? h.createdAt ?? h.updatedAt,"")}</small></div><p>{text(h.note ?? h.reason ?? h.comment,"No note")}</p><small>{text(h.admin ?? h.adminName ?? h.createdBy,"")}</small></div>):null}{!notes.length&&(!Array.isArray(history)||!history.length)?<p className="v19-muted">No review notes have been recorded yet.</p>:null}</> })()}</div></div>:null}
        </div>
        {pendingStatuses.includes(selected.approvalStatus)?<div className="v19-bottom-actions"><Button className="success-button" onClick={()=>void approve(selected)}>✓ Approve Restaurant</Button><Button className="danger-button" onClick={()=>void reject(selected)}>× Reject Application</Button><Button className="gold-button">↻ Request Changes</Button></div>:null}
      </aside> : null}
    </div>
  </div>;
}

export function RestaurantsV17() {
  const { data, loading, error } = useApiResource(async () => {
    const list = (await restaurantService.list()).map(normalizeRestaurant);
    return Promise.all(list.map(fetchFullRestaurant));
  }, [], [] as any[]);
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [locationFilter, setLocationFilter] = useState("ALL");
  const [selected, setSelected] = useState<any | null>(null);
  const [detailTab, setDetailTab] = useState("overview");
  const [period, setPeriod] = useState<"today"|"week"|"month">("today");
  const [restaurantData, setRestaurantData] = useState<any>({ payments: [], takeaway: [], tickets: [], reviews: [], menu: [], settings: null });
  const restaurantStatuses=useMemo(()=>Array.from(new Set(data.map((r:any)=>String(r.approvalStatus||"").trim()).filter(Boolean))).sort(),[data]);
  const restaurantTypes=useMemo(()=>Array.from(new Set(data.map((r:any)=>String(r.cuisine||"").trim()).filter(Boolean))).sort(),[data]);
  const restaurantLocations=useMemo(()=>Array.from(new Set(data.map((r:any)=>String(r.location||"").trim()).filter(Boolean))).sort(),[data]);
  const filtered = data.filter((r:any) => {
    if(statusFilter!=="ALL" && String(r.approvalStatus)!==statusFilter) return false;
    if(typeFilter!=="ALL" && String(r.cuisine)!==typeFilter) return false;
    if(locationFilter!=="ALL" && String(r.location)!==locationFilter) return false;
    return `${r.restaurantName} ${r.location} ${r.cuisine} ${r.ownerName??""}`.toLowerCase().includes(query.trim().toLowerCase());
  });

  const sameRestaurant = (row:any, restaurant:any) => String(row?.restaurantId ?? "") === String(restaurant?.id ?? "") || String(row?.restaurantName ?? "").trim().toLowerCase() === String(restaurant?.restaurantName ?? "").trim().toLowerCase();
  const inPeriod = (raw:any) => {
    const value = raw?.createdAt ?? raw?.date ?? raw?.paidAt ?? raw?.bookedAt;
    const d = value ? new Date(value) : null;
    if (!d || Number.isNaN(d.getTime())) return true;
    const now = new Date();
    const start = new Date(now);
    if (period === "today") start.setHours(0,0,0,0);
    else if (period === "week") { const day=(now.getDay()+6)%7; start.setDate(now.getDate()-day); start.setHours(0,0,0,0); }
    else { start.setDate(1); start.setHours(0,0,0,0); }
    return d >= start && d <= now;
  };

  async function choose(item:any){
    const full = await fetchFullRestaurant(item);
    setSelected(full); setDetailTab("overview"); setPeriod("today");
    const [payments, takeaway, tickets, reviews, menu, settings] = await Promise.all([
      paymentService.listTransactions().catch(()=>[]),
      takeawayService.list().catch(()=>[]),
      diniTicketService.list().catch(()=>[]),
      reviewService.list().catch(()=>[]),
      apiClient.get<any>(`/admin/restaurants/${item.id}/menu`).then(unwrapArray).catch(()=>[]),
      apiClient.get<any>(`/admin/restaurants/${item.id}/settings`).catch(()=>null),
    ]);
    setRestaurantData({
      payments: (payments as any[]).filter(x=>sameRestaurant(x, full)),
      takeaway: (takeaway as any[]).filter(x=>sameRestaurant(x, full)),
      tickets: (tickets as any[]).filter(x=>sameRestaurant(x, full)),
      reviews: (reviews as any[]).filter(x=>sameRestaurant(x, full)),
      menu,
      settings,
    });
  }

  const periodPayments=(restaurantData.payments||[]).filter(inPeriod);
  const periodTakeaway=(restaurantData.takeaway||[]).filter(inPeriod);
  const periodTickets=(restaurantData.tickets||[]).filter(inPeriod);
  const orderRows=[...periodPayments].sort((a:any,b:any)=>new Date(b.date??b.createdAt??0).getTime()-new Date(a.date??a.createdAt??0).getTime());
  const totalOrders = orderRows.length || periodTakeaway.length + periodTickets.length;
  const totalAmount = periodPayments.reduce((a:number,b:any)=>a+Number(b.gatewayPaidAmount??b.amount??0),0);
  const itemsOrdered = periodTakeaway.length + periodTickets.length;
  const uniqueCustomers = new Set([...periodPayments,...periodTakeaway,...periodTickets].map((x:any)=>String(x.customerName??x.customerId??"")).filter(Boolean)).size;
  const dayRevenue = Array.from({length:7},(_,i)=>{ const d=new Date(); d.setDate(d.getDate()-(6-i)); const key=d.toDateString(); return periodPayments.filter((x:any)=>{const rd=new Date(x.date??x.createdAt??0);return rd.toDateString()===key}).reduce((a:number,b:any)=>a+Number(b.gatewayPaidAmount??b.amount??0),0); });
  const maxRevenue=Math.max(1,...dayRevenue);
  const points=dayRevenue.map((v,i)=>`${i*100},${120-(v/maxRevenue)*90}`).join(" ");

  return <div className="v19-reference-page"><div className={selected?"v19-split-layout restaurants has-detail":"v19-split-layout restaurants"}>
    <main className="v19-main-pane"><div className="v19-title-row"><div><h1>Restaurants <span>({data.length})</span></h1><p>Live restaurant profiles from DiniBee</p></div></div>
      <div className="v23-restaurant-search"><FilterInput value={query} onChange={setQuery} placeholder="Search restaurant name…"/></div>
      <div className="v19-filter-row restaurant v23-filter-second-line"><select className="v19-native-select" value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}><option value="ALL">All Status</option>{restaurantStatuses.map(v=><option key={v} value={v}>{v==="APPROVED"?"Active":v.replaceAll("_"," ")}</option>)}</select><select className="v19-native-select" value={typeFilter} onChange={e=>setTypeFilter(e.target.value)}><option value="ALL">All Types</option>{restaurantTypes.map(v=><option key={v} value={v}>{v}</option>)}</select><select className="v19-native-select" value={locationFilter} onChange={e=>setLocationFilter(e.target.value)}><option value="ALL">All Locations</option>{restaurantLocations.map(v=><option key={v} value={v}>{v}</option>)}</select></div>
      {error?<div className="page-error panel-card">{error}</div>:null}
      <div className="v19-restaurants-grid">{filtered.map(r=><button className={selected?.id===r.id?"v19-restaurant-card selected":"v19-restaurant-card"} key={r.id} onClick={()=>void choose(r)}><div className="v19-card-image restaurant"><RestaurantVisual item={r} large/><span className="v19-status-chip">{r.approvalStatus==="APPROVED"?"Active":r.approvalStatus}</span></div><div className="v19-card-copy"><h3>{r.restaurantName}</h3><p className="rating">★ {r.rating || "—"} <small>· {r.cuisine}</small></p><p>⌖ {r.location}</p><div className="v19-restaurant-metrics"><span><ShoppingBag size={16}/> <b>{r.orders}</b><small>Orders Today</small></span><span>₹ <b>{money(r.todaySales)}</b><small>Sales Today</small></span></div></div></button>)}{!loading&&!filtered.length?<div className="v17-empty">No restaurants found.</div>:null}</div>
    </main>
    {selected?<aside className="v19-detail-pane restaurant-detail"><button className="v19-close" onClick={()=>setSelected(null)}>×</button><div className="v19-hero restaurant"><RestaurantVisual item={selected} large/></div><div className="v19-identity-row restaurant"><RestaurantVisual item={{...selected,coverImageUrl:selected.logoUrl,imageUrl:selected.logoUrl}}/><div><h2>{selected.restaurantName}</h2><p>{selected.location} · {selected.cuisine}</p><small>{selected.phone}</small></div><StatusBadge status={selected.approvalStatus==="APPROVED"?"ACTIVE":selected.approvalStatus}/></div><nav className="v19-detail-tabs">{["overview","orders","menu","reviews","gallery","settings"].map(tab=><button key={tab} className={detailTab===tab?"active":""} onClick={()=>setDetailTab(tab)}>{tab.charAt(0).toUpperCase()+tab.slice(1)}</button>)}</nav>
      {detailTab==="overview"?<div className="v19-detail-scroll restaurant-overview"><div className="v19-period-row"><button className={period==="today"?"active":""} onClick={()=>setPeriod("today")}>Today</button><button className={period==="week"?"active":""} onClick={()=>setPeriod("week")}>This Week</button><button className={period==="month"?"active":""} onClick={()=>setPeriod("month")}>This Month</button><span>📅 {new Date().toLocaleDateString("en-IN")}</span></div><div className="v19-kpi-cards"><div><span className="v23-kpi-icon blue"><ShoppingBag/></span><b>{totalOrders}</b><small>Total Orders</small></div><div><span className="v23-kpi-icon green"><IndianRupee/></span><b>{money(totalAmount)}</b><small>Total Order Amount</small></div><div><span className="v23-kpi-icon purple"><Package/></span><b>{itemsOrdered}</b><small>Items Ordered</small></div><div><span className="v23-kpi-icon orange"><Users/></span><b>{uniqueCustomers}</b><small>Unique Customers</small></div></div>
      <section className="v19-sales-panel"><div className="v19-section-title"><h4>Sales Overview</h4><span>Revenue</span></div><div className="v23-sales-chart"><svg viewBox="0 0 600 140" preserveAspectRatio="none"><defs><linearGradient id="salesFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#ff9800" stopOpacity=".24"/><stop offset="100%" stopColor="#ff9800" stopOpacity=".02"/></linearGradient></defs><polyline points={points} fill="none" stroke="currentColor" strokeWidth="3"/><polygon points={`0,140 ${points} 600,140`} fill="url(#salesFill)"/>{dayRevenue.map((v,i)=><circle key={i} cx={i*100} cy={120-(v/maxRevenue)*90} r="4" fill="currentColor"/> )}</svg><div className="v23-chart-labels">{["Mon","Tue","Wed","Thu","Fri","Sat","Sun"].map(x=><span key={x}>{x}</span>)}</div></div></section>
      <section className="v19-recent-orders"><div className="v19-section-title"><h4>Recent Orders ({period==="today"?"Today":period==="week"?"This Week":"This Month"})</h4><span>{orderRows.length} live</span></div><div className="v23-recent-order-table"><div className="head"><b>Order ID</b><b>Customer</b><b>Items</b><b>Amount</b><b>Payment</b><b>Status</b><b>Time</b><b>Action</b></div>{orderRows.slice(0,8).map((r:any)=><div key={r.id}><span>#{text(r.referenceId??r.orderNumber??r.id).slice(0,10)}</span><span>{text(r.customerName)}</span><span>{r.items??1}</span><b>{money(r.gatewayPaidAmount??r.amount)}</b><span>{text(r.paymentMethod??r.paymentStatus)}</span><StatusBadge status={r.status??r.paymentStatus}/><span>{text(r.date??r.createdAt)}</span><button onClick={()=>toast.info(`Order ${text(r.referenceId??r.id)}`)}>View</button></div>)}{!orderRows.length?<p>No orders in this period.</p>:null}</div></section></div>:null}
      {detailTab==="orders"?<div className="v19-detail-scroll"><h4>Orders</h4><div className="v23-module-list">{[...restaurantData.takeaway,...restaurantData.tickets].map((r:any)=><div key={r.id}><b>{r.orderNumber||r.tokenNumber}</b><span>{r.customerName}</span><span>{money(r.amount)}</span><StatusBadge status={r.status||r.queueStatus}/></div>)}{!restaurantData.takeaway.length&&!restaurantData.tickets.length?<p>No live orders returned.</p>:null}</div></div>:null}
      {detailTab==="menu"?<div className="v19-detail-scroll"><h4>Menu</h4><div className="v23-module-list">{(restaurantData.menu||[]).map((m:any,i:number)=><div key={m.id??i}><b>{text(m.name??m.itemName,"Menu Item")}</b><span>{text(m.category)}</span><span>{money(m.price)}</span><StatusBadge status={(m.available??m.isAvailable)!==false?"ACTIVE":"INACTIVE"}/></div>)}{!restaurantData.menu?.length?<p>No menu items returned by the admin menu endpoint.</p>:null}</div></div>:null}
      {detailTab==="reviews"?<div className="v19-detail-scroll"><h4>Reviews</h4><div className="v23-review-notes">{(restaurantData.reviews||[]).map((r:any)=><div className="v23-note-card" key={r.id}><div><b>{r.customerName}</b><span>★ {r.restaurantRating}</span></div><p>{r.reviewText||"No comment"}</p><small>{r.createdAt}</small></div>)}{!restaurantData.reviews?.length?<p>No reviews found.</p>:null}</div></div>:null}
      {detailTab==="gallery"?<div className="v19-detail-scroll"><h4>Gallery</h4><div className="v23-gallery">{restaurantImageCandidates(selected).map((src:string,i:number)=><img key={i} src={src} alt={`${selected.restaurantName} ${i+1}`}/>)}</div></div>:null}
      {detailTab==="settings"?<div className="v19-detail-scroll"><h4>Settings</h4><dl className="v19-definition-list"><div><dt>Services</dt><dd>{(selected.services||[]).join(", ")||"—"}</dd></div><div><dt>Approval Status</dt><dd>{selected.approvalStatus}</dd></div><div><dt>Operating Hours</dt><dd>{text(selected.operatingHours)}</dd></div><div><dt>Reservation Settings</dt><dd>{text(selected.reservationSettings?.mode??selected.reservationSettings)}</dd></div><div><dt>Takeaway Settings</dt><dd>{text(selected.takeawaySettings?.enabled??selected.takeawaySettings)}</dd></div></dl></div>:null}
    </aside>:null}
  </div></div>;
}
export function CustomersV17() {
  const resource = useApiResource(async () => {
    const payload = await adminApi.customers();
    return unwrapArray(payload).map(normalizeCustomer);
  }, [], [] as any[]);
  const { data, loading, error, reload } = resource;
  const [query, setQuery] = useState("");
  const [customerType, setCustomerType] = useState("ALL");
  const [customerStatus, setCustomerStatus] = useState("ALL");
  const [customerLocation, setCustomerLocation] = useState("ALL");
  const [customerSort, setCustomerSort] = useState<"newest"|"oldest">("newest");
  const [selected, setSelected] = useState<any | null>(null);
  const [tab, setTab] = useState("overview");
  const [detail, setDetail] = useState<any>({ orders:[], reservations:[], addresses:[], wallet:null, walletTx:[], support:[], activity:[], reviews:[] });
  const customerLocations=useMemo(()=>Array.from(new Set(data.map((c:any)=>String(c.location||"").trim()).filter((v:string)=>v && v!=="—"))).sort(),[data]);
  const filtered = data.filter((c:any) => {
    if(customerType==="PREMIUM"&&!c.premium) return false;
    if(customerType==="NON_PREMIUM"&&c.premium) return false;
    if(customerStatus!=="ALL"&&String(c.status).toUpperCase()!==customerStatus) return false;
    if(customerLocation!=="ALL"&&String(c.location)!==customerLocation) return false;
    return `${c.name} ${c.phone} ${c.email} ${c.location}`.toLowerCase().includes(query.trim().toLowerCase());
  }).sort((a:any,b:any)=>{
    const av=new Date(a.createdAt??a.joinedAt??0).getTime()||0;
    const bv=new Date(b.createdAt??b.joinedAt??0).getTime()||0;
    return customerSort==="newest"?bv-av:av-bv;
  });
  const premium = data.filter((c) => c.premium).length;
  const nonPremium = data.length - premium;
  const pct = (n:number) => data.length ? Math.round((n / data.length) * 1000) / 10 : 0;
  const initials=(name:string)=>name.split(/\s+/).map(x=>x[0]).join("").slice(0,2).toUpperCase();
  const customerMatch=(row:any,c:any)=>String(row?.customerId??"")===String(c.id)||String(row?.customerName??row?.userName??"").trim().toLowerCase()===String(c.name).trim().toLowerCase();

  async function chooseCustomer(c:any){
    setSelected(c); setTab("overview");
    const [reservations,takeaway,tickets,payments,wallets,walletTx,support,reviews,profile] = await Promise.all([
      reservationService.list().catch(()=>[]), takeawayService.list().catch(()=>[]), diniTicketService.list().catch(()=>[]), paymentService.listTransactions().catch(()=>[]),
      walletService.listWallets().catch(()=>[]), walletService.listTransactions().catch(()=>[]), supportService.list("CUSTOMER").catch(()=>[]), reviewService.list().catch(()=>[]),
      apiClient.get<any>(`/admin/users/${c.id}`).catch(()=>null),
    ]);
    const profileData=(profile as any)?.data??profile??{};
    const res=(reservations as any[]).filter(x=>customerMatch(x,c));
    const take=(takeaway as any[]).filter(x=>customerMatch(x,c));
    const tick=(tickets as any[]).filter(x=>customerMatch(x,c));
    const pay=(payments as any[]).filter(x=>customerMatch(x,c));
    const sup=(support as any[]).filter(x=>customerMatch(x,c));
    const rev=(reviews as any[]).filter(x=>customerMatch(x,c));
    const wallet=(wallets as any[]).find(x=>String(x.customerId)===String(c.id)||String(x.customerName).toLowerCase()===String(c.name).toLowerCase())??null;
    const tx=(walletTx as any[]).filter(x=>String(x.customerId)===String(c.id)||String(x.customerName).toLowerCase()===String(c.name).toLowerCase());
    const addresses=profileData?.addresses??profileData?.savedAddresses??profileData?.addressBook??(profileData?.address?[profileData.address]:[]);
    const activity=[...res.map(x=>({type:"Booking",title:x.bookingNumber,status:x.status,date:x.date})),...take.map(x=>({type:"Takeaway",title:x.orderNumber,status:x.status,date:x.createdAt})),...tick.map(x=>({type:"DiniTicket",title:x.tokenNumber,status:x.queueStatus,date:x.createdAt}))].sort((a,b)=>new Date(b.date??0).getTime()-new Date(a.date??0).getTime());
    setSelected({...c,...profileData,premium:normalizeCustomer({...c,...profileData}).premium});
    setDetail({orders:[...take,...tick,...pay],reservations:res,addresses,wallet,walletTx:tx,support:sup,activity,reviews:rev});
  }

  async function blockCustomer(){
    if(!selected) return;
    const nextBlocked=selected.status!=="BLOCKED";
    try{
      const endpoint=`/admin/users/${selected.id}/${nextBlocked?"block":"unblock"}`;
      try{
        await apiClient.put<any>(endpoint);
      }catch(first:any){
        await apiClient.put<any>(`${endpoint}?reason=${encodeURIComponent("Updated by DiniBee admin")}`);
      }
    }catch(e:any){
      toast.error(e?.message??"Unable to update customer status");
      return;
    }
    setSelected({...selected,status:nextBlocked?"BLOCKED":"ACTIVE"}); toast.success(nextBlocked?"Customer blocked":"Customer unblocked"); await reload();
  }
  async function sendCustomerNotification(){
    if(!selected) return;
    const message=window.prompt(`Notification message for ${selected.name}`,"Your DiniBee account has a new update.");
    if(!message?.trim()) return;
    try{await notificationService.send({title:"DiniBee Update",message:message.trim(),type:"SYSTEM_NOTICE",audience:"SELECTED_CUSTOMERS",recipientIds:[selected.id]});toast.success("Notification sent");}catch(e:any){toast.error(e?.message??"Unable to send notification");}
  }
  async function viewInApp(){
    if(!selected) return;
    try{await navigator.clipboard?.writeText(selected.id);}catch{}
    toast.info("Customer ID copied. Open the DiniBee customer app and search this customer ID.");
  }

  const avgRating=detail.reviews?.length? (detail.reviews.reduce((a:number,b:any)=>a+Number(b.restaurantRating??0),0)/detail.reviews.length).toFixed(1):"—";
  const exportCustomers=()=>csvDownload("dinibee-customers.csv",filtered.map((c:any)=>({Customer:c.name,Phone:c.phone,Email:c.email,Location:c.location,Type:c.premium?"Premium":"Non-Premium",Status:c.status,Joined:c.joined,Orders:c.orders,TotalSpent:c.totalSpent})));

  return <div className="ref-page customers-ref">
    <div className="ref-page-head"><div><h1>Customers <span>({data.length.toLocaleString("en-IN")})</span></h1><p>All users from DiniBee customer app</p></div><div className="ref-head-actions"><Button variant="outline" onClick={exportCustomers}>⇩ Export</Button></div></div>
    <div className="ref-customer-summary"><article className="purple"><div className="ref-summary-icon crown"><Crown/></div><div><small>Premium Customers</small><strong>{premium}</strong><span>{pct(premium)}% of total users</span></div></article><article><div className="ref-summary-icon gray"><UserRound/></div><div><small>Non-Premium Customers</small><strong>{nonPremium}</strong><span>{pct(nonPremium)}% of total users</span></div></article></div>
    <div className="ref-filter-row customers"><FilterInput value={query} onChange={setQuery} placeholder="Search by name, phone, email…"/><select className="ref-select" value={customerType} onChange={e=>setCustomerType(e.target.value)}><option value="ALL">All Customer Types</option><option value="PREMIUM">Premium</option><option value="NON_PREMIUM">Non-Premium</option></select><select className="ref-select" value={customerStatus} onChange={e=>setCustomerStatus(e.target.value)}><option value="ALL">All Status</option><option value="ACTIVE">Active</option><option value="BLOCKED">Blocked</option></select><select className="ref-select" value={customerLocation} onChange={e=>setCustomerLocation(e.target.value)}><option value="ALL">All Locations</option>{customerLocations.map(v=><option key={v} value={v}>{v}</option>)}</select><select className="ref-select" value={customerSort} onChange={e=>setCustomerSort(e.target.value as "newest"|"oldest")}><option value="newest">Newest First</option><option value="oldest">Oldest First</option></select></div>
    {error?<div className="page-error panel-card">{error}</div>:null}
    <div className={selected?"ref-master-detail customer-detail-open":"ref-master-detail"}>
      <main className="ref-customer-grid">{filtered.map((c)=><article key={c.id} className={selected?.id===c.id?"ref-customer-card selected":"ref-customer-card"}><div className="ref-customer-card-top">{c.imageUrl?<img src={c.imageUrl} alt=""/>:<div className="ref-avatar">{initials(c.name)}</div>}<div><h3>{c.name}</h3><span className={c.premium?"ref-tag premium":"ref-tag"}>{c.premium?"★ Premium":"Non-Premium"}</span><span className={c.status==="BLOCKED"?"ref-tag danger":"ref-tag active"}>{c.status==="BLOCKED"?"Blocked":"Active"}</span></div></div><div className="ref-contact"><p>☎ {c.phone}</p><p>✉ {c.email}</p><p>⌖ {c.location}</p><p>▣ Joined {c.joined}</p></div><div className="ref-card-metrics"><div><b>{c.orders}</b><small>Total Orders</small></div><div><b>{money(c.totalSpent)}</b><small>Total Spent</small></div></div><button className="ref-card-action" onClick={()=>void chooseCustomer(c)}>View Details</button></article>)}{!loading&&!filtered.length?<div className="v17-empty">No customers found.</div>:null}</main>
      {selected?<aside className="ref-detail-panel customer-panel"><button className="ref-x" onClick={()=>setSelected(null)}>×</button><div className="ref-profile-banner">{selected.imageUrl?<img src={selected.imageUrl} alt=""/>:<div className="ref-avatar xl">{initials(selected.name)}</div>}<div><h2>{selected.name} {selected.premium?<span className="ref-tag premium">★ Premium</span>:<span className="ref-tag">Non-Premium</span>} <span className={selected.status==="BLOCKED"?"ref-tag danger":"ref-tag active"}>{selected.status==="BLOCKED"?"Blocked":"Active"}</span></h2><p>Customer ID: #{selected.id.slice(0,10).toUpperCase()}</p><p>Joined on {selected.joined}</p></div></div>
        <nav className="ref-tabs">{["overview","orders","addresses","wallet","support","activity"].map(t=><button key={t} className={tab===t?"active":""} onClick={()=>setTab(t)}>{t[0].toUpperCase()+t.slice(1)}</button>)}</nav>
        {tab==="overview"?<div className="ref-detail-scroll"><div className="ref-section-title"><h3>Basic Information</h3><button>✎ Edit</button></div><dl className="ref-info-list"><div><dt>♙ Name</dt><dd>{selected.name}</dd></div><div><dt>☎ Phone</dt><dd>{selected.phone}</dd></div><div><dt>✉ Email</dt><dd>{selected.email}</dd></div><div><dt>▣ Date of Birth</dt><dd>{text(selected.dateOfBirth??selected.dob)}</dd></div><div><dt>⚥ Gender</dt><dd>{text(selected.gender)}</dd></div><div><dt>⌖ Location</dt><dd>{selected.location}</dd></div><div><dt>▣ Member Since</dt><dd>{selected.joined}</dd></div><div><dt>♛ Customer Type</dt><dd>{selected.premium?<span className="ref-tag premium">Premium Customer</span>:<span className="ref-tag">Non-Premium Customer</span>}</dd></div><div><dt>◉ Status</dt><dd><span className={selected.status==="BLOCKED"?"ref-tag danger":"ref-tag active"}>{selected.status}</span></dd></div></dl><div className="ref-customer-kpis"><div><span className="v23-kpi-icon purple"><Crown/></span><b>{selected.orders}</b><small>Total Orders</small></div><div><span className="v23-kpi-icon green"><IndianRupee/></span><b>{money(selected.totalSpent)}</b><small>Total Spent</small></div><div><span className="v23-kpi-icon orange"><Star/></span><b>{avgRating}</b><small>Avg. Rating</small></div><div><span className="v23-kpi-icon red"><Heart/></span><b>{selected.savedItemsCount??0}</b><small>Saved Items</small></div></div><div className="ref-section-title"><h3>Recent Orders</h3><button onClick={()=>setTab("orders")}>View All</button></div><div className="v23-module-list">{detail.orders.slice(0,5).map((o:any,i:number)=><div key={o.id??i}><b>{text(o.orderNumber??o.referenceId??o.tokenNumber)}</b><span>{text(o.restaurantName)}</span><span>{money(o.amount??o.gatewayPaidAmount)}</span><StatusBadge status={o.status??o.queueStatus??o.paymentStatus}/></div>)}{!detail.orders.length?<p>No recent orders.</p>:null}</div></div>:null}
        {tab==="orders"?<div className="ref-detail-scroll"><h3>Orders</h3><div className="v23-module-list">{detail.orders.map((o:any,i:number)=><div key={o.id??i}><b>{text(o.orderNumber??o.referenceId??o.tokenNumber)}</b><span>{text(o.restaurantName)}</span><span>{money(o.amount??o.gatewayPaidAmount)}</span><StatusBadge status={o.status??o.queueStatus??o.paymentStatus}/></div>)}{!detail.orders.length?<p>No live orders found.</p>:null}</div></div>:null}
        {tab==="addresses"?<div className="ref-detail-scroll"><h3>Addresses</h3><div className="v23-address-list">{(detail.addresses||[]).map((a:any,i:number)=><div className="v23-note-card" key={a.id??i}><b>{text(a.label??a.type,"Address")}</b><p>{[a.addressLine1,a.addressLine2,a.area,a.city,a.state,a.postalCode].filter(Boolean).join(", ")||text(a)}</p></div>)}{!detail.addresses?.length?<p>No saved addresses returned by customer profile.</p>:null}</div></div>:null}
        {tab==="wallet"?<div className="ref-detail-scroll"><h3>Wallet</h3>{detail.wallet?<><div className="v23-wallet-card"><IndianRupee/><div><small>Points Balance</small><b>{detail.wallet.balance}</b><span>{money(detail.wallet.approximateRedeemValue)}</span></div></div><div className="v23-module-list">{detail.walletTx.map((x:any)=><div key={x.id}><b>{x.type}</b><span>{x.reason}</span><span>{x.points>0?`+${x.points}`:x.points} pts</span><small>{x.createdAt}</small></div>)}</div></>:<p>No wallet account found.</p>}</div>:null}
        {tab==="support"?<div className="ref-detail-scroll"><h3>Support</h3><div className="v23-module-list">{detail.support.map((x:any)=><div key={x.id}><b>{x.ticketNumber}</b><span>{x.category}</span><span>{x.lastMessage}</span><StatusBadge status={x.status}/></div>)}{!detail.support.length?<p>No support tickets found.</p>:null}</div></div>:null}
        {tab==="activity"?<div className="ref-detail-scroll"><h3>Activity</h3><div className="v23-activity-list">{detail.activity.map((x:any,i:number)=><div key={i}><span className="dot"/><div><b>{x.type}: {x.title}</b><p>{x.status}</p><small>{x.date}</small></div></div>)}{!detail.activity.length?<p>No activity available.</p>:null}</div></div>:null}
        <div className="ref-bottom-actions"><button className="danger" onClick={()=>void blockCustomer()}><Ban/> {selected.status==="BLOCKED"?"Unblock User":"Block User"}</button><button onClick={()=>void sendCustomerNotification()}><Bell/> Send Notification</button><button onClick={viewInApp}><ExternalLink/> View in App</button></div>
      </aside>:null}
    </div>
  </div>;
}
export function ReservationsV17() {
  const { data, loading, error, reload } = useApiResource(() => reservationService.list(), [], [] as ReservationRecord[]);
  const [selected, setSelected] = useState<any | null>(null);
  const [query, setQuery] = useState("");
  const [tab,setTab]=useState("overview");
  const [statusTab,setStatusTab]=useState("ALL");
  const [restaurantFilter,setRestaurantFilter]=useState("ALL");
  const [statusFilter,setStatusFilter]=useState("ALL");
  const [partyFilter,setPartyFilter]=useState("ALL");
  const [dateFilter,setDateFilter]=useState("ALL");
  const [history,setHistory]=useState<any[]>([]);
  const [restaurantDetails,setRestaurantDetails]=useState<Record<string,any>>({});
  const [customers,setCustomers]=useState<any[]>([]);

  useEffect(()=>{
    let alive=true;
    (async()=>{
      const ids=Array.from(new Set(data.map((r:any)=>String(r.restaurantId||"")).filter(Boolean)));
      const pairs=await Promise.all(ids.map(async id=>{
        try{return [id,await restaurantService.getDetail(id)] as const;}catch{return [id,null] as const;}
      }));
      if(alive) setRestaurantDetails(Object.fromEntries(pairs.filter(([,v])=>v)));
      try{
        const payload=await adminApi.customers();
        if(alive) setCustomers(unwrapArray(payload).map(normalizeCustomer));
      }catch{}
    })();
    return()=>{alive=false};
  },[data]);

  const restaurantOptions=useMemo(()=>Array.from(new Set(data.map(r=>r.restaurantName).filter(Boolean))).sort(),[data]);
  const confirmedStatuses=new Set(["CONFIRMED","ARRIVED","CHECKED_IN","DINING","COMPLETED"]);
  const confirmed=data.filter(r=>confirmedStatuses.has(r.status)).length;
  const pending=data.filter(r=>r.status==="PENDING").length;
  const cancelled=data.filter(r=>r.status==="CANCELLED").length;

  function inDateFilter(r:any){
    if(dateFilter==="ALL") return true;
    const d=new Date(`${r.date}T00:00:00`); if(Number.isNaN(d.getTime())) return true;
    const now=new Date(); now.setHours(0,0,0,0);
    if(dateFilter==="TODAY") return d.getTime()===now.getTime();
    if(dateFilter==="TOMORROW"){const t=new Date(now);t.setDate(t.getDate()+1);return d.getTime()===t.getTime();}
    if(dateFilter==="WEEK"){const e=new Date(now);e.setDate(e.getDate()+7);return d>=now&&d<=e;}
    return true;
  }

  const filtered=data.filter((r:any)=>{
    const q=`${r.bookingNumber} ${r.customerName} ${r.customerPhone??""} ${r.customerEmail??""} ${r.restaurantName}`.toLowerCase();
    if(query&&!q.includes(query.toLowerCase())) return false;
    if(statusTab==="CONFIRMED"&&!confirmedStatuses.has(r.status)) return false;
    if(statusTab==="PENDING"&&r.status!=="PENDING") return false;
    if(statusTab==="CANCELLED"&&r.status!=="CANCELLED") return false;
    if(restaurantFilter!=="ALL"&&r.restaurantName!==restaurantFilter) return false;
    if(statusFilter!=="ALL"&&r.status!==statusFilter) return false;
    if(partyFilter!=="ALL"&&String(r.guestCount)!==partyFilter) return false;
    if(!inDateFilter(r)) return false;
    return true;
  });

  async function chooseReservation(r:any){
    setSelected(r); setTab("overview"); setHistory([]);
    try{setHistory(await reservationService.history(r.id));}catch{
      setHistory([{newStatus:r.status,oldStatus:null,reason:"Current reservation status",changedAt:r.updatedAt??r.createdAt??r.date}]);
    }
  }

  const selectedRestaurant=selected?restaurantDetails[String(selected.restaurantId)]??{}:{};
  const selectedCustomer=selected?customers.find(c=>String(c.name).trim().toLowerCase()===String(selected.customerName).trim().toLowerCase())??{}:{};
  const selectedImage=selected?productionMediaUrl(selected.restaurantCoverImageUrl)||productionMediaUrl(selected.restaurantLogoUrl)||restaurantImage(selectedRestaurant):"";
  const selectedPhone=text(selected?.customerPhone??selectedCustomer.phone);
  const selectedEmail=text(selected?.customerEmail??selectedCustomer.email);
  const selectedLocation=text(selected?.restaurantLocation??selectedRestaurant.location);

  function resetFilters(){setQuery("");setRestaurantFilter("ALL");setStatusFilter("ALL");setPartyFilter("ALL");setDateFilter("ALL");setStatusTab("ALL");}

  return <div className="ref-page bookings-ref">
    <div className="ref-page-head"><div><h1>Table Bookings</h1><p>All restaurant reservations from DiniBee platform</p></div><div className="ref-head-actions"><button className="ref-date">▣ Live Reservations</button><Button variant="outline" onClick={()=>void reload()}>↻ Refresh</Button></div></div>
    <nav className="ref-tabs top">
      <button className={statusTab==="ALL"?"active":""} onClick={()=>setStatusTab("ALL")}>All Bookings ({data.length})</button>
      <button className={statusTab==="CONFIRMED"?"active":""} onClick={()=>setStatusTab("CONFIRMED")}>Confirmed ({confirmed})</button>
      <button className={statusTab==="PENDING"?"active":""} onClick={()=>setStatusTab("PENDING")}>Pending ({pending})</button>
      <button className={statusTab==="CANCELLED"?"active":""} onClick={()=>setStatusTab("CANCELLED")}>Cancelled ({cancelled})</button>
    </nav>
    <div className="ref-stat-row"><article className="mint"><span>▣</span><div><small>Total Bookings</small><b>{data.length.toLocaleString("en-IN")}</b><em>↑ live</em></div></article><article className="blue"><span>✓</span><div><small>Confirmed</small><b>{confirmed}</b><em>{data.length?Math.round(confirmed/data.length*100):0}% of total</em></div></article><article className="orange"><span>◷</span><div><small>Pending</small><b>{pending}</b><em>{data.length?Math.round(pending/data.length*100):0}% of total</em></div></article><article className="red"><span>×</span><div><small>Cancelled</small><b>{cancelled}</b><em>{data.length?Math.round(cancelled/data.length*100):0}% of total</em></div></article></div>
    <div className="ref-filter-row bookings">
      <select className="ref-select" value={restaurantFilter} onChange={e=>setRestaurantFilter(e.target.value)}><option value="ALL">All Restaurants</option>{restaurantOptions.map(x=><option key={x} value={x}>{x}</option>)}</select>
      <select className="ref-select" value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}><option value="ALL">All Status</option>{["PENDING","CONFIRMED","ARRIVED","CHECKED_IN","DINING","COMPLETED","CANCELLED","NO_SHOW","REJECTED"].map(x=><option key={x} value={x}>{x.replaceAll("_"," ")}</option>)}</select>
      <select className="ref-select" value={partyFilter} onChange={e=>setPartyFilter(e.target.value)}><option value="ALL">All Party Sizes</option>{Array.from(new Set(data.map(r=>String(r.guestCount)))).sort((a,b)=>Number(a)-Number(b)).map(x=><option key={x} value={x}>{x} People</option>)}</select>
      <select className="ref-select" value={dateFilter} onChange={e=>setDateFilter(e.target.value)}><option value="ALL">All Dates</option><option value="TODAY">Today</option><option value="TOMORROW">Tomorrow</option><option value="WEEK">Next 7 Days</option></select>
      <FilterInput value={query} onChange={setQuery} placeholder="Search by customer name, phone, booking ID…"/><button className="ref-reset" onClick={resetFilters}>Reset</button>
    </div>
    {error?<div className="page-error panel-card">{error}</div>:null}
    <div className={selected?"ref-master-detail booking-detail-open":"ref-master-detail"}><main className="ref-booking-list">{filtered.map((r:any)=>{
      const rd=restaurantDetails[String(r.restaurantId)]??{};
      const rowImage=productionMediaUrl(r.restaurantLogoUrl)||productionMediaUrl(r.restaurantCoverImageUrl)||restaurantImage(rd);
      return <article key={r.id} className="ref-booking-row">
        <div className="ref-avatar sm">{r.customerName.slice(0,2).toUpperCase()}</div><div className="ref-booking-customer"><h3>{r.customerName}</h3><small>#{r.bookingNumber}</small></div>
        <div className="ref-restaurant-cell">{rowImage?<img className="ref-food-thumb" src={rowImage} alt={r.restaurantName}/>:<div className="ref-food-thumb">🏪</div>}<div><b>{r.restaurantName}</b><small>⌖ {text(r.restaurantLocation??rd.location,"DiniBee Restaurant")}</small></div></div>
        <div className="ref-booking-meta"><span>♟ {r.guestCount} People</span><span>▣ {r.date}</span><span>◷ {r.time}</span></div><div className="ref-booking-status"><StatusBadge status={r.status}/><small>Booked {text(r.createdAt??r.date)}</small><button onClick={()=>void chooseReservation(r)}>View Details</button></div><button className="ref-more">⋮</button>
      </article>})}{!loading&&!filtered.length?<div className="v17-empty">No bookings match the selected filters.</div>:null}<div className="ref-pagination"><span>Showing {filtered.length?1:0} to {Math.min(filtered.length,6)} of {filtered.length} bookings</span><div>‹ <b>1</b> ›</div><span>Live data</span></div></main>
    {selected?<aside className="ref-detail-panel booking-panel"><button className="ref-x" onClick={()=>setSelected(null)}>×</button><div className="ref-detail-hero-line">{selectedImage?<img className="ref-food-thumb lg" src={selectedImage} alt={selected.restaurantName}/>:<div className="ref-food-thumb lg">🏪</div>}<div><h2>#{selected.bookingNumber} <StatusBadge status={selected.status}/></h2><p>Booked on {text(selected.createdAt??selected.date)}</p><b>{selected.restaurantName}</b><small>⌖ {selectedLocation}</small></div></div><nav className="ref-tabs">{["overview","customer","history"].map(t=><button key={t} className={tab===t?"active":""} onClick={()=>setTab(t)}>{t[0].toUpperCase()+t.slice(1)}</button>)}</nav>
      {tab==="overview"?<div className="ref-detail-scroll"><div className="ref-section-title"><h3>Booking Information</h3></div><dl className="ref-info-list compact"><div><dt>▣ Booking ID</dt><dd>#{selected.bookingNumber}</dd></div><div><dt>▣ Date</dt><dd>{selected.date}</dd></div><div><dt>◷ Time</dt><dd>{selected.time}{selected.endTime?` - ${selected.endTime}`:""}</dd></div><div><dt>♟ Party Size</dt><dd>{selected.guestCount} People</dd></div><div><dt>▣ Service Type</dt><dd><span className="ref-tag premium">▣ {selected.bookingType||"Table Booking"}</span></dd></div><div><dt>◉ Status</dt><dd><StatusBadge status={selected.status}/></dd></div><div><dt>⌖ Booking Source</dt><dd>DiniBee App</dd></div><div><dt>▣ Table</dt><dd>{text(selected.tableId)}</dd></div></dl><section className="ref-box"><h3>Special Requests</h3><p>{text(selected.specialRequest,"No special request provided.")}</p></section><section className="ref-box"><h3>Restaurant Information</h3><div className="ref-restaurant-info">{selectedImage?<img className="ref-food-thumb" src={selectedImage} alt=""/>:<div className="ref-food-thumb">🏪</div>}<div><b>{selected.restaurantName}</b><p>⌖ {selectedLocation}</p></div></div></section></div>:null}
      {tab==="customer"?<div className="ref-detail-scroll"><h3>Customer Information</h3><div className="ref-person-line"><div className="ref-avatar sm">{selected.customerName.slice(0,2).toUpperCase()}</div><div><b>{selected.customerName}</b><p>☎ {selectedPhone}</p><p>✉ {selectedEmail}</p></div></div><dl className="ref-info-list compact"><div><dt>Customer</dt><dd>{selected.customerName}</dd></div><div><dt>Phone</dt><dd>{selectedPhone}</dd></div><div><dt>Email</dt><dd>{selectedEmail}</dd></div><div><dt>Customer ID</dt><dd>{text(selected.customerId)}</dd></div></dl></div>:null}
      {tab==="history"?<div className="ref-detail-scroll"><h3>Reservation History</h3><div className="v23-activity-list">{history.map((h:any,i:number)=><div key={h.id??i}><span className="dot"/><div><b>{text(h.oldStatus,"Created")} → {text(h.newStatus)}</b><p>{text(h.reason,"Status updated")}</p><small>{text(h.changedAt)}{h.changedByName?` · ${h.changedByName}`:""}</small></div></div>)}{!history.length?<p>No reservation history returned.</p>:null}</div></div>:null}
      <div className="ref-bottom-actions"><button onClick={()=>toast.info("Reschedule is managed by restaurant/customer workflow")}>▣ Reschedule</button><button className="danger" onClick={()=>toast.info("Cancellation is managed by restaurant/customer workflow")}>⊗ Cancel Booking</button><button onClick={()=>toast.info("Use Notifications to contact this customer")}>➤ Send Notification</button></div></aside>:null}</div>
  </div>;
}

export function TakeawayV17() {
  const { data, loading, error } = useApiResource(() => takeawayService.list(), [], [] as TakeawayRecord[]);
  const [selected, setSelected] = useState<TakeawayRecord | null>(null);
  const [query,setQuery]=useState("");
  const [tab,setTab]=useState("ALL");
  const [restaurant,setRestaurant]=useState("ALL");
  const [status,setStatus]=useState("ALL");
  const [from,setFrom]=useState("");
  const [to,setTo]=useState("");
  const completed=data.filter(r=>["COMPLETED","PICKED_UP","COLLECTED"].includes(String(r.status))).length;
  const preparing=data.filter(r=>["PREPARING","READY","READY_FOR_PICKUP","CONFIRMED","NEW","PLACED","ACCEPTED"].includes(String(r.status))).length;
  const cancelled=data.filter(r=>["CANCELLED","REJECTED"].includes(String(r.status))).length;
  const restaurants=Array.from(new Set(data.map(r=>r.restaurantName).filter(Boolean))).sort();
  const statuses=Array.from(new Set(data.map(r=>String(r.status)).filter(Boolean))).sort();
  const rows=data.filter(r=>{
    const rs=String(r.status);
    if(tab==="COMPLETED" && !["COMPLETED","PICKED_UP","COLLECTED"].includes(rs)) return false;
    if(tab==="PREPARING" && !["PREPARING","READY","READY_FOR_PICKUP","CONFIRMED","NEW","PLACED","ACCEPTED"].includes(rs)) return false;
    if(tab==="CANCELLED" && !["CANCELLED","REJECTED"].includes(rs)) return false;
    if(restaurant!=="ALL" && r.restaurantName!==restaurant) return false;
    if(status!=="ALL" && rs!==status) return false;
    const d=safeDate(r.createdAt); if(from && d && d < new Date(`${from}T00:00:00`)) return false; if(to && d && d > new Date(`${to}T23:59:59`)) return false;
    const hay=`${r.orderNumber} ${r.customerName} ${r.customerPhone??""} ${r.customerEmail??""} ${r.restaurantName} ${r.itemsSummary??""}`.toLowerCase();
    return hay.includes(query.toLowerCase());
  });
  const reset=()=>{setQuery("");setTab("ALL");setRestaurant("ALL");setStatus("ALL");setFrom("");setTo("");};
  const exportRows=()=>csvDownload("dinibee-takeaway-orders.csv", rows.map(r=>({Order:r.orderNumber,Customer:r.customerName,Phone:r.customerPhone,Restaurant:r.restaurantName,Items:r.itemsSummary,Status:r.status,Amount:r.amount,Payment:r.paymentStatus,Created:r.createdAt})));
  const printBill=(r:TakeawayRecord)=>{const w=window.open("","_blank","width=680,height=800"); if(!w){toast.error("Pop-up blocked");return;} w.document.write(`<html><head><title>DiniBee Bill ${r.orderNumber}</title><style>body{font-family:Arial;padding:30px}h1{color:#e59a00}.row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #ddd}</style></head><body><h1>DiniBee Takeaway Bill</h1><div class='row'><b>Order</b><span>${r.orderNumber}</span></div><div class='row'><b>Customer</b><span>${r.customerName}</span></div><div class='row'><b>Restaurant</b><span>${r.restaurantName}</span></div><div class='row'><b>Items</b><span>${r.itemsSummary??r.type}</span></div><div class='row'><b>Total</b><span>${money(r.amount)}</span></div><div class='row'><b>Status</b><span>${r.status}</span></div></body></html>`);w.document.close();w.focus();setTimeout(()=>w.print(),250);};
  const openRefund=(r:TakeawayRecord)=>{if(!r.paymentId){toast.error("No payment is linked to this order");return;} window.location.href=`/refunds?paymentId=${encodeURIComponent(r.paymentId)}&reference=${encodeURIComponent(r.orderNumber)}`;};
  return <div className="ref-page takeaway-ref">
    <div className="ref-page-head"><div><h1>Take Away Orders</h1><p>Manage all takeaway orders from DiniBee platform</p></div><div className="ref-head-actions"><div className="ref-date-inputs"><input type="date" value={from} onChange={e=>setFrom(e.target.value)}/><span>–</span><input type="date" value={to} onChange={e=>setTo(e.target.value)}/></div><Button variant="outline" onClick={exportRows}>⇩ Export</Button></div></div>
    <div className="ref-stat-row"><article className="orange"><span>♨</span><div><small>Total Orders</small><b>{data.length}</b><em>live</em></div></article><article className="mint"><span>✓</span><div><small>Completed</small><b>{completed}</b><em>{data.length?Math.round(completed/data.length*100):0}% of total</em></div></article><article className="orange"><span>◷</span><div><small>Preparing</small><b>{preparing}</b><em>{data.length?Math.round(preparing/data.length*100):0}% of total</em></div></article><article className="purple"><span>×</span><div><small>Cancelled</small><b>{cancelled}</b><em>{data.length?Math.round(cancelled/data.length*100):0}% of total</em></div></article></div>
    <nav className="ref-tabs top"><button className={tab==="ALL"?"active":""} onClick={()=>setTab("ALL")}>All Orders ({data.length})</button><button className={tab==="COMPLETED"?"active":""} onClick={()=>setTab("COMPLETED")}>Completed ({completed})</button><button className={tab==="PREPARING"?"active":""} onClick={()=>setTab("PREPARING")}>Preparing ({preparing})</button><button className={tab==="CANCELLED"?"active":""} onClick={()=>setTab("CANCELLED")}>Cancelled ({cancelled})</button></nav>
    <div className="ref-filter-row takeaway"><select className="ref-select" value={restaurant} onChange={e=>setRestaurant(e.target.value)}><option value="ALL">All Restaurants</option>{restaurants.map(v=><option key={v} value={v}>{v}</option>)}</select><select className="ref-select" value={status} onChange={e=>setStatus(e.target.value)}><option value="ALL">All Status</option>{statuses.map(v=><option key={v} value={v}>{v.replaceAll("_"," ")}</option>)}</select><FilterInput value={query} onChange={setQuery} placeholder="Search by order ID, customer name, phone…"/><button className="ref-reset" onClick={reset}>Reset</button></div>
    {error?<div className="page-error panel-card">{error}</div>:null}
    <div className={selected?"ref-master-detail order-detail-open":"ref-master-detail"}><main><div className="ref-data-table takeaway"><div className="ref-table-head"><span>□</span><span>Order ID</span><span>Customer</span><span>Restaurant</span><span>Items</span><span>Order Time</span><span>Amount</span><span>Status</span><span>Action</span></div>{rows.map(r=>{const rest=mediaUrlFor(r,"restaurant"),food=mediaUrlFor(r,"food");return <div className="ref-table-row" key={r.id}><span>□</span><b>#{r.orderNumber}</b><div className="ref-person-cell"><img className="ref-avatar xs" src={avatarDataUri(r.customerName)} alt=""/><span><b>{r.customerName}</b><small>{r.customerPhone||"Customer"}</small></span></div><div className="ref-restaurant-cell">{rest?<img className="ref-food-thumb xs" src={rest} alt=""/>:<div className="ref-food-thumb xs">🏪</div>}<span><b>{r.restaurantName}</b><small>⌖ {r.restaurantLocation||"DiniBee Restaurant"}</small></span></div><span className="ref-items-cell">{food?<img className="ref-food-thumb xs" src={food} alt=""/>:null}<small>{r.itemsSummary||r.type.replaceAll("_"," ")}</small></span><span>{safeDate(r.createdAt)?.toLocaleString("en-IN",{day:"2-digit",month:"short",hour:"2-digit",minute:"2-digit"})||r.createdAt}</span><b>{money(r.amount)}</b><StatusBadge status={r.status}/><div><button onClick={()=>setSelected(r)}>View</button><button className="ref-more">⋮</button></div></div>})}{!loading&&!rows.length?<div className="v17-empty">No takeaway orders match the selected filters.</div>:null}</div></main>
    {selected?<aside className="ref-detail-panel order-panel"><button className="ref-x" onClick={()=>setSelected(null)}>×</button><div className="ref-detail-hero-line">{mediaUrlFor(selected,"food")?<img className="ref-food-thumb lg" src={mediaUrlFor(selected,"food")} alt=""/>:<div className="ref-food-thumb lg">🍽</div>}<div><h2>#{selected.orderNumber} <StatusBadge status={selected.status}/></h2><p>{safeDate(selected.createdAt)?.toLocaleString("en-IN")}</p></div></div><section className="ref-box"><h3>Customer Information</h3><div className="ref-person-line"><img className="ref-avatar sm" src={avatarDataUri(selected.customerName)} alt=""/><div><b>{selected.customerName}</b><p>☎ {selected.customerPhone||"—"}</p><p>✉ {selected.customerEmail||"—"}</p></div></div></section><section className="ref-box"><h3>Restaurant Information</h3><div className="ref-restaurant-info">{mediaUrlFor(selected,"restaurant")?<img className="ref-food-thumb" src={mediaUrlFor(selected,"restaurant")} alt=""/>:<div className="ref-food-thumb">🏪</div>}<div><b>{selected.restaurantName}</b><p>⌖ {selected.restaurantLocation||"—"}</p></div></div></section><section className="ref-box"><h3>Order Items</h3><div className="ref-order-item">{mediaUrlFor(selected,"food")?<img className="ref-food-thumb xs" src={mediaUrlFor(selected,"food")} alt=""/>:null}<span>{selected.itemsSummary||selected.type.replaceAll("_"," ")}</span><b>{money(selected.amount)}</b></div><div className="ref-total-line"><span>Total Amount</span><b>{money(selected.amount)}</b></div></section><section className="ref-box"><h3>Order Information</h3><dl className="ref-info-list compact"><div><dt>Order ID</dt><dd>#{selected.orderNumber}</dd></div><div><dt>Payment</dt><dd>{selected.paymentStatus}</dd></div><div><dt>Payment Method</dt><dd>{selected.paymentMethod||"—"}</dd></div><div><dt>Status</dt><dd><StatusBadge status={selected.status}/></dd></div></dl></section><div className="ref-bottom-actions"><button onClick={()=>printBill(selected)}>▣ Print Bill</button><button className="danger" onClick={()=>openRefund(selected)}>↶ Refund Order</button><button onClick={()=>void sendRecordNotification(selected,"Takeaway order update",`Your takeaway order ${selected.orderNumber} is ${String(selected.status).replaceAll("_"," ")}.`).catch(e=>toast.error(e?.message||"Notification failed"))}>➤ Send Notification</button></div></aside>:null}</div>
  </div>;
}

export function DiniTicketV17() {
  const { data, loading, error } = useApiResource(() => diniTicketService.list(), [], [] as DiniTicketRecord[]);
  const [selected, setSelected] = useState<DiniTicketRecord | null>(null);
  const [query,setQuery]=useState(""); const [tab,setTab]=useState("ALL"); const [restaurant,setRestaurant]=useState("ALL"); const [status,setStatus]=useState("ALL"); const [from,setFrom]=useState(""); const [to,setTo]=useState("");
  const qs=(r:DiniTicketRecord)=>String(r.queueStatus??"").trim().toUpperCase();
  const isUpcoming=(v:string)=>["WAITING","PENDING","PLACED","PREPARING","READY","CALLED","ARRIVED","SEATED"].includes(v);
  const isCancelled=(v:string)=>["CANCELLED","CANCELED","NO_SHOW","EXPIRED"].includes(v);
  const completed=data.filter(r=>qs(r)==="COMPLETED").length, upcoming=data.filter(r=>isUpcoming(qs(r))).length, cancelled=data.filter(r=>isCancelled(qs(r))).length;
  const restaurants=Array.from(new Set(data.map(r=>r.restaurantName).filter(Boolean))).sort(); const statuses=Array.from(new Set(data.map(r=>qs(r)).filter(Boolean))).sort();
  const rows=data.filter(r=>{const st=qs(r);if(tab==="UPCOMING"&&!isUpcoming(st))return false;if(tab==="COMPLETED"&&st!=="COMPLETED")return false;if(tab==="CANCELLED"&&!isCancelled(st))return false;if(restaurant!=="ALL"&&r.restaurantName!==restaurant)return false;if(status!=="ALL"&&st!==status)return false;const d=safeDate((r as any).createdAt??(r as any).orderedAt??(r as any).expectedAt??(r as any).updatedAt);if(from&&(!d||d<new Date(`${from}T00:00:00`)))return false;if(to&&(!d||d>new Date(`${to}T23:59:59`)))return false;return `${r.tokenNumber} ${r.orderNumber} ${r.customerName} ${r.customerPhone??""} ${r.restaurantName} ${r.itemsSummary??""}`.toLowerCase().includes(query.toLowerCase())});
  const reset=()=>{setQuery("");setTab("ALL");setRestaurant("ALL");setStatus("ALL");setFrom("");setTo("");};
  const exportRows=()=>csvDownload("dinibee-diniticket-orders.csv",rows.map(r=>({Token:r.tokenNumber,Order:r.orderNumber,Customer:r.customerName,Restaurant:r.restaurantName,Items:r.itemsSummary,Amount:r.amount,Status:r.queueStatus,Payment:r.paymentStatus,Created:r.createdAt})));
  const downloadTicket=(r:DiniTicketRecord)=>{const body=`DiniBee DiniTicket\nToken: ${r.tokenNumber}\nOrder: ${r.orderNumber}\nCustomer: ${r.customerName}\nRestaurant: ${r.restaurantName}\nItems: ${r.itemsSummary||"—"}\nAmount: ${money(r.amount)}\nStatus: ${r.queueStatus}\nCreated: ${r.createdAt}\n`;const url=URL.createObjectURL(new Blob([body],{type:"text/plain"}));const a=document.createElement("a");a.href=url;a.download=`DiniBee-${r.tokenNumber}.txt`;a.click();URL.revokeObjectURL(url);};
  const openRefund=(r:DiniTicketRecord)=>{if(!r.paymentId){toast.error("No payment is linked to this ticket");return;}window.location.href=`/refunds?paymentId=${encodeURIComponent(r.paymentId)}&reference=${encodeURIComponent(r.tokenNumber)}`;};
  return <div className="ref-page diniticket-ref">
    <div className="ref-page-head"><div><h1>DiniTicket Orders</h1><p>Manage all DiniTicket orders from DiniBee platform</p></div><div className="ref-head-actions"><div className="ref-date-inputs"><input type="date" value={from} onChange={e=>setFrom(e.target.value)}/><span>–</span><input type="date" value={to} onChange={e=>setTo(e.target.value)}/></div><Button variant="outline" onClick={exportRows}>⇩ Export</Button></div></div>
    <div className="ref-stat-row"><article className="purple"><span>◈</span><div><small>Total Tickets</small><b>{data.length}</b><em>live</em></div></article><article className="mint"><span>✓</span><div><small>Completed</small><b>{completed}</b><em>{data.length?Math.round(completed/data.length*100):0}% of total</em></div></article><article className="orange"><span>◷</span><div><small>Upcoming</small><b>{upcoming}</b><em>{data.length?Math.round(upcoming/data.length*100):0}% of total</em></div></article><article className="red"><span>×</span><div><small>Cancelled</small><b>{cancelled}</b><em>{data.length?Math.round(cancelled/data.length*100):0}% of total</em></div></article></div>
    <nav className="ref-tabs top"><button className={tab==="ALL"?"active":""} onClick={()=>setTab("ALL")}>All Tickets ({data.length})</button><button className={tab==="UPCOMING"?"active":""} onClick={()=>setTab("UPCOMING")}>Upcoming ({upcoming})</button><button className={tab==="COMPLETED"?"active":""} onClick={()=>setTab("COMPLETED")}>Completed ({completed})</button><button className={tab==="CANCELLED"?"active":""} onClick={()=>setTab("CANCELLED")}>Cancelled ({cancelled})</button></nav>
    <div className="ref-filter-row takeaway"><select className="ref-select" value={restaurant} onChange={e=>setRestaurant(e.target.value)}><option value="ALL">All Restaurants</option>{restaurants.map(v=><option key={v}>{v}</option>)}</select><select className="ref-select" value={status} onChange={e=>setStatus(e.target.value)}><option value="ALL">All Status</option>{statuses.map(v=><option key={v}>{v.replaceAll("_"," ")}</option>)}</select><FilterInput value={query} onChange={setQuery} placeholder="Search by ticket ID, customer name, restaurant…"/><button className="ref-reset" onClick={reset}>Reset</button></div>
    {error?<div className="page-error panel-card">{error}</div>:null}
    <div className={selected?"ref-master-detail ticket-detail-open":"ref-master-detail"}><main><div className="ref-ticket-list">{rows.map(r=>{const img=mediaUrlFor(r,"food")||mediaUrlFor(r,"restaurant");return <article key={r.id} className="ref-ticket-row">{img?<img className="ref-event-thumb" src={img} alt=""/>:<div className="ref-event-thumb">🎟</div>}<div className="ref-ticket-event"><h3>DiniTicket Order</h3><small>⌖ {r.restaurantName}</small><span className="ref-tag orange">DiniTicket</span></div><div className="ref-person-cell"><img className="ref-avatar xs" src={avatarDataUri(r.customerName)} alt=""/><span><b>{r.customerName}</b><small>{r.customerPhone||"Customer"}</small></span></div><div className="ref-ticket-time"><span>▣ {safeDate(r.createdAt)?.toLocaleDateString("en-IN")}</span><span>◷ {safeDate(r.createdAt)?.toLocaleTimeString("en-IN",{hour:"2-digit",minute:"2-digit"})}</span></div><div className="ref-ticket-price"><span>{r.orderNumber||r.tokenNumber}</span><b>{money(r.amount)}</b></div><StatusBadge status={r.queueStatus}/><button onClick={()=>setSelected(r)}>View</button><button className="ref-more">⋮</button></article>})}{!loading&&!rows.length?<div className="v17-empty">No DiniTicket orders match the selected filters.</div>:null}</div></main>
    {selected?<aside className="ref-detail-panel ticket-panel"><button className="ref-x" onClick={()=>setSelected(null)}>×</button><div className="ref-detail-hero-line">{(mediaUrlFor(selected,"food")||mediaUrlFor(selected,"restaurant"))?<img className="ref-event-thumb lg" src={mediaUrlFor(selected,"food")||mediaUrlFor(selected,"restaurant")} alt=""/>:<div className="ref-event-thumb lg">🎟</div>}<div><h2>DiniTicket Order <StatusBadge status={selected.queueStatus}/></h2><b>#{selected.tokenNumber}</b><p>{safeDate(selected.createdAt)?.toLocaleString("en-IN")}</p></div></div><section className="ref-box"><h3>Event Information</h3><dl className="ref-info-list compact"><div><dt>Event Name</dt><dd>DiniTicket Order</dd></div><div><dt>Restaurant</dt><dd>{selected.restaurantName}</dd></div><div><dt>Location</dt><dd>{selected.restaurantLocation||"—"}</dd></div><div><dt>Items</dt><dd>{selected.itemsSummary||"—"}</dd></div></dl></section><section className="ref-box"><h3>Customer Information</h3><div className="ref-person-line"><img className="ref-avatar sm" src={avatarDataUri(selected.customerName)} alt=""/><div><b>{selected.customerName}</b><p>☎ {selected.customerPhone||"—"}</p><p>✉ {selected.customerEmail||"—"}</p></div></div></section><section className="ref-box"><h3>Ticket Information</h3><dl className="ref-info-list compact"><div><dt>Ticket ID</dt><dd>#{selected.tokenNumber}</dd></div><div><dt>Order ID</dt><dd>{selected.orderNumber}</dd></div><div><dt>Total Amount</dt><dd>{money(selected.amount)}</dd></div><div><dt>Status</dt><dd><StatusBadge status={selected.queueStatus}/></dd></div><div><dt>Payment</dt><dd>{selected.paymentStatus}</dd></div></dl></section><div className="ref-bottom-actions"><button onClick={()=>void sendRecordNotification(selected,"DiniTicket update",`Your token ${selected.tokenNumber} is ${String(selected.queueStatus).replaceAll("_"," ")}.`).catch(e=>toast.error(e?.message||"Notification failed"))}>➤ Send Ticket</button><button className="danger" onClick={()=>openRefund(selected)}>↶ Refund Ticket</button><button onClick={()=>downloadTicket(selected)}>⇩ Download</button></div></aside>:null}</div>
  </div>;
}

export function TransactionsV17() {
  const { data, loading, error, reload } = useApiResource(() => paymentService.listTransactions(), [], [] as PaymentRecord[]);
  const [selected, setSelected] = useState<PaymentRecord | null>(null);
  const [query,setQuery]=useState("");
  const total = data.reduce((sum,r)=>sum+r.gatewayPaidAmount,0);
  const dine = data.filter(r=>["DINE_IN","GROUP_BOOKING","CORPORATE_BOOKING"].includes(r.paymentType)).reduce((s,r)=>s+r.gatewayPaidAmount,0);
  const takeaway = data.filter(r=>r.paymentType==="TAKEAWAY").reduce((s,r)=>s+r.gatewayPaidAmount,0);
  const dini = data.filter(r=>r.paymentType==="DINITICKET").reduce((s,r)=>s+r.gatewayPaidAmount,0);
  const rows=data.filter(r=>`${r.referenceId} ${r.customerName} ${r.restaurantName} ${r.transactionId}`.toLowerCase().includes(query.toLowerCase()));
  const methods=Array.from(new Set(data.map(r=>r.paymentMethod||"Unknown")));
  return <div className="ref-page payments-ref">
    <div className="ref-page-head"><div><h1>Payments</h1><p>All payment transactions from DiniBee platform</p></div><div className="ref-head-actions"><button className="ref-date">▣ Oct 1, 2026 - Oct 31, 2026⌄</button><Button variant="outline">⇩ Export</Button></div></div>
    <div className="ref-stat-row"><article className="mint"><span>₹</span><div><small>Total Revenue</small><b>{money(total)}</b><em>{data.length} transactions</em></div></article><article className="blue"><span>▣</span><div><small>Table Booking</small><b>{money(dine)}</b><em>{data.filter(r=>["DINE_IN","GROUP_BOOKING","CORPORATE_BOOKING"].includes(r.paymentType)).length} transactions</em></div></article><article className="orange"><span>♨</span><div><small>Take Away</small><b>{money(takeaway)}</b><em>{data.filter(r=>r.paymentType==="TAKEAWAY").length} transactions</em></div></article><article className="purple"><span>◈</span><div><small>DiniTicket</small><b>{money(dini)}</b><em>{data.filter(r=>r.paymentType==="DINITICKET").length} transactions</em></div></article></div>
    <div className="ref-payment-charts"><article><div className="ref-section-title"><h3>Revenue Overview</h3><div><span>● Table Booking</span><span>● Take Away</span><span>● DiniTicket</span></div></div><div className="ref-line-chart"><svg viewBox="0 0 700 170" preserveAspectRatio="none"><polyline points="0,135 80,112 160,124 240,103 320,106 400,91 480,96 560,73 640,86 700,69"/><polyline points="0,148 80,134 160,143 240,128 320,119 400,121 480,101 560,119 640,99 700,112"/><polyline points="0,118 80,94 160,105 240,86 320,87 400,73 480,48 560,68 640,44 700,64"/></svg><div className="ref-chart-labels"><span>Oct 1</span><span>Oct 5</span><span>Oct 10</span><span>Oct 15</span><span>Oct 20</span><span>Oct 25</span><span>Oct 31</span></div></div></article><article><h3>Payment Methods</h3><div className="ref-donut-wrap"><div className="ref-donut"><b>{money(total/100000)}L</b><small>Total</small></div><div className="ref-methods">{methods.slice(0,5).map((m,i)=><div key={m}><span><i className={`dot d${i}`}/>{m}</span><b>{money(data.filter(r=>r.paymentMethod===m).reduce((a,b)=>a+b.gatewayPaidAmount,0))}</b></div>)}</div></div></article></div>
    <nav className="ref-tabs top"><button className="active">All Payments ({data.length})</button><button>Table Bookings</button><button>Take Away</button><button>DiniTickets</button><button>Refunds</button></nav>
    <div className="ref-filter-row payment"><select className="ref-select"><option>All Services</option></select><select className="ref-select"><option>All Payment Methods</option></select><select className="ref-select"><option>All Status</option></select><select className="ref-select"><option>All Restaurants</option></select><FilterInput value={query} onChange={setQuery} placeholder="Search by order ID, customer, restaurant…"/><button className="ref-reset">Reset</button></div>
    {error?<div className="page-error panel-card">{error}</div>:null}
    <div className={selected?"ref-master-detail payment-detail-open":"ref-master-detail"}><main><div className="ref-data-table payment"><div className="ref-table-head"><span>□</span><span>Order ID</span><span>Date & Time</span><span>Service Type</span><span>Customer</span><span>Restaurant</span><span>Payment Method</span><span>Amount</span><span>Status</span><span>Action</span></div>{rows.map(r=><div className="ref-table-row" key={r.id}><span>□</span><b>#{r.referenceId}</b><span>{r.date}</span><span><i className="ref-tag blue">{r.paymentType.replaceAll("_"," ")}</i></span><span>{r.customerName}</span><span>{r.restaurantName}</span><span>{r.paymentMethod}</span><b>{money(r.gatewayPaidAmount)}</b><StatusBadge status={r.status}/><button onClick={()=>setSelected(r)}>View</button></div>)}{!loading&&!rows.length?<div className="v17-empty">No transactions found.</div>:null}</div><div className="ref-pagination"><span>Showing 1 to {Math.min(rows.length,8)} of {rows.length} transactions</span><div>‹ <b>1</b> 2 3 4 5 … ›</div><span>Rows per page <b>8⌄</b></span></div></main>
    {selected?<aside className="ref-detail-panel payment-panel"><button className="ref-x" onClick={()=>setSelected(null)}>×</button><div className="ref-detail-hero-line"><div className="ref-food-thumb lg">🍽</div><div><h2>Order #{selected.referenceId} <StatusBadge status={selected.status}/></h2><p>{selected.date}</p><b>{selected.restaurantName}</b><span className="ref-tag blue">{selected.paymentType.replaceAll("_"," ")}</span></div></div><section className="ref-box"><h3>Payment Information</h3><dl className="ref-info-list compact"><div><dt>Transaction ID</dt><dd>{selected.transactionId}</dd></div><div><dt>Payment Method</dt><dd>{selected.paymentMethod}</dd></div><div><dt>Amount</dt><dd>{money(selected.gatewayPaidAmount)}</dd></div><div><dt>Payment Status</dt><dd><StatusBadge status={selected.status}/></dd></div><div><dt>Paid At</dt><dd>{selected.date}</dd></div><div><dt>Gateway</dt><dd>Razorpay</dd></div></dl></section><section className="ref-box"><h3>Order Information</h3><dl className="ref-info-list compact"><div><dt>Order ID</dt><dd>#{selected.referenceId}</dd></div><div><dt>Service Type</dt><dd>{selected.paymentType.replaceAll("_"," ")}</dd></div><div><dt>Restaurant</dt><dd>{selected.restaurantName}</dd></div><div><dt>Total Amount</dt><dd>{money(selected.gatewayPaidAmount)}</dd></div></dl></section><section className="ref-box"><h3>Customer Information</h3><div className="ref-person-line"><div className="ref-avatar sm">{selected.customerName.slice(0,1)}</div><div><b>{selected.customerName} <span className="ref-tag premium">★ Premium</span></b><p>Customer account</p></div></div></section><section className="ref-box"><h3>Payment Timeline</h3><div className="ref-timeline"><p>● Payment Initiated <span>{selected.date}</span></p><p>● Payment Successful <span>{selected.date}</span></p><p>● Order Confirmed <span>{selected.date}</span></p></div></section><div className="ref-bottom-actions"><button>➤ Send Receipt</button><button className="danger">↶ Refund Payment</button><button>↗ View in App</button></div></aside>:null}</div>
  </div>;
}
