package com.codekutter.lazyman.v2.iterators;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.Cache;
import com.codekutter.lazyman.v2.RunIteration;
import com.codekutter.lazyman.v2.model.IndexedPath;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import lombok.NonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class L0RunIteration extends RunIteration {
    public L0RunIteration(int iteration,
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
        int i0 = point.sortIndex().get(0);
        int i1 = point.sortIndex().get(1);
        int i2 = point.sortIndex().get(2);
        Path pn = point.path(i2);
        Path p0 = point.path(i0);
        Path p1 = point.path(i1);
        checkPoint(point, p0, p1);
        if (point.canConnect())
            connect(point, p0, pn);
        if (point.canConnect())
            connect(point, p1, pn);
    }

    private void checkPoint(Point point, Path p0, Path p1) throws Exception {
        Path[] paths = point.connections();
        int index = 0;
        for (Path path : paths) {
            if (path == null) continue;
            if (path.actualLength() < p0.actualLength() ||
                    path.actualLength() < p1.actualLength()) {
                Point t = path.target(point);
                t.disconnect(point);
                point.disconnect(t);
                point.connections()[index] = null;
            }
            index++;
        }
    }

    private boolean connect(Point point, Path path, Path next) throws Exception {
        double delta = next.actualLength() - path.actualLength();
        Point t = path.target(point);
        if (t.canConnect()) {
            t.connect(path, 0);
            point.connect(path, 0);
            t.elevation(delta);
            return true;
        } else {
            Path p = t.longest();
            if (p.actualLength() > path.actualLength()) {
                Point p2 = p.target(t);
                p2.disconnect(t);
                t.disconnect(p2);
                t.connect(path, 0);
                point.connect(path, 0);
                t.elevation(delta);
                return true;
            } else {
                p = t.nextConnection(p);
                if (p.actualLength() > path.actualLength()) {
                    Point p2 = p.target(t);
                    p2.disconnect(t);
                    t.disconnect(p2);
                    t.connect(path, 0);
                    point.connect(path, 0);
                    t.elevation(delta);
                    return true;
                }
            }
        }
        return false;
    }
}
