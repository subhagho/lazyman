package com.codekutter.lazyman.core.model;

import com.codekutter.lazyman.core.RingProcessor;
import com.codekutter.lazyman.core.TSPDataMap;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ClosedRing extends Ring {
    private List<RingProcessor.Connect> targets = new ArrayList<>();

    public ClosedRing(short number) {
        super(number);
    }

    public ClosedRing(@NonNull Ring source) {
        super(source, true);
    }

    @Override
    public void computeConnections(@NonNull Connections connections, @NonNull TSPDataMap data, @NonNull List<Ring> rings) {
        for (Ring ring : rings) {
            if (ring.number() == number()) continue;
            if (!ring.isClosed()) continue;
            if (!canConnect(ring)) continue;

            computeConnections(data, ring);
        }
    }

    private void computeConnections(TSPDataMap data, Ring target) {
        List<RingProcessor.Connect> connects = new ArrayList<>();
        for (int ii = 0; ii < paths().size(); ii++) {
            double ds = paths().get(ii).distance();
            for (int jj = 0; jj < target.paths().size(); jj++) {
                double dt = target.paths().get(jj).distance();
                Path[] paths = data.findMinPaths(paths().get(ii), target.paths().get(jj));
                if (paths != null && paths[0] != null && paths[1] != null) {
                    double delta = (paths[0].distance() + paths[1].distance()) - (ds + dt);
                    RingProcessor.Connect connect = new RingProcessor.Connect();
                    connect.p1(paths[0]);
                    connect.p2(paths[1]);
                    connect.sourcePath(paths().get(ii));
                    connect.targetPath(target.paths().get(jj));
                    connect.delta(delta);
                    connect.source(this);
                    connect.target(target);
                    connects.add(connect);

                    data.togglePath(paths[0].A().sequence(), paths[0].B().sequence(), true);
                    data.togglePath(paths[1].A().sequence(), paths[1].B().sequence(), true);
                }
            }
        }
        if (!connects.isEmpty()) {
            targets.addAll(connects);
        }
    }

    public List<RingProcessor.Connect> getSortedConnects() {
        if (!targets.isEmpty())
            targets.sort(new RingProcessor.ConnectSorter());
        return targets;
    }
}
