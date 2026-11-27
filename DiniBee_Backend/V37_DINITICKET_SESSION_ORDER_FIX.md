# v37 DiniTicket session-order fix

- DiniTicket session menu Save now preserves database MenuItem IDs for foods with the same name.
- Removed the delete/recreate-on-every-save behavior that made customer checkout IDs stale and caused HTTP 409 MENU_ITEM_UNAVAILABLE.
- Removed foods are still soft-deleted; new foods receive new IDs.
- Duplicate food names in one session save are rejected.
