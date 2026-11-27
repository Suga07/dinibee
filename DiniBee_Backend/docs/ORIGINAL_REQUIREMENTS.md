# BOOKEAT / DINIBEE – COMPLETE PRODUCTION BACKEND DEVELOPMENT PROMPT

You are a senior Java Spring Boot backend architect working on a production-level restaurant reservation and ordering platform called **BookEat / DiniBee**.

Three frontend applications are already created:

1. **BookEat Customer App**
   - Flutter
   - Android and iOS
   - Used by restaurant customers.
2. **Restaurant Owner App**
   - Flutter
   - Android and iOS
   - Used by restaurant owners/managers.
3. **BookEat Admin Panel**
   - React / Next.js / TypeScript
   - Used by platform administrators.

Your task is to create **ONE centralized production-ready Spring Boot backend and ONE PostgreSQL database** for all three applications.

The project must follow professional enterprise/MNC development standards.

---

# 1. TECHNOLOGY STACK

Use:

- Java 17 or Java 21 LTS
- Spring Boot stable release
- Spring Web
- Spring Data JPA
- Spring Security
- JWT Authentication
- PostgreSQL
- Hibernate
- Maven
- Lombok
- Bean Validation
- MapStruct if useful
- Flyway or Liquibase database migrations
- Swagger / OpenAPI
- Spring Mail
- No external phone authentication provider is included in this implementation
- WebSocket/SSE where real-time updates are useful
- Razorpay payment integration architecture
- AWS-ready deployment architecture
- Docker support
- JUnit 5
- Mockito

Do not use deprecated APIs.

Do not use snapshot dependencies in production.

---

# 2. PROJECT ARCHITECTURE

Use modular clean architecture.

Base package:

com.bookeat

Recommended structure:

com.bookeat
├── auth
├── user
├── customer
├── owner
├── admin
├── restaurant
├── onboarding
├── serviceactivation
├── reservation
│ ├── regular
│ ├── group
│ └── corporate
├── tablemanagement
├── diningsession
├── menu
├── category
├── foodorder
├── takeaway
├── ontheway
├── diniticket
├── payment
├── wallet
├── loyalty
├── settlement
├── refund
├── commission
├── coupon
├── offer
├── review
├── notification
├── support
├── report
├── analytics
├── location
├── common
├── config
├── security
└── exception

Each business module should normally contain:

controller
service
service.impl
repository
entity
dto
mapper
request
response
specification

Do not expose JPA entities directly from REST controllers.

Always use DTOs.

---

# 3. THREE CLIENT APPLICATIONS

All three applications must communicate with the same backend.

Base API:

/api/v1

Example production URL:

