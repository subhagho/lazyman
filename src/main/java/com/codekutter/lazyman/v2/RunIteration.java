package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.model.IndexedPath;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import com.codekutter.lazyman.v2.utils.OutputPrinter;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public abstract class RunIteration {


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

    public abstract void run() throws Exception;

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
