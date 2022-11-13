package com.codekutter.lazyman.v2.iterators;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.Cache;
import com.codekutter.lazyman.v2.RunIteration;
import com.codekutter.lazyman.v2.model.IndexedPath;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class L2RunIteration extends RunIteration {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class Bid implements Comparable<Bid> {
        private Point point;
        private Path path;
        private Path assigned;
        private double length;
        private double height;
        private double delta;

        @Override
        public int compareTo(Bid o) {
            double d = o.delta - delta;
            int ret = 0;
            if (d < -1 || d > 1) {
                ret = (int) d;
            } else if (d < 0) {
                ret = -1;
            } else if (d > 0) {
                ret = 1;
            }
            return ret;
        }
    }

    public L2RunIteration(int iteration,
                          int startIndex,
                          @NonNull Cache cache) {
        super(iteration, startIndex, cache);
    }


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
        List<Bid> bids = new LinkedList<>();
        boolean emptyTake = false;
        for (Path path : point.connections()) {
            if (path != null) continue;
            int index = -1;
            while (index < point.paths().size()) {
                IndexedPath p = point.next(index);
                if (p == null) break;
                index = p.index();
                if (reserve(point, p, bids, emptyTake)) {
                    emptyTake = true;
                }
            }
        }
        if (!bids.isEmpty()) {
            Collections.sort(bids);
            Bid b = bids.get(0);
            connect(b.point, b.path, b.assigned, b.length, b.height, b.delta);
        }
    }

    private boolean reserve(Point point,
                            IndexedPath pp,
                            List<Bid> bids,
                            boolean emptyTaken) throws Exception {
        Path path = pp.path();
        Point t = path.target(point);
        if (t.canConnect() && !emptyTaken) {
            double delta = path.actualLength();
            if (pp.next() != null) {
                delta = pp.next().actualLength() + delta;
            }

            Bid bid = new Bid();
            bid.point = point;
            bid.assigned = null;
            bid.path = path;
            bid.length = delta - path.actualLength();
            bid.height = 0;
            bid.delta = delta / path.actualLength();

            bids.add(bid);
            return true;
        } else if (pp.next() != null) {
            Path p = t.longest();
            if (p != null) {
                check(point, p, path, pp.next(), bids);
                p = t.nextConnection(p);
                if (p != null) {
                    check(point, p, path, pp.next(), bids);
                }
            }
        }
        return false;
    }

    private void check(Point point,
                       Path assigned,
                       Path path,
                       Path next,
                       List<Bid> bids) throws Exception {

        double d1 = assigned.compute(2);
        double h = path.actualLength();
        if (next != null) {
            h = next.actualLength() - h;
        }
        double l = point.computeDelta(path);
        double d2 = path.compute(h, l, (point.chainLength() == 0 ? 1 : point.chainLength()));
        if (d2 > d1) {
            Bid bid = new Bid();
            bid.point = point;
            bid.assigned = assigned;
            bid.path = path;
            bid.length = l;
            bid.height = h;
            bid.delta = (d2 - d1);

            bids.add(bid);
        }
    }

    private void connect(Point point,
                         Path path,
                         Path assigned,
                         double length,
                         double height,
                         double delta) throws Exception {
        if (assigned == null) {
            Point t = path.target(point);
            t.connect(path, 2);
            point.connect(path, 2);
            path.length(length);
        } else {
            Point t = path.target(point);
            Point p2 = assigned.target(t);
            t.disconnect(p2);
            p2.disconnect(t);

            point.connect(path, 2);
            t.connect(path, 2);
            t.elevation(point.elevation() + height);
            path.length(length);
        }
    }
}
