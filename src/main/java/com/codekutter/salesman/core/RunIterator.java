package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import lombok.NonNull;

public class RunIterator {
    private final TSPDataMap data;
    private final Connections connections;

    public RunIterator(@NonNull TSPDataMap data, @NonNull Connections connections) {
        this.data = data;
        this.connections = connections;
    }

    public void run(int iteration, @NonNull Point point, int index) {
        Connections.Connection connection = connections.get(point, true);
        if (connection.isComplete()) {
            return;
        }

        Path[] paths = data.get(index);
        if (paths == null) {
            throw new IllegalArgumentException(String.format("No path data found for sequence. [sequence=%d]", index));
        }
        for (int ii = 0; ii < connection.connections().length; ii++) {
            if (connection.connections()[ii] != null) continue;
            Path path = reserve(point, connection, paths);
            if (path != null) {
                connections.add(path);
            }
        }
    }

    private Path reserve(Point point, Connections.Connection connection, Path[] paths) {
        for (int ii = 0; ii < paths.length; ii++) {
            Path path = paths[ii];
            if (path == null) continue;

            if (!canUse(path, point, connection)) continue;
            Point target = getTarget(point, path);
            Connections.Connection tc = connections.get(target, true);
            int idx = -1;
            if (tc.connections()[0] == null) idx = 0;
            else if (tc.connections()[1] == null) idx = 1;
            if (idx >= 0) {
                return path;
            } else {
                Path pn = findNextValid(paths, ii);
                if (pn == null) {
                    continue;
                }

                double dist = pn.distance();
                Path tp = findPathToReplace(tc, dist);
                if (tp == null) continue;
                double h = Math.sqrt(Math.pow(dist, 2) - Math.pow(path.length(), 2));
                connections.remove(target, path);
                target.elevation(h);
                return path;
            }
        }
        return null;
    }

    private Path findNextValid(Path[] paths, int index) {
        Path p = null;
        for (int ii = index + 1; ii < paths.length; ii++) {
            if (paths[ii] != null) {
                p = paths[ii];
                break;
            }
        }
        return p;
    }

    private Path findPathToReplace(Connections.Connection connection, double distance) {
        Path p = null;
        if (connection.connections()[0].distance() < distance) {
            p = connection.connections()[0];
        }
        if (connection.connections()[1].distance() < distance) {
            if (p != null && connection.connections()[1].distance() > p.distance()) {
                p = connection.connections()[1];
            }
        }
        return p;
    }

    private Point getTarget(Point point, Path path) {
        if (path.A().equals(point)) {
            return path.B();
        } else {
            return path.A();
        }
    }

    private boolean canUse(Path path, Point point, Connections.Connection connection) {
        if (path.A().sequence() != point.sequence() && path.B().sequence() != point.sequence()) {
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
