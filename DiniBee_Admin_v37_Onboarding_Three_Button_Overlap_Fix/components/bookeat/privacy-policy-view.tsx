"use client";

import React, { useState } from "react";
import {
  ShieldCheck,
  Lock,
  MapPin,
  CreditCard,
  Trash2,
  Mail,
  FileText,
  Printer,
  Copy,
  Check,
  ExternalLink,
  ChevronRight,
  Info,
  Building2,
  CalendarCheck,
  UserCheck,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";

interface PrivacyPolicyViewProps {
  inAdminLayout?: boolean;
}

export function PrivacyPolicyView({ inAdminLayout = false }: PrivacyPolicyViewProps) {
  const [copied, setCopied] = useState(false);

  const handleCopyLink = () => {
    if (typeof window !== "undefined") {
      const url = `${window.location.origin}/privacy-policy`;
      navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };

  const handlePrint = () => {
    if (typeof window !== "undefined") {
      window.print();
    }
  };

  return (
    <div className={`privacy-policy-container ${inAdminLayout ? "p-4 md:p-8 max-w-6xl mx-auto" : "min-h-screen bg-slate-50 text-slate-900"}`}>
      {/* Public Header (only displayed if outside admin shell) */}
      {!inAdminLayout && (
        <header className="sticky top-0 z-30 bg-white/90 backdrop-blur-md border-b border-slate-200 px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center p-1.5 shadow-sm">
              <img src="/dinibee-mark.png" alt="DiniBee" className="w-full h-full object-contain" />
            </span>
            <div>
              <span className="font-extrabold text-lg tracking-tight text-slate-900">
                Dini<span className="text-amber-500">Bee</span>
              </span>
              <span className="text-xs text-slate-500 block font-medium">Customer & Platform Legal Portal</span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleCopyLink}
              className="hidden sm:inline-flex items-center gap-1.5 text-xs font-medium"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
              {copied ? "Link Copied" : "Copy Policy URL"}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handlePrint}
              className="inline-flex items-center gap-1.5 text-xs font-medium"
            >
              <Printer className="w-3.5 h-3.5" />
              Print
            </Button>
          </div>
        </header>
      )}

      {/* Main Content Area */}
      <div className={`${!inAdminLayout ? "max-w-5xl mx-auto px-4 sm:px-6 py-10" : "space-y-6"}`}>
        
        {/* Hero Banner / Card */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-slate-900 via-slate-850 to-slate-950 text-white p-6 sm:p-10 shadow-xl border border-slate-800">
          <div className="absolute top-0 right-0 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
          
          <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="space-y-3">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-300 border border-amber-500/30">
                <ShieldCheck className="w-3.5 h-3.5" />
                Google Play Policy Compliant
              </div>
              <h1 className="text-2xl sm:text-4xl font-extrabold tracking-tight">
                DiniBee Privacy Policy
              </h1>
              <p className="text-sm sm:text-base text-slate-300 max-w-2xl leading-relaxed">
                This Privacy Policy explains how DiniBee collects, uses, protects, and discloses personal information for our restaurant reservation, takeaway ordering, and dining services.
              </p>
              <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 pt-2">
                <span><strong>Application:</strong> DiniBee Customer (tech.dinibee.customer)</span>
                <span>•</span>
                <span><strong>Effective Date:</strong> October 2026</span>
                <span>•</span>
                <span><strong>Status:</strong> Active & Verified</span>
              </div>
            </div>

            {inAdminLayout && (
              <div className="flex flex-col sm:flex-row md:flex-col gap-2 shrink-0">
                <Button
                  onClick={handleCopyLink}
                  className="bg-amber-500 hover:bg-amber-600 text-slate-950 font-semibold text-xs px-4 py-2 rounded-lg flex items-center gap-2 shadow-md"
                >
                  {copied ? <Check className="w-4 h-4 text-slate-950" /> : <Copy className="w-4 h-4" />}
                  {copied ? "URL Copied to Clipboard" : "Copy Public URL for Play Console"}
                </Button>
                <Button
                  variant="outline"
                  onClick={handlePrint}
                  className="bg-white/10 hover:bg-white/20 text-white border-white/20 text-xs px-4 py-2 rounded-lg flex items-center gap-2"
                >
                  <Printer className="w-4 h-4" />
                  Print / Export PDF
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* Highlights / Quick Compliance Badges */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-6">
          <div className="bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0 border border-emerald-100">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Zero Ad Tracking</h4>
              <p className="text-xs text-slate-500 mt-0.5">We do NOT use or collect Android Advertising ID (AD_ID).</p>
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center shrink-0 border border-blue-100">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Protected Location</h4>
              <p className="text-xs text-slate-500 mt-0.5">Used strictly for nearby restaurants & arrival check-in.</p>
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 border border-amber-100">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-semibold text-slate-900">PCI-DSS Payments</h4>
              <p className="text-xs text-slate-500 mt-0.5">Encrypted transactions handled via verified gateways (Razorpay).</p>
            </div>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-200/80 shadow-sm flex items-start gap-3.5">
            <div className="w-10 h-10 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center shrink-0 border border-purple-100">
              <Trash2 className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-semibold text-slate-900">Full User Control</h4>
              <p className="text-xs text-slate-500 mt-0.5">Instant account & data deletion available anytime.</p>
            </div>
          </div>
        </div>

        {/* Detailed Policy Content Sections */}
        <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-6 sm:p-10 space-y-8 mt-6">
          
          {/* Section 1 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">1</span>
              <h2>Introduction & Scope</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              Welcome to <strong>DiniBee</strong> ("we," "our," or "us"). DiniBee provides seamless table reservation, takeaway ordering, dining bill settlements, and restaurant discovery services through the DiniBee Customer mobile application (Package: <code>tech.dinibee.customer</code>), our website, and associated management platforms.
            </p>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              We value your trust and are committed to protecting your personal information. This Privacy Policy details the types of data we collect, how it is processed and secured, and your rights regarding your data when using DiniBee.
            </p>
          </section>

          <hr className="border-slate-100" />

          {/* Section 2 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">2</span>
              <h2>Information We Collect</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              To deliver high-quality dining, booking, and ordering services, we collect only the necessary information:
            </p>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-2">
                <div className="flex items-center gap-2 font-semibold text-slate-800 text-sm">
                  <UserCheck className="w-4 h-4 text-amber-600" />
                  <span>Account & Contact Information</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Your name, mobile phone number, and email address collected during registration or login (via OTP verification) to maintain your profile and communicate reservation status.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-2">
                <div className="flex items-center gap-2 font-semibold text-slate-800 text-sm">
                  <MapPin className="w-4 h-4 text-amber-600" />
                  <span>Precise & Approximate Location</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  With your explicit in-app consent, we access your device location to show nearby restaurants, display accurate distance/navigation, and verify on-premise check-in.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-2">
                <div className="flex items-center gap-2 font-semibold text-slate-800 text-sm">
                  <CalendarCheck className="w-4 h-4 text-amber-600" />
                  <span>Reservations & Orders</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Booking date, time slot, party size, dietary/seating preferences, takeaway menu items, and active DiniTicket orders to fulfill dining requests with restaurants.
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/70 space-y-2">
                <div className="flex items-center gap-2 font-semibold text-slate-800 text-sm">
                  <CreditCard className="w-4 h-4 text-amber-600" />
                  <span>Transaction Records</span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed">
                  Payment reference IDs, billed amounts, timestamps, and payment statuses. <em>Note: DiniBee does not store complete card numbers, CVVs, or banking credentials.</em>
                </p>
              </div>
            </div>
          </section>

          <hr className="border-slate-100" />

          {/* Section 3 - Advertising ID Declaration (Highlight for Google Play) */}
          <section className="space-y-3 bg-amber-500/5 border border-amber-500/20 p-5 sm:p-6 rounded-xl">
            <div className="flex items-center gap-2 text-amber-700 font-bold text-lg sm:text-xl">
              <ShieldCheck className="w-5 h-5 text-amber-600" />
              <h2>Google Play Advertising ID (AD_ID) Policy Declaration</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-700 leading-relaxed">
              In accordance with Google Play Developer Policy targeting Android 13 (API level 33) and above:
            </p>
            <div className="bg-white p-4 rounded-lg border border-amber-200 text-sm text-slate-800 space-y-2">
              <p className="font-semibold text-amber-800 flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-600" />
                Declaration: DiniBee does NOT use or request the Android Advertising ID.
              </p>
              <p className="text-xs text-slate-600">
                The DiniBee customer application (<code>tech.dinibee.customer</code>) does not request the <code>com.google.android.gms.permission.AD_ID</code> permission, does not integrate third-party advertising SDKs (such as Google Mobile Ads, AdMob, or ad trackers), and does not track users across other apps or websites for targeted advertising.
              </p>
            </div>
          </section>

          <hr className="border-slate-100" />

          {/* Section 4 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">3</span>
              <h2>How We Use Your Information</h2>
            </div>
            <ul className="space-y-2.5 text-sm sm:text-base text-slate-600 list-disc list-inside leading-relaxed">
              <li><strong>Reservation Fulfillment:</strong> Relaying guest counts, names, and booking slots to partner restaurants so tables are prepared upon your arrival.</li>
              <li><strong>Order Management:</strong> Processing food orders, tracking takeaway preparation, and enabling dynamic DiniTicket sessions.</li>
              <li><strong>Payment Processing:</strong> Verifying restaurant bill settlements, calculating applicable cashback/discounts, and generating invoices via certified payment gateways.</li>
              <li><strong>Service Communications:</strong> Sending booking confirmations, booking reminders, security verification OTPs, and customer support notifications.</li>
              <li><strong>Fraud Prevention & Safety:</strong> Ensuring legitimate table reservations, authentic reviews, and preventing platform abuse.</li>
            </ul>
          </section>

          <hr className="border-slate-100" />

          {/* Section 5 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">4</span>
              <h2>Data Sharing & Third Parties</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              We never sell, rent, or monetize your personal information to data brokers or third parties. We share data strictly on a need-to-know basis with:
            </p>
            <div className="space-y-2 text-sm text-slate-600 pl-2">
              <div className="flex items-start gap-2">
                <ChevronRight className="w-4 h-4 text-amber-600 shrink-0 mt-1" />
                <span><strong>Partner Restaurants:</strong> The specific restaurant where you make a reservation or place an order receives your name, party details, and contact number to prepare your table and food.</span>
              </div>
              <div className="flex items-start gap-2">
                <ChevronRight className="w-4 h-4 text-amber-600 shrink-0 mt-1" />
                <span><strong>Payment Gateways (Razorpay):</strong> Encrypted payment requests are securely handled by PCI-DSS certified payment service providers for transaction processing.</span>
              </div>
              <div className="flex items-start gap-2">
                <ChevronRight className="w-4 h-4 text-amber-600 shrink-0 mt-1" />
                <span><strong>Legal & Compliance:</strong> When compelled by valid legal processes, applicable Indian statutes, or governmental authority requests.</span>
              </div>
            </div>
          </section>

          <hr className="border-slate-100" />

          {/* Section 6 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">5</span>
              <h2>Data Security & Encryption</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              We employ industry-standard technical, operational, and organizational safeguards to protect your personal data. All network communication between the DiniBee mobile app and our servers is secured using modern TLS 1.3 / HTTPS encryption. Access to sensitive platform databases is strictly restricted to authorized personnel with multi-factor authentication.
            </p>
          </section>

          <hr className="border-slate-100" />

          {/* Section 7 - User Rights & Data Deletion */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">6</span>
              <h2>Your Rights & Account Deletion</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              You maintain complete authority over your personal information:
            </p>
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-3">
              <h4 className="font-semibold text-slate-800 text-sm flex items-center gap-2">
                <Trash2 className="w-4 h-4 text-red-600" />
                How to Request Account & Data Deletion
              </h4>
              <p className="text-xs text-slate-600 leading-relaxed">
                In compliance with Google Play Data Safety policies, users can delete their account and associated personal data at any time:
              </p>
              <ol className="list-decimal list-inside text-xs text-slate-600 space-y-1.5 pl-1">
                <li><strong>In-App Deletion:</strong> Open the DiniBee app → Navigate to <em>Profile</em> → <em>Settings</em> → <em>Delete Account</em>.</li>
                <li><strong>Email Request:</strong> Email our dedicated privacy team at <a href="mailto:support@dinibee.com" className="text-amber-600 font-medium underline">support@dinibee.com</a> with the subject line <em>"Account Deletion Request"</em> from your registered email/phone.</li>
              </ol>
              <p className="text-xs text-slate-500 italic">
                Account deletion requests are permanently fulfilled within 30 days. Historical financial transaction logs are retained strictly as required by applicable taxation and accounting regulations.
              </p>
            </div>
          </section>

          <hr className="border-slate-100" />

          {/* Section 8 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">7</span>
              <h2>Children's Privacy</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              DiniBee services are intended for adults and individuals aged 18 and above (or minors under parental supervision). We do not knowingly solicit or collect data from children under the age of 13. If you become aware that a child has provided us with personal data, please contact us immediately to remove such records.
            </p>
          </section>

          <hr className="border-slate-100" />

          {/* Section 9 */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">8</span>
              <h2>Updates to This Policy</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              We may periodically revise this Privacy Policy to reflect platform enhancements, operational changes, or new regulatory guidelines. Any updates will be published on this page with an updated "Effective Date."
            </p>
          </section>

          <hr className="border-slate-100" />

          {/* Section 10 - Contact & Grievance */}
          <section className="space-y-3">
            <div className="flex items-center gap-2.5 text-amber-600 font-bold text-lg sm:text-xl">
              <span className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center text-xs font-extrabold">9</span>
              <h2>Contact & Grievance Officer</h2>
            </div>
            <p className="text-sm sm:text-base text-slate-600 leading-relaxed">
              For any questions, concerns, feedback, or grievance redressal regarding this Privacy Policy or your personal information, please reach out to:
            </p>
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-1.5 text-sm text-slate-700">
              <p><strong>DiniBee Support & Grievance Cell</strong></p>
              <p className="flex items-center gap-2 text-slate-600">
                <Mail className="w-4 h-4 text-amber-600" />
                <span>Email: <a href="mailto:support@dinibee.com" className="text-amber-600 hover:underline">support@dinibee.com</a></span>
              </p>
              <p className="flex items-center gap-2 text-slate-600">
                <Building2 className="w-4 h-4 text-amber-600" />
                <span>Entity: DiniBee Platform Technologies</span>
              </p>
            </div>
          </section>

        </div>

        {/* Footer */}
        <footer className="text-center text-xs text-slate-400 py-6">
          <p>© {new Date().getFullYear()} DiniBee. All rights reserved. Registered trademark of DiniBee Technologies.</p>
        </footer>

      </div>
    </div>
  );
}
