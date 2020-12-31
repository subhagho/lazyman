package com.codekutter.salesman.core.model;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.openhft.chronicle.bytes.BytesMarshallable;

@Getter
@Setter
@Accessors(fluent = true)
public class Path implements BytesMarshallable {
    private double length;
    @Setter(AccessLevel.NONE)
    private double elevation = 0;
    @Setter(AccessLevel.NONE)
    private double distance;

    public void elevate(double elevation) {
        Preconditions.checkArgument(elevation >= 0);
        this.elevation = elevation;

        distance = Math.sqrt(Math.pow(elevation, 2) + Math.pow(length, 2));
    }

    public void compute(Point a, Point b) {
        double dx = a.X() - b.X();
        double dy = a.Y() - b.Y();

        length = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        elevation = 0;
    }

    @Override
    public String toString() {
        return "Path{" +
                "length=" + length +
                ", elevation=" + elevation +
                ", distance=" + distance +
                '}';
    }
}
