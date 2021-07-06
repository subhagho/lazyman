package com.codekutter.lazyman.core.model;

import com.codekutter.lazyman.core.TSPDataMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Setter
@Accessors(fluent = true)
public class Ring {

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

    public List<Point> getPoints() {
        List<Point> points = new ArrayList<>();
        Map<Integer, Point> added = new HashMap<>();
        for (Path path : paths) {
            if (!added.containsKey(path.A().sequence())) {
                points.add(path.A());
                added.put(path.A().sequence(), path.A());
            }
            if (!added.containsKey(path.B().sequence())) {
                points.add(path.B());
                added.put(path.B().sequence(), path.B());
            }
        }
        return points;
    }
}
