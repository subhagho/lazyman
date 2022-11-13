package com.codekutter.lazyman.v2.iterators;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.Cache;
import com.codekutter.lazyman.v2.RunIteration;
import com.codekutter.lazyman.v2.model.IndexedPath;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import lombok.NonNull;

public class L1RunIteration extends RunIteration {
    public L1RunIteration(int iteration,
                          int startIndex,
                          @NonNull Cache cache) {
        super(iteration, startIndex, cache);
    }

    @Override
    public void run() throws Exception {
        long stime = System.currentTimeMillis();
        for (Point point : points()) {
            if (point.isConnected()) continue;
            allocate(point);
        }
        long time = System.currentTimeMillis() - stime;
        LogUtils.info(getClass(), String.format("[%d] Run time=%d", iteration(), time));
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
            t.connect(path, 1);
            point.connect(path, 1);
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
        double d1 = assigned.compute(1);
        double h = next.actualLength() - path.actualLength();
        double l = point.computeDelta(path);
        double d2 = path.compute(h, l);
        if (d2 > d1) {
            Point p2 = assigned.target(t);
            t.disconnect(p2);
            p2.disconnect(t);

            point.connect(path, 1);
            t.connect(path, 1);
            t.elevation(point.elevation() + h);
            path.length(l);
            return true;
        }
        return false;
    }
}
