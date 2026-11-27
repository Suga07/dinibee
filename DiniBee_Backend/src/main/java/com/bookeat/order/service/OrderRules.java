package com.bookeat.order.service;
import com.bookeat.order.enums.*;
import java.math.*;
import java.util.*;
public final class OrderRules {
    private OrderRules() {}
    public static boolean terminal(FoodOrderStatus s){return s==FoodOrderStatus.COMPLETED||s==FoodOrderStatus.CANCELLED||s==FoodOrderStatus.REJECTED;}
    public static boolean transition(FoodOrderStatus from,FoodOrderStatus to,boolean takeaway){return switch(to){case ACCEPTED->from==FoodOrderStatus.PLACED;case PREPARING->from==FoodOrderStatus.ACCEPTED;case READY->from==FoodOrderStatus.PREPARING;case SERVED->!takeaway&&from==FoodOrderStatus.READY;case COMPLETED->takeaway?from==FoodOrderStatus.READY:from==FoodOrderStatus.SERVED;case REJECTED->from==FoodOrderStatus.PLACED||from==FoodOrderStatus.ACCEPTED||from==FoodOrderStatus.PREPARING||from==FoodOrderStatus.READY;case CANCELLED->from==FoodOrderStatus.PLACED;default->false;};}
    public static List<BigDecimal> splitEqual(BigDecimal total,int count){if(count<1||total.signum()<0)throw new IllegalArgumentException("Invalid split");var parts=total.movePointRight(2).toBigIntegerExact().divideAndRemainder(BigInteger.valueOf(count));var result=new ArrayList<BigDecimal>();for(int i=0;i<count;i++)result.add(new BigDecimal(parts[0].add(i<parts[1].intValueExact()?BigInteger.ONE:BigInteger.ZERO),2));return List.copyOf(result);}
}
