export type BusinessService = "RESTAURANT" | "TAKEAWAY" | "DINITICKET";

export type RestaurantApprovalStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "CHANGES_REQUESTED"
  | "APPROVED"
  | "REJECTED"
  | "SUSPENDED"
  | "PENDING"; // legacy alias used in some demo-data paths
export type BookingType = "REGULAR" | "GROUP" | "CORPORATE";
export type ArrivalStatus = "NOT_ARRIVED" | "ARRIVED";
export type TableStatus = "AVAILABLE" | "RESERVED" | "OCCUPIED" | "CLEANING" | "DISABLED";
export type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CHECKED_IN"
  | "DINING"
  | "BILL_GENERATED"
  | "PAYMENT_PENDING"
  | "PAID"
  | "COMPLETED"
  | "CANCELLED"
  | "NO_SHOW";
export type PaymentStatus =
  | "PENDING"
  | "PAID"
  | "FAILED"
  | "REFUNDED"
  | "PARTIALLY_REFUNDED";
export type FoodOrderStatus = "NEW" | "ACCEPTED" | "PREPARING" | "READY" | "SERVED" | "COMPLETED" | "CANCELLED";
export type JourneyStatus = "NOT_STARTED" | "LEAVING_NOW" | "ON_THE_WAY" | "NEARBY" | "ARRIVED";
export type TakeawayStatus = "NEW" | "CONFIRMED" | "WAITING_FOR_CUSTOMER" | "PREPARING" | "READY" | "PICKED_UP" | "COMPLETED" | "REJECTED" | "CANCELLED";
export type DiniTicketStatus = "WAITING" | "PREPARING" | "READY" | "CALLED" | "COLLECTED" | "COMPLETED" | "CANCELLED" | "NO_SHOW";
export type SupportUserType = "CUSTOMER" | "OWNER";
export type SupportStatus = "OPEN" | "IN_PROGRESS" | "WAITING_CUSTOMER" | "WAITING_OWNER" | "RESOLVED" | "CLOSED";
export type WalletTransactionType = "EARN" | "REDEEM" | "ADJUSTMENT" | "EXPIRED" | "REFUND_REVERSAL";
export type SettlementStatus = "PENDING" | "PROCESSING" | "PAID" | "FAILED" | "ON_HOLD";
export type InvoiceStatus = "DRAFT" | "ISSUED" | "PARTIALLY_PAID" | "PAID" | "OVERDUE" | "CANCELLED";
export type RefundStatus = "REQUESTED" | "PROCESSING" | "COMPLETED" | "FAILED" | "REJECTED";
export type NotificationAudience = "ALL_CUSTOMERS" | "SELECTED_CUSTOMERS" | "ALL_RESTAURANTS" | "SELECTED_RESTAURANTS";
export type NotificationType = "BOOKING" | "PAYMENT" | "REFUND" | "WALLET_POINTS" | "SUPPORT_TICKET" | "RESTAURANT_APPROVAL" | "PROMOTION" | "SYSTEM_NOTICE";

export interface RestaurantRecord {
  requestedServices: any;
  data: any;
  id: string;
  version?: number;
  v?: number;
  restaurantName: string;
  ownerName: string;
  phone: string;
  email: string;
  location: string;
  approvalStatus: RestaurantApprovalStatus;
  services: BusinessService[];
  createdAt: string;
  suspended: boolean;
  businessName: string;
  gstNumber: string;
  fssaiNumber: string;
  operatingHours: string;
  bankAccount: string;
  documents: Array<{ name: string; status: "VERIFIED" | "PENDING" | "REJECTED" }>;
  approvalHistory: Array<{ status: RestaurantApprovalStatus; date: string; admin: string; note: string }>;
}

export interface GroupGuest {
  id: string;
  name: string;
  joined: boolean;
  orderAmount: number;
  paymentStatus: PaymentStatus;
}

