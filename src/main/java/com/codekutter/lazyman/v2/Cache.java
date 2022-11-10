package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cache {
    private Map<String, Point> points;
    private List<String> index;

    public Cache init(int size) {
        Preconditions.checkArgument(size > 0);
        points = new LinkedHashMap<>(size);
        index = new ArrayList<>(size);
        return this;
    }

    public void add(int sequence, Double X, Double Y) throws Exception {
        if (X == null) X = -1.0;
        if (Y == null) Y = -1.0;
        Point point = new Point(sequence, X, Y, points.size() - 1);
        String key = point.toString();
        if (points.containsKey(key)) {
            throw new Exception(String.format("Point already loaded. [point=%s]", point));
        }
        points.put(key, point);
        index.set(sequence, key);
    }

    public Point get(int index) {
        Preconditions.checkArgument(index >= 0 && index < this.index.size());
        return points.get(this.index.get(index));
    }

    public Path add(int seq1, int seq2, double distance) throws Exception {
        Point p1 = get(seq1);
        Preconditions.checkNotNull(p1);
        Point p2 = get(seq2);
        Preconditions.checkNotNull(p2);
        Path p = new Path(p1, p2);
        p.actualLength(distance);
        if (!p1.hasPath(p2)) {
            p1.add(p);
        }
        if (!p2.hasPath(p1)) {
            p2.add(p);
        }
        return p;
    }

    public Path get(int seq1, int seq2) {
        Point p1 = get(seq1);
        Preconditions.checkNotNull(p1);
        return p1.path(seq2);
    }

    public void postLoad() throws Exception {
        for (String key : index) {
            Point p = points.get(key);
            p.sort();
        }
    }
}
