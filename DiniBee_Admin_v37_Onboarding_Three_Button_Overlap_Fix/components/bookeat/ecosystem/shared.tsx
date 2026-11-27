"use client";

import type { ReactNode } from "react";
import { Database, ShieldCheck } from "lucide-react";

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { isDemoDataEnabled } from "@/lib/bookeat/services/data-source";
import type { BusinessService } from "@/lib/bookeat/types";

export function formatMoney(value: number) {
  const safeValue = Number.isFinite(Number(value)) ? Number(value) : 0;
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(safeValue);
}

export function formatCount(value: number) {
  return new Intl.NumberFormat("en-IN").format(value);
}

export function DataSourceNotice() {
  return (
    <span className={`data-source-notice ${isDemoDataEnabled ? "demo" : "live"}`}>
      {isDemoDataEnabled ? <Database /> : <ShieldCheck />}
      {isDemoDataEnabled ? "Backend-ready demo source" : "Connected to live API"}
    </span>
  );
}

export function ServicePills({ services }: { services?: BusinessService[] }) {
  const list = Array.isArray(services) ? services : [];
  return (
    <span className="service-pills">
      {list.map((service) => (
        <span key={service} className={String(service).toLowerCase()}>
          {service === "DINITICKET" ? "DiniTicket" : String(service).charAt(0) + String(service).slice(1).toLowerCase()}
        </span>
      ))}
    </span>
  );
}

export function DetailSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section className="ecosystem-detail-section">
      <div className="ecosystem-section-heading">
        <strong>{title}</strong>
        {description ? <small>{description}</small> : null}
      </div>
      {children}
    </section>
  );
}

export function DetailGrid({
  fields,
}: {
  fields: Array<{ label: string; value: ReactNode }>;
}) {
  return (
    <dl className="ecosystem-detail-grid">
      {fields.map((field) => (
        <div key={field.label}>
          <dt>{field.label}</dt>
          <dd>{field.value}</dd>
        </div>
      ))}
    </dl>
  );
}

export function ConfirmActionDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  destructive = false,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  destructive?: boolean;
  onConfirm: () => void | Promise<void>;
}) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent className="action-dialog">
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction
            className={destructive ? "danger-button" : "gold-button"}
            onClick={() => void onConfirm()}
          >
            {confirmLabel}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