export interface ReservationRecord {
  id: string;
  bookingNumber: string;
  bookingType: BookingType;
  customerId: string;
  customerName: string;
  restaurantId: string;
  restaurantName: string;
  customerPhone?: string;
  customerEmail?: string;
  restaurantLogoUrl?: string;
  restaurantCoverImageUrl?: string;
  restaurantLocation?: string;
  specialRequest?: string;
  createdAt?: string;
  updatedAt?: string;
  endTime?: string;
  tableId: string;
  date: string;
  time: string;
  guestCount: number;
  arrivalStatus: ArrivalStatus;
  status: BookingStatus;
  paymentStatus: PaymentStatus;
  amount: number;
  checkInTime?: string;
  checkOutTime?: string;
  diningDuration?: string;
  tableStatus: TableStatus;
  rewardPoints: number;
  reviewStatus: "PENDING" | "SUBMITTED";
  groupId?: string;
  groupGuests?: GroupGuest[];
  orderingDeadline?: string;
  orderingStatus?: "OPEN" | "CLOSED";
  paymentMode?: "HOST_PAYS" | "SPLIT_BY_GUEST" | "SPLIT_EQUALLY" | "PAY_AT_RESTAURANT";
  companyName?: string;
  corporateAccount?: string;
  contactPerson?: string;
  eventName?: string;
  perPersonBudget?: number;
  totalBudget?: number;
  billingMethod?: "COMPANY_PAYS" | "EMPLOYEE_PAYS" | "SPLIT" | "COMPANY_CONTRIBUTION";
  recurrence?: "ONE_TIME" | "DAILY" | "WEEKLY" | "MONTHLY" | "CUSTOM";
  invoiceTerms?: "PAY_NOW" | "PAY_AT_RESTAURANT" | "NET_7" | "NET_15" | "NET_30" | "CUSTOM";
}

export interface FoodOrderRecord {
  id: string;
  orderNumber: string;
  bookingNumber: string;
  customerName: string;
  restaurantName: string;
  customerPhone?: string;
  customerEmail?: string;
  restaurantLogoUrl?: string;
  restaurantCoverImageUrl?: string;
  restaurantLocation?: string;
  specialRequest?: string;
  createdAt?: string;
  updatedAt?: string;
  endTime?: string;
  tableId: string;
  bookingType: BookingType;
  amount: number;
  orderStatus: FoodOrderStatus;
  paymentStatus: PaymentStatus;
  createdAt: string;
  participantOrders?: Array<{ name: string; items: number; amount: number }>;
}

export interface TakeawayRecord {
  id: string;
  orderNumber: string;
  type: "REGULAR" | "ON_THE_WAY";
  customerName: string;
  restaurantName: string;
  journeyStatus: JourneyStatus;
  eta?: string;
  amount: number;
  paymentStatus: PaymentStatus;
  refundStatus: "NONE" | "REQUESTED" | "REFUNDED";
  status: TakeawayStatus;
  createdAt: string;
  customerId?: string;
  customerUserId?: string;
  customerPhone?: string;
  customerEmail?: string;
  restaurantId?: string;
  restaurantLogoUrl?: string;
  restaurantCoverImageUrl?: string;
  restaurantLocation?: string;
  foodOrderId?: string;
  paymentId?: string;
  paymentMethod?: string;
  foodImageUrl?: string;
  itemsSummary?: string;
}

export interface DiniTicketRecord {
  id: string;
  tokenNumber: string;
  customerName: string;
  restaurantName: string;
  orderNumber: string;
  amount: number;
  paymentStatus: PaymentStatus;
  queueStatus: DiniTicketStatus;
  arrivalStatus: "NOT_STARTED" | "ON_THE_WAY" | "ARRIVED";
  createdAt: string;
  waitMinutes: number;
  customerId?: string;
  customerUserId?: string;
  customerPhone?: string;
  customerEmail?: string;
  restaurantId?: string;
  restaurantLogoUrl?: string;
  restaurantCoverImageUrl?: string;
  restaurantLocation?: string;
  foodOrderId?: string;
  paymentId?: string;
  foodImageUrl?: string;
  itemsSummary?: string;
}

export interface PaymentRecord {
  id: string;
  transactionId: string;
  referenceId: string;
  customerName: string;
  restaurantName: string;
  paymentType: "DINE_IN" | "TAKEAWAY" | "DINITICKET" | "GROUP_BOOKING" | "CORPORATE_BOOKING";
  grossAmount: number;
  rewardDiscount: number;
  gatewayPaidAmount: number;
  paymentMethod: string;
  status: PaymentStatus;
  date: string;
}

