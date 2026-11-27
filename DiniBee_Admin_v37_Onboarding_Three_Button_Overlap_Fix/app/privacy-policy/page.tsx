import type { Metadata } from "next";
import { PrivacyPolicyView } from "@/components/bookeat/privacy-policy-view";

export const metadata: Metadata = {
  title: "Privacy Policy | DiniBee",
  description:
    "DiniBee Customer and Platform Privacy Policy. Comprehensive details on data collection, location privacy, secure payments, user rights, and zero advertising tracking.",
};

export default function PrivacyPolicyPage() {
  return <PrivacyPolicyView inAdminLayout={false} />;
}
