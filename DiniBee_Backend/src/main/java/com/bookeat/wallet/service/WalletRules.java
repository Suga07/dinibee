package com.bookeat.wallet.service;
import java.math.*;
public final class WalletRules {
 private WalletRules(){}
 public static BigDecimal redeem(long available,long points,BigDecimal value){if(points<=0||points>available||value.signum()<=0)throw new IllegalArgumentException("Invalid redemption");return value.multiply(BigDecimal.valueOf(points)).setScale(2,RoundingMode.HALF_UP);}
 public static BigDecimal balance(BigDecimal before,BigDecimal delta){var after=before.add(delta).setScale(2,RoundingMode.UNNECESSARY);if(after.signum()<0||after.precision()>18)throw new IllegalArgumentException("Invalid balance");return after;}
 public static long points(long before,long delta){long after=Math.addExact(before,delta);if(after<0)throw new IllegalArgumentException("Invalid points");return after;}
 public record PointBalance(long points,long debt){}
 public static PointBalance adjustPoints(long before,long debt,long delta,boolean allowDebt){if(before<0||debt<0)throw new IllegalArgumentException("Invalid ledger");if(delta>0){long offset=Math.min(debt,delta);return new PointBalance(points(before,delta-offset),debt-offset);}if(allowDebt){long wanted=Math.negateExact(delta);long taken=Math.min(wanted,before);return new PointBalance(before-taken,Math.addExact(debt,wanted-taken));}return new PointBalance(points(before,delta),debt);}
}
