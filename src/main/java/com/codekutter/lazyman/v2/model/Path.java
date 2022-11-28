package com.codekutter.lazyman.v2.model;

import com.codekutter.lazyman.v2.utils.Utils;
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
        return compute(h, length, A);
    }

    public double compute(double height, double length, Point point) {
        Preconditions.checkNotNull(A);
        Preconditions.checkNotNull(B);
        double d = Math.sqrt(((height * height) + (length * length)));
        double c1 = ((double) point.chainLength());
        double c2 = ((double) A.chainLength());
        return d;// + ((c1 * c2) / (d * d));
    }

    @Override
    public int compareTo(@NonNull Path o) {
        return Utils.compareTo(actualLength, o.actualLength);
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

    public String pathKey() {
        Point S = null;
        Point T = null;
        if (A.compare(B) <= 0) {
            S = A;
            T = B;
        } else {
            S = B;
            T = A;
        }
        return String.format("[%f, %f]:[%f, %f]", S.X(), S.Y(), T.X(), T.Y());
    }

    public String edgeString() {
        return String.format("%d:%d=%5.2f", A.sequence(), B.sequence(), actualLength);
    }
}
