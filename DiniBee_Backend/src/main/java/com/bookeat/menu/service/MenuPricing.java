package com.bookeat.menu.service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
/** All option prices are per unit. A variant replaces the base/discount price; addons add to it. */
public final class MenuPricing {
    private MenuPricing() {}
    public static boolean validDiscount(BigDecimal base,BigDecimal discount){return base!=null && base.signum()>=0 && (discount==null || (discount.signum()>=0 && discount.compareTo(base)<=0));}
    public static BigDecimal sellingPrice(BigDecimal base,BigDecimal discount) {
        if(!validDiscount(base,discount)) throw new IllegalArgumentException("Invalid price");
        return (discount==null?base:discount).setScale(2,RoundingMode.UNNECESSARY);
    }
    public static BigDecimal unitPrice(BigDecimal base,BigDecimal discount,BigDecimal variant,List<BigDecimal> addons) {
        var total=variant==null?sellingPrice(base,discount):nonnegative(variant);
        for(var addon:addons) total=total.add(nonnegative(addon));return total.setScale(2,RoundingMode.UNNECESSARY);
    }
    private static BigDecimal nonnegative(BigDecimal value){if(value==null || value.signum()<0)throw new IllegalArgumentException("Invalid option price");return value.setScale(2,RoundingMode.UNNECESSARY);}
}
