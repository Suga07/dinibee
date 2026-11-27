package com.bookeat.payment.service;
import java.math.*;import java.nio.charset.StandardCharsets;import java.security.*;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import java.util.HexFormat;
public final class PaymentRules {
 private PaymentRules(){}
 public static long minor(BigDecimal amount){if(amount==null||amount.signum()<0)throw new IllegalArgumentException("Invalid amount");return amount.movePointRight(2).longValueExact();}
 public static BigDecimal major(long amount){if(amount<0)throw new IllegalArgumentException("Invalid amount");return BigDecimal.valueOf(amount,2);}
 public static BigDecimal commission(BigDecimal amount,boolean percent,BigDecimal value){if(amount.signum()<0||value.signum()<0||(percent&&value.compareTo(BigDecimal.valueOf(100))>0))throw new IllegalArgumentException("Invalid commission");return (percent?amount.multiply(value).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP):value).min(amount).setScale(2,RoundingMode.HALF_UP);}
 public static boolean refundAllowed(BigDecimal captured,BigDecimal reserved,BigDecimal requested){return requested.signum()>0&&reserved.signum()>=0&&reserved.add(requested).compareTo(captured)<=0;}
 public static boolean signature(String secret,byte[] data,String supplied){if(secret==null||secret.isBlank()||supplied==null||!supplied.matches("[a-fA-F0-9]{64}"))return false;try{Mac h=Mac.getInstance("HmacSHA256");h.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return MessageDigest.isEqual(h.doFinal(data),HexFormat.of().parseHex(supplied));}catch(GeneralSecurityException|IllegalArgumentException e){return false;}}
}
