package com.codekutter.lazyman.core.model;

import com.google.common.base.Preconditions;
import lombok.*;
import lombok.experimental.Accessors;
import net.openhft.chronicle.bytes.BytesMarshallable;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
@ToString
public class Path implements BytesMarshallable, Comparable<Path> {
    public static final double G = 9.8f;
    public static final double MU = 0.2f;
    @Setter(AccessLevel.NONE)
    private Point A;
    @Setter(AccessLevel.NONE)
    private Point B;
    private double elevation = 0;
    private double length;
    private final double actualLength;
    private boolean usable = true;

    public Path(@NonNull Point A, @NonNull Point B, double length) {
        this.A = A;
        this.B = B;
        this.actualLength = length;
        this.length = actualLength;
    }

    public Path(@NonNull Point A, @NonNull Point B) {
        Preconditions.checkArgument(A.Y() != null && A.X() != null);
        Preconditions.checkArgument(B.Y() != null && B.X() != null);

        actualLength = Math.sqrt(Math.pow((A.X() - B.X()), 2) + Math.pow((A.Y() - B.Y()), 2));
        this.A = A;
        this.B = B;
        this.length = actualLength;
    }

    public double distance() {
        if (A != null && B != null) {
            double d = 0;
            double h = 0;
            if (elevation == 0) {
                h = A.elevation() - B.elevation();
                d = Math.sqrt(Math.pow(length, 2) + Math.pow(h, 2));
            } else {
                double h1 = elevation - A.elevation();
                double d1 = Math.sqrt(Math.pow(length / 2f, 2) + Math.pow(h1, 2));
                double h2 = elevation - B.elevation();
                double d2 = Math.sqrt(Math.pow(length / 2f, 2) + Math.pow(h2, 2));
                d = d1 + d2;
                h = Math.max(h1, h2);
            }
            if (d > 0) {
                return d;
            }
        }
        return Double.MAX_VALUE;
    }

    public double elevation() {
        if (A != null && B != null) {
            return Math.sqrt(Math.pow(A.elevation() - B.elevation(), 2));
        }
        return Long.MAX_VALUE;
    }

    public Point getTarget(@NonNull Point source) {
        if (A.sequence() == source.sequence()) {
            return B;
        } else if (B.sequence() == source.sequence()) {
            return A;
        }
        return null;
    }

    public Point getTarget(int source) {
        if (A.sequence() == source) {
            return B;
        } else if (B.sequence() == source) {
            return A;
        }
        return null;
    }

    public Point connectingPoint(@NonNull Path path) {
        if (hasPoint(path.A)) return path.A;
        else if (hasPoint(path.B)) return path.B;
        return null;
    }

    public boolean hasPoint(@NonNull Point point) {
        return A.sequence() == point.sequence() || B.sequence() == point.sequence();
    }

    @Override
    public int compareTo(@NotNull Path o) {
        return (int) (distance() - o.distance());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return (A.equals(path.A) && B.equals(path.B)) || (A.equals(path.B) && B.equals(path.A));
    }

    public String pathKey() {
        Point a = (A.sequence() < B.sequence() ? A : B);
        Point b = (A.sequence() < B.sequence() ? B : A);
        return String.format("%d->%d", a.sequence(), b.sequence());
    }

    public String edgeString() {
        String pk = pathKey();
        return String.format("%s[%f]", pk, actualLength);
    }

    @Override
    public int hashCode() {
        return Objects.hash(A, B);
    }

