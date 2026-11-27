# v39 Order ID + DiniTicket image fix

- Food-order public IDs remain 4-character uppercase alphanumeric codes for new orders.
- Migration V54 backfills older FO-/UUID-style food order numbers to 4-character public codes.
- Food item image URL is snapshotted into `food_order_items` when the order is created.
- Existing order-item image URLs are backfilled from `menu_items`.
- Order responses prefer the snapshot so DiniTicket images survive menu archive/edit/session rollover.