export interface SettlementRecord {
  id: string;
  settlementId: string;
  restaurantName: string;
  period: string;
  grossAmount: number;
  commission: number;
  gatewayFee: number;
  refundAdjustment: number;
  netSettlement: number;
  status: SettlementStatus;
  createdDate: string;
  paidDate?: string;
}

export interface WalletOverview {
  customersWithWallet: number;
  totalPointsIssued: number;
  totalPointsRedeemed: number;
  outstandingPoints: number;
  todayEarnedPoints: number;
  todayRedeemedPoints: number;
}

export interface CustomerWallet {
  customerId: string;
  customerName: string;
  balance: number;
  approximateRedeemValue: number;
  totalEarned: number;
  totalRedeemed: number;
}

export interface WalletTransaction {
  id: string;
  customerId: string;
  customerName: string;
  type: WalletTransactionType;
  points: number;
  relatedBookingId?: string;
  relatedOrderId?: string;
  reason: string;
  createdAt: string;
  adminName?: string;
}

export interface RewardRules {
  pointsConversionRate: number;
  minimumRedeemPoints: number;
  maximumRedeemPercentage: number;
  pointsExpiryDays: number;
  enabled: boolean;
}

export interface SupportMessage {
  id: string;
  senderType: "ADMIN" | "CUSTOMER" | "OWNER";
  senderName: string;
  message: string;
  createdAt: string;
  attachmentName?: string;
}

export interface SupportTicketRecord {
  id: string;
  version?: number;
  ticketNumber: string;
  userType: SupportUserType;
  userName: string;
  restaurantName?: string;
  relatedEntityType: "BOOKING" | "FOOD_ORDER" | "TAKEAWAY" | "DINITICKET" | "PAYMENT" | "WALLET" | "OTHER";
  relatedEntityId?: string;
  category: string;
  subject?: string;
  description?: string;
  lastMessage: string;
  status: SupportStatus;
  assignedAdminName: string;
  createdAt: string;
  updatedAt: string;
  messages: SupportMessage[];
}

export interface CorporateInvoiceRecord {
  id: string;
  invoiceId: string;
  companyName: string;
  restaurantName: string;
  corporateBookingNumber: string;
  invoiceDate: string;
  dueDate: string;
  subtotal: number;
  tax: number;
  total: number;
  paidAmount: number;
  pendingAmount: number;
  status: InvoiceStatus;
}

export interface ReviewRecord {
  id: string;
  customerName: string;
  restaurantName: string;
  referenceId: string;
  restaurantRating: number;
  foodQuality: number;
  service: number;
  ambience: number;
  valueForMoney: number;
  reviewText: string;
  imageCount: number;
  createdAt: string;
  status: "ACTIVE" | "REPORTED" | "HIDDEN" | "REMOVED";
}

export interface RefundRecord {
  id: string;
  refundId: string;
  originalTransactionId: string;
  relatedEntityId: string;
  relatedTicketId?: string;
  customerName: string;
  restaurantName: string;
  serviceType: "DINE_IN" | "TAKEAWAY" | "DINITICKET" | "GROUP_BOOKING" | "CORPORATE_BOOKING";
  originalAmount: number;
  refundAmount: number;
  reason: string;
  gatewayReference?: string;
  requestedAt: string;
  processedAt?: string;
  status: RefundStatus;
}

export interface NotificationRecord {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  audience: NotificationAudience;
  recipientCount?: number;
  createdBy: string;
  createdAt: string;
  scheduledAt?: string;
  status: "DRAFT" | "QUEUED" | "SENT" | "FAILED";
}

export interface DashboardSnapshot {
  restaurants: { total: number; pending: number; approved: number };
  customers: number;
  reservations: { today: number; regular: number; group: number; corporate: number };
  dining: { active: number; checkedIn: number; dining: number; occupiedTables: number };
  takeaway: { total: number; onTheWay: number };
  diniTicket: { orders: number; waiting: number };
  finance: { revenue: number; commission: number; pendingSettlements: number };
  support: { customerOpen: number; ownerOpen: number };
  wallet: { issued: number; redeemed: number };
  recentActivity: Array<{ id: string; title: string; detail: string; time: string; status: string }>;
  serviceVolume: Array<{ name: string; value: number }>;
  revenueTrend: Array<{ day: string; gross: number; commission: number }>;
}
