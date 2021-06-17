package com.codekutter.salesman.core.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
@ToString
public class Connections {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class ConnectionPath {
        private Path path;
        private boolean biddable = true;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    @ToString
    public static class Connection {
        private final Point point;
        private ConnectionPath[] connections = new ConnectionPath[2];

        public Connection(@NonNull Point point) {
            this.point = point;
        }

        public int add(@NonNull Path path) {
            return add(path, true);
        }

        public int add(@NonNull Path path, boolean biddable) {
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
            ConnectionPath cp = new ConnectionPath();
            cp.path = path;
            cp.biddable = biddable;
            connections[idx] = cp;
            return idx;
        }

        public Connection add(@NonNull Path path, int index, boolean biddable) {
            Preconditions.checkArgument(isValidPath(path));
            Preconditions.checkArgument(connections[index] == null);
            ConnectionPath cp = new ConnectionPath();
            cp.path = path;
            cp.biddable = biddable;
            connections[index] = cp;
            return this;
        }

        public int remove(@NonNull Path path) {
            Preconditions.checkArgument(isValidPath(path));
            int idx = -1;
            if (connections[0] != null && connections[0].path.equals(path)) {
                connections[0] = null;
                idx = 0;
            } else if (connections[1] != null && connections[1].path.equals(path)) {
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
                        if (connections[ii].path.A().sequence() == sequence || connections[ii].path.B().sequence() == sequence) {
                            return connections[ii].path;
                        }
                    }
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Connection that = (Connection) o;
            return point.equals(that.point) && Arrays.equals(connections, that.connections);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(point);
            result = 31 * result + Arrays.hashCode(connections);
            return result;
        }
    }

    private final int size;
    private Map<String, Connection> connections = new HashMap<>();

    public Connections(int size) {
        this.size = size;
    }

    public Connection get(@NonNull Point point) {
        return get(point, false);
    }

    public Connection get(@NonNull Point point, boolean create) {
        if (create) {
            if (!connections.containsKey(point.hashKey())) {
                connections.put(point.hashKey(), new Connection(point));
            }
        }
        return connections.get(point.hashKey());
    }

    public Connections replace(@NonNull Path newp, @NonNull Path oldp) {
        remove(oldp);
        add(newp);
        return this;
    }

    public Connections add(@NonNull Path path) {
        return add(path, true);
    }

    public Connections add(@NonNull Path path, boolean biddable) {
        add(path.A(), path, biddable);
        add(path.B(), path, biddable);
        return this;
    }

    private void add(Point p, Path path, boolean biddable) throws ArrayIndexOutOfBoundsException {
        Connection pa = connections.get(p.hashKey());
        if (pa == null) {
            pa = new Connection(p);
            connections.put(p.hashKey(), pa);
        }
        pa.add(path, biddable);
    }

    public Connections remove(@NonNull Path path) {
        remove(path.A(), path);
        //remove(path.B(), path);
        return this;
    }

    public boolean hasPath(@NonNull Path path) {
        Point a = path.A();
        if (connections.containsKey(a.hashKey())) {
            Connection c = connections.get(a.hashKey());
            if (c.connections != null) {
                if (c.connections[0] != null && c.connections[0].path.equals(path)) {
                    return true;
                } else if (c.connections[1] != null && c.connections[1].path.equals(path)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void remove(Point p, Path path) throws IllegalArgumentException {
        Connection pa = connections.get(p.hashKey());
        if (pa == null) {
            throw new IllegalArgumentException(String.format("Point doesn't have any connections. [point=%s]", p.toString()));
        }
        pa.remove(path);
        Point tp = path.getTarget(p);
        Connection tc = connections.get(tp.hashKey());
        if (tc == null) {
            throw new IllegalArgumentException(String.format("Point doesn't have any connections. [point=%s]", tp.toString()));
        }
        tc.remove(path);
    }

    public boolean reachedClosure() {
        for (String key : connections.keySet()) {
            Connection c = connections.get(key);
            if (!c.isComplete()) return false;
        }
        return true;
    }

    public boolean isIdentical(@NonNull Connections copy) {
        for (String key : connections.keySet()) {
            Connection c1 = connections.get(key);
            if (!copy.connections.containsKey(key)) {
                return false;
            }
            Connection c2 = copy.connections.get(key);
            if (c1 != null) {
                if (!c1.equals(c2)) {
                    return false;
                }
            } else if (c2 != null) {
                return false;
            }
        }
        return true;
    }

    public Connections copy() {
        Connections connections = new Connections(size);
        for (String key : this.connections.keySet()) {
            connections.connections.put(key, this.connections.get(key));
        }
        return connections;
    }
}
