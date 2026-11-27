# Razorpay local test

Do not put the Razorpay Key Secret in Flutter or source control.

PowerShell:

```powershell
$env:RAZORPAY_PAYMENTS_ENABLED="true"
$env:RAZORPAY_KEY_ID="<your regenerated rzp_test key id>"
$env:RAZORPAY_KEY_SECRET="<your regenerated test secret>"
$env:RAZORPAY_WEBHOOK_ENABLED="false"
mvn spring-boot:run
```

Backend base URL: `http://localhost:8080/api/v1`

Create an online payment with `gateway: "RAZORPAY"` using `POST /api/v1/customer/payments/create-order` and an `Idempotency-Key` header. The response contains `checkout.apiKey` (safe public key) and `checkout.paymentSessionId` (Razorpay order id). After Razorpay Checkout succeeds, POST the returned payment id and signature to `/api/v1/customer/payments/verify` with the BookEat payment UUID.

Webhook is intentionally disabled for first localhost testing. Payment confirmation still requires server-side Razorpay signature verification and provider payment fetch.
