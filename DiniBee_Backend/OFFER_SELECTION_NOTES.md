DiniBee offer selection v66 notes

Customer v73 sends selected CASHBACK offer after creating a manual bill. Backend validates offer ID, active dates, restaurant, amount, premium restrictions, and bill ownership. Selections are stored in V66__pay_bill_offer_selection.sql.

IMPORTANT: This does not credit cashback to a wallet or deduct payment totals. Bank and instant discount offers are intentionally rejected because bank/payment-method verification is not yet implemented. Revalidation during gateway capture and reward issuance are required before live cashback promises. Full bill Razorpay amount remains authoritative.
