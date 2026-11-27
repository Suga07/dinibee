package com.bookeat.order.service;
import java.math.*;
public final class OrderPricing {
    private OrderPricing() {}
    public record Totals(BigDecimal subtotal,BigDecimal discount,BigDecimal taxPercent,BigDecimal tax,BigDecimal total) {}
    public static Totals totals(BigDecimal gross,BigDecimal net,BigDecimal rate){if(gross==null||net==null||rate==null||gross.signum()<0||net.signum()<0||net.compareTo(gross)>0||rate.signum()<0||rate.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Invalid order totals");gross=gross.setScale(2,RoundingMode.UNNECESSARY);net=net.setScale(2,RoundingMode.UNNECESSARY);rate=rate.setScale(2,RoundingMode.UNNECESSARY);var tax=net.multiply(rate).movePointLeft(2).setScale(2,RoundingMode.HALF_UP);return new Totals(gross,gross.subtract(net),rate,tax,net.add(tax));}
}
