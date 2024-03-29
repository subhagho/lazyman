package com.codekutter.lazyman.core.model;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public class Point {
    private int sequence;
    private Double X = null;
    private Double Y = null;
    private double elevation = 0;
    private String key;
    private short ring = -1;

    public Point() {
    }

    public Point(double x, double y) {
        X = x;
        Y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        boolean ret = true;
        ret = sequence == point.sequence;
        ret = ret && (X == null && point.X == null || (X != null && X.equals(point.X))) && (Y == null && point.Y == null || (Y != null && Y.equals(point.Y)));

        return ret;
    }

    @Override
    public String toString() {
        return String.format("[%d (%d)]{%f}", sequence, ring, elevation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, X, Y, elevation);
    }

    public String print() {
        return String.format("%d (%f, %f, %f)", sequence, X, Y, elevation);
    }

    public String hashKey() {
        if (Strings.isNullOrEmpty(key)) {
            key = String.format("%d (%f, %f)", sequence, X, Y);
        }
        return key;
    }
}
