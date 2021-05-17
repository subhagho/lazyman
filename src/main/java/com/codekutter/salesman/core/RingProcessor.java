package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.codekutter.salesman.core.model.Ring;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

public class RingProcessor {
    @Getter
    @Setter
    @Accessors(fluent = true)
    private class Connect {
        Path p1;
        Path p2;
        Path spath;
        Path tpath;
        double delta;
    }

    public void process(@NonNull List<Ring> rings, @NonNull String name,
                        TSPDataMap source, Connections connections) {
        markConnections(connections, source);
        for (Ring ring : rings) {
            if (!ring.isClosed()) {
                markOpenRingConnections(ring, source);
            }
            markRingConnections(ring, rings, source);
        }
    }

    private void markRingConnections(Ring ring, List<Ring> rings, TSPDataMap source) {
        List<Connect> connects = new ArrayList<>();
        for (Ring ir : rings) {
            if (!canConnect(ring, ir)) continue;
            Connect c = null;
            if (ring.isClosed() && ir.isClosed()) {
                c = connectClosedRings(ring, ir, source);
                if (c != null) {
                    connects.add(c);
                }
            } else if (!ring.isClosed() && !ir.isClosed()) {
                c = connectOpenRings(ring, ir, source);
                if (c != null) {
                    connects.add(c);
                }
            } else {
                c = connectClosedToOpenRings(ring, ir, source);
                if (c != null) {
                    connects.add(c);
                }
            }
        }
        if (!connects.isEmpty()) {
            Connect mc = null;
            for (Connect c : connects) {
                if (mc == null) {
                    mc = c;
                } else {
                    if (c.delta < mc.delta()) {
                        mc = c;
                    }
                }
            }
            if (mc != null) {
                double h = Math.sqrt(Math.pow(mc.delta, 2) + Math.pow(mc.spath.A().elevation() + mc.spath.B().elevation(), 2));
                mc.spath.elevation(h);
            }
        }
    }

    private boolean canConnect(Ring source, Ring target) {
        if (source.number() == target.number()) return false;
        Ring ps = source.enclosing();
        Ring pt = target.enclosing();
        if (ps != null || pt != null) {
            boolean ret = false;
            if (ps != null && pt != null) {
                if (ps.number() == pt.number()) {
                    ret = true;
                } else if (ps.number() == target.number() || pt.number() == source.number()) {
                    ret = true;
                }
            }
            return ret;
        }
        return true;
    }

    private Connect connectClosedRings(Ring source, Ring target, TSPDataMap data) {
        double mindist = Double.MAX_VALUE;
        Connect minpaths = null;
        for (int ii = 0; ii < source.ring().size(); ii++) {
            double ds = source.ring().get(ii).distance();
            for (int jj = 0; jj < target.ring().size(); jj++) {
                double dt = target.ring().get(jj).distance();
                Path[] paths = findMinPaths(source.ring().get(ii), target.ring().get(jj), data);
                if (paths != null && paths[0] != null && paths[1] != null) {
                    double delta = (paths[0].distance() + paths[1].distance()) - (ds + dt);
                    if (delta < mindist) {
                        mindist = delta;
                        if (minpaths == null) {
                            minpaths = new Connect();
                        }
                        minpaths.p1 = paths[0];
                        minpaths.p2 = paths[1];
                        minpaths.spath = source.ring().get(ii);
                        minpaths.tpath = target.ring().get(jj);
                        minpaths.delta = delta;
                    }
                }
            }
        }
        if (minpaths != null) {
            data.togglePath(minpaths.p1.A().sequence(), minpaths.p1.B().sequence(), true);
            data.togglePath(minpaths.p2.A().sequence(), minpaths.p2.B().sequence(), true);
        }
        return minpaths;
    }

    private Connect connectClosedToOpenRings(Ring source, Ring target, TSPDataMap data) {
        return null;
    }

    private Connect connectOpenRings(Ring source, Ring target, TSPDataMap data) {
        return null;
    }

    private Path[] findMinPaths(Path p1, Path p2, TSPDataMap data) {
        Path[] paths = new Path[2];
        double d1 = 0, d2 = 0;
        Point a1 = p1.A();
        Point a2 = p2.A();
        Point b1 = p1.B();
        Point b2 = p2.B();
        Path pa = data.get(a1.sequence(), a2.sequence());
        Path pb = data.get(b1.sequence(), b2.sequence());
        paths[0] = pa;
        paths[1] = pb;
        d1 = pa.distance() + pb.distance();

        a1 = p1.A();
        a2 = p2.B();
        b1 = p1.B();
        b2 = p2.A();
        pa = data.get(a1.sequence(), a2.sequence());
        pb = data.get(b1.sequence(), b2.sequence());
        d2 = pa.distance() + pb.distance();
        if (d2 < d1) {
            paths[0] = pa;
            paths[1] = pb;
        }
        return paths;
    }

    private void markOpenRingConnections(Ring ring, TSPDataMap source) {
        for (Path po : ring.ring()) {
            Point poa = po.A();
            Point pob = po.B();
            for (Path pi : ring.ring()) {
                if (po.equals(pi)) continue;
                Point pia = pi.A();
                Point pib = pi.B();
                source.togglePath(poa.sequence(), pia.sequence(), false);
                source.togglePath(poa.sequence(), pib.sequence(), false);
                source.togglePath(pob.sequence(), pib.sequence(), false);
                source.togglePath(pob.sequence(), pia.sequence(), false);
            }
        }
    }

    private void markConnections(Connections connections, TSPDataMap source) {
        for (Connections.Connection connection : connections.connections().values()) {
            if (connection.connections() != null) {
                Point point = connection.point();
                Path[] paths = source.get(point.sequence());
                for (Path p : connection.connections()) {
                    if (p == null) continue;
                    for (Path path : paths) {
                        if (path != null) {
                            if (p.equals(path)) {
                                if (path.length() < 0) {
                                    path.length(1f * path.length());
                                }
                            } else {
                                if (path.length() > 0) {
                                    path.length(-1f * path.length());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
