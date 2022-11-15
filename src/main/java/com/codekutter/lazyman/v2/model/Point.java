package com.codekutter.lazyman.v2.model;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Accessors(fluent = true)
public class Point {
    private final int sequence;
    private final Double X;
    private final Double Y;
    private double elevation = 0;
    private double delta = 0;
    private double minConnectionDistance = 0;
    private double minLength = 0;
    private final Map<Integer, Path> paths;
    private List<Integer> sortIndex;
    private final Path[] connections = new Path[2];
    private int connectCount = 0;
    private int chainLength = 0;

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

    public Point(@NonNull Point point) {
        sequence = point.sequence();
        X = point.X;
        Y = point.Y;
        elevation = point.elevation;
        delta = point.delta;
        minConnectionDistance = point.minConnectionDistance();
        minLength = point.minLength();
        paths = point.paths;
        sortIndex = point.sortIndex;
        connections[0] = point.connections[0];
        connections[1] = point.connections[1];
    }

    public void add(@NonNull Path path) throws Exception {
        Point t = path.target(this);
        paths.put(t.sequence, path);
    }

    public void clearConnections() {
        connections[0] = null;
        connections[1] = null;
        connectCount = 0;
    }

    public void connect(@NonNull Path path) throws Exception {
        if (hasConnection(path)) return;
        if (connections[0] == null) {
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
        connectCount++;
        updateChain(0);
    }

    private void updateChain(int value) throws Exception {
        Set<Integer> visited = new LinkedHashSet<>();
        chainLength = connectionCount(this, visited) + value;
        visited.clear();
        updateChain(this, chainLength, visited);
    }

    private void updateChain(Point source, int value, Set<Integer> visited) throws Exception {
        if (visited.contains(sequence)) {
            throw new Exception(String.format("Point already there [%s] [%d]", visited, sequence));
        }
        visited.add(sequence);
        if (connections[0] != null) {
            Point t = connections[0].target(this);
            if (!t.equals(source) && !visited.contains(t.sequence)) {
                t.updateChain(this, value, visited);
            }
        }
        if (connections[1] != null) {
            Point t = connections[1].target(this);
            if (!t.equals(source) && !visited.contains(t.sequence)) {
                t.updateChain(this, value, visited);
            }
        }
        chainLength = value;
    }

    private int connectionCount(Point source, Set<Integer> visited) throws Exception {
        if (visited.contains(sequence)) {
            throw new Exception(String.format("Point already there [%s] [%d]", visited, sequence));
        }
        visited.add(sequence);
        int c = 0;
        if (connections[0] != null) {
            Point t = connections[0].target(this);
            if (!t.equals(source) && !visited.contains(t.sequence)) {
                c += t.connectionCount(this, visited);
                c++;
            }
        }
        if (connections[1] != null) {
            Point t = connections[1].target(this);
            if (!t.equals(source) && !visited.contains(t.sequence)) {
                c += t.connectionCount(this, visited);
                c++;
            }
        }
        return c;
    }

    public boolean hasPath(@NonNull Point point) {
        return paths.containsKey(point.sequence);
    }

    public Point delta(double delta) {
        this.delta = delta;
        return this;
    }

    public Point elevation(double elevation) {
        this.elevation = elevation;
        return this;
    }

    public Path longest() {
        Path p1 = connections[0];
        if (p1 == null) {
            return connections[1];
        }
        Path p2 = connections[1];
        if (p2 == null) return p1;
        if (p1.actualLength() > p2.actualLength()) {
            return p1;
        } else {
            return p2;
        }
    }

    public boolean isConnected() {
        return (connections[0] != null && connections[1] != null);
    }

    public Path nextConnection(@NonNull Path path) {
        if (connections[0] != null && !connections[0].equals(path)) {
            return connections[0];
        } else if (connections[1] != null && !connections[1].equals(path)) {
            return connections[1];
        }
        return null;
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
        return hasConnection(path);
    }

    public Path isConnectedTo(@NonNull Point point) {
        Path path = paths.get(point.sequence);
        if (isConnectedTo(path)) {
            return path;
        }
        return null;
    }

    public Path path(int sequence) {
        return paths.get(sequence);
    }

    public double distance(@NonNull Point target) {
        double x = X - target.X;
        double y = Y - target.Y;
        return Math.sqrt((x * x) + (y * y));
    }

    public IndexedPath next(int index) {
        IndexedPath ip = null;
        while (true) {
            index++;
            if (index >= sortIndex.size()) break;

            int key = sortIndex.get(index);
            Path p = paths.get(key);
            if (!isConnectedTo(p)) {
                if (ip == null) {
                    ip = new IndexedPath().index(index).path(p);
                } else {
                    ip.next(p);
                    break;
                }
            }
        }
        return ip;
    }

    public double computeDelta(@NonNull Path path) throws Exception {
        Point t = path.target(this);
        double d = path.actualLength();
        Path curr = null;
        if (connections[0] != null) {
            curr = connections[0];
        } else if (connections[1] != null) {
            curr = connections[1];
        }
        if (curr == null) {
            d -= minLength;
            d += minConnectionDistance;
        } else {
            double d1 = curr.actualLength();
            d += d1;
        }
        return d - minConnectionDistance;
    }

    public void disconnect(@NonNull Point target) throws Exception {
        if (connections[0] != null) {
            Point t = connections[0].target(this);
            if (t.equals(target)) {
                t.updateChain(-1);
                connections[0] = null;
                connectCount--;
                updateChain(-1);
                return;
            }
        }
        if (connections[1] != null) {
            Point t = connections[1].target(this);
            if (t.equals(target)) {
                t.updateChain(-1);
                connections[1] = null;
                connectCount--;
                updateChain(-1);
                return;
            }
        }
        throw new Exception(String.format("Not connected to point. [point=%s]", target));
    }

    public boolean canConnect() {
        return (connections[0] == null || connections[1] == null) && (connectCount < 2);
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

    public boolean isEqual(@NonNull Point target) {
        if (equals(target)) {
            boolean ret = false;
            if (connections[0] == null && connections[1] == null) {
                if (target.connections[0] == null && target.connections[1] == null) {
                    ret = true;
                }
            } else {
                if (hasConnection(target.connections[0]) &&
                        hasConnection(target.connections[1])) {
                    ret = true;
                }
            }
            return ret;
        }
        return false;
    }

    public boolean hasConnection(Path path) {
        if (path == null) {
            return connections[0] == null || connections[1] == null;
        } else {
            if (connections[0] != null) {
                if (connections[0].equals(path)) return true;
            }
            if (connections[1] != null) {
                return connections[1].equals(path);
            }
        }
        return false;
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
        return String.format("{%d}[%4.2f](%d)", sequence, elevation, chainLength);
    }

    public int compare(@NonNull Point other) {
        double ret = X - other.X;
        if (ret == 0) {
            ret = Y - other.Y;
        }
        if (ret < -1 || ret > 1) return (int) ret;
        else if (ret < 0) return -1;
        else if (ret > 0) return 1;
        else return 0;
    }

    public String hashKey() {
        return String.format("%d (%f, %f)", sequence, X, Y);
    }
}
