package com.codekutter.salesman.core.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public class Bid {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class BidTarget {
        private final Point point;
        private double elevation;
        private int count;

        public BidTarget(@NonNull Point point) {
            this.point = point;
        }
    }

    private final Point point;
    private Map<String, BidTarget> targets = new HashMap<>();

    public Bid(@NonNull Point point) {
        this.point = point;
    }

    public void add(@NonNull Path path, double elevation) {
        Point t = path.getTarget(point);
        BidTarget bt = targets.get(t.hashKey());
        if (bt == null) {
            bt = new BidTarget(t);
            targets.put(t.hashKey(), bt);
        }
        bt.elevation = elevation;
        bt.count++;
        point.elevation(computeElevation());
    }

    public double computeElevation() {
        int mc = 0;
        double e = 0;
        for (BidTarget bt : targets.values()) {
            e += Math.pow(bt.elevation, bt.count);
            if (bt.count > mc) {
                mc = bt.count;
            }
        }
        e = Math.pow(e, 1f / mc);
        return e;
    }

    public boolean shouldBid(@NonNull Path path, double elevation) {
        Point t = path.getTarget(point);
        BidTarget bt = targets.get(t.hashKey());
        if (bt != null) {
            if (bt.count >= 2) {
                return !(elevation <= bt.elevation);
            }
        }
        return true;
    }
}
