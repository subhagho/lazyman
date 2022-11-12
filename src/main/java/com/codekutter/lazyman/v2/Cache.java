package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public class Cache {
    private Map<String, Point> points;
    private List<String> index;
    private int size;

    public Cache init(int size) {
        Preconditions.checkArgument(size > 0);
        points = new LinkedHashMap<>(size);
        index = new ArrayList<>(size);
        for (int ii = 0; ii < size; ii++) {
            index.add(null);
        }
        this.size = size;
        return this;
    }

    public Point add(int sequence, Double X, Double Y) throws Exception {
        if (X == null) X = -1.0;
        if (Y == null) Y = -1.0;
        Point point = new Point(sequence, X, Y, size);
        String key = point.toString();
        if (points.containsKey(key)) {
            throw new Exception(String.format("Point already loaded. [point=%s]", point));
        }
        points.put(key, point);
        index.set(sequence, key);

        return point;
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

    public List<Point> copyPoints(int startIndex) {
        List<Point> pl = new ArrayList<>(index.size());
        for (int ii = 0; ii < index.size(); ii++) {
            int indx = ii + startIndex;
            if (indx >= index.size()) {
                indx -= index.size();
            }
            String key = index.get(indx);
            Point p = new Point(points.get(key));
            pl.add(p);
        }
        return pl;
    }

    public List<Point> pointList(int startIndex) {
        List<Point> pl = new ArrayList<>(index.size());
        for (int ii = 0; ii < index.size(); ii++) {
            int indx = ii + startIndex;
            if (indx >= index.size()) {
                indx -= index.size();
            }
            String key = index.get(indx);
            Point p = points.get(key);
            pl.add(p);
        }
        return pl;
    }

    public void postLoad() throws Exception {
        for (String key : index) {
            Point p = points.get(key);
            p.sort();
        }
    }
}
