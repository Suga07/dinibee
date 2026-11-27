package com.bookeat.dining.service;
import com.bookeat.dining.enums.DiningStatus;import java.time.Instant;import java.math.BigDecimal;
public final class DiningRules {
 private DiningRules(){}
 public static boolean arrivalWindow(Instant now,Instant starts,Instant ends,Instant noShow){Instant latest=noShow.isAfter(starts.plusSeconds(900))?noShow:starts.plusSeconds(900);if(latest.isAfter(ends))latest=ends;return !now.isBefore(starts)&&now.isBefore(latest);}
 public static boolean withinRadius(double km,int meters){return Double.isFinite(km)&&km>=0&&meters>0&&km*1000<=meters;}
 public static boolean canOrder(DiningStatus state){return state==DiningStatus.CHECKED_IN||state==DiningStatus.DINING;}
 public static boolean settled(BigDecimal due){return due.signum()==0;}
}
