package com.codekutter.lazyman.core.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class RingRoute {
    private final short ring;
    private final Path ends;
    private List<Path> paths = new ArrayList<>();

    public RingRoute(short ring, @NonNull Path ends) {
        this.ring = ring;
        this.ends = ends;
    }

    public RingRoute(@NonNull Ring ring) {
        this.ring = ring.number();
        Point ps = null;
        Point pe = null;
        paths = ring.paths();
        for (int ii = 0; ii < paths.size(); ii++) {
            Path path = paths.get(ii);
            if (ps == null) {
                Path pn = paths.get(ii + 1);
                if (!pn.hasPoint(path.A())) {
                    ps = path.A();
                } else if (!pn.hasPoint(path.B())) {
                    ps = path.B();
                } else {
                    throw new RuntimeException(String.format("Invalid start path. [ring=%d]...", ring.number()));
                }
            } else if (ii == paths.size() - 1) {
                Path pp = paths.get(ii - 1);
                if (!pp.hasPoint(path.A())) {
                    pe = path.A();
                } else if (!pp.hasPoint(path.B())) {
                    pe = path.B();
                } else {
                    throw new RuntimeException(String.format("Invalid end path. [ring=%d]...", ring.number()));
                }
            }
        }
        if (pe == null || ps == null) {
            throw new RuntimeException(String.format("Start/End point not found. [ring=%d]...", ring.number()));
        }
        ends = new Path(ps, pe);
    }

    public RingRoute addPath(@NonNull Path path) {
        paths.add(path);
        return this;
    }

    public double distance() {
        Preconditions.checkState(paths != null && paths.size() > 0);
        double d = 0;
        for (Path path : paths) {
            d += path.distance();
        }
        return d;
    }

    public static class SortRoute implements Comparator<RingRoute> {

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
        public int compare(RingRoute o1, RingRoute o2) {
            double ret = 0;
            if (o1 == null && o2 == null) {
                ret = 0;
            } else if (o1 != null && o2 == null) {
                ret = 1;
            } else if (o1 == null) {
                ret = -1;
            } else {
                ret = o1.distance() - o2.distance();
                if (ret < 0) {
                    ret = Math.floor(ret);
                } else if (ret > 0) {
                    ret = Math.ceil(ret);
                }
            }
            return (int) ret;
        }
    }
}
