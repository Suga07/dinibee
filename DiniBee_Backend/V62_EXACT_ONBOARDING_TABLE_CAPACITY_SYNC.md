# V62 exact onboarding table capacity sync
Root cause: older onboarding saved only planned table count + planned seat count and materialized live tables by averaging seats across tables. Example: 2x2-seat + 2x4-seat + 1x10-seat could become incorrect 7/8-seat categories.

Fix:
- Complete onboarding form `tables` is now reconciled into `restaurant_tables` using each table's exact capacity.
- Existing matching table numbers are updated instead of skipped.
- Missing exact tables are created.
- Leftover old auto-generated Tn / Table n rows not present in the form are disabled when they have no current/future active reservation.
- Flyway V62 repairs existing restaurant data at deployment from stored `restaurants.onboarding_form`.
