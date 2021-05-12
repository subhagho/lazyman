package com.codekutter.salesman.core;

import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Bid;
import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import lombok.NonNull;

import java.util.Arrays;

public class RunIterator {
    private final TSPDataMap data;
    private final Connections connections;

    public RunIterator(@NonNull TSPDataMap data, @NonNull Connections connections) {
        this.data = data;
        this.connections = connections;
    }

    public void run(Connections.Connection previous, @NonNull Point point, int index) {
        Connections.Connection connection = connections.get(point, true);
        if (connection.isComplete()) {
            return;
        }

        Path[] paths = data.get(index);
        if (paths == null) {
            throw new IllegalArgumentException(String.format("No path data found for sequence. [sequence=%d]", index));
        }
        try {
            Arrays.sort(paths, new Path.SortByDistance());
            for (int ii = 0; ii < connection.connections().length; ii++) {
                if (connection.connections()[ii] != null) continue;
                Path path = reserve(point, connection, ii, paths, paths.length);
                if (path != null) {
                    Point t = path.getTarget(point);
                    addBid(point, path, t.elevation());
                    connections.add(path);
                } else {
                    throw new RuntimeException(String.format("Unable to reserve path. [point=%s]", point.toString()));
                }
            }
        } catch (Throwable t) {
            LogUtils.error(getClass(), t);
        }
    }

    public void checkOptimal(@NonNull Point point, int index) {
        Connections.Connection connection = connections.get(point, true);
        if (!connection.isComplete()) return;

        Path[] paths = data.get(index);
        if (paths == null) {
            throw new IllegalArgumentException(String.format("No path data found for sequence. [sequence=%d]", index));
        }
        Arrays.sort(paths, new Path.SortByDistance());
        int idx = (connection.connections()[0].distance() < connection.connections()[1].distance() ? 0 : 1);
        int stopIdx = findPathIndex(paths, connection.connections()[idx]);
        if (stopIdx < 0) throw new RuntimeException("Path Index not found...");
        Path path = reserve(point, connection, idx, paths, stopIdx);
        if (path != null) {
            Point t = path.getTarget(point);
            addBid(point, path, t.elevation());
            connections.remove(point, connection.connections()[idx]);
            connections.add(path);
        }
        idx = 1 - idx;
        stopIdx = findPathIndex(paths, connection.connections()[idx]);
        if (stopIdx < 0) throw new RuntimeException("Path Index not found...");
        path = reserve(point, connection, idx, paths, stopIdx);
        if (path != null) {
            Point t = path.getTarget(point);
            addBid(point, path, t.elevation());
            connections.remove(point, connection.connections()[idx]);
            connections.add(path);
        }
    }

    private double getMinDistance(Point point, Connections.Connection connection) {
        double[] dists = data.getMinDistances(point);
        if (connection.connections() == null
                || (connection.connections()[0] == null && connection.connections()[1] == null)) {
            return dists[0];
        } else {
            return (dists[0] + dists[1]);
        }
    }

    private int findPathIndex(Path[] paths, Path path) {
        for (int ii = 0; ii < paths.length; ii++) {
            if (paths[ii] == null) continue;
            if (paths[ii].equals(path)) {
                return ii;
            }
        }
        return -1;
    }

    private Path reserve(Point point, Connections.Connection connection, int index, Path[] paths, int stopIndex) {
        Bid bid = connections.bidHistory().get(point.hashKey());
        double mindist = getMinDistance(point, connection);
        double useddist = 0;
        if (connection.connections() != null) {
            if (index == 0 && connection.connections()[1] != null) {
                useddist = connection.connections()[1].distance();
            } else if (connection.connections()[0] != null) {
                useddist = connection.connections()[0].distance();
            }
        }
        for (int ii = 0; ii < stopIndex; ii++) {
            Path path = paths[ii];
            if (path == null) continue;
            Path pn = findNextValid(paths, point, connection, ii);
            if (pn == null || pn.actualLength() < 0) {
                continue;
            }
            double dist = pn.distance();
            if (!canUse(path, point, connection)) continue;
            Point target = path.getTarget(point);
            Connections.Connection tc = connections.get(target, true);
            int idx = -1;
            if (tc.connections()[0] == null) idx = 0;
            else if (tc.connections()[1] == null) idx = 1;
            if (idx >= 0) {
                return path;
            } else {
                double h = Math.pow((pn.distance() + useddist - mindist), 0.5f) + point.elevation();
                Path tp = findPathToReplace(tc, dist);
                if (tp == null) continue;
                //if (bid != null && !bid.shouldBid(path, h)) continue;
                connections.remove(target, tp);
                target.elevation(h);
                return path;
            }
        }
        return null;
    }

    private void addBid(Point point, Path path, double elevation) {
        Bid bid = connections.bidHistory().get(point.hashKey());
        if (bid == null) {
            bid = new Bid(point);
            connections.bidHistory().put(point.hashKey(), bid);
        }
        bid.add(path, elevation);
    }

    private Path findNextValid(Path[] paths, Point point, Connections.Connection connection, int index) {
        Path p = null;
        for (int ii = index + 1; ii < paths.length; ii++) {
            if (paths[ii] != null) {
                p = paths[ii];
                if (!canUse(p, point, connection)) continue;
                break;
            }
        }
        return p;
    }

    private Path findPathToReplace(Connections.Connection connection, double distance) {
        Path p = null;
        if (connection.connections()[0].length() >= 0 && connection.connections()[0].distance() < distance) {
            p = connection.connections()[0];
        }
        if (connection.connections()[1].length() >= 0 && connection.connections()[1].distance() < distance) {
            if (p != null && connection.connections()[1].distance() > p.distance()) {
                p = connection.connections()[1];
            }
        }
        return p;
    }

    private boolean canUse(Path path, Point point, Connections.Connection connection) {
        if (path.A().sequence() != point.sequence() && path.B().sequence() != point.sequence()) {
            return false;
        } else if (path.length() < 0) {
            return false;
        } else {
            if (connection.connections() != null) {
                for (Path p : connection.connections()) {
                    if (p != null) {
                        if (path.equals(p)) return false;
                    }
                }
            }
        }
        return true;
    }
}
