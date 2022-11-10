package com.codekutter.lazyman.v2.model;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public class Path implements Comparable<Path> {
    private Point A;
    private Point B;
    private double length;
    @Setter(AccessLevel.NONE)
    private double distance;
    private double actualLength;

    public Path(@NonNull Point A, @NonNull Point B) {
        this.A = A;
        this.B = B;
        double h = A.X() - B.X();
        double d = A.Y() - B.Y();
        actualLength = Math.sqrt((h * h) + (d * d));
        length = 0;
    }

    public double compute() {
        Preconditions.checkNotNull(A);
        Preconditions.checkNotNull(B);
        double h = A.height(B);
        distance = Math.PI / 2 * (Math.sqrt(((h * h) + (length * length)) / 2));
        return distance;
    }

    @Override
    public int compareTo(@NonNull Path o) {
        double d = (actualLength - o.actualLength);
        int ret = 0;
        if (d > 1 || d < -1) {
            ret = (int) d;
        } else if (d < 1 && d > 0) {
            ret = 1;
        } else if (d < 0) {
            ret = -1;
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return (A.equals(path.A) && B.equals(path.B)) || (B.equals(path.A) && A.equals(path.B));
    }

    @Override
    public int hashCode() {
        return Objects.hash(A, B);
    }

    public Point target(@NonNull Point source) throws Exception {
        if (source.equals(A)) return B;
        if (source.equals(B)) return A;
        throw new Exception(String.format("Point not in path. [point=%s]", source));
    }

    @Override
    public String toString() {
        return "Path{" +
                "A=" + A +
                ", B=" + B +
                ", actualLength=" + actualLength +
                '}';
    }
}
