package com.codekutter.salesman.core.model;

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
    private final short number;
    private List<Path> ring = new ArrayList<>();
    private boolean isClosed = true;
    private short level = 0;
    private Ring enclosing = null;

    public Ring(short number) {
        this.number = number;
    }

    public Ring add(@NonNull Path connection) {
        ring.add(connection);
        return this;
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