[https://api.bookeat.com/api/v1](https://api.bookeat.com/api/v1)

Customer Flutter:

/api/v1/customer/\*\*

Restaurant Owner Flutter:

/api/v1/owner/\*\*

Admin Panel:

/api/v1/admin/\*\*

Shared APIs can use:

/api/v1/auth/\*\*
/api/v1/restaurants/\*\*
/api/v1/public/\*\*

---

# 4. USER ROLES

Implement Role Based Access Control.

Roles:

CUSTOMER
OWNER
RESTAURANT\_MANAGER
RESTAURANT\_STAFF
ADMIN
SUPER\_ADMIN

Create:

User
Role
Permission

Support multiple roles when necessary.

Example:

User

- id
- firstName
- lastName
- email
- phoneNumber
- passwordHash
- status
- emailVerified
- createdAt
- updatedAt

Use BCrypt or Argon2 password hashing.

Never save plain-text passwords.

---

# 5. AUTHENTICATION

Create complete authentication.

Required APIs:

POST /auth/register/customer

POST /auth/register/owner

POST /auth/login

POST /auth/refresh-token

POST /auth/logout

POST /auth/forgot-password

POST /auth/reset-password

POST /auth/change-password

POST /auth/send-email-otp

POST /auth/verify-email-otp



GET /auth/me

Use:

Access Token
Refresh Token

Access token should be short lived.

Refresh tokens should be stored securely and be revocable.

JWT should contain:

userId
email
roles
permissions

Do not include passwords or sensitive data.

---

# 6. RESTAURANT OWNER ONBOARDING

Restaurant owner app already includes onboarding.

Create a complete onboarding backend.

Workflow:

Owner Register
↓
Create Restaurant Application
↓
Restaurant Details
↓
Location
↓
Business Information
↓
Operating Hours
↓
Service Selection
↓
Table Setup
↓
Reservation Settings
↓
Takeaway Settings
↓
Menu Setup
↓
Payment Details
↓
Policies
↓
Team Access
↓
Review & Submit
↓
Admin Verification
↓
Approve / Reject
↓
Restaurant Activated

Restaurant services:

RESTAURANT
TAKEAWAY
DINITICKET

Owner may select:

only RESTAURANT

or

RESTAURANT + TAKEAWAY

or

RESTAURANT + TAKEAWAY + DINITICKET

etc.

Create entity:

RestaurantService

Fields:

id
restaurantId
serviceType
enabled
activatedAt
disabledAt

Enum:

RESTAURANT
TAKEAWAY
DINITICKET

---

# 7. RESTAURANT ENTITY

Restaurant fields should include approximately:

id
ownerId
restaurantName
description
restaurantType
cuisineTypes
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
openingTime
closingTime
logoUrl
coverImageUrl
averageRating
totalReviews
approvalStatus
restaurantStatus
createdAt
updatedAt

Approval statuses:

DRAFT
SUBMITTED
UNDER\_REVIEW
APPROVED
REJECTED
SUSPENDED

---

# 8. ADMIN RESTAURANT APPROVAL

Admin should be able to:

View pending restaurants
View onboarding documents
Verify business information
Approve restaurant
Reject restaurant
Request changes
Suspend restaurant
Reactivate restaurant
Enable or disable restaurant services

APIs:

GET /admin/restaurants

GET /admin/restaurants/pending

GET /admin/restaurants/{id}

PUT /admin/restaurants/{id}/approve

PUT /admin/restaurants/{id}/reject

PUT /admin/restaurants/{id}/suspend

PUT /admin/restaurants/{id}/activate

PATCH /admin/restaurants/{id}/services

Store:

approvedBy
approvedAt
rejectionReason
suspensionReason

---

# 9. OWNER DASHBOARD

GET /owner/dashboard

Return:

todayReservations
upcomingReservations
todayRevenue
monthlyRevenue
pendingOrders
activeTables
occupiedTables
availableTables
activeDiniTickets
pendingTakeawayOrders
customerCount
averageRating

Do not hardcode dashboard values.

Calculate from database.

---

# 10. RESTAURANT TABLE MANAGEMENT

Restaurant owner creates tables.

Table fields:

id
restaurantId
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

Table status:

AVAILABLE
RESERVED
OCCUPIED
BLOCKED
MAINTENANCE

Owner APIs:

POST /owner/tables

GET /owner/tables

GET /owner/tables/{id}

PUT /owner/tables/{id}

DELETE /owner/tables/{id}

PATCH /owner/tables/{id}/status

Customer must only see bookable tables.

---

# 11. RESTAURANT AVAILABILITY

Create an availability engine.

Input:

restaurantId
date
time
guestCount

System should calculate:

restaurant opening hours
reservation duration
existing reservations
table capacity
blocked tables
restaurant holidays
special schedules

API:

GET /restaurants/{restaurantId}/availability

Parameters:

date
time
guests

Return:

availableTimeSlots
availableTables
estimatedDuration

Prevent double booking.

Use database locking or transaction strategy where needed.

---

# 12. REGULAR RESERVATION

Customer booking flow:

Restaurant
↓
Date
↓
Time
↓
Guest Count
↓
Special Request
↓
Choose Table
↓
Optional Food Pre-order
↓
Booking Summary
↓
Payment if required
↓
Booking Confirmation

Reservation fields:

id
bookingReference
customerId
restaurantId
tableId
reservationDate
startTime
endTime
guestCount
specialRequest
reservationStatus
paymentStatus
bookingType
createdAt
updatedAt

BookingType:

REGULAR
GROUP
CORPORATE

ReservationStatus:

PENDING
CONFIRMED
ARRIVED
CHECKED\_IN
DINING
COMPLETED
CANCELLED
NO\_SHOW
REJECTED

APIs:

POST /customer/reservations

GET /customer/reservations

GET /customer/reservations/{id}

PUT /customer/reservations/{id}

DELETE /customer/reservations/{id}

PUT /customer/reservations/{id}/cancel

Owner:

GET /owner/reservations

GET /owner/reservations/{id}

PUT /owner/reservations/{id}/confirm

PUT /owner/reservations/{id}/reject

---

# 13. GROUP BOOKING

Implement complete Group Booking.

Flow:

Customer creates group booking
↓
Invite link / QR generated
↓
Friends/family join group
↓
Each participant can select/order foods
↓
Orders attached to group
↓
Combined group bill created
↓
Split payment or host payment
↓
Booking completed

Entities:

GroupBooking
GroupMember
GroupInvitation
GroupOrder

Fields:

groupId
hostCustomerId
restaurantId
reservationId
inviteCode
inviteUrl
qrCode
memberLimit
status

Support:

JOINED
INVITED
LEFT
REMOVED

APIs:

POST /customer/group-bookings

POST /customer/group-bookings/{id}/invite

POST /customer/group-bookings/join/{inviteCode}

GET /customer/group-bookings/{id}

GET /customer/group-bookings/{id}/members

DELETE /customer/group-bookings/{id}/members/{memberId}

---

# 14. CORPORATE BOOKING

Create corporate booking support.

Features:

Company booking
Employee/guest list
Individual food orders
Corporate budget
Combined order
Company billing
Tax invoice
Recurring reservation

Entities:

CorporateAccount
CorporateBooking
CorporateGuest
CorporateBudget
CorporateInvoice

Fields:

companyName
gstNumber
billingAddress
contactPerson
budgetLimit
invoiceStatus
recurrenceRule

Support:

ONE\_TIME
DAILY
WEEKLY
MONTHLY

---

# 15. MENU MANAGEMENT

Restaurant Owner controls menu.

Entities:

MenuCategory
MenuItem
MenuVariant
MenuAddon

Category:

Starters
Main Course
Desserts
Drinks
etc.

MenuItem fields:

id
restaurantId
categoryId
name
description
price
discountPrice
foodType
spiceLevel
imageUrl
available
preOrderAvailable
takeawayAvailable
preparationTime

FoodType:

VEG
NON\_VEG
VEGAN
EGG

Owner CRUD APIs.

Customer only gets active menu items.

---

# 16. FOOD PRE-ORDER

Customer may pre-order foods during table reservation.

Entities:

FoodOrder
FoodOrderItem

Order should optionally connect to:

reservationId
groupBookingId
corporateBookingId
takeawayId
diniTicketId

Order statuses:

PLACED
ACCEPTED
PREPARING
READY
SERVED
COMPLETED
CANCELLED

---

# 17. REGULAR TAKEAWAY

Customer selects:

Restaurant
↓
Food
↓
Cart
↓
Pickup Time
↓
Payment
↓
Order Confirmation
↓
Restaurant Preparation
↓
Ready for Pickup
↓
Collected

Entity:

TakeawayOrder

Fields:

id
orderNumber
customerId
restaurantId
pickupTime
subtotal
tax
discount
total
paymentStatus
orderStatus

---

# 18. ON-THE-WAY TAKEAWAY

Create special On-the-Way Takeaway functionality.

Customer:

chooses restaurant
orders food
shares current GPS / route
system estimates arrival
restaurant prepares food accordingly

Fields:

currentLatitude
currentLongitude
destinationLatitude
destinationLongitude
estimatedArrival
customerDistance
preparationStartTime

Statuses:

ORDER\_PLACED
CUSTOMER\_ON\_THE\_WAY
PREPARING
READY
CUSTOMER\_ARRIVED
COLLECTED

Location tracking should respect customer permission.

Do not collect GPS in background without permission.

---

# 19. DINITICKET

DiniTicket is restaurant queue management.

Flow:

Customer selects restaurant
↓
Requests token
↓
System creates queue ticket
↓
Shows token number
↓
Shows people ahead
↓
Estimated waiting time
↓
Owner calls next token
↓
Customer arrives
↓
Optional table allocation
↓
Dining begins

Entity:

DiniTicket

Fields:

id
ticketNumber
restaurantId
customerId
guestCount
queuePosition
estimatedWaitMinutes
status
issuedAt
calledAt
checkedInAt

Status:

WAITING
CALLED
ARRIVED
SEATED
COMPLETED
CANCELLED
EXPIRED
NO\_SHOW

Owner APIs:

GET /owner/dinitickets

PUT /owner/dinitickets/{id}/call

PUT /owner/dinitickets/{id}/seat

PUT /owner/dinitickets/{id}/complete

Customer:

POST /customer/dinitickets

GET /customer/dinitickets/current

DELETE /customer/dinitickets/{id}

Use WebSocket or SSE for real-time queue changes if appropriate.

---

# 20. CUSTOMER CHECK-IN

After arriving at the restaurant, customer should check in.

Support:

GPS check-in
QR check-in

GPS verification:

restaurant latitude
restaurant longitude
customer latitude
customer longitude

Calculate distance.

Allow check-in only within configured radius.

Example:

50–200 meters.

Entity:

DiningSession

Fields:

id
reservationId
customerId
restaurantId
tableId
checkedInAt
checkedOutAt
checkInMethod
checkInLatitude
checkInLongitude
status

CheckInMethod:

GPS
QR
OWNER\_MANUAL

---

# 21. CUSTOMER CHECKOUT

Customer completes dining.

Flow:

Food Orders
↓
Final Bill
↓
Discount
↓
Reward Points
↓
Coupon
↓
Taxes
↓
Payment
↓
Payment Success
↓
Checkout
↓
Reward Points Credited
↓
Review Request

DiningSession status:

CHECKED\_IN
DINING
PAYMENT\_PENDING
PAID
CHECKED\_OUT

---

# 22. PAYMENT

Design Razorpay-ready payment architecture.

Do not directly trust payment success from Flutter.

Flow:

Frontend requests payment order
↓
Backend creates Razorpay order
↓
Frontend completes Razorpay payment
↓
Frontend sends payment ID/signature
↓
Backend verifies signature
↓
Backend marks payment SUCCESS

Entities:

Payment
PaymentTransaction

Fields:

paymentId
customerId
restaurantId
referenceType
referenceId
gateway
gatewayOrderId
gatewayPaymentId
amount
currency
status
paymentMethod
createdAt
verifiedAt

Status:

CREATED
PENDING
SUCCESS
FAILED
REFUNDED
PARTIALLY\_REFUNDED

ReferenceType:

RESERVATION
FOOD\_ORDER
TAKEAWAY
GROUP\_BOOKING
CORPORATE\_BOOKING
DINITICKET
DINING\_BILL

---

# 23. WHO RECEIVES PAYMENT

Design payment settlement correctly.

Recommended model:

Customer
↓
BookEat Payment Gateway Account
↓
Platform records transaction
↓
BookEat commission deducted
↓
Restaurant payable calculated
↓
Settlement generated
↓
Restaurant receives settlement

Fields:

grossAmount
platformCommission
paymentGatewayFee
taxAmount
restaurantNetAmount

Create:

CommissionRule
Settlement
SettlementTransaction

SettlementStatus:

PENDING
PROCESSING
COMPLETED
FAILED
ON\_HOLD

---

# 24. REFUNDS

Create refund module.

Reasons:

CUSTOMER\_CANCELLED
RESTAURANT\_CANCELLED
PAYMENT\_ERROR
DUPLICATE\_PAYMENT
SERVICE\_FAILURE
ADMIN\_APPROVED

Statuses:

REQUESTED
APPROVED
REJECTED
PROCESSING
REFUNDED
FAILED

---

# 25. DINIBEE WALLET

Customer has DiniBee wallet.

Entity:

Wallet

Fields:

id
customerId
balance
rewardPoints
createdAt
updatedAt

Create WalletTransaction:

id
walletId
transactionType
referenceType
referenceId
points
amount
balanceBefore
balanceAfter

Transaction types:

CREDIT
DEBIT
REWARD\_EARNED
REWARD\_REDEEMED
REFUND
ADMIN\_ADJUSTMENT

---

# 26. LOYALTY / REWARD POINTS

Customer should earn points after eligible completed transactions.

Create configurable reward rules.

Examples:

₹100 spent = 10 points

Review submitted = 5 points

First booking = bonus points

Referral = bonus points

Birthday reward

Admin should configure these values.

Create:

RewardRule
RewardTransaction

Customers should have:

GET /customer/rewards

GET /customer/rewards/history

GET /customer/rewards/how-to-earn

At billing time customer can choose:

Use reward points

Backend must calculate maximum redeemable points.

Never trust point calculation from frontend.

---

# 27. OFFERS

Admin or restaurant owner may create offers depending on permissions.

Offer fields:

title
description
offerType
discountType
discountValue
minimumAmount
maximumDiscount
startDate
endDate
restaurantId
usageLimit
customerUsageLimit
active

DiscountType:

PERCENTAGE
FIXED

---

# 28. COUPONS

Create:

Coupon

Fields:

code
description
discountType
discountValue
minimumOrderValue
maximumDiscount
validFrom
validUntil
usageLimit
perUserLimit
restaurantId
active

Validate coupons server-side.

---

# 29. REVIEWS & RATINGS

After completed booking/dining/takeaway:

Customer can review restaurant.

Fields:

customerId
restaurantId
reservationId
rating
foodRating
serviceRating
ambienceRating
comment
images
ownerReply
status

Rating:

1–5

Only customers with completed eligible transactions should review.

Owner may reply.

Admin may moderate inappropriate reviews.

---

# 30. CUSTOMER SUPPORT

Customer app support workflow.

Customer can:

Create support ticket
Chat
Attach image
Select category
View status
Reply
Close ticket

Entity:

SupportTicket

ticketNumber
createdBy
creatorType
category
subject
description
priority
status
assignedAdminId
createdAt
resolvedAt

Statuses:

OPEN
IN\_PROGRESS
WAITING\_CUSTOMER
RESOLVED
CLOSED

---

# 31. OWNER SUPPORT

Restaurant owner should also have support.

Owner sends request to admin.

Examples:

Payment problem
Settlement issue
Technical issue
Reservation issue
DiniTicket issue
Restaurant profile issue

---

# 32. NOTIFICATIONS

Support:

In-app notifications
Push notifications
Email notifications

Entity:

Notification

Fields:

userId
title
message
type
referenceType
referenceId
read
createdAt

Examples:

Booking confirmed
Booking cancelled
Restaurant approved
Restaurant rejected
Food ready
DiniTicket called
Settlement completed
Reward points earned

---

# 33. ADMIN DASHBOARD

Create aggregate admin dashboard API.

GET /admin/dashboard

Return:

totalCustomers
totalRestaurants
pendingRestaurants
approvedRestaurants
totalReservations
todayReservations
totalOrders
totalRevenue
platformCommission
pendingSettlements
refundRequests
activeDiniTickets
supportTickets
monthlyRevenueChart
reservationChart
restaurantGrowthChart
customerGrowthChart

Calculate from DB.

No hardcoded values.

---

# 34. ADMIN USER MANAGEMENT

Admin can:

View customers
View owners
View staff
Block user
Unblock user
Activate
Deactivate

Admin cannot view user password.

---

# 35. ADMIN ROLES & PERMISSIONS

Super Admin should create admin accounts.

Examples:

SUPER\_ADMIN

FINANCE\_ADMIN

SUPPORT\_ADMIN

RESTAURANT\_VERIFICATION\_ADMIN

OPERATIONS\_ADMIN

CONTENT\_ADMIN

Implement permissions.

Examples:

RESTAURANT\_APPROVE
USER\_BLOCK
PAYMENT\_VIEW
REFUND\_APPROVE
SETTLEMENT\_MANAGE
COUPON\_MANAGE
ADMIN\_MANAGE

---

# 36. REPORTS

Create report endpoints.

Admin reports:

Reservation report
Revenue report
Restaurant report
Customer report
Order report
Settlement report
Refund report
Wallet report
DiniTicket report

Owner reports:

Daily sales
Monthly sales
Reservations
Table occupancy
Food sales
Takeaway orders
DiniTicket performance
Customer count

Support filters:

fromDate
toDate
restaurantId
status

Support CSV/Excel export architecture if required later.

---

# 37. SEARCH

Customer restaurant search should support:

restaurant name
cuisine
location
rating
distance
price
offers
availability

Implement pagination.

Example:

GET /restaurants?page=0&size=20&city=Chennai&cuisine=Indian

Do not return all restaurants in one response.

---

# 38. LOCATION

Restaurant search can use:

latitude
longitude
radius

Use Haversine/PostGIS if necessary.

Return:

distanceKm

Avoid exposing sensitive user location permanently.

---

# 39. DATABASE

Use PostgreSQL.

Design normalized tables.

Required tables should include approximately:

users
roles
permissions
user\_roles
role\_permissions

customers

restaurant\_owners
restaurants
restaurant\_services
restaurant\_documents
restaurant\_operating\_hours

restaurant\_tables

reservations

group\_bookings
group\_members

corporate\_accounts
corporate\_bookings
corporate\_guests

menu\_categories
menu\_items
menu\_variants
menu\_addons

food\_orders
food\_order\_items

takeaway\_orders

dini\_tickets

dining\_sessions

payments
payment\_transactions

wallets
wallet\_transactions

reward\_rules
reward\_transactions

offers
coupons

commission\_rules
settlements
settlement\_transactions

refunds

reviews

notifications

support\_tickets
support\_messages

refresh\_tokens

audit\_logs

Do not store everything in one large table.

---

# 40. DATABASE RELATIONSHIPS

Clearly implement relationships.

Examples:

User 1 → 1 Customer

User 1 → 1 RestaurantOwner

RestaurantOwner 1 → N Restaurants

Restaurant 1 → N Tables

Restaurant 1 → N MenuItems

Restaurant 1 → N Reservations

Customer 1 → N Reservations

Reservation N → 1 Table

Reservation 1 → N FoodOrders

FoodOrder 1 → N FoodOrderItems

Customer 1 → 1 Wallet

Wallet 1 → N WalletTransactions

Restaurant 1 → N Settlements

---

# 41. BASE ENTITY

Create common auditable base entity.

Fields:

id
createdAt
updatedAt

Optional:

createdBy
updatedBy
version

Use optimistic locking with:

@Version

where useful.

---

# 42. API RESPONSE FORMAT

Use consistent responses.

Success:

{
"success": true,
"message": "Reservation created successfully",
"data": {},
"timestamp": "..."
}

Error:

{
"success": false,
"message": "Table is already reserved",
"errorCode": "TABLE\_NOT\_AVAILABLE",
"errors": [],
"timestamp": "..."
}

Create:

ApiResponse

---

# 43. PAGINATION

Paginated response:

{
"content": [],
"page": 0,
"size": 20,
"totalElements": 100,
"totalPages": 5,
"first": true,
"last": false
}

---

# 44. EXCEPTION HANDLING

Use:

@RestControllerAdvice

Create custom exceptions such as:

ResourceNotFoundException
BadRequestException
UnauthorizedException
ForbiddenException
ConflictException
TableNotAvailableException
InvalidCouponException
InsufficientWalletBalanceException
PaymentVerificationException

Do not expose stack traces to clients.

---

# 45. VALIDATION

Use Jakarta Validation.

Examples:

@NotBlank

@NotNull

@Email

@Size

@Min

@Max

@Positive

Validate all request DTOs.

Example:

@Valid @RequestBody CreateReservationRequest request

---

# 46. TRANSACTIONS

Use @Transactional for operations such as:

Reservation creation
Payment verification
Wallet updates
Settlement
Refund
Reward redemption
Group booking join
DiniTicket seat allocation

Prevent partial database writes.

---

# 47. SECURITY

Configure Spring Security.

Public endpoints:

/api/v1/auth/\*\*
/api/v1/public/\*\*
/swagger-ui/\*\*
/v3/api-docs/\*\*

Protected endpoints:

/customer/\*\* → CUSTOMER

/owner/\*\* → OWNER / RESTAURANT\_MANAGER

/admin/\*\* → ADMIN / SUPER\_ADMIN

Use:

@PreAuthorize

where needed.

Configure CORS safely.

Do not use "\*” origins in production.

---

# 48. AUDIT LOG

Critical admin operations must create audit logs.

Examples:

Restaurant approved
Restaurant rejected
User blocked
Refund approved
Wallet adjustment
Settlement changed
Admin created

AuditLog:

id
adminId
action
entityType
entityId
oldValue
newValue
ipAddress
createdAt

---

# 49. FILE UPLOAD

Need uploads for:

restaurant logo
restaurant cover image
business documents
menu image
review image
support attachment

Create file-storage abstraction.

Development:

local storage

Production:

AWS S3

Do not save large binary files directly inside PostgreSQL.

Store file URL/key.

---

# 50. SWAGGER

Add OpenAPI documentation.

Swagger:

/swagger-ui.html

or

/swagger-ui/index.html

Every controller should contain:

operation description
request schema
response schema
status codes

---

# 51. POSTMAN

Create Postman-ready API structure.

Collection folders:

01 Auth
02 Customer
03 Restaurants
04 Reservations
05 Group Booking
06 Corporate Booking
07 Menu
08 Orders
09 Takeaway
10 DiniTicket
11 Wallet
12 Payments
13 Reviews
14 Support
15 Owner
16 Admin
17 Reports

Use variables:

{{baseUrl}}

{{customerToken}}

{{ownerToken}}

{{adminToken}}

{{restaurantId}}

{{reservationId}}

---

# 52. API VERSIONING

All APIs should use:

/api/v1/

Avoid unversioned production endpoints.

---

# 53. DATABASE MIGRATION

Use Flyway.

Create migrations such as:

V1\_\_create\_auth\_tables.sql

V2\_\_create\_restaurant\_tables.sql

V3\_\_create\_reservation\_tables.sql

V4\_\_create\_menu\_tables.sql

V5\_\_create\_payment\_tables.sql

V6\_\_create\_wallet\_tables.sql

etc.

Do not use:

spring.jpa.hibernate.ddl-auto=create

for production.

Development may use:

validate

after Flyway migrations.

---

# 54. ENVIRONMENT CONFIGURATION

Create:

application.properties

application-dev.properties

application-prod.properties

Secrets must come from environment variables.

Example:

DB\_URL
DB\_USERNAME
DB\_PASSWORD
JWT\_SECRET
RAZORPAY\_KEY\_ID
RAZORPAY\_KEY\_SECRET
AWS\_ACCESS\_KEY
AWS\_SECRET\_KEY
MAIL\_USERNAME
MAIL\_PASSWORD

Never commit secrets to GitHub.

---

# 55. DEVELOPMENT DATABASE CONFIG

Example:

spring.datasource.url=jdbc\:postgresql://localhost:5432/bookeat

spring.datasource.username=postgres

spring.datasource.password=${DB\_PASSWORD}

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

spring.jpa.hibernate.ddl-auto=validate

spring.jpa.show-sql=true

spring.flyway.enabled=true

---

# 56. LOGGING

Use proper logging.

Use SLF4J.

Do not use:

System.out.println()

Log:

request failures
payment failures
authentication failures
important service operations

Never log:

password
OTP
JWT tokens
card details

---

# 57. TESTING

Create:

Unit tests
Service tests
Repository tests
Controller tests
Integration tests

Important cases:

duplicate registration
invalid login
expired JWT
restaurant approval
double booking attempt
wrong table capacity
invalid coupon
insufficient wallet points
duplicate payment verification
refund
DiniTicket queue
unauthorized admin endpoint

Use:

JUnit 5
Mockito
MockMvc
Testcontainers PostgreSQL if possible

---

# 58. DOUBLE BOOKING PROTECTION

This requirement is critical.

Two customers must not reserve the same table/time simultaneously.

Use an appropriate combination of:

database constraints
transactions
locking
availability validation

Do not solve this only in frontend.

---

# 59. IDEMPOTENCY

Payment and booking-sensitive APIs should support idempotency where appropriate.

Example header:

Idempotency-Key

Prevent accidental duplicate:

payments
reservations
refunds
orders

---

# 60. SOFT DELETE

Use soft-delete for business records when deletion should preserve history.

Possible fields:

deleted
deletedAt

Do not permanently delete completed reservations/payments.

---

# 61. STATUS HISTORY

Important workflows should retain history.

Create status history when useful.

Examples:

ReservationStatusHistory
OrderStatusHistory
RestaurantApprovalHistory
PaymentStatusHistory

Store:

oldStatus
newStatus
changedBy
changedAt
reason

---

# 62. DATE AND TIME

Use:

Instant
OffsetDateTime
LocalDate
LocalTime

Store timestamps consistently.

Prefer UTC for global timestamps.

Restaurant schedules may use restaurant-local timezone.

---

# 63. ADMIN PANEL MOCK DATA REPLACEMENT

The Admin frontend currently uses mock/demo data.

Backend integration should be gradual.

Do NOT delete mock data first.

Process:

Step 1:
Create backend endpoint.

Step 2:
Test using Postman.

Step 3:
Connect frontend service.

Step 4:
Set:

NEXT\_PUBLIC\_USE\_DEMO\_DATA=false

Step 5:
Confirm real API works.

Step 6:
Remove unused mock code only after all pages are migrated.

---

# 64. FLUTTER BACKEND CONNECTION

Both Flutter applications should use a centralized API client.

Recommended Flutter architecture:

lib/
├── core
│ ├── network
│ │ ├── api\_client.dart
│ │ ├── api\_endpoints.dart
│ │ ├── api\_exception.dart
│ │ └── auth\_interceptor.dart
│ └── storage
│ └── secure\_storage.dart

Store JWT securely.

Use:

flutter\_secure\_storage

Do not save JWT in plain SharedPreferences when secure storage is available.

Automatically attach:

Authorization: Bearer TOKEN

Handle HTTP:

200
201
204
400
401
403
404
409
422
500

If access token expires, use refresh token.

---

# 65. IOS AND ANDROID

The same Spring Boot backend must work for:

Android Flutter app

iOS Flutter app

No separate backend is required.

Only API base URL/environment configuration may differ.

---

# 66. CORS

Development example:

[http://localhost:3000](http://localhost:3000/)
[http://localhost:5173](http://localhost:5173/)

Production:

[https://admin.bookeat.com](https://admin.bookeat.com/)

Only approved origins should be configured.

Mobile Flutter APIs do not depend on browser CORS in the same way.

---

# 67. DOCKER

Create:

Dockerfile

docker-compose.yml

docker-compose should support:

Spring Boot backend
PostgreSQL

Optional later:

Redis

Example architecture:

Backend
↓
PostgreSQL

Redis can later support:

OTP
rate limiting
cache
temporary queue data

---

# 68. RATE LIMITING

Protect:

Login
OTP
Forgot Password
Coupon attempts
DiniTicket creation

Implement rate limiting if possible.

---

# 69. CACHE

Use cache only where useful.

Possible candidates:

restaurant search
restaurant details
menu
configuration
reward rules

Do not cache rapidly changing reservation availability without careful invalidation.

---

# 70. API DOCUMENTATION

For every API provide:

Method

Endpoint

Role

Request

Response

Possible errors

Example:

POST /api/v1/customer/reservations

Role:

CUSTOMER

Request:

{
"restaurantId": 12,
"tableId": 42,
"date": "2026-09-20",
"time": "19:30",
"guestCount": 4,
"specialRequest": "Window seat"
}

Response:

{
"success": true,
"message": "Reservation created successfully",
"data": {
"reservationId": 156,
"bookingReference": "BK-20260920-00156",
"status": "CONFIRMED"
}
}

---

# 71. IMPLEMENTATION ORDER

Build backend in this exact general order.

PHASE 1 – FOUNDATION

1. Spring Boot project creation
2. PostgreSQL
3. BaseEntity
4. Common API response
5. Exception handling
6. Flyway
7. User
8. Role
9. Permission
10. JWT security

PHASE 2 – AUTH

11. Customer registration
12. Owner registration
13. Login
14. Refresh token
15. Logout
16. Forgot password
17. OTP

PHASE 3 – RESTAURANT ONBOARDING

18. Restaurant owner profile
19. Restaurant
20. Restaurant services
21. Address
22. Business documents
23. Operating hours
24. Onboarding submission
25. Admin approval

PHASE 4 – RESTAURANT MANAGEMENT

26. Owner dashboard
27. Tables
28. Menu categories
29. Menu items
30. Availability

PHASE 5 – CUSTOMER

31. Restaurant listing
32. Restaurant search
33. Restaurant details
34. Favorite restaurant if required

PHASE 6 – RESERVATION

35. Regular reservation
36. Reservation availability
37. Reservation management
38. Customer cancellation
39. Owner confirmation
40. Group booking
41. Corporate booking

PHASE 7 – FOOD

42. Food pre-order
43. Food orders
44. Takeaway
45. On-the-Way Takeaway

PHASE 8 – DINITICKET

46. Queue ticket
47. Queue position
48. Owner call token
49. Customer arrival
50. Table seating

PHASE 9 – DINING

51. GPS/QR check-in
52. Dining session
53. Final bill
54. Checkout

PHASE 10 – PAYMENTS

55. Razorpay order
56. Payment verification
57. Payment transaction
58. Refund
59. Settlement
60. Commission

PHASE 11 – WALLET

61. Wallet
62. Reward points
63. Point redemption
64. Reward rules
65. Wallet history

PHASE 12 – ENGAGEMENT

66. Offers
67. Coupons
68. Reviews
69. Notifications
70. Customer support
71. Owner support

PHASE 13 – ADMIN

72. Dashboard
73. Customers
74. Owners
75. Restaurants
76. Reservations
77. Orders
78. Payments
79. Settlement
80. Refunds
81. Wallet
82. Reviews
83. Support
84. Admin users
85. Roles/permissions
86. Reports

PHASE 14 – FINALIZATION

87. Swagger
88. Postman
89. Automated testing
90. Docker
91. Production config
92. AWS deployment
93. Flutter integration
94. Admin frontend integration
95. End-to-end testing

---

# 72. REQUIRED OUTPUT WHILE DEVELOPING

Do not generate the entire backend as one massive unstructured answer.

Develop module by module.

For every module provide:

1. Purpose
2. Database/entity design
3. Enum
4. Request DTO
5. Response DTO
6. Repository
7. Mapper
8. Service interface
9. Service implementation
10. Controller
11. Validation
12. Exception handling
13. Security permissions
14. API endpoints
15. Postman request
16. Sample response
17. Required database migration
18. Tests
19. Frontend integration notes

Do not skip files.

Always provide complete compilable code.

If one file depends on another file, create the dependency first.

---

# 73. CODE QUALITY RULES

Follow SOLID principles.

Keep controllers thin.

Business logic belongs in services.

Database access belongs in repositories.

Use constructor injection.

Do not use field injection.

Preferred:

@RequiredArgsConstructor

private final RestaurantService restaurantService;

Avoid:

@Autowired
private RestaurantService restaurantService;

Use meaningful variable names.

Avoid duplicated code.

Use enums instead of arbitrary status strings.

---

# 74. IMPORTANT BUSINESS RULES

Enforce all important rules in backend.

Examples:

Customer cannot book suspended restaurant.

Customer cannot book unavailable table.

Customer cannot book table smaller than minimum capacity rules.

Customer cannot redeem more reward points than wallet balance.

Customer cannot review restaurant before completing an eligible booking/order.

Owner cannot edit another restaurant.

Owner cannot access another restaurant's payment information.

Admin privileges depend on permissions.

Suspended restaurant should disappear from normal customer search.

Cancelled reservations should release table availability.

Payment success must be verified by backend.

Completed payment records cannot be simply deleted.

---

# 75. RESTAURANT OWNER DATA ISOLATION

Every owner-facing request must verify ownership.

Example:

Owner A owns Restaurant 10.

Owner A must NOT access:

/owner/restaurants/20/reservations

if Restaurant 20 belongs to Owner B.

Never trust restaurantId provided by frontend alone.

Validate ownership from authenticated user.

---

# 76. CUSTOMER DATA ISOLATION

Customer A cannot fetch:

Customer B reservations
Customer B wallet
Customer B group data
Customer B payments

Determine customer identity using authenticated JWT.

Do not accept arbitrary customerId for normal self-service requests.

---

# 77. ADMIN AUTHORIZATION

Admin endpoint should use permissions.

Example:

@PreAuthorize("hasAuthority('RESTAURANT\_APPROVE')")

Do not give every admin SUPER\_ADMIN rights.

---

# 78. PAYMENT SECURITY

Never store:

CVV
Full card number
UPI PIN

Payment gateway handles sensitive payment credentials.

Backend stores gateway references only.

---

# 79. ERROR CODES

Create standardized error codes.

Examples:

AUTH\_INVALID\_CREDENTIALS

AUTH\_TOKEN\_EXPIRED

USER\_ALREADY\_EXISTS

RESTAURANT\_NOT\_FOUND

RESTAURANT\_NOT\_APPROVED

TABLE\_NOT\_AVAILABLE

RESERVATION\_NOT\_FOUND

RESERVATION\_ALREADY\_CANCELLED

INVALID\_COUPON

INSUFFICIENT\_REWARD\_POINTS

PAYMENT\_VERIFICATION\_FAILED

DINITICKET\_ALREADY\_ACTIVE

ACCESS\_DENIED

---

# 80. EXPECTED FINAL SYSTEM

The final architecture must operate like:

BookEat Customer Flutter App
│
│
▼
REST API/JWT
│
▼
SPRING BOOT API
/
/
PostgreSQL External Services
│
├── Razorpay
├── Email
└── AWS S3

```
        ▲
        │
┌───────┴────────┐
│                │

```

Restaurant Admin Panel
Owner Flutter React/Next.js

All three applications must communicate through the same backend and use the same source of truth.

---

# 81. STARTING INSTRUCTION

Start development with:

PHASE 1 — FOUNDATION

First provide:

1. Recommended Spring Boot version
2. Java version
3. Spring Initializr dependencies
4. Complete pom.xml
5. Project folder/package structure
6. PostgreSQL database creation
7. application.properties
8. application-dev.properties
9. application-prod.properties
10. BaseEntity
11. ApiResponse
12. GlobalExceptionHandler
13. ErrorCode structure
14. Flyway configuration
15. Initial database migration
16. Health check API

After completing these files, continue with:

PHASE 2 — AUTHENTICATION AND JWT SECURITY.

Do not jump directly to reservations or payments before the foundation, authentication and restaurant ownership architecture are finished.

The final backend must be maintainable, scalable, secure, testable, production-ready, and suitable for the existing BookEat Customer App, Restaurant Owner App, and Admin Panel.