package com.codekutter.lazyman.v2.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.*;

@Getter
@Accessors(fluent = true)
public class Journey {
    private static class Pointers {
        private LinkedList<Point> tour;
        private Point start;
        private Point current;
        private int index;
        private Map<Integer, Integer> visited = new HashMap<>();

        public boolean isStart() {
            return current.equals(start);
        }

        public PathRoute route() {
            PathRoute route = new PathRoute();
            for (Point point : tour) {
                route.add(point);
            }
            return route;
        }
    }

    private final List<Point> points;
    private List<PathRoute> route;
    private double distance = 0;

    public Journey(@NonNull List<Point> points) {
        this.points = points;
    }

    public void load() throws Exception {
        route = new ArrayList<>();
        Pointers pointers = new Pointers();
        pointers.start = points.get(0);
        pointers.current = pointers.start;
        pointers.index = 0;
        pointers.tour = new LinkedList<>();

        while (pointers.current != null) {
            if (pointers.isStart()) {
                pointers.tour.addFirst(pointers.current);
                pointers.visited.put(pointers.current.sequence(), pointers.index);
                Path p = pointers.current.connections()[0];
                if (p == null) {
                    if (closeRing(pointers))
                        continue;
                    else break;
                }
                pointers.current = p.target(pointers.current);
                distance += p.actualLength();
                pointers.tour.add(pointers.current);
            } else {
                pointers.visited.put(pointers.current.sequence(), pointers.index);
                Path p = pointers.current.connections()[0];
                if (p == null) {
                    if (closeRing(pointers))
                        continue;
                    else break;
                }
                Point t = p.target(pointers.current);
                if (!pointers.visited.containsKey(t.sequence())) {
                    pointers.tour.add(t);
                    pointers.current = t;
                    distance += p.actualLength();
                } else {
                    p = pointers.current.connections()[1];
                    if (p == null) {
                        if (closeRing(pointers))
                            continue;
                        else break;
                    }
                    t = p.target(pointers.current);
                    if (pointers.visited.containsKey(t.sequence())) {
                        if (closeRing(pointers))
                            continue;
                        else break;
                    }
                    pointers.tour.add(t);
                    pointers.current = t;
                    distance += p.actualLength();
                }
            }
            pointers.index++;
        }
    }

    private boolean closeRing(Pointers pointers) {
        boolean ret = true;
        route.add(pointers.route());
        pointers.tour = new LinkedList<>();
        pointers.index = findNextIndex(pointers.visited);
        if (pointers.index < 0) ret = false;
        else {
            pointers.start = points.get(pointers.index);
            pointers.current = pointers.start;
        }
        return ret;
    }

    private int findNextIndex(Map<Integer, Integer> map) {
        for (int ii = 0; ii < points.size(); ii++) {
            if (!map.containsKey(ii)) return ii;
        }
        return -1;
    }

    public boolean isComplete() {
        return route.size() == 1;
    }
}
