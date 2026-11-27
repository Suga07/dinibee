"use client";

import { useMemo, useState, type ReactNode } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  Download,
  Eye,
  Filter,
  LoaderCircle,
  MoreHorizontal,
  RefreshCw,
  Search,
  SlidersHorizontal,
  TriangleAlert,
  type LucideIcon,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export type DataValue = string | number;
export type DataRow = { id: string; [key: string]: DataValue };
export type Column<T extends DataRow = DataRow> = {
  key: keyof T & string;
  label: string;
  className?: string;
  render?: (value: DataValue, row: T) => ReactNode;
};

const positiveStatuses = new Set([
  "Active",
  "Approved",
  "Confirmed",
  "Completed",
  "Paid",
  "Published",
  "Ready",
  "Resolved",
  "Verified",
  "Checked-In",
  "Picked Up",
  "Enabled",
  "Sent",
]);

const dangerStatuses = new Set([
  "Blocked",
  "Cancelled",
  "Closed",
  "Disabled",
  "Expired",
  "Failed",
  "Hidden",
  "Invalid",
  "Rejected",
  "Suspended",
  "No Show",
]);

const infoStatuses = new Set([
  "Accepted",
  "Checked-In",
  "Dining",
  "In Progress",
  "Processing",
  "Scheduled",
  "Under Review",
]);

export function StatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase().trim().replace(/[\s-]+/g, "_");
  const display = normalized
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
  const className = dangerStatuses.has(status) || [
    "BLOCKED", "CANCELLED", "CLOSED", "DISABLED", "EXPIRED", "FAILED",
    "HIDDEN", "INVALID", "REJECTED", "SUSPENDED", "NO_SHOW", "REMOVED", "OVERDUE",
  ].includes(normalized)
    ? "status-badge danger"
    : positiveStatuses.has(status) || [
        "ACTIVE", "APPROVED", "ARRIVED", "AVAILABLE", "COLLECTED", "COMPLETED",
        "CONFIRMED", "ENABLED", "PAID", "PICKED_UP", "PUBLISHED", "READY",
        "RESOLVED", "SERVED", "SENT", "VERIFIED", "CHECKED_IN",
      ].includes(normalized)
      ? "status-badge success"
      : infoStatuses.has(status) || [
          "ACCEPTED", "CALLED", "DINING", "IN_PROGRESS", "ISSUED", "NEARBY",
          "OCCUPIED", "ON_THE_WAY", "PARTIALLY_PAID", "PARTIALLY_REFUNDED",
          "PROCESSING", "SCHEDULED", "UNDER_REVIEW",
        ].includes(normalized)
        ? "status-badge info"
        : "status-badge warning";

  return (
      <span className={className}>
      <i />
      {display}
    </span>
  );
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <div className="page-header">
      <div>
        <p>{eyebrow}</p>
        <h1>{title}</h1>
        <span>{description}</span>
      </div>
      {actions ? <div className="page-actions">{actions}</div> : null}
    </div>
  );
}

export function StatCard({
  label,
  value,
  note,
  trend,
  icon: Icon,
  tone = "gold",
}: {
  label: string;
  value: string;
  note: string;
  trend?: "up" | "down" | "live";
  icon: LucideIcon;
  tone?: "gold" | "green" | "blue" | "purple" | "red";
}) {
  return (
    <article className="stat-card panel-card">
      <div className={`stat-icon ${tone}`}><Icon /></div>
      <div className="stat-copy">
        <span>{label}</span>
        <strong>{value}</strong>
        <small className={trend ? `trend-${trend}` : ""}>
          {trend === "up" ? <ArrowUpRight /> : null}
          {trend === "down" ? <ArrowDownRight /> : null}
          {trend === "live" ? <i className="live-dot" /> : null}
          {note}
        </small>
      </div>
    </article>
  );
}

export function InitialAvatar({ name }: { name?: string }) {
  const safeName = String(name || "U");
  const initials = safeName
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
  return <span className="initial-avatar">{initials || "U"}</span>;
}

export function EntityCell({
  title,
  subtitle,
  icon,
}: {
  title?: string;
  subtitle?: string;
  icon?: ReactNode;
}) {
  const safeTitle = String(title || "—");
  return (
    <span className="entity-cell">
      {icon ?? <InitialAvatar name={safeTitle} />}
      <span>
        <strong>{safeTitle}</strong>
        {subtitle ? <small>{String(subtitle)}</small> : null}
      </span>
    </span>
  );
}

