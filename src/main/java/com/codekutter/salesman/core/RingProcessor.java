package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.codekutter.salesman.core.model.Ring;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RingProcessor {
    @Getter
    @Setter
    @Accessors(fluent = true)
    private static class Connect {
        Ring source;
        Ring target;
        Path p1;
        Path p2;
        Path sourcePath;
        Path targetPath;
        double delta = Double.MAX_VALUE;

        public double getOpenDelta() {
            double d = 0;
            if (p1 != null) {
                d += p1.distance();
            }
            if (p2 != null) {
                d += p2.distance();
            }
            d -= sourcePath.distance();
            return d;
        }
    }

    public void process(@NonNull List<Ring> rings,
                        @NonNull TSPDataMap data, @NonNull Connections connections) {
        markConnections(connections, data);
        List<Ring> openRings = new ArrayList<>();
        for (Ring ring : rings) {
            if (!ring.isClosed()) {
                markOpenRingConnections(ring, data);
                openRings.add(ring);
            }
            markRingConnections(ring, rings, openRings, data, connections);
        }
    }

    private void markRingConnections(Ring ring, List<Ring> rings, List<Ring> openRings,
                                     TSPDataMap data, Connections connections) {
        Map<String, List<Connect>> connects = new HashMap<>();
        if (ring.isClosed()) {
            Connect c = connectClosedToOpenRings(ring, openRings, data);
            if (c != null) {
                addConnect(connects, c);
            }
        }
        for (Ring ir : rings) {
            if (!canConnect(ring, ir)) continue;
            Connect c = null;
            if (ring.isClosed() && ir.isClosed()) {
                c = connectClosedRings(ring, ir, data);
                if (c != null) {
                    addConnect(connects, c);
                }
            } else if (!ring.isClosed() && !ir.isClosed()) {
                c = connectOpenRings(ring, ir, data);
                if (c != null) {
                    addConnect(connects, c);
                }
            } else {
                if (!ring.isClosed()) {
                    c = connectOpenToClosedRings(ring, ir, data);
                    if (c != null) {
                        addConnect(connects, c);
                    }
                }
            }
        }
        if (!connects.isEmpty()) {
            if (ring.isClosed()) {
                resolveClosedConnect(ring, connects, data, connections);
            } else {
                resolveOpenConnect(ring, connects, data, connections);
            }
        }
    }

    private void resolveClosedConnect(Ring ring, Map<String, List<Connect>> connects,
                                      TSPDataMap data, Connections connections) {
        Connect connect = null;
        double delta = Double.MAX_VALUE;
        for (String key : connects.keySet()) {
            List<Connect> cs = connects.get(key);
            if (!cs.isEmpty()) {
                if (cs.size() == 1) {
                    double d = cs.get(0).getOpenDelta();
                    if (d < delta) {
                        connect = cs.get(0);
                        delta = d;
                    }
                } else {
                    Path p1 = null;
                    Path p2 = null;
                    Ring target = null;
                    Path targetPath = null;
                    for (Connect c : cs) {
                        int sc = 0;
                        if (p1 == null || c.p1.distance() < p1.distance()) {
                            p1 = c.p1;
                            sc++;
                        }
                        if (p2 == null || c.p2.distance() < p2.distance()) {
                            p2 = c.p2;
                            sc++;
                        }
                        if (sc == 2 && c.source.isClosed() && (c.target != null && c.target.isClosed())) {
                            target = c.target;
                            targetPath = c.targetPath;
                        } else if (sc > 0) {
                            target = null;
                            targetPath = null;
                        }
                    }
                    Connect nc = new Connect();
                    nc.sourcePath = cs.get(0).sourcePath;
                    nc.targetPath = targetPath;
                    nc.target = target;
                    nc.source = ring;
                    nc.p1 = p1;
                    nc.p2 = p2;
                    if (delta > nc.getOpenDelta()) {
                        connect = nc;
                    }
                }
            }
        }
        if (connect != null) {
            connections.remove(connect.sourcePath);
            delta = connect.getOpenDelta();
            boolean done = false;
            if (connect.source.isClosed() && connect.target != null) {
                delta -= connect.targetPath.distance();
                if (delta <= 0) {
                    if (connections.hasPath(connect.targetPath))
                        connections.remove(connect.targetPath);
                    connections.add(connect.p1);
                    connections.add(connect.p2);
                    done = true;
                }
            }
            if (!done) {
                Point tp1 = null;
                Point tp2 = null;
                Point sp1 = connect.p1.connectingPoint(connect.sourcePath);
                if (sp1 == null) {
                    throw new RuntimeException(String.format("No connection found. [source=%s][target=%s]",
                            connect.sourcePath.toString(), connect.p1.toString()));
                }
                tp1 = connect.p1.getTarget(sp1);
                Preconditions.checkNotNull(tp1);
                Point sp2 = connect.p2.connectingPoint(connect.sourcePath);
                if (sp2 == null) {
                    throw new RuntimeException(String.format("No connection found. [source=%s][target=%s]",
                            connect.sourcePath.toString(), connect.p2.toString()));
                }
                tp2 = connect.p2.getTarget(sp2);
                Preconditions.checkNotNull(tp2);
                double r1 = connect(sp1, tp1, data);
                double r2 = connect(sp2, tp2, data);
                double r = Math.max(r1, r2);
                double h = Math.max(sp1.elevation(), sp2.elevation());
                double hn = Math.sqrt(Math.pow(r / 2f, 2) - Math.pow(connect.sourcePath.length() / 2f, 2));
                connect.sourcePath.elevation(h + hn);
            }
        }
    }

    private double connect(Point sp, Point tp, TSPDataMap data) {
        double dist = data.getDistance(sp.sequence(), tp.sequence());
        Path[] paths = data.get(tp.sequence());
        double md = Double.MAX_VALUE;
        for (Path p : paths) {
            if (p != null && p.distance() < md) {
                md = p.distance();
            }
        }
        double ht = Math.sqrt(Math.pow(dist, 2) - Math.pow(md, 2));
        tp.elevation(ht + tp.elevation());

        paths = data.get(sp.sequence());
        md = Double.MAX_VALUE;
        for (Path p : paths) {
            if (p != null && p.distance() < md) {
                md = p.distance();
            }
        }
        double hs = Math.sqrt(Math.pow(dist, 2) - Math.pow(md, 2));
        sp.elevation(sp.elevation() + Math.max(hs, ht));
        return dist;
    }

    private void resolveOpenConnect(Ring ring, Map<String, List<Connect>> connects,
                                    TSPDataMap data, Connections connections) {

    }

    private void addConnect(Map<String, List<Connect>> connects, Connect connect) {
        String key = connect.sourcePath.pathKey();
        if (!connects.containsKey(key)) {
            connects.put(key, new ArrayList<>());
        }
        connects.get(key).add(connect);
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
        for (int ii = 0; ii < source.paths().size(); ii++) {
            double ds = source.paths().get(ii).distance();
            for (int jj = 0; jj < target.paths().size(); jj++) {
                double dt = target.paths().get(jj).distance();
                Path[] paths = findMinPaths(source.paths().get(ii), target.paths().get(jj), data);
                if (paths != null && paths[0] != null && paths[1] != null) {
                    double delta = (paths[0].distance() + paths[1].distance()) - (ds + dt);
                    if (delta < mindist) {
                        mindist = delta;
                        if (minpaths == null) {
                            minpaths = new Connect();
                        }
                        minpaths.p1 = paths[0];
                        minpaths.p2 = paths[1];
                        minpaths.sourcePath = source.paths().get(ii);
                        minpaths.targetPath = target.paths().get(jj);
                        minpaths.delta = delta;
                    }
                }
            }
        }
        if (minpaths != null) {
            data.togglePath(minpaths.p1.A().sequence(), minpaths.p1.B().sequence(), false);
            data.togglePath(minpaths.p2.A().sequence(), minpaths.p2.B().sequence(), false);
            minpaths.source = source;
            minpaths.target = target;
        }
        return minpaths;
    }

    private Connect connectClosedToOpenRings(Ring source, List<Ring> openRings, TSPDataMap data) {
        Connect minpaths = null;
        for (int ii = 0; ii < source.paths().size(); ii++) {
            for (Ring ring : openRings) {
                Connect c = findConnection(source.paths().get(ii), ring, data);
                if (c != null) {
                    if (minpaths == null) {
                        minpaths = c;
                    } else {
                        double d1 = c.getOpenDelta();
                        double d2 = minpaths.getOpenDelta();
                        if (d1 < d2) {
                            minpaths = c;
                        }
                    }
                }
            }
        }
        if (minpaths != null) {
            minpaths.source = source;
        }
        return minpaths;
    }

    private Connect findConnection(Path source, Ring target, TSPDataMap data) {
        Point s1 = source.A();
        Point s2 = source.B();
        double d = Double.MAX_VALUE;
        Connect c = new Connect();
        c.sourcePath = source;
        c.targetPath = null;
        List<Path> ps1 = getSortedPaths(s1, target, data);
        List<Path> ps2 = getSortedPaths(s2, target, data);
        for (Path p1 : ps1) {
            Point t1 = p1.getTarget(s1);
            double d1 = p1.distance();
            for (Path p2 : ps2) {
                Point t2 = p2.getTarget(s2);
                if (t1.equals(t2)) continue;

                double d2 = p2.distance();
                if (d1 + d2 < d) {
                    d = d1 + d2;
                    c.p1 = p1;
                    c.p2 = p2;
                }
            }
        }
        data.togglePath(c.p1.A().sequence(), c.p1.B().sequence(), true);
        data.togglePath(c.p2.A().sequence(), c.p2.B().sequence(), true);
        return c;
    }

    private List<Path> getSortedPaths(Point point, Ring target, TSPDataMap dataMap) {
        List<Path> paths = new ArrayList<>();
        Point lp = null;
        for (int ii = 0; ii < target.paths().size(); ii++) {
            Path path = target.paths().get(ii);
            if (lp == null) {
                Point p1 = path.A();
                Point p2 = path.B();
                Path p = dataMap.get(point.sequence(), p1.sequence());
                paths.add(p);
                p = dataMap.get(point.sequence(), p2.sequence());
                paths.add(p);

                Path pn = target.paths().get(ii + 1);
                if (pn.hasPoint(p1)) {
                    lp = p1;
                } else if (pn.hasPoint(p2)) {
                    lp = p2;
                } else {
                    throw new RuntimeException(String.format("Invalid ring: next path not found. [path=%s]", pn.toString()));
                }
            } else {
                Point pn = path.getTarget(lp);
                Path p = dataMap.get(point.sequence(), pn.sequence());
                paths.add(p);
                lp = path.getTarget(lp);
            }
        }
        paths.sort(new Path.SortByDistance());
        return paths;
    }

    private Connect connectOpenRings(Ring source, Ring target, TSPDataMap data) {
        return null;
    }

    private Connect connectOpenToClosedRings(Ring source, Ring target, TSPDataMap data) {
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
        if (pa == null) {
            throw new RuntimeException(String.format("Path not found. [A=%s][B=%s]", a1, a2));
        }
        Path pb = data.get(b1.sequence(), b2.sequence());
        if (pb == null) {
            throw new RuntimeException(String.format("Path not found. [A=%s][B=%s]", b1, b2));
        }
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
        for (Path po : ring.paths()) {
            Point poa = po.A();
            Point pob = po.B();
            for (Path pi : ring.paths()) {
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
