package com.codekutter.lazyman.core;

import com.codekutter.lazyman.model.Connection;
import com.codekutter.lazyman.model.Path2D;
import com.codekutter.lazyman.model.Point2D;
import com.codekutter.lazyman.model.PointVertex;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

public class DataCache implements Closeable {
    final int recordCount;
    private final Map<String, Point2D> points;
    private final Map<String, Path2D> paths;

    private final Map<String, List<Path2D>> pointPaths;
    private final Map<String, PointVertex> vertices;

    public DataCache(int size) {
        recordCount = size;
        points = new HashMap<>(recordCount);
        paths = new HashMap<>(recordCount * recordCount);
        pointPaths = new HashMap<>(recordCount);
        vertices = new HashMap<>(recordCount);
    }

    public boolean containsPoint(@NonNull Point2D point) {
        String key = point.key();
        return points.containsKey(key);
    }

    public DataCache addPoint(@NonNull Point2D point) {
        String key = point.key();
        points.put(key, point);
        return this;
    }

    public Point2D point(String key) {
        return points.get(key);
    }

    public Collection<Point2D> points() {
        return points.values();
    }

    public DataCache addPath(@NonNull Path2D path) {
        String key = Path2D.pathKey(path);
        Point2D a = path.A();
        Point2D b = path.B();

        if (!containsPoint(a)) {
            addPoint(a);
        }
        if (!containsPoint(b)) {
            addPoint(b);
        }

        paths.put(key, path);
        addPointPath(a, path);
        addPointPath(b, path);

        return this;
    }

    public DataCache addPointPath(@NonNull Point2D point, @NonNull Path2D path) {
        Preconditions.checkArgument(path.hasPoint(point));
        if (!pointPaths.containsKey(point.key())) {
            pointPaths.put(point.key(), new ArrayList<>());
        }
        Point2D op = path.getTarget(point);
        Preconditions.checkState(op != null);
        if (getPath(point, op) == null) {
            List<Path2D> paths = pointPaths.get(point.key());
            paths.add(path);
        }
        return this;
    }

    public Path2D getPath(@NonNull Point2D a, @NonNull Point2D b) {
        if (pointPaths.containsKey(a.key())) {
            List<Path2D> paths = pointPaths.get(a.key());
            if (paths != null) {
                for (Path2D p : paths) {
                    if (p.hasPoint(b)) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public Collection<Path2D> getPaths(@NonNull Point2D point) {
        if (pointPaths.containsKey(point.key())) {
            return pointPaths.get(point.key());
        }
        return null;
    }

    public DataCache postLoad() {
        for (String key : pointPaths.keySet()) {
            List<Path2D> paths = pointPaths.get(key);
            paths.sort(new Path2D.SortByDistance());
        }
        for (String pk : points.keySet()) {
            Point2D p = points.get(pk);
            PointVertex pv = getVertex(p);

            List<Path2D> paths = pointPaths.get(p.key());
            Preconditions.checkState(paths != null && !paths.isEmpty());

            Path2D[] pp = new Path2D[2];
            pp[0] = paths.get(0);
            pp[1] = paths.get(1);
            Preconditions.checkState(pp[0] != null && pp[1] != null);

            PointVertex pt0 = getVertex(p, pp[0]);
            PointVertex pt1 = getVertex(p, pp[1]);

            Connection c0 = new Connection(pv, pt0, pp[0]);
            Connection c1 = new Connection(pv, pt1, pp[1]);

            pv.minPaths(c0, c1);

            pp[0] = paths.get(2);
            pp[1] = paths.get(3);
            pt0 = getVertex(p, pp[0]);
            pt1 = getVertex(p, pp[1]);

            c0 = new Connection(pv, pt0, pp[0]);
            c1 = new Connection(pv, pt1, pp[1]);

            double dd = c0.distance() + c1.distance();
            double h = dd - pv.minDistance();
            pv.Z(h);
        }
        return this;
    }

    private PointVertex getVertex(Point2D point, Path2D path) {
        Point2D target = path.getTarget(point);
        Preconditions.checkState(target != null);

        return getVertex(target);
    }

    private PointVertex getVertex(Point2D point) {
        PointVertex pv = vertices.get(point.key());
        if (pv == null) {
            pv = new PointVertex(point);
            vertices.put(point.key(), pv);
        }
        return pv;
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * <p> As noted in {@link AutoCloseable#close()}, cases where the
     * close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally
     * <em>mark</em> the {@code Closeable} as closed, prior to throwing
     * the {@code IOException}.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        points.clear();
        paths.clear();
        for (String key : pointPaths.keySet()) {
            List<Path2D> paths = pointPaths.get(key);
            paths.clear();
        }
        pointPaths.clear();
    }
}
