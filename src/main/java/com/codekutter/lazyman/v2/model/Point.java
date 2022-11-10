package com.codekutter.lazyman.v2.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Setter
@Accessors(fluent = true)
public class Point {
    private int sequence;
    private Double X;
    private Double Y;
    private double elevation = 0;
    private double delta = 0;
    private double minConnectionDistance = 0;
    private Map<Integer, Path> paths;
    private List<Integer> sortIndex;
    private Path[] connections = new Path[2];

    public Point(int sequence,
                 @NonNull Double X,
                 @NonNull Double Y,
                 int size) {
        Preconditions.checkArgument(sequence >= 0);
        this.sequence = sequence;
        this.X = X;
        this.Y = Y;
        paths = new LinkedHashMap<>(size);
        connections[0] = null;
        connections[1] = null;
    }

    public void add(@NonNull Path path) throws Exception {
        Point t = path.target(this);
        paths.put(t.sequence, path);
    }

    public void connect(@NonNull Path path) throws Exception {
        if (connections[0] != null) {
            connections[0] = path;
        } else if (connections[1] == null) {
            connections[1] = path;
        } else {
            throw new Exception(String.format("[%s] No empty connection slot found.", this));
        }
        if (connections[0] != null && connections[1] != null) {
            double d1 = connections[0].compute();
            double d2 = connections[1].compute();
            delta = (d1 + d2) - minConnectionDistance;
        }
    }

    public boolean hasPath(@NonNull Point point) {
        return paths.containsKey(point.sequence);
    }

    public boolean isConnected() {
        return (connections[0] != null && connections[1] != null);
    }

    public Point nextConnection(@NonNull Point point) throws Exception {
        boolean connected = false;
        Point next = null;
        if (connections[0] != null) {
            Point t = connections[0].target(this);
            if (t.equals(point)) {
                connected = true;
            } else {
                next = t;
            }
        }
        if (connections[1] != null) {
            Point t = connections[1].target(this);
            if (t.equals(point)) {
                connected = true;
            } else {
                next = t;
            }
        }
        if (!connected) {
            throw new Exception(String.format("Not connected to point. [point=%s]", point));
        }
        return next;
    }

    public boolean isConnectedTo(@NonNull Path path) {
        if (connections[0] != null &&
                connections[0].equals(path)) {
            return true;
        } else return connections[1] != null &&
                connections[1].equals(path);
    }

    public Path path(int sequence) {
        return paths.get(sequence);
    }

    public Path next(int index) {
        while (index < paths.size()) {
            index++;
            int key = sortIndex.get(index);
            Path p = paths.get(key);
            if (!isConnectedTo(p)) {
                return p;
            }
        }
        return null;
    }

    public void disconnect(@NonNull Point target) throws Exception {
        if (connections[0] != null) {
            Point t = connections[0].target(this);
            if (t.equals(target)) {
                connections[0] = null;
                return;
            }
        } else if (connections[1] != null) {
            Point t = connections[1].target(this);
            if (t.equals(target)) {
                connections[1] = null;
                return;
            }
        }
        throw new Exception(String.format("Not connected to point. [point=%s]", target));
    }

    public boolean canConnect() {
        return (connections[0] == null || connections[1] == null);
    }

    public void sort() throws Exception {

        List<Path> ps = new ArrayList<>(paths.values());
        Collections.sort(ps);
        if (sortIndex == null) {
            sortIndex = new ArrayList<>(ps.size());
        } else {
            sortIndex.clear();
        }
        for (Path p : ps) {
            Point t = p.target(this);
            sortIndex.add(t.sequence);
        }
        int i1 = sortIndex.get(0);
        int i2 = sortIndex.get(1);
        minConnectionDistance = paths.get(i1).actualLength() + paths.get(i2).actualLength();
    }

    public double height(@NonNull Point B) {
        return elevation - B.elevation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return (sequence == point.sequence) && X.equals(point.X) && Y.equals(point.Y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequence, X, Y);
    }

    @Override
    public String toString() {
        return "Point{" +
                "sequence=" + sequence +
                ", X=" + X +
                ", Y=" + Y +
                '}';
    }
}