    public static class SortByDistance implements Comparator<Path> {

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         * <p>
         * The implementor must ensure that {@code sgn(compare(x, y)) ==
         * -sgn(compare(y, x))} for all {@code x} and {@code y}.  (This
         * implies that {@code compare(x, y)} must throw an exception if and only
         * if {@code compare(y, x)} throws an exception.)<p>
         * <p>
         * The implementor must also ensure that the relation is transitive:
         * {@code ((compare(x, y)>0) && (compare(y, z)>0))} implies
         * {@code compare(x, z)>0}.<p>
         * <p>
         * Finally, the implementor must ensure that {@code compare(x, y)==0}
         * implies that {@code sgn(compare(x, z))==sgn(compare(y, z))} for all
         * {@code z}.<p>
         * <p>
         * It is generally the case, but <i>not</i> strictly required that
         * {@code (compare(x, y)==0) == (x.equals(y))}.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."<p>
         * <p>
         * In the foregoing description, the notation
         * {@code sgn(}<i>expression</i>{@code )} designates the mathematical
         * <i>signum</i> function, which is defined to return one of {@code -1},
         * {@code 0}, or {@code 1} according to whether the value of
         * <i>expression</i> is negative, zero, or positive, respectively.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * first argument is less than, equal to, or greater than the
         * second.
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(Path o1, Path o2) {
            if (o1 == null && o2 == null) return 0;
            else if (o1 != null && o2 == null) return -1;
            else if (o1 == null) return 1;

            double ret = o1.distance() - o2.distance();
            int v = 0;
            if (ret < 0) {
                v = (int) Math.floor(ret);
            } else if (ret > 0) {
                v = (int) Math.ceil(ret);
            }
            return v;
        }

        /**
         * Returns a comparator that imposes the reverse ordering of this
         * comparator.
         *
         * @return a comparator that imposes the reverse ordering of this
         * comparator.
         * @since 1.8
         */
        @Override
        public Comparator<Path> reversed() {
            return Comparator.super.reversed();
        }
    }

    public static class SortByLength implements Comparator<Path> {

        /**
         * Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         * <p>
         * The implementor must ensure that {@code sgn(compare(x, y)) ==
         * -sgn(compare(y, x))} for all {@code x} and {@code y}.  (This
         * implies that {@code compare(x, y)} must throw an exception if and only
         * if {@code compare(y, x)} throws an exception.)<p>
         * <p>
         * The implementor must also ensure that the relation is transitive:
         * {@code ((compare(x, y)>0) && (compare(y, z)>0))} implies
         * {@code compare(x, z)>0}.<p>
         * <p>
         * Finally, the implementor must ensure that {@code compare(x, y)==0}
         * implies that {@code sgn(compare(x, z))==sgn(compare(y, z))} for all
         * {@code z}.<p>
         * <p>
         * It is generally the case, but <i>not</i> strictly required that
         * {@code (compare(x, y)==0) == (x.equals(y))}.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."<p>
         * <p>
         * In the foregoing description, the notation
         * {@code sgn(}<i>expression</i>{@code )} designates the mathematical
         * <i>signum</i> function, which is defined to return one of {@code -1},
         * {@code 0}, or {@code 1} according to whether the value of
         * <i>expression</i> is negative, zero, or positive, respectively.
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * first argument is less than, equal to, or greater than the
         * second.
         * @throws NullPointerException if an argument is null and this
         *                              comparator does not permit null arguments
         * @throws ClassCastException   if the arguments' types prevent them from
         *                              being compared by this comparator.
         */
        @Override
        public int compare(Path o1, Path o2) {
            if (o1 == null && o2 == null) return 0;
            else if (o1 != null && o2 == null) return -1;
            else if (o1 == null) return 1;

            double ret = o1.actualLength - o2.actualLength;
            int v = 0;
            if (ret < 0) {
                v = (int) Math.floor(ret);
            } else if (ret > 0) {
                v = (int) Math.ceil(ret);
            }
            return v;
        }

        /**
         * Returns a comparator that imposes the reverse ordering of this
         * comparator.
         *
         * @return a comparator that imposes the reverse ordering of this
         * comparator.
         * @since 1.8
         */
        @Override
        public Comparator<Path> reversed() {
            return Comparator.super.reversed();
        }
    }

    public static class PathComparator implements Comparator<Path> {
        @Override
        public int compare(Path o1, Path o2) {
            return o1.compareTo(o2);
        }
    }
}
