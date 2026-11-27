# v26 Customer action endpoint fix

Fixed the Customers page to use backend routes that actually exist in the current DiniBee backend:

- customer safe profile: `GET /api/v1/admin/users/{id}`
- block customer: `PUT /api/v1/admin/users/{id}/block`
- unblock customer: `PUT /api/v1/admin/users/{id}/unblock`

The previous UI incorrectly called `/api/v1/admin/customers/{id}/block` and `/status`, which return 404 because `AdminCustomerController` only exposes the customer list endpoint.

The "View in App" action no longer launches an unregistered `dinibee://` browser protocol. It safely copies the customer ID and shows a message instead.
