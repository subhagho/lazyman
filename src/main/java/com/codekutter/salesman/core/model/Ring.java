package com.codekutter.salesman.core.model;

import com.codekutter.salesman.core.TSPDataMap;
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
public class Ring {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class RingRoute {
        private final short ring;
        private List<Path> paths = new ArrayList<>();

        public RingRoute(short ring) {
            this.ring = ring;
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

    private final short number;
    private List<Path> paths = new ArrayList<>();
    private boolean isClosed = true;
    private short level = 0;
    private Ring enclosing = null;

    public Ring(short number) {
        this.number = number;
    }

    public Ring(@NonNull Ring source, boolean closed) {
        this.number = source.number;
        this.isClosed = closed;
        this.level = source.level;
        this.paths = source.paths;
        this.enclosing = source.enclosing;
    }

    public Ring add(@NonNull Path connection) {
        if (!paths.isEmpty()) {
            if (exists(connection)) {
                throw new RuntimeException(String.format("Path already exists. [path=%s]", connection.toString()));
            }
            Path lp = paths.get(paths.size() - 1);
            if (lp.hasPoint(connection.A()) || lp.hasPoint(connection.B())) {
                paths.add(connection);
            } else {
                lp = null;
                boolean added = false;
                for (int ii = 0; ii < paths.size(); ii++) {
                    Path np = paths.get(ii);
                    if (np.hasPoint(connection.A()) || np.hasPoint(connection.B())) {
                        if (lp == null) {
                            paths.add(ii, connection);
                            added = true;
                        } else if (lp.hasPoint(connection.A()) || lp.hasPoint(connection.B())) {
                            paths.add(ii, connection);
                            added = true;
                        }
                    }
                    lp = np;
                }
                if (!added) {
                    throw new RuntimeException(String.format("Cannot add connection. [path=%s]", connection.toString()));
                }
            }
        } else
            paths.add(connection);
        return this;
    }

    public boolean exists(@NonNull Path path) {
        if (!paths.isEmpty()) {
            for (Path p : paths) {
                if (p.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void validate() {
        Path s = paths.get(0);
        Path e = paths.get(paths.size() - 1);
        if (isClosed) {
            if (!s.hasPoint(e.A()) && !s.hasPoint(e.B())) {
                throw new RuntimeException("Ring is marked as closed, but isn't closed...");
            }
        } else {
            if (paths.size() > 2 && (s.hasPoint(e.A()) || s.hasPoint(e.B()))) {
                throw new RuntimeException("Ring is marked as open, but is closed...");
            }
        }
    }

    public Point[] getPolygon() {
        if (isClosed && paths != null && !paths.isEmpty()) {
            Point[] array = new Point[paths.size() + 1];
            Path lastP = null;
            for (int ii = 0; ii < paths.size(); ii++) {
                Path p = paths.get(ii);
                if (lastP == null) {
                    array[0] = p.A();
                    array[1] = p.B();
                } else {
                    Point target = p.getTarget(lastP.A());
                    if (target == null) {
                        target = p.getTarget(lastP.B());
                    }
                    if (target == null && ii < paths.size() - 1) {
                        throw new RuntimeException(String.format("Invalid ring path : [path=%s]", p.toString()));
                    }
                    array[ii + 1] = target;
                }
                lastP = p;
            }
            return array;
        }
        return null;
    }

    protected boolean canConnect(Ring target) {
        if (number() == target.number()) return false;
        Ring ps = enclosing();
        Ring pt = target.enclosing();
        if (ps != null || pt != null) {
            boolean ret = false;
            if (ps != null && pt != null) {
                if (ps.number() == pt.number()) {
                    ret = true;
                } else if (ps.number() == target.number() || pt.number() == number()) {
                    ret = true;
                }
            }
            return ret;
        }
        return true;
    }

    public void computeConnections(@NonNull Connections connections, @NonNull TSPDataMap data, @NonNull List<Ring> rings) {
        throw new RuntimeException("Method should not be called...");
    }
}
