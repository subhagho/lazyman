package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.model.IndexedPath;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import com.codekutter.lazyman.v2.utils.OutputPrinter;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
public class RunIteration {
    private final Cache cache;
    private List<Point> points;
    private final int iteration;
    private final int startIndex;

    public RunIteration(int iteration,
                        int startIndex,
                        @NonNull Cache cache) {
        this.iteration = iteration;
        this.startIndex = startIndex;
        this.cache = cache;
        points = cache.pointList(startIndex);
    }

    public void run() throws Exception {
        long stime = System.currentTimeMillis();
        for (Point point : points) {
            if (point.isConnected()) continue;
            allocate(point);
        }
        long time = System.currentTimeMillis() - stime;
        LogUtils.info(getClass(), String.format("[%d] Run time=%d", iteration, time));
    }

    private void allocate(Point point) throws Exception {
        for (Path path : point.connections()) {
            if (path != null) continue;
            int index = -1;
            while (index < point.paths().size()) {
                IndexedPath p = point.next(index);
                if (p == null) break;
                index = p.index();
                if (reserve(point, p)) break;
            }
        }
    }

    private boolean reserve(Point point, IndexedPath pp) throws Exception {
        Path path = pp.path();
        Point t = path.target(point);
        if (t.canConnect()) {
            t.connect(path);
            point.connect(path);
            double delta = pp.next().actualLength() - path.actualLength();
            path.length(delta);
            return true;
        } else if (pp.next() != null) {
            Path p = t.longest();
            if (p != null) {
                if (check(point, p, path, pp.next())) {
                    return true;
                } else {
                    p = t.nextConnection(p);
                    if (p != null) {
                        return check(point, p, path, pp.next());
                    }
                }
            }
        }
        return false;
    }

    private boolean check(Point point,
                          Path assigned,
                          Path path,
                          Path next) throws Exception {
        Point t = path.target(point);
        double d1 = assigned.compute();
        double h = next.actualLength() - path.actualLength();
        double l = point.computeDelta(path);
        double d2 = path.compute(h, l);
        if (d2 > d1) {
            Point p2 = assigned.target(t);
            t.disconnect(p2);
            p2.disconnect(t);

            point.connect(path);
            t.connect(path);
            t.elevation(point.elevation() + h);
            path.length(l);
            return true;
        }
        return false;
    }

    public boolean isCompleted() {
        for (Point point : points) {
            if (!point.isConnected()) return false;
        }
        return true;
    }

    public boolean compare(List<Point> points) {
        for (int ii = 0; ii < this.points.size(); ii++) {
            Point p = this.points.get(ii);
            Point t = points.get(ii);
            if (!p.isEqual(t)) {
                return false;
            }
        }
        return true;
    }

    public String print() throws Exception {
        return OutputPrinter.print(this);
    }

    public void save() {
        points = cache.copyPoints(startIndex);
    }
}
