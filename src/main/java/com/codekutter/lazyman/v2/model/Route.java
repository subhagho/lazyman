package com.codekutter.lazyman.v2.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Accessors(fluent = true)
public class Route {
    private static class Bid implements Comparable<Bid> {
        Point p1;
        Path path1;
        Point p2;
        Path path2;
        double delta;

        @Override
        public int compareTo(Bid o) {
            double d = delta - o.delta;
            int ret = 0;
            if (d < -1 || d > 1) {
                ret = (int) d;
            } else if (d < 0) {
                ret = -1;
            } else if (d > 0) {
                ret = 1;
            }
            return ret;
        }
    }

    private final LinkedList<Point> points;
    private final Map<Integer, Point> map;
    private final Path[] connections = new Path[2];
    private Bid bid = null;

    public Route(@NonNull LinkedList<Point> points) {
        this.points = points;
        this.map = new HashMap<>(points.size());
        for (Point point : points) {
            map.put(point.sequence(), point);
        }
    }

    public boolean hasPoint(@NonNull Point point) {
        return map.containsKey(point.sequence());
    }

    public boolean hasConnection(@NonNull Path path) {
        if (connections[0] != null && connections[0].equals(path)) {
            return true;
        }
        return connections[1] != null && connections[1].equals(path);
    }

    public void connect() throws Exception {
        int index = 0;
        Point start = points.get(index);
        Point current = start;
        List<Bid> bids = new ArrayList<>();
        while (index < points.size() - 1) {
            Point next = points.get(index + 1);
            Bid b = check(current, next);
            if (b != null) {
                bids.add(b);
            }
            b = check(next, current);
            if (b != null) {
                bids.add(b);
            }
            current = next;
            index++;
        }
        if (!bids.isEmpty()) {
            Collections.sort(bids);
            bid = bids.get(0);
            connections[0] = bid.path1;
            connections[1] = bid.path2;
        }
    }

    private Bid check(Point p1, Point p2) throws Exception {
        Path path1 = forPoint(p1, 0, null);
        if (path1 != null) {
            Point t1 = path1.target(p1);
            Path path2 = forPoint(p2, 0, t1);
            if (path2 != null) {
                Bid bid = new Bid();
                Path cp = p1.path(p2.sequence());
                bid.p1 = p1;
                bid.path1 = path1;
                bid.p2 = p2;
                bid.path2 = path2;
                bid.delta = (path1.actualLength() + path2.actualLength()) - cp.actualLength();

                return bid;
            }
        }
        return null;
    }

    private Path forPoint(Point point, int index, Point used) throws Exception {
        for (int ii = index; ii < point.sortIndex().size(); ii++) {
            int key = point.sortIndex().get(ii);
            Path p = point.path(key);
            Point t = p.target(point);
            if (!map.containsKey(t.sequence())) {
                if (used == null || !used.equals(t))
                    return p;
            }
        }
        return null;
    }

    public void removeConnection(@NonNull Path path) throws Exception {
        if (connections[0] != null) {
            if (connections[0].equals(path)) {
                connections[0] = null;
                return;
            }
        }
        if (connections[1] != null) {
            if (connections[1].equals(path)) {
                connections[1] = null;
                return;
            }
        }
        throw new Exception(String.format("Not connected to path. [path=%s]", path));
    }

    public void connectTo(@NonNull Path path) throws Exception {
        if (connections[0] == null) {
            connections[0] = path;
        } else if (connections[1] != null) {
            connections[1] = path;
        } else {
            throw new Exception("No empty connection slot available...");
        }
    }

    public boolean isConnected() {
        return (connections[0] != null && connections[1] != null);
    }
}
