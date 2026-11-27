package com.bookeat.table.service;
import com.bookeat.table.enums.TableStatus;
import java.util.Locale;
public final class TableRules {
    private TableRules() {}
    public static String normalizeNumber(String number){return number.trim().toUpperCase(Locale.ROOT);}
    public static boolean validCapacity(int capacity,int minimum,int maximum){return capacity>=1 && capacity<=500 && minimum>=1 && minimum<=maximum && maximum<=capacity;}
    public static boolean bookable(boolean active,TableStatus status,int minimum,int maximum,int capacity,int guests) {
        return active && status==TableStatus.AVAILABLE && validCapacity(capacity,minimum,maximum) && guests>=minimum && guests<=maximum;
    }
}
