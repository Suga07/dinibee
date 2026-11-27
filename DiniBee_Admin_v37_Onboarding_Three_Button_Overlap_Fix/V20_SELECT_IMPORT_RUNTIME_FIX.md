# V20 Select Runtime Fix

Fixed runtime error on Restaurant Onboarding and Restaurants pages:

`Select is not defined`

Cause: `Select`, `SelectTrigger`, `SelectValue`, `SelectContent`, and `SelectItem` were used in `components/bookeat/ecosystem/v17-pages.tsx` without importing them.

Fix: added the missing import from `@/components/ui/select`.

No API routes or page workflows were changed.
