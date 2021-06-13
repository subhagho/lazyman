package com.codekutter.salesman.core.model;

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
                RingRoute rr = computeRoute(path, data);
                if (rr != null) {
                    routes.put(rr.ends().pathKey(), rr);
                }
            }
        }
    }

    private RingRoute computeRoute(Path path, TSPDataMap data) {
        RingRoute rr = new RingRoute(number(), path);

        return rr;
    }
}