export function DataTable<T extends DataRow>({
  rows,
  columns,
  searchPlaceholder,
  statusKey = "status",
  statusOptions,
  onView,
  actionLabel = "View details",
  emptyTitle = "No records found",
  emptyDescription = "Try changing your search or filter.",
  loading: externalLoading = false,
  error = "",
  onRetry,
  onRefresh,
  toolbarExtras,
}: {
  rows: T[];
  columns: Column<T>[];
  searchPlaceholder: string;
  statusKey?: keyof T & string;
  statusOptions?: string[];
  onView?: (row: T) => void;
  actionLabel?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  loading?: boolean;
  error?: string;
  onRetry?: () => void | Promise<void>;
  onRefresh?: () => void | Promise<void>;
  toolbarExtras?: ReactNode;
}) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("All");
  const [page, setPage] = useState(1);
  const [refreshing, setRefreshing] = useState(false);
  const pageSize = 5;
  const loading = externalLoading || refreshing;

  const statuses = useMemo(() => {
    if (statusOptions) return statusOptions;
    return Array.from(new Set(rows.map((row) => String(row[statusKey]))));
  }, [rows, statusKey, statusOptions]);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return rows.filter((row) => {
      const matchesQuery =
        !normalized ||
        Object.values(row).some((value) =>
          String(value).toLowerCase().includes(normalized),
        );
      const matchesStatus = status === "All" || String(row[statusKey]) === status;
      return matchesQuery && matchesStatus;
    });
  }, [query, rows, status, statusKey]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const visible = filtered.slice((page - 1) * pageSize, page * pageSize);

  async function refresh() {
    if (onRefresh) {
      await onRefresh();
      return;
    }
    setRefreshing(true);
    window.setTimeout(() => setRefreshing(false), 550);
  }

  return (
    <section className="data-panel panel-card">
      <div className="table-toolbar">
        <label className="table-search">
          <Search />
          <Input
            aria-label={searchPlaceholder}
            placeholder={searchPlaceholder}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(1);
            }}
          />
        </label>
        <div className="toolbar-controls">
          {toolbarExtras}
          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
          >
            <SelectTrigger className="filter-select" aria-label="Filter by status">
              <Filter />
              <SelectValue placeholder="All statuses" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="All">All statuses</SelectItem>
              {statuses.filter(Boolean).map((item) => (
                <SelectItem key={item} value={item}>{item}</SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button variant="outline" className="toolbar-button" onClick={() => void refresh()} disabled={loading}>
            {loading ? <LoaderCircle className="spin" /> : onRefresh ? <RefreshCw /> : <SlidersHorizontal />}
            Refresh
          </Button>
          <Button variant="outline" className="toolbar-button" onClick={() => undefined}>
            <Download /> Export
          </Button>
        </div>
      </div>

      {error && !loading ? (
        <div className="table-error" role="alert">
          <TriangleAlert />
          <span><strong>Unable to load data</strong><small>{error}</small></span>
          {onRetry ? <Button variant="outline" onClick={() => void onRetry()}><RefreshCw />Retry</Button> : null}
        </div>
      ) : null}

      {!error || loading ? <div className="table-scroll">
        <Table className="bookeat-table">
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column.key} className={column.className}>{column.label}</TableHead>
              ))}
              {onView ? <TableHead className="action-column">Action</TableHead> : null}
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading
              ? Array.from({ length: 4 }).map((_, index) => (
                  <TableRow key={index}>
                    {columns.map((column) => (
                      <TableCell key={column.key}><Skeleton className="h-5 w-24" /></TableCell>
                    ))}
                    {onView ? <TableCell><Skeleton className="h-8 w-8" /></TableCell> : null}
                  </TableRow>
                ))
              : visible.map((row) => (
                  <TableRow key={row.id} onDoubleClick={() => onView?.(row)}>
                    {columns.map((column) => (
                      <TableCell key={column.key} className={column.className}>
                        {column.render
                          ? column.render(row[column.key], row)
                          : row[column.key]}
                      </TableCell>
                    ))}
                    {onView ? (
                      <TableCell className="action-column">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" aria-label={`Actions for ${row.id}`}>
                              <MoreHorizontal />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => onView(row)}>
                              <Eye /> {actionLabel}
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    ) : null}
                  </TableRow>
                ))}
          </TableBody>
        </Table>
      </div> : null}

      {!error && !loading && visible.length === 0 ? (
        <div className="table-empty">
          <Search />
          <strong>{emptyTitle}</strong>
          <span>{emptyDescription}</span>
          <Button variant="outline" onClick={() => { setQuery(""); setStatus("All"); }}>
            Clear filters
          </Button>
        </div>
      ) : null}

      {!error ? <div className="table-footer">
        <span>
          Showing {visible.length ? (page - 1) * pageSize + 1 : 0}–{Math.min(page * pageSize, filtered.length)} of {filtered.length}
        </span>
        <div className="pager" aria-label="Table pagination">
          <Button
            variant="outline"
            size="icon"
            disabled={page === 1}
            onClick={() => setPage((current) => Math.max(1, current - 1))}
          >
            <ChevronLeft />
          </Button>
          {Array.from({ length: totalPages }).slice(0, 3).map((_, index) => (
            <Button
              key={index}
              size="icon"
              variant={page === index + 1 ? "default" : "outline"}
              onClick={() => setPage(index + 1)}
            >
              {index + 1}
            </Button>
          ))}
          <Button
            variant="outline"
            size="icon"
            disabled={page === totalPages}
            onClick={() => setPage((current) => Math.min(totalPages, current + 1))}
          >
            <ChevronRight />
          </Button>
        </div>
      </div> : null}
    </section>
  );
}
