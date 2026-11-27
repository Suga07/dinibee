package com.bookeat.payment.service;
import com.bookeat.payment.entity.Payment;import com.bookeat.payment.entity.Refund;import java.util.UUID;
public interface RefundObserver {void completed(Payment payment,Refund refund,UUID actor);}
