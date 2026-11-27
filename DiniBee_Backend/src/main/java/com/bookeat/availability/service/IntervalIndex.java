package com.bookeat.availability.service;
import java.time.Instant;
import java.util.*;
/** Immutable union of half-open [start,end) ranges, with logarithmic overlap checks. */
public final class IntervalIndex {
    public record Range(Instant start,Instant end) {
        public Range {if(start==null || end==null || !start.isBefore(end))throw new IllegalArgumentException("Invalid interval");}
    }
    private final List<Range> ranges;
    public IntervalIndex(Collection<Range> input) {
        var sorted=input.stream().sorted(Comparator.comparing(Range::start)).toList();var merged=new ArrayList<Range>();
        for(var r:sorted){
            if(merged.isEmpty() || merged.get(merged.size()-1).end().isBefore(r.start()))merged.add(r);
            else {var last=merged.remove(merged.size()-1);merged.add(new Range(last.start(),last.end().isAfter(r.end())?last.end():r.end()));}
        }
        ranges=List.copyOf(merged);
    }
    public boolean overlaps(Instant start,Instant end) {
        if(!start.isBefore(end))throw new IllegalArgumentException("Invalid interval");
        int low=0,high=ranges.size();
        while(low<high){int middle=(low+high)>>>1;if(ranges.get(middle).start().isBefore(end))low=middle+1;else high=middle;}
        return low>0 && ranges.get(low-1).end().isAfter(start);
    }
}
