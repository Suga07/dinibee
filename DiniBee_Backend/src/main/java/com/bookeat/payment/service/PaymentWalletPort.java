package com.bookeat.payment.service;
import java.util.UUID;import java.math.BigDecimal;
public interface PaymentWalletPort {void debit(UUID customer,UUID payment,BigDecimal amount,UUID actor);void refund(UUID customer,UUID refund,BigDecimal amount,UUID actor);}
