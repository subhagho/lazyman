package com.codekutter.lazyman.model;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public class Point2D {
    private int sequence;
    private Double X = null;
    private Double Y = null;
    private String key;

    public Point2D() {
    }

    public Point2D(double x, double y) {
        X = x;
        Y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point2D point = (Point2D) o;
        boolean ret = true;
        ret = sequence == point.sequence;
        ret = ret && (X == null && point.X == null || (X != null && X.equals(point.X))) && (Y == null && point.Y == null || (Y != null && Y.equals(point.Y)));

        return ret;
    }

    @Override
    public String toString() {
        return String.format("[%d (%d)][X=%f, Y=%f]", sequence, X, Y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, X, Y);
    }

    public String print() {
        return String.format("%d (%f, %f)", sequence, X, Y);
    }

    public String hashKey() {
        if (Strings.isNullOrEmpty(key)) {
            key = String.format("%d [X=%f, Y=%f]", sequence, X, Y);
        }
        return key;
    }
}
