package com.codekutter.salesman.core.model;

import com.codekutter.salesman.core.RunIterator;
import com.codekutter.salesman.core.TSPDataMap;
import lombok.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenRing extends Ring {
    private Map<String, RingRoute> routes = new HashMap<>();

    public OpenRing(short number) {
        super(number);
    }

    public OpenRing(@NonNull Ring source) {
        super(source, false);
    }

    @Override
    public void computeConnections(@NonNull Connections connections, @NonNull TSPDataMap data, @NonNull List<Ring> rings) {
        computeRoutes(data);
    }

    private void computeRoutes(TSPDataMap data) {
        RingRoute or = new RingRoute(this);
        routes.put(or.ends().pathKey(), or);
        List<Point> points = getPoints();
        for (Point sp : points) {
            for (Point tp : points) {
                if (sp.sequence() == tp.sequence()) continue;
                Path path = new Path(sp, tp);
                if (routes.containsKey(path.pathKey())) continue;
                RingRoute rr = computeRoute(path, points, data);
                if (rr != null) {
                    routes.put(rr.ends().pathKey(), rr);
                }
            }
        }
    }

    private RingRoute computeRoute(Path path, List<Point> points, TSPDataMap data) {
        RingRoute rr = new RingRoute(number(), path);
        Connections connections = new Connections(points.size());
        connections.add(path, false);
        RunIterator iterator = new RunIterator(data, connections);
        Connections snapshot = null;

        while (true) {
            for (Point point : points) {
                iterator.run(point);
            }
            if (connections.reachedClosure()) {
                break;
            }
            if (snapshot != null) {
                if (snapshot.isIdentical(connections)) {
                    break;
                }
            }
            snapshot = connections.copy();
        }
        return computeRoute(connections, path, points, rr);
    }

    private RingRoute computeRoute(Connections connections, Path path, List<Point> points, RingRoute route) {
        Map<String, Path> passed = new HashMap<>();
        Path cp = null;
        Point lp = path.A();
        Connections.Connection cs = connections.get(path.A());
        if (cs != null) {
            if (cs.connections()[0] != null && cs.connections()[0].path().equals(path)) {
                cp = (cs.connections()[1] != null ? cs.connections()[1].path() : null);
            } else if (cs.connections()[1] != null && cs.connections()[1].path().equals(path)) {
                cp = (cs.connections()[0] != null ? cs.connections()[0].path() : null);
            }
        }
        passed.put(lp.key(), cp);
        while (cp != null) {
            Point np = cp.getTarget(lp);
            if (passed.containsKey(np.key())) {
                break;
            }
            cs = connections.get(np);
            if (cs != null) {
                if (cs.connections()[0] != null && cs.connections()[0].path().equals(cp)) {
                    cp = (cs.connections()[1] != null ? cs.connections()[1].path() : null);
                } else if (cs.connections()[1] != null && cs.connections()[1].path().equals(cp)) {
                    cp = (cs.connections()[0] != null ? cs.connections()[0].path() : null);
                }
            }
            if (cp != null) {
                passed.put(np.key(), cp);
                lp = np;
                route.paths().add(cp);
            }
        }
        if (passed.size() == points.size()) {
            return route;
        }
        return null;
    }
}
