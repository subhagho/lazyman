package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.model.Journey;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Accessors(fluent = true)
public class RunIteratorV3 extends RunIteration {
    private Journey journey;

    public RunIteratorV3(int iteration, int startIndex, @NonNull Cache cache) {
        super(iteration, startIndex, cache);
    }

    @Override
    public void run() throws Exception {
        List<Point> points = cache().pointList(0);
        for (Point point : points) {
            Journey j = run(point);
            if (j != null) {
                if (journey == null) {
                    if (j.isComplete())
                        journey = j;
                } else {
                    if (j.isComplete() && j.distance() < journey.distance()) {
                        journey = j;
                    }
                }
            }
            LogUtils.info(getClass(), String.format("Finished point. [%s]", point));
        }
    }

    private Journey run(Point start) throws Exception {
        Journey journey = null;
        for (Point p : start.targets()) {
            Point s = new Point(start);
            Map<Integer, Point> visited = new HashMap<>();
            ArrayList<Point> route = new ArrayList<>();
            visited.put(s.sequence(), s);
            route.add(s);
            Point np = new Point(p);
            Journey j = addPoint(s, np, route, visited);
            if (journey == null) {
                if (j != null) {
                    if (j.isComplete())
                        journey = j;
                }
            } else if (j != null && j.isComplete()) {
                if (j.distance() < journey.distance()) {
                    journey = j;
                }
            }
        }
        return journey;
    }

    private Journey route(Point current, Map<Integer, Point> visited, ArrayList<Point> route) throws Exception {
        Journey journey = null;
        for (Point p : current.targets()) {
            Map<Integer, Point> v = new HashMap<>(visited);
            ArrayList<Point> r = new ArrayList<>(route);
            Point cp = new Point(current);
            Point np = new Point(p);
            Journey j = addPoint(cp, np, r, v);
            if (journey == null) {
                if (j != null) {
                    if (j.isComplete())
                        journey = j;
                }
            } else if (j != null && j.isComplete()) {
                if (j.distance() < journey.distance()) {
                    journey = j;
                }
            }
        }
        return journey;
    }

    private Journey addPoint(Point source, Point target, ArrayList<Point> route, Map<Integer, Point> visited) throws Exception {
        if (visited.containsKey(target.sequence())) {
            if (route.size() == cache().size()) {
                Point start = route.get(0);
                if (start == target) {
                    Path path = source.path(target.sequence());
                    source.connect(path);
                    target.connect(path);
                    route.add(target);
                    visited.put(target.sequence(), target);
                    Journey j = new Journey(route);
                    j.load();
                    return j;
                }
            }
        } else {
            Path path = source.path(target.sequence());
            source.connect(path);
            target.connect(path);
            route.add(target);
            visited.put(target.sequence(), target);
            return route(target, visited, route);
        }
        return null;
    }
}
