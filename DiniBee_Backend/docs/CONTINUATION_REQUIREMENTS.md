# BOOKEAT / DINIBEE BACKEND CONTINUATION MASTER PROMPT

## PHASE 3 → PHASE 10+ → FINAL INTEGRATION

You are continuing an existing production-grade Java Spring Boot backend for the BookEat / DiniBee restaurant platform.

The project already contains:

- Phase 1 — Foundation
- Phase 2 — Authentication & JWT

DO NOT recreate Phase 1 or Phase 2.

DO NOT create a second backend.

DO NOT change package names unnecessarily.

DO NOT break existing APIs.

DO NOT modify old Flyway migrations that may already be applied.

Continue inside the existing backend project.

---

# EXISTING FRONTENDS

The backend must support three existing applications:

## 1. BookEat Customer App

Technology:

Flutter

Purpose:

Customer restaurant discovery
Restaurant booking
Group booking
Corporate booking
Food pre-order
Takeaway
On-the-Way Takeaway
DiniTicket
Payments
Wallet
Rewards
Reviews
Support
Profile

---

## 2. Restaurant Owner App

Technology:

Flutter

Purpose:

Restaurant onboarding
Restaurant approval status
Dashboard
Tables
Reservations
Menu
Food Orders
Takeaway
DiniTicket
Payments
Analytics
Notifications
Support
Settings
Profile

---

## 3. BookEat Admin Panel

Technology:

React / Next.js / TypeScript

Purpose:

Restaurant approval
Users
Restaurants
Reservations
Orders
Payments
Settlements
Refunds
Wallet
Rewards
Reviews
Support
Reports
Admin roles
Platform configuration

---

# SHARED BACKEND

All applications use:

Spring Boot
PostgreSQL
REST API
JWT
Flyway
Swagger
Docker

Base URL:

/api/v1

Example:

