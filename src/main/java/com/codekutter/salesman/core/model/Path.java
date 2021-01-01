package com.codekutter.salesman.core.model;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.openhft.chronicle.bytes.BytesMarshallable;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Accessors(fluent = true)
public class Path implements BytesMarshallable, Comparable<Path> {
    @Setter(AccessLevel.NONE)
    private Point A;
    @Setter(AccessLevel.NONE)
    private Point B;
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

    public void compute(@NonNull Point a, @NonNull Point b) {
        double dx = a.X() - b.X();
        double dy = a.Y() - b.Y();

        length = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
        distance = length;
        elevation = 0;

        A = a;
        B = b;
    }

    @Override
    public String toString() {
        return "Path{" +
                "[A=" + A +
                ", B=" + B +
                "], length=" + length +
                ", elevation=" + elevation +
                ", distance=" + distance +
                '}';
    }

    @Override
    public int compareTo(@NotNull Path o) {
        return (int) (distance - o.distance());
    }
}
