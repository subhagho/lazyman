package com.codekutter.lazyman.model;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;

@Getter
@Setter
@Accessors(fluent = true)
public class PointVertex {
    private final Point2D point;
    private final String key;

    private double Z = 0;
    @Setter(AccessLevel.NONE)
    private Path2D[] minPaths = new Path2D[2];
    @Setter(AccessLevel.NONE)
    private double minDistance = 0;

    private Connection[] connections = new Connection[2];

    public PointVertex(@NonNull Point2D point) {
        this.point = point;
        this.key = point().key();
    }

    public double minPaths(@NonNull Connection pa, @NonNull Connection pb) {
        Preconditions.checkArgument(pa.path().hasPoint(point));
        Preconditions.checkArgument(pb.path().hasPoint(point));
        minPaths[0] = pa.path();
        minPaths[1] = pb.path();
        minDistance = pa.path().length() + pb.path().length();

        connections[0] = pa;
        connections[1] = pb;

        return minDistance;
    }

    public void addConnection(@NonNull Connection connection, Connection replace) {
        if (replace != null) {
            if (connections[0] != null && connections[0].equals(replace)) {
                connections[0] = connection;
            } else if (connections[1] != null && connections[1].equals(replace)) {
                connections[1] = connection;
            } else {
                throw new IllegalArgumentException(String.format("Path to replace not found. [point=%s][replace=%s]", point.toString(), replace.toString()));
            }
        } else {
            if (connections[0] == null) {
                connections[0] = connection;
            } else if (connections[1] == null) {
                connections[1] = connection;
            } else {
                throw new IllegalArgumentException(String.format("No empty connections and connection to replace not specified. [point=%s]", point.toString()));
            }
        }
    }

    public boolean removeConnection(@NonNull Connection connection) {
        boolean ret = false;
        if (connections[0] != null && connections[0].equals(connection)) {
            connections[0] = null;
            ret = true;
        } else if (connections[1] != null && connections[1].equals(connection)) {
            connections[1] = null;
            ret = true;
        }
        return ret;
    }

    public boolean removeConnection(@NonNull Path2D path) {
        boolean ret = false;
        if (connections[0] != null && connections[0].path().equals(path)) {
            connections[0] = null;
            ret = true;
        } else if (connections[1] != null && connections[1].path().equals(path)) {
            connections[1] = null;
            ret = true;
        }
        return ret;
    }

    public boolean removeConnection(@NonNull Point2D point) {
        Preconditions.checkArgument(!this.point.equals(point));
        boolean ret = false;
        if (connections[0] != null && connections[0].path().hasPoint(point)) {
            connections[0] = null;
            ret = true;
        } else if (connections[1] != null && connections[1].path().hasPoint(point)) {
            connections[1] = null;
            ret = true;
        }
        return ret;
    }

    public double distanceDelta() {
        double d = 0;
        if (connections[0] != null && connections[1] != null) {
            double dd = connections[0].distance() + connections[1].distance();
            d = dd - minDistance;
        }
        return d;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * <p>
     * The {@code equals} method implements an equivalence relation
     * on non-null object references:
     * <ul>
     * <li>It is <i>reflexive</i>: for any non-null reference value
     *     {@code x}, {@code x.equals(x)} should return
     *     {@code true}.
     * <li>It is <i>symmetric</i>: for any non-null reference values
     *     {@code x} and {@code y}, {@code x.equals(y)}
     *     should return {@code true} if and only if
     *     {@code y.equals(x)} returns {@code true}.
     * <li>It is <i>transitive</i>: for any non-null reference values
     *     {@code x}, {@code y}, and {@code z}, if
     *     {@code x.equals(y)} returns {@code true} and
     *     {@code y.equals(z)} returns {@code true}, then
     *     {@code x.equals(z)} should return {@code true}.
     * <li>It is <i>consistent</i>: for any non-null reference values
     *     {@code x} and {@code y}, multiple invocations of
     *     {@code x.equals(y)} consistently return {@code true}
     *     or consistently return {@code false}, provided no
     *     information used in {@code equals} comparisons on the
     *     objects is modified.
     * <li>For any non-null reference value {@code x},
     *     {@code x.equals(null)} should return {@code false}.
     * </ul>
     * <p>
     * The {@code equals} method for class {@code Object} implements
     * the most discriminating possible equivalence relation on objects;
     * that is, for any non-null reference values {@code x} and
     * {@code y}, this method returns {@code true} if and only
     * if {@code x} and {@code y} refer to the same object
     * ({@code x == y} has the value {@code true}).
     * <p>
     * Note that it is generally necessary to override the {@code hashCode}
     * method whenever this method is overridden, so as to maintain the
     * general contract for the {@code hashCode} method, which states
     * that equal objects must have equal hash codes.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     * @see #hashCode()
     * @see HashMap
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PointVertex) {
            PointVertex pv = (PointVertex) obj;
            return key.equals(pv.key);
        }
        return super.equals(obj);
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return String.format("%s, Z=%f", point.toString(), Z);
    }
}
