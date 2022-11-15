package com.codekutter.lazyman.v2.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class PathRoute {
    private int size;
    private double length;
    private LinkedList<Point> route = new LinkedList<>();
    private Point start;

    public void add(@NonNull Point point) {
        if (start == null) {
            start = point;
        } else {
            Point last = route.getLast();
            Path p = last.path(point.sequence());
            length += p.actualLength();
        }
        route.add(point);
        size++;
    }

    public Point[] next(Point start) {
        Point[] pair = new Point[2];
        int index = 0;
        if (start != null) {
            index = index(start);
        }
        if (index >= 0) {
            index = index + 1;
            if (index < route.size()) {
                pair[0] = route.get(index);
                if (index + 1 < size) {
                    pair[1] = route.get(index + 1);
                }
            }
        }
        return pair;
    }

    private int index(Point point) {
        for (int ii = 0; ii < route.size(); ii++) {
            if (route.get(ii).equals(point)) {
                return ii;
            }
        }
        return -1;
    }
}
