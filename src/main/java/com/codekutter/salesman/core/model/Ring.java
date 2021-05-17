package com.codekutter.salesman.core.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class Ring {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class RingConnection {
        private short number;
        private Path[] paths;
    }

    private final short number;
    private List<Path> ring = new ArrayList<>();
    private boolean isClosed = true;
    private short level = 0;
    private Ring enclosing = null;
    private RingConnection[] connections;

    public Ring(short number) {
        this.number = number;
    }

    public Ring add(@NonNull Path connection) {
        ring.add(connection);
        return this;
    }

    public void initConnections(int size) {
        connections = new RingConnection[size];
    }

    public void addConnection(short ring, @NonNull Path[] paths) {
        Preconditions.checkState(connections != null);
        Preconditions.checkArgument(ring < connections.length);

        if (connections[ring] == null) {
            connections[ring] = new RingConnection();
            connections[ring].number = ring;
        }
        connections[ring].paths = paths;
    }

    public Path[] findRingConnection(@NonNull Point point) {
        if (connections != null) {
            for (int ii = 0; ii < connections.length; ii++) {
                if (connections[ii] != null && connections[ii].paths != null) {
                    for (Path p : connections[ii].paths) {
                        if (p != null) {
                            if (p.hasPoint(point)) {
                                return connections[ii].paths;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public Point[] getPolygon() {
        if (isClosed && ring != null && !ring.isEmpty()) {
            Point[] array = new Point[ring.size() + 1];
            Path lastP = null;
            for (int ii = 0; ii < ring.size(); ii++) {
                Path p = ring.get(ii);
                if (lastP == null) {
                    array[0] = p.A();
                    array[1] = p.B();
                } else {
                    Point target = p.getTarget(lastP.A());
                    if (target == null) {
                        target = p.getTarget(lastP.B());
                    }
                    if (target == null && ii < ring.size() - 1) {
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
}
