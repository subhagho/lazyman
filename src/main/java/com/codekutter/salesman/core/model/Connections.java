package com.codekutter.salesman.core.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public class Connections {
    @Getter
    @Setter
    @Accessors(fluent = true)
    @ToString
    public static class Connection {
        private final Point point;
        private Path[] connections = new Path[2];
        private short ring = -1;

        public Connection(@NonNull Point point) {
            this.point = point;
        }

        public int add(@NonNull Path path) {
            Preconditions.checkArgument(isValidPath(path));
            int idx = -1;
            if (connections[0] == null) {
                idx = 0;
            } else if (connections[1] == null) {
                idx = 1;
            }
            if (idx < 0) {
                throw new ArrayIndexOutOfBoundsException(String.format("No empty connection index found. [connection=%s]", toString()));
            }
            connections[idx] = path;
            return idx;
        }

        public Connection add(@NonNull Path path, int index) {
            Preconditions.checkArgument(isValidPath(path));
            Preconditions.checkArgument(connections[index] == null);
            connections[index] = path;
            return this;
        }

        public int remove(@NonNull Path path) {
            Preconditions.checkArgument(isValidPath(path));
            int idx = -1;
            if (connections[0] != null && connections[0].equals(path)) {
                connections[0] = null;
                idx = 0;
            } else if (connections[1] != null && connections[1].equals(path)) {
                connections[1] = null;
                idx = 1;
            }
            if (idx < 0) {
                throw new IllegalArgumentException(String.format("Connection not found. [path=%s]", path.toString()));
            }
            return idx;
        }

        public boolean isValidPath(@NonNull Path path) {
            if (path.A().equals(point) || path.B().equals(point)) {
                return true;
            }
            return false;
        }

        public boolean isComplete() {
            return (connections[0] != null && connections[1] != null);
        }

        public Path hasSequence(int sequence) {
            if (sequence != point.sequence()) {
                for (int ii = 0; ii < connections.length; ii++) {
                    if (connections[ii] != null) {
                        if (connections[ii].A().sequence() == sequence || connections[ii].B().sequence() == sequence) {
                            return connections[ii];
                        }
                    }
                }
            }
            return null;
        }
    }

    private final int size;
    private Map<Point, Connection> connections = new HashMap<>();

    public Connections(int size) {
        this.size = size;
    }

    public Connection get(@NonNull Point point) {
        return get(point, false);
    }

    public Connection get(@NonNull Point point, boolean create) {
        if (create) {
            if (!connections.containsKey(point)) {
                connections.put(point, new Connection(point));
            }
        }
        return connections.get(point);
    }

    public Connections replace(@NonNull Path newp, @NonNull Path oldp) {
        remove(oldp);
        add(newp);
        return this;
    }


    public Connections add(@NonNull Path path) {
        add(path.A(), path);
        add(path.B(), path);
        return this;
    }

    private void add(Point p, Path path) throws ArrayIndexOutOfBoundsException {
        Connection pa = connections.get(p);
        if (pa == null) {
            pa = new Connection(p);
            connections.put(p, pa);
        }
        pa.add(path);
    }

    public Connections remove(@NonNull Path path) {
        remove(path.A(), path);
        remove(path.B(), path);
        return this;
    }

    public void remove(Point p, Path path) throws IllegalArgumentException {
        Connection pa = connections.get(p);
        if (pa == null) {
            throw new IllegalArgumentException(String.format("Point doesn't have any connections. [point=%s]", p.toString()));
        }
        pa.remove(path);
    }

    public boolean reachedClosure() {
        for (Point p : connections.keySet()) {
            Connection c = connections.get(p);
            if (!c.isComplete()) return false;
        }
        return true;
    }
}
