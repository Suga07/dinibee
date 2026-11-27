# MyDreams OTP setup

The code now implements real backend OTP endpoints for CUSTOMER and OWNER. No demo OTP remains in the Owner login.

## Required environment variables

MYDREAMS_OTP_ENABLED=true
MYDREAMS_API_KEY=<new API key>
MYDREAMS_OTP_URL_TEMPLATE=<exact HTTP GET URL from Developers > API Documentation>

The URL template supports `{apiKey}`, `{mobile}`, `{otp}`, and `{message}` placeholders. Example shape only (NOT the provider contract):
`https://provider.example/send?key={apiKey}&mobile={mobile}&message={message}`

You must copy the exact URL/parameter names from MyDreams API Documentation. The provider contract was not included in the uploaded source, so it is intentionally not guessed.

Endpoints:
POST /api/v1/auth/mobile-otp/send  body {phoneNumber, role:CUSTOMER|OWNER}
POST /api/v1/auth/mobile-otp/verify body {challengeId, code}

OTP: 6 digits, existing backend TTL, resend cooldown, attempt limit, hashed challenge storage, JWT returned after successful verification.