[http://localhost:8080/api/v1](http://localhost:8080/api/v1)

Production example:

[https://api.bookeat.com/api/v1](https://api.bookeat.com/api/v1)

---

# MAIN DEVELOPMENT ORDER

Build in this order:

Phase 3 — Owner Onboarding + Admin Approval

Phase 4 — Tables + Menu + Availability Foundation

Phase 5 — Customer Restaurant APIs

Phase 6 — Reservations

- Regular
- Group
- Corporate

Phase 7 — Food Orders + Takeaway

Phase 8 — DiniTicket

Phase 9 — Check-in + Dining

Phase 10 — Payments

Phase 11 — Wallet + Rewards

Phase 12 — Reviews + Support + Notifications

Phase 13 — Admin + Reports + Analytics

Phase 14 — Frontend Integration

Phase 15 — Testing + Security + Production Readiness

Do not jump randomly between phases.

---

# PHASE 3

# OWNER ONBOARDING + ADMIN APPROVAL

Implement complete owner onboarding.

Workflow:

Owner registration
↓
Login
↓
Create Restaurant
↓
Restaurant Details
↓
Location
↓
Business Details
↓
Operating Hours
↓
Select Services
↓
Table Setup configuration
↓
Reservation Settings
↓
Takeaway Settings
↓
DiniTicket Settings
↓
Menu Setup status
↓
Settlement Details
↓
Policies
↓
Team Access
↓
Review
↓
Submit
↓
Admin Review
↓
Approve / Reject / Request Changes
↓
Restaurant Activated

---

# RESTAURANT SERVICES

Create enum:

RESTAURANT

TAKEAWAY

DINITICKET

Restaurant may enable:

RESTAURANT

or:

RESTAURANT + TAKEAWAY

or:

RESTAURANT + DINITICKET

or:

all three.

Create:

RestaurantServiceActivation

Fields:

id
restaurant
serviceType
enabled
activatedAt
disabledAt

Unique:

restaurant + serviceType

---

# RESTAURANT

Create Restaurant entity.

Fields:

id
owner
restaurantName
description
restaurantType
email
phone
alternatePhone
addressLine1
addressLine2
city
state
postalCode
country
latitude
longitude
businessRegistrationNumber
gstNumber
fssaiNumber
logoUrl
coverImageUrl
approvalStatus
status
submittedAt
approvedAt
approvedBy
rejectionReason
changeRequestReason
createdAt
updatedAt
version

Use BaseEntity where already available.

---

# RESTAURANT APPROVAL STATUS

DRAFT

SUBMITTED

UNDER\_REVIEW

CHANGES\_REQUESTED

APPROVED

REJECTED

SUSPENDED

---

# RESTAURANT STATUS

INACTIVE

ACTIVE

TEMPORARILY\_CLOSED

SUSPENDED

---

# OPERATING HOURS

Create:

RestaurantOperatingHour

Fields:

restaurant
dayOfWeek
openingTime
closingTime
closed

Unique:

restaurant + dayOfWeek

---

# SPECIAL SCHEDULE

Create:

RestaurantSpecialSchedule

Fields:

restaurant
date
closed
openingTime
closingTime
reason

Used later by availability engine.

---

# BUSINESS DOCUMENTS

Create:

RestaurantDocument

Types:

FSSAI
GST
BUSINESS\_REGISTRATION
OWNER\_ID
PAN
OTHER

Fields:

restaurant
documentType
documentNumber
fileUrl
verificationStatus
verifiedBy
verifiedAt
rejectionReason

Statuses:

PENDING
VERIFIED
REJECTED

---

# RESERVATION SETTINGS

Create:

RestaurantReservationSettings

Fields:

restaurant
reservationEnabled
minimumGuests
maximumGuestsPerBooking
slotDurationMinutes
reservationDurationMinutes
advanceBookingDays
minimumAdvanceMinutes
gracePeriodMinutes
cancellationAllowed
cancellationBeforeMinutes
autoConfirm
specialRequestAllowed
preOrderAllowed
gpsCheckInEnabled
qrCheckInEnabled
gpsCheckInRadiusMeters

---

# TAKEAWAY SETTINGS

Create:

RestaurantTakeawaySettings

Fields:

restaurant
enabled
regularTakeawayEnabled
onTheWayEnabled
minimumOrderAmount
averagePreparationMinutes
maximumAdvanceOrderMinutes
acceptCash
acceptOnlinePayment

---

# DINITICKET SETTINGS

Create:

DiniTicketSettings

Fields:

restaurant
enabled
maximumQueueSize
defaultEstimatedMinutesPerParty
allowPreOrder
allowRemoteJoin
ticketExpiryMinutes
autoExpireTickets

---

# POLICIES

Create:

RestaurantPolicy

Fields:

restaurant
cancellationPolicy
refundPolicy
childPolicy
petPolicy
dressCode
additionalPolicy

---

# SETTLEMENT PROFILE

Create:

RestaurantSettlementProfile

Fields:

restaurant
accountHolderName
bankName
encryptedAccountNumber
ifscCode
upiId
taxName
gstNumber
verified
verifiedAt

Never expose full bank number.

Return masked value.

---

# ONBOARDING PROGRESS

Create:

RestaurantOnboardingProgress

Fields:

restaurant
currentStep
restaurantDetailsComplete
locationComplete
businessInfoComplete
hoursComplete
servicesComplete
tableSetupComplete
reservationSettingsComplete
takeawaySettingsComplete
diniticketSettingsComplete
menuSetupComplete
paymentDetailsComplete
policiesComplete
teamAccessComplete
submitted

Backend is the source of truth.

---

# OWNER RESTAURANT APIs

POST
/owner/restaurants

GET
/owner/restaurants

GET
/owner/restaurants/{restaurantId}

PUT
/owner/restaurants/{restaurantId}

PUT
/owner/restaurants/{restaurantId}/location

PUT
/owner/restaurants/{restaurantId}/business-info

PUT
/owner/restaurants/{restaurantId}/operating-hours

PUT
/owner/restaurants/{restaurantId}/services

PUT
/owner/restaurants/{restaurantId}/reservation-settings

PUT
/owner/restaurants/{restaurantId}/takeaway-settings

PUT
/owner/restaurants/{restaurantId}/diniticket-settings

PUT
/owner/restaurants/{restaurantId}/policies

PUT
/owner/restaurants/{restaurantId}/settlement-profile

GET
/owner/restaurants/{restaurantId}/onboarding-progress

POST
/owner/restaurants/{restaurantId}/submit

GET
/owner/dashboard-context

---

# OWNER DASHBOARD CONTEXT

Return:

owner

restaurant

approvalStatus

restaurantStatus

enabledServices

permissions

onboardingComplete

Example:

{
"owner": {
"id": "..."
},
"restaurant": {
"id": "...",
"restaurantName": "ABC Restaurant",
"approvalStatus": "APPROVED",
"status": "ACTIVE"
},
"services": [
"RESTAURANT",
"TAKEAWAY",
"DINITICKET"
],
"onboardingComplete": true
}

Flutter uses enabled services to display modules dynamically.

---

# OWNER SECURITY

Owner A must not access Owner B restaurant.

Create reusable:

RestaurantAccessService

Example:

getOwnedRestaurant(authenticatedUserId, restaurantId)

Never trust restaurantId from frontend alone.

---

# ADMIN RESTAURANT APIs

GET
/admin/restaurants

GET
/admin/restaurants/pending

GET
/admin/restaurants/{restaurantId}

PUT
/admin/restaurants/{restaurantId}/under-review

PUT
/admin/restaurants/{restaurantId}/approve

PUT
/admin/restaurants/{restaurantId}/reject

PUT
/admin/restaurants/{restaurantId}/request-changes

PUT
/admin/restaurants/{restaurantId}/suspend

PUT
/admin/restaurants/{restaurantId}/reactivate

PATCH
/admin/restaurants/{restaurantId}/services/{serviceType}

Use permissions.

Examples:

RESTAURANT\_VIEW

RESTAURANT\_APPROVE

RESTAURANT\_REJECT

RESTAURANT\_SUSPEND

RESTAURANT\_SERVICE\_MANAGE

---

# PHASE 4

# TABLE MANAGEMENT + MENU + AVAILABILITY FOUNDATION

---

# TABLE ENTITY

Create:

RestaurantTable

Fields:

id
restaurant
tableNumber
tableName
capacity
minimumGuests
maximumGuests
floor
section
shape
status
xPosition
yPosition
width
height
rotation
active

Statuses:

AVAILABLE
RESERVED
OCCUPIED
BLOCKED
MAINTENANCE

Shapes:

ROUND
SQUARE
RECTANGLE
OVAL
CUSTOM

---

# TABLE APIs

Owner:

POST
/owner/restaurants/{restaurantId}/tables

GET
/owner/restaurants/{restaurantId}/tables

GET
/owner/restaurants/{restaurantId}/tables/{tableId}

PUT
/owner/restaurants/{restaurantId}/tables/{tableId}

DELETE
/owner/restaurants/{restaurantId}/tables/{tableId}

PATCH
/owner/restaurants/{restaurantId}/tables/{tableId}/status

Customer will later get only bookable tables.

---

# TABLE BUSINESS RULES

Prevent duplicate tableNumber inside restaurant.

Validate capacity.

Do not allow normal booking against:

BLOCKED

MAINTENANCE

OCCUPIED

inactive table

---

# MENU CATEGORY

Create:

MenuCategory

Fields:

id
restaurant
name
description
displayOrder
active

---

# MENU ITEM

Create:

MenuItem

Fields:

id
restaurant
category
name
description
basePrice
discountPrice
foodType
spiceLevel
imageUrl
available
preOrderAvailable
takeawayAvailable
diniTicketPreOrderAvailable
preparationTimeMinutes
featured
displayOrder

Food types:

VEG
NON\_VEG
VEGAN
EGG

---

# MENU VARIANTS

Create:

MenuVariant

Example:

Small

Medium

Large

Fields:

menuItem
name
price
active

---

# MENU ADDONS

Create:

MenuAddon

Fields:

menuItem
name
price
active

---

# MENU OWNER APIs

POST
/owner/restaurants/{restaurantId}/menu/categories

GET
/owner/restaurants/{restaurantId}/menu/categories

PUT
/owner/restaurants/{restaurantId}/menu/categories/{categoryId}

DELETE
/owner/restaurants/{restaurantId}/menu/categories/{categoryId}

POST
/owner/restaurants/{restaurantId}/menu/items

GET
/owner/restaurants/{restaurantId}/menu/items

GET
/owner/restaurants/{restaurantId}/menu/items/{itemId}

PUT
/owner/restaurants/{restaurantId}/menu/items/{itemId}

DELETE
/owner/restaurants/{restaurantId}/menu/items/{itemId}

PATCH
/owner/restaurants/{restaurantId}/menu/items/{itemId}/availability

---

# AVAILABILITY FOUNDATION

Create availability service.

Input:

restaurantId
date
requestedTime
guestCount

Availability must consider:

restaurant approval

restaurant status

service activation

operating hours

special closures

reservation settings

table capacity

active tables

existing bookings

slot duration

reservation duration

grace period

blocked/maintenance tables

---

# AVAILABILITY API

GET
/restaurants/{restaurantId}/availability

Parameters:

date
time
guests

Return:

available

availableTimeSlots

availableTables

reservationDuration

message

Do not yet create booking here.

---

# DOUBLE BOOKING PREPARATION

Design database architecture now to prevent future double bookings.

Use:

transactions

locking strategy

indexed date/time columns

server-side availability validation

Never rely only on Flutter availability screen.

---

# PHASE 5

# CUSTOMER RESTAURANT APIs

Create customer/public restaurant discovery.

Only return restaurants that are:

APPROVED

ACTIVE

and service RESTAURANT is enabled where restaurant booking is required.

---

# CUSTOMER RESTAURANT APIs

GET
/restaurants

GET
/restaurants/{restaurantId}

GET
/restaurants/{restaurantId}/menu

GET
/restaurants/{restaurantId}/operating-hours

GET
/restaurants/{restaurantId}/availability

GET
/restaurants/search

---

# RESTAURANT SEARCH

Filters:

restaurantName

city

latitude

longitude

radius

cuisine

rating

foodType

serviceType

openNow

offerAvailable

sort

page

size

Use pagination.

---

# FAVORITES

Optional but recommended:

POST
/customer/favorites/{restaurantId}

DELETE
/customer/favorites/{restaurantId}

GET
/customer/favorites

Create:

CustomerFavoriteRestaurant

Unique:

customer + restaurant

---

# PHASE 6

# RESERVATION SYSTEM

Implement:

Regular Booking

Group Booking

Corporate Booking

---

# REGULAR RESERVATION

Create:

Reservation

Fields:

id
bookingReference
customer
restaurant
table
reservationDate
startTime
endTime
guestCount
specialRequest
bookingType
status
paymentStatus
checkedInAt
completedAt
cancelledAt
cancellationReason
createdAt
updatedAt

BookingType:

REGULAR
GROUP
CORPORATE

Status:

PENDING

CONFIRMED

ARRIVED

CHECKED\_IN

DINING

COMPLETED

CANCELLED

NO\_SHOW

REJECTED

---

# REGULAR BOOKING FLOW

Customer chooses:

Restaurant
↓
Date
↓
Time
↓
Guests
↓
Table
↓
Special Request
↓
Optional Food Pre-order
↓
Summary
↓
Payment if needed
↓
Confirmation

---

# CUSTOMER RESERVATION APIS

POST
/customer/reservations

GET
/customer/reservations

GET
/customer/reservations/{reservationId}

PUT
/customer/reservations/{reservationId}

PUT
/customer/reservations/{reservationId}/cancel

---

# OWNER RESERVATION APIS

GET
/owner/restaurants/{restaurantId}/reservations

GET
/owner/restaurants/{restaurantId}/reservations/{reservationId}

PUT
/owner/restaurants/{restaurantId}/reservations/{reservationId}/confirm

PUT
/owner/restaurants/{restaurantId}/reservations/{reservationId}/reject

PUT
/owner/restaurants/{restaurantId}/reservations/{reservationId}/no-show

---

# REGULAR BOOKING RULES

Validate again at booking time.

Do not trust previous availability response.

Use transaction.

Prevent same table + overlapping reservation.

Generate unique bookingReference.

---

# RESERVATION STATUS HISTORY

Create:

ReservationStatusHistory

Fields:

reservation
oldStatus
newStatus
changedBy
reason
changedAt

---

# GROUP BOOKING

Create:

GroupBooking

Fields:

id
reservation
hostCustomer
restaurant
inviteCode
inviteUrl
qrCode
memberLimit
status
paymentMode

Status:

CREATED
ACTIVE
LOCKED
COMPLETED
CANCELLED

---

# GROUP MEMBER

Create:

GroupMember

Fields:

groupBooking
customer
displayName
memberStatus
joinedAt

Status:

INVITED
JOINED
LEFT
REMOVED

---

# GROUP APIs

POST
/customer/group-bookings

GET
/customer/group-bookings/{groupId}

POST
/customer/group-bookings/{groupId}/invite

POST
/customer/group-bookings/join/{inviteCode}

GET
/customer/group-bookings/{groupId}/members

DELETE
/customer/group-bookings/{groupId}/members/{memberId}

POST
/customer/group-bookings/{groupId}/lock

---

# GROUP PAYMENT MODE

HOST\_PAYS

SPLIT\_EQUAL

SPLIT\_BY\_ITEMS

Implement payment architecture later.

---

# CORPORATE ACCOUNT

Create:

CorporateAccount

Fields:

id
companyName
gstNumber
billingAddress
contactPersonName
contactEmail
contactPhone
active

---

# CORPORATE BOOKING

Create:

CorporateBooking

Fields:

id
corporateAccount
reservation
restaurant
budgetLimit
billingType
recurrenceType
recurrenceRule
invoiceRequired

Recurrence:

NONE
DAILY
WEEKLY
MONTHLY

---

# CORPORATE GUEST

Create:

CorporateGuest

Fields:

corporateBooking
name
email
phone
employeeCode
status

---

# CORPORATE APIS

POST
/customer/corporate-bookings

GET
/customer/corporate-bookings

GET
/customer/corporate-bookings/{id}

POST
/customer/corporate-bookings/{id}/guests

PUT
/customer/corporate-bookings/{id}

PUT
/customer/corporate-bookings/{id}/cancel

---

# PHASE 7

# FOOD ORDERS + TAKEAWAY

---

# FOOD ORDER

Create:

FoodOrder

Fields:

id
orderNumber
customer
restaurant
reservation
groupBooking
corporateBooking
takeawayOrder
diniTicket
orderType
status
subtotal
discount
tax
total
specialInstructions

OrderType:

DINE\_IN\_PREORDER

DINE\_IN

TAKEAWAY

ON\_THE\_WAY

DINITICKET\_PREORDER

---

# FOOD ORDER ITEM

Fields:

order
menuItem
variant
quantity
unitPrice
subtotal
notes

Support addons appropriately.

---

# FOOD ORDER STATUS

PLACED

ACCEPTED

PREPARING

READY

SERVED

COMPLETED

CANCELLED

REJECTED

---

# CUSTOMER FOOD APIs

POST
/customer/orders

GET
/customer/orders

GET
/customer/orders/{orderId}

PUT
/customer/orders/{orderId}/cancel

---

# OWNER FOOD APIs

GET
/owner/restaurants/{restaurantId}/orders

GET
/owner/restaurants/{restaurantId}/orders/{orderId}

PUT
/owner/restaurants/{restaurantId}/orders/{orderId}/accept

PUT
/owner/restaurants/{restaurantId}/orders/{orderId}/prepare

PUT
/owner/restaurants/{restaurantId}/orders/{orderId}/ready

PUT
/owner/restaurants/{restaurantId}/orders/{orderId}/serve

PUT
/owner/restaurants/{restaurantId}/orders/{orderId}/complete

PUT
/owner/restaurants/{restaurantId}/orders/{orderId}/reject

---

# REGULAR TAKEAWAY

Create:

TakeawayOrder

Fields:

id
orderNumber
customer
restaurant
pickupTime
status
subtotal
tax
discount
total
paymentStatus
pickupCode
collectedAt

Status:

PLACED
ACCEPTED
PREPARING
READY\_FOR\_PICKUP
COLLECTED
CANCELLED
REJECTED

---

# TAKEAWAY APIs

POST
/customer/takeaway/orders

GET
/customer/takeaway/orders

GET
/customer/takeaway/orders/{id}

PUT
/customer/takeaway/orders/{id}/cancel

Owner:

GET
/owner/restaurants/{restaurantId}/takeaway/orders

PUT
/owner/restaurants/{restaurantId}/takeaway/orders/{id}/accept

PUT
/owner/restaurants/{restaurantId}/takeaway/orders/{id}/preparing

PUT
/owner/restaurants/{restaurantId}/takeaway/orders/{id}/ready

PUT
/owner/restaurants/{restaurantId}/takeaway/orders/{id}/collected

---

# ON-THE-WAY TAKEAWAY

Create:

OnTheWayOrderTracking

Fields:

takeawayOrder
customerLatitude
customerLongitude
restaurantLatitude
restaurantLongitude
estimatedArrival
distanceKm
lastLocationUpdatedAt

Flow:

Order
↓
Customer starts travel
↓
Customer shares location with permission
↓
Backend estimates arrival
↓
Restaurant prepares based on ETA
↓
Ready
↓
Arrival
↓
Collection

Never track location without explicit customer permission.

---

# PHASE 8

# DINITICKET

DiniTicket is queue management.

---

# DINITICKET ENTITY

Fields:

id
ticketNumber
restaurant
customer
guestCount
queuePosition
estimatedWaitMinutes
status
issuedAt
calledAt
arrivedAt
seatedAt
completedAt
expiresAt

Statuses:

WAITING

CALLED

ARRIVED

SEATED

COMPLETED

CANCELLED

EXPIRED

NO\_SHOW

---

# DINITICKET FLOW

Customer selects restaurant
↓
Requests ticket
↓
Backend generates queue token
↓
Position shown
↓
Waiting time shown
↓
Owner calls customer
↓
Customer arrives
↓
Owner seats customer
↓
Optional table link
↓
Dining starts

---

# CUSTOMER DINITICKET APIs

POST
/customer/dinitickets

GET
/customer/dinitickets/current

GET
/customer/dinitickets/{id}

PUT
/customer/dinitickets/{id}/cancel

---

# OWNER DINITICKET APIs

GET
/owner/restaurants/{restaurantId}/dinitickets

GET
/owner/restaurants/{restaurantId}/dinitickets/waiting

PUT
/owner/restaurants/{restaurantId}/dinitickets/{id}/call

PUT
/owner/restaurants/{restaurantId}/dinitickets/{id}/arrived

PUT
/owner/restaurants/{restaurantId}/dinitickets/{id}/seat

PUT
/owner/restaurants/{restaurantId}/dinitickets/{id}/no-show

PUT
/owner/restaurants/{restaurantId}/dinitickets/{id}/complete

---

# DINITICKET RULES

Customer should normally have only one active ticket per restaurant.

Respect maximum queue size.

Auto-expire when configured.

Queue position must be server-generated.

Do not trust customer-provided queue number.

---

# REAL-TIME QUEUE

Prepare WebSocket or SSE architecture.

Events:

TICKET\_CREATED

QUEUE\_POSITION\_CHANGED

TICKET\_CALLED

TICKET\_EXPIRED

TICKET\_SEATED

---

# PHASE 9

# CHECK-IN + DINING

---

# DINING SESSION

Create:

DiningSession

Fields:

id
reservation
diniTicket
customer
restaurant
table
status
checkInMethod
checkInLatitude
checkInLongitude
checkedInAt
diningStartedAt
paymentPendingAt
paidAt
checkedOutAt

Status:

CHECKED\_IN

DINING

PAYMENT\_PENDING

PAID

CHECKED\_OUT

CANCELLED

---

# CHECK-IN METHODS

GPS

QR

OWNER\_MANUAL

---

# GPS CHECK-IN

Customer sends location.

Backend compares:

customerLatitude

customerLongitude

restaurantLatitude

restaurantLongitude

Calculate distance.

Allow only within configured restaurant radius.

Do not accept frontend-computed distance as authoritative.

---

# QR CHECK-IN

Generate secure reservation check-in token.

QR should not expose raw reservation ID alone.

Use:

signed token

or

short-lived random secure token

---

# CHECK-IN APIs

POST
/customer/reservations/{reservationId}/check-in/gps

POST
/customer/reservations/{reservationId}/check-in/qr

Owner:

POST
/owner/restaurants/{restaurantId}/reservations/{reservationId}/manual-check-in

---

# DINING APIs

GET
/customer/dining-sessions/current

GET
/customer/dining-sessions/{id}

Owner:

GET
/owner/restaurants/{restaurantId}/dining-sessions

PUT
/owner/restaurants/{restaurantId}/dining-sessions/{id}/start

PUT
/owner/restaurants/{restaurantId}/dining-sessions/{id}/request-payment

PUT
/owner/restaurants/{restaurantId}/dining-sessions/{id}/checkout

---

# PHASE 10

# PAYMENTS

Use Razorpay-ready architecture.

Do not trust Flutter payment success.

---

# PAYMENT

Create:

Payment

Fields:

id
customer
restaurant
referenceType
referenceId
gateway
gatewayOrderId
gatewayPaymentId
gatewaySignature
amount
currency
status
paymentMethod
createdAt
verifiedAt

---

# PAYMENT REFERENCE TYPES

RESERVATION

FOOD\_ORDER

TAKEAWAY

GROUP\_BOOKING

CORPORATE\_BOOKING

DINITICKET

DINING\_BILL

---

# PAYMENT STATUS

CREATED

PENDING

SUCCESS

FAILED

REFUNDED

PARTIALLY\_REFUNDED

---

# PAYMENT FLOW

Frontend
↓
Backend create gateway order
↓
Razorpay order returned
↓
Frontend payment UI
↓
Gateway returns payment info
↓
Frontend sends IDs/signature
↓
Backend verifies
↓
Backend updates SUCCESS
↓
Related business transaction completed

---

# PAYMENT APIs

POST
/customer/payments/create-order

POST
/customer/payments/verify

GET
/customer/payments

GET
/customer/payments/{id}

---

# IDEMPOTENCY

Support:

Idempotency-Key

for sensitive operations.

Prevent duplicate:

payment order

booking

refund

order

---

# COMMISSION

Create:

CommissionRule

Fields:

serviceType
commissionType
commissionValue
effectiveFrom
effectiveUntil
active

CommissionType:

PERCENTAGE

FIXED

---

# SETTLEMENT

Create:

Settlement

Fields:

restaurant
periodStart
periodEnd
grossAmount
commissionAmount
gatewayFee
taxAmount
refundAmount
netAmount
status
settlementReference
processedAt

Status:

PENDING

PROCESSING

COMPLETED

FAILED

ON\_HOLD

---

# REFUND

Create:

Refund

Fields:

payment
amount
reason
status
gatewayRefundId
requestedBy
approvedBy
requestedAt
processedAt

Statuses:

REQUESTED

APPROVED

REJECTED

PROCESSING

REFUNDED

FAILED

---

# PHASE 11

# WALLET + REWARDS

---

# WALLET

Create one wallet per customer.

Fields:

customer
balance
rewardPoints

---

# WALLET TRANSACTION

Fields:

wallet
transactionType
referenceType
referenceId
amount
points
balanceBefore
balanceAfter
createdAt

Types:

CREDIT

DEBIT

REWARD\_EARNED

REWARD\_REDEEMED

REFUND

ADMIN\_ADJUSTMENT

---

# REWARD RULE

Create configurable:

RewardRule

Examples:

Spend reward

First booking

Review reward

Referral reward

Birthday reward

Fields:

ruleType
minimumSpend
points
conversionValue
active
effectiveFrom
effectiveUntil

---

# REWARD APIS

GET
/customer/rewards

GET
/customer/rewards/history

GET
/customer/rewards/how-to-earn

POST
/customer/rewards/calculate-redemption

POST
/customer/rewards/redeem

Backend calculates values.

Never trust frontend point calculation.

---

# ADMIN WALLET APIs

GET
/admin/wallets

GET
/admin/wallets/{customerId}

POST
/admin/wallets/{customerId}/adjust

Every manual adjustment requires:

reason

audit log

permission

---

# PHASE 12

# REVIEWS + SUPPORT + NOTIFICATIONS

---

# REVIEW

Create:

Review

Fields:

customer
restaurant
reservation
foodOrder
rating
foodRating
serviceRating
ambienceRating
comment
ownerReply
status
createdAt

Only completed eligible customer should review.

Rating range:

1–5

---

# REVIEW APIs

POST
/customer/reviews

GET
/customer/reviews

PUT
/customer/reviews/{id}

DELETE
/customer/reviews/{id}

GET
/restaurants/{restaurantId}/reviews

Owner:

POST
/owner/restaurants/{restaurantId}/reviews/{reviewId}/reply

Admin:

PUT
/admin/reviews/{reviewId}/hide

PUT
/admin/reviews/{reviewId}/restore

---

# SUPPORT

Create:

SupportTicket

Fields:

ticketNumber
createdByUser
creatorType
category
subject
description
priority
status
assignedAdmin
createdAt
resolvedAt

Creator types:

CUSTOMER

OWNER

---

# SUPPORT MESSAGE

Fields:

ticket
senderUser
message
attachmentUrl
createdAt

---

# SUPPORT STATUS

OPEN

IN\_PROGRESS

WAITING\_CUSTOMER

WAITING\_OWNER

RESOLVED

CLOSED

---

# CUSTOMER SUPPORT APIs

POST
/customer/support/tickets

GET
/customer/support/tickets

GET
/customer/support/tickets/{id}

POST
/customer/support/tickets/{id}/messages

PUT
/customer/support/tickets/{id}/close

---

# OWNER SUPPORT APIs

POST
/owner/support/tickets

GET
/owner/support/tickets

GET
/owner/support/tickets/{id}

POST
/owner/support/tickets/{id}/messages

---

# ADMIN SUPPORT APIs

GET
/admin/support/tickets

GET
/admin/support/tickets/{id}

PUT
/admin/support/tickets/{id}/assign

PUT
/admin/support/tickets/{id}/status

POST
/admin/support/tickets/{id}/messages

---

# NOTIFICATIONS

Create:

Notification

Fields:

user
title
message
notificationType
referenceType
referenceId
read
createdAt

Types include:

BOOKING\_CONFIRMED

BOOKING\_CANCELLED

RESTAURANT\_APPROVED

RESTAURANT\_REJECTED

FOOD\_ORDER\_READY

TAKEAWAY\_READY

DINITICKET\_CALLED

PAYMENT\_SUCCESS

REFUND\_COMPLETED

SETTLEMENT\_COMPLETED

REWARD\_POINTS\_EARNED

SUPPORT\_REPLY

---

# NOTIFICATION APIs

GET
/customer/notifications

PUT
/customer/notifications/{id}/read

PUT
/customer/notifications/read-all

Same equivalent for owner.

---

# PHASE 13

# ADMIN + REPORTS + ANALYTICS

---

# ADMIN DASHBOARD

GET
/admin/dashboard

Return:

totalCustomers

totalOwners

totalRestaurants

pendingRestaurants

approvedRestaurants

activeRestaurants

totalReservations

todayReservations

totalOrders

todayOrders

totalRevenue

platformCommission

pendingSettlements

refundRequests

activeDiniTickets

openSupportTickets

restaurantGrowth

customerGrowth

reservationTrend

revenueTrend

No mock values.

---

# ADMIN USERS

Admin should manage:

Customers

Owners

Restaurants

Bookings

Orders

Payments

Settlements

Refunds

Wallets

Reviews

Support

Offers

Coupons

Notifications

Admin users

Roles

Permissions

Settings

---

# ADMIN USER MANAGEMENT

GET
/admin/users

GET
/admin/users/{id}

PUT
/admin/users/{id}/block

PUT
/admin/users/{id}/unblock

PUT
/admin/users/{id}/activate

PUT
/admin/users/{id}/deactivate

Admin must never receive password hash.

---

# ADMIN ROLES

Support:

SUPER\_ADMIN

OPERATIONS\_ADMIN

FINANCE\_ADMIN

SUPPORT\_ADMIN

RESTAURANT\_VERIFICATION\_ADMIN

CONTENT\_ADMIN

Use fine-grained permissions.

---

# REPORTS

Admin reports:

Reservations

Revenue

Restaurants

Customers

Orders

Takeaway

DiniTicket

Payments

Settlements

Refunds

Wallet

Rewards

Reviews

Support

---

# REPORT FILTERS

fromDate

toDate

restaurantId

status

serviceType

city

---

# OWNER REPORTS

Daily Sales

Monthly Sales

Reservations

Table Occupancy

Menu Performance

Food Orders

Takeaway

DiniTicket

Customer Count

Ratings

---

# EXPORT PREPARATION

Prepare export services for:

CSV

Excel

Do not tightly couple export formatting to controllers.

---

# PHASE 14

# FRONTEND INTEGRATION

---

# CUSTOMER FLUTTER

Create API integration documentation for:

Auth

Restaurants

Menu

Availability

Reservations

Group Booking

Corporate Booking

Food Orders

Takeaway

DiniTicket

Dining

Payments

Wallet

Rewards

Reviews

Support

Notifications

---

# OWNER FLUTTER

Document APIs for:

Onboarding

Approval Status

Dashboard Context

Tables

Menu

Reservations

Food Orders

Takeaway

DiniTicket

Dining

Payments

Analytics

Notifications

Support

Settings

---

# ADMIN PANEL

Existing admin frontend may currently use mock data.

Do not remove all mock data first.

Use gradual migration:

1. Build backend endpoint
2. Test endpoint in Postman
3. Connect admin service
4. Disable mock data for that module
5. Test UI
6. Remove obsolete mock code

---

# API CLIENT STANDARD

Flutter should use centralized:

ApiClient

ApiEndpoints

AuthInterceptor

ApiException

SecureTokenStorage

RefreshTokenInterceptor

Use:

flutter\_secure\_storage

---

# ADMIN BASE URL

Example:

NEXT\_PUBLIC\_API\_BASE\_URL=[http://localhost:8080/api/v1](http://localhost:8080/api/v1)

NEXT\_PUBLIC\_USE\_DEMO\_DATA=false

---

# PHASE 15

# TESTING + SECURITY + PRODUCTION

---

# TESTING

Add:

Unit Tests

Repository Tests

Service Tests

Controller Tests

Integration Tests

Security Tests

Use:

JUnit 5

Mockito

MockMvc

Testcontainers PostgreSQL

---

# CRITICAL TEST CASES

Authentication:

duplicate registration

wrong password

expired token

refresh token reuse

unauthorized endpoint

---

Restaurant:

owner creates restaurant

owner cannot access another restaurant

incomplete onboarding cannot submit

admin approval

admin rejection

suspension

---

Tables:

duplicate table number

capacity validation

blocked table unavailable

---

Reservations:

double booking

overlapping reservation

invalid guest capacity

cancelled booking releases availability

unauthorized reservation access

---

Group:

join valid invite

invalid invite

member removal

group locking

---

Corporate:

budget validation

guest management

---

Food:

unavailable item

wrong restaurant menu item

invalid quantity

---

Takeaway:

takeaway disabled

invalid pickup time

---

DiniTicket:

maximum queue size

duplicate active ticket

ticket expiry

queue position

---

Dining:

GPS outside radius

invalid QR

duplicate check-in

---

Payment:

invalid Razorpay signature

duplicate verification

idempotency

refund amount greater than payment

---

Wallet:

insufficient reward points

duplicate reward credit

admin adjustment audit

---

Reviews:

review before completion rejected

duplicate eligible review handling

---

# SECURITY

Never expose:

password

passwordHash

refresh tokens

OTP

full bank details

gateway secrets

internal stack traces

---

# CORS

Development:

localhost admin URLs

Production:

only approved admin domains

Do not use wildcard production CORS.

---

# RATE LIMITING

Protect:

Login

OTP

Forgot Password

Payment creation

Coupon attempts

DiniTicket creation

Support spam

---

# AUDIT LOGS

Log:

restaurant approval

restaurant rejection

suspension

wallet adjustment

refund approval

settlement change

admin user creation

role changes

service enable/disable

Never log secrets.

---

# DATABASE

Use PostgreSQL.

Use Flyway only for schema changes.

Never modify already applied migrations.

New phase migrations should increment sequentially.

Example:

V4 restaurant onboarding

V5 restaurant permissions

V6 tables

V7 menu

V8 reservation

V9 group booking

V10 corporate

V11 orders

V12 takeaway

V13 diniticket

V14 dining

V15 payment

V16 wallet

V17 reviews

V18 support

etc.

Exact numbering must follow the existing project.

---

# TRANSACTIONS

Use @Transactional for:

restaurant submission

restaurant approval

reservation creation

group booking join

corporate booking

food order creation

takeaway creation

DiniTicket seat allocation

check-in

payment verification

wallet redemption

reward credit

refund

settlement

---

# CONCURRENCY

Protect against:

double booking

duplicate payment

duplicate wallet credit

duplicate queue ticket

duplicate refund

Use suitable:

unique constraints

database locks

optimistic locking

pessimistic locking where truly needed

idempotency keys

---

# RESPONSE STANDARD

Continue using existing ApiResponse.

Example:

{
"success": true,
"message": "Reservation created successfully",
"data": {},
"timestamp": "..."
}

Error:

{
"success": false,
"message": "Table not available",
"errorCode": "TABLE\_NOT\_AVAILABLE",
"errors": [],
"timestamp": "..."
}

---

# PAGINATION STANDARD

Use consistent pagination.

{
"content": [],
"page": 0,
"size": 20,
"totalElements": 0,
"totalPages": 0,
"first": true,
"last": true
}

---

# CODE QUALITY

Follow:

SOLID

Clean architecture principles

Constructor injection

Thin controllers

Service-layer business logic

Repository-layer persistence

DTOs instead of exposing entities

Enums instead of arbitrary strings

Reusable mappers

Central exception handling

---

# FILE UPLOAD

Create storage abstraction.

Development:

local filesystem if required

Production:

AWS S3

Store:

file key

file URL

metadata

Never store large files directly in PostgreSQL.

---

# SWAGGER

Every API should document:

Purpose

Role

Permission

Request

Response

Validation

Possible errors

---

# POSTMAN

Keep one master Postman collection.

Suggested folders:

01 Auth

02 Owner Onboarding

03 Admin Restaurants

04 Tables

05 Menu

06 Customer Restaurants

07 Reservations

08 Group Booking

09 Corporate Booking

10 Food Orders

11 Takeaway

12 DiniTicket

13 Dining

14 Payments

15 Wallet

16 Rewards

17 Reviews

18 Support

19 Notifications

20 Admin

21 Reports

Variables:

baseUrl

customerToken

ownerToken

adminToken

restaurantId

tableId

menuItemId

reservationId

groupBookingId

corporateBookingId

orderId

takeawayId

diniTicketId

paymentId

---

# REQUIRED DELIVERY METHOD

Do not provide isolated snippets only.

Modify the existing backend.

For each phase:

1. Create entities
2. Create enums
3. Create Flyway migration
4. Create repositories
5. Create request DTOs
6. Create response DTOs
7. Create mapper
8. Create service interface
9. Create service implementation
10. Create controller
11. Add security permissions
12. Add validation
13. Add exceptions
14. Add tests
15. Add Swagger docs
16. Add Postman requests
17. Update README
18. Update API documentation
19. Run tests
20. Run Maven package

---

# VERY IMPORTANT

Never say:

"Tests passed"

unless tests were actually executed successfully.

Before each phase delivery run:

mvn clean test

Then:

mvn clean package

If Docker/Testcontainers are available, run PostgreSQL integration tests.

If a command cannot be executed, clearly report:

NOT EXECUTED

with reason.

---

# FINAL PROJECT EXPECTATION

Final architecture:

BookEat Customer Flutter
│
│
▼
REST API / JWT
│
▼
Spring Boot Backend
│ │
│ ├── Razorpay
│ ├── Email
│ └── AWS S3
│
▼
PostgreSQL
▲
│
┌────┴───────────┐
│ │
Restaurant BookEat Admin
Owner Flutter Panel

ONE backend.

ONE PostgreSQL database.

THREE frontend applications.

The backend is the single source of truth.

---

# START NOW

Continue from the existing backend with:

PHASE 3 — OWNER ONBOARDING + ADMIN APPROVAL

Do not recreate authentication.

Do not recreate the project.

First inspect the existing package structure, entities, security, migrations and test setup.

Then implement Phase 3 completely.

After Phase 3 passes compilation and tests, continue sequentially:

Phase 4

Phase 5

Phase 6

Phase 7

Phase 8

Phase 9

Phase 10+

Do not skip phases.

Do not create duplicate entities or duplicate APIs.

Preserve backward compatibility wherever possible.

Final result must be production-ready and compatible with:

BookEat Customer Flutter App

Restaurant Owner Flutter App

BookEat Admin Panel.