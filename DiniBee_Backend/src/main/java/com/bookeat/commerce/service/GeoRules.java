package com.bookeat.commerce.service;
public final class GeoRules {
    private GeoRules() {}
    public static double distanceKm(double a,double b,double c,double d){double x=Math.sin(Math.toRadians(c-a)/2),y=Math.sin(Math.toRadians(d-b)/2);double h=x*x+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*y*y;return 6371.0088*2*Math.asin(Math.sqrt(Math.min(1,Math.max(0,h))));}
    public static long etaMinutes(double distanceKm){if(!Double.isFinite(distanceKm)||distanceKm<0)throw new IllegalArgumentException("Invalid distance");return Math.max(1,(long)Math.ceil(distanceKm/30*60));}
}
