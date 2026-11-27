# DiniBee Admin v37 — Restaurant Onboarding action bar overlap fix

- Based on Admin Panel v36.
- Restaurant Onboarding details pane now uses a two-row flex layout: independently scrollable content and a separate footer containing the three review buttons.
- No absolute-positioned overlay. Buttons do not cover Overview, Documents, Photos, Bank Details, or Notes contents.
- Responsive mobile panel height supported.
- No changes to API URLs, authentication, database, approval/rejection callbacks, or existing page navigation.
- **Existing caveat:** The Request Changes button is wired exactly as in v36 (no onClick), and this patch does not change that behavior.

Changed: `components/bookeat/ecosystem/v17-pages.tsx`, `app/globals.css`.

Deploy: back up current `/opt/dinibee-admin`, then deploy updated source with your existing env and build/restart method.
