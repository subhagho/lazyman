package com.codekutter.salesman.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.codekutter.salesman.core.model.Ring;
import com.codekutter.salesman.ui.Helper;
import com.codekutter.salesman.ui.Viewer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Runner {
    @Parameter(names = {"--config", "-c"}, description = "Configuration Properties file.", required = true)
    private String config;
    @Parameter(names = {"--data", "-d"}, description = "TSP Input Data file.", required = true)
    private String tspData;
    @Parameter(names = {"--type", "-t"}, description = "TSP File type.", required = true)
    private String tspDataType;
    @Parameter(names = {"--result", "-r"}, description = "Result tour file path.", required = false)
    private String tourfile;
    @Parameter(names = {"--view", "-v"}, description = "View output.")
    private boolean view = false;
    @Setter(AccessLevel.NONE)
    private TSPDataReader reader;
    @Setter(AccessLevel.NONE)
    private Connections connections;
    @Setter(AccessLevel.NONE)
    private RunIterator iterator;

    private void run() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(tspData));

        try {
            Config.init(config);
            DataType type = DataType.TSP;
            if (!Strings.isNullOrEmpty(tspDataType)) {
                type = DataType.valueOf(tspDataType);
            }
            reader = new TSPDataReader(tspData, type);
            reader.read();
            reader.load();

            if (!Strings.isNullOrEmpty(tourfile)) {
                reader.readTours(tourfile);
            }
            connections = new Connections(reader.getNodeCount());
            iterator = new RunIterator(reader.cache(), connections);

            int iteration = 0;
            int lastPrintCount = 10000;
            long st = System.currentTimeMillis();
            Connections snapshot = null;
            short iCount = 0;
            while (true) {
                lastPrintCount = run(reader, snapshot, iteration, lastPrintCount, st);
                if (connections.reachedClosure()) {
                    break;
                }
                if (snapshot != null) {
                    if (snapshot.isIdentical(connections)) {
                        if (iCount > 3)
                            break;
                        else iCount++;
                    } else {
                        iCount = 0;
                    }
                }
                snapshot = connections.copy();
                iteration++;
            }
            List<Ring> rings = null;
            if (hasLocalEquilibrium()) {
                LogUtils.info(getClass(), "Local equilibrium detected...");
                rings = detectRings();
            }
            LogUtils.info(getClass(), String.format("Reached equilibrium : [#iterations=%d][time=%d]", iteration, (System.currentTimeMillis() - st)));
            OutputPrinter.print(reader.cache(), connections, iteration, rings);
            if (view) {
                Helper.connections = connections;
                Helper.points = reader.cache().points();
                Helper.tours = reader.tours();

                Viewer.show();
            }
        } catch (Exception ex) {
            LogUtils.error(getClass(), ex);
            throw new RuntimeException(ex);
        }
    }

    private int run(TSPDataReader reader, Connections snapshot, int iteration, int lastPrintCount, long starttime) {
        long st = System.currentTimeMillis();
        for (int ii = 0; ii < reader.getNodeCount(); ii++) {
            Point p = reader.cache().points()[ii];
            Connections.Connection prev = null;
            if (snapshot != null) {
                prev = snapshot.get(p);
            }
            iterator.run(prev, p, ii);
        }
        if (iteration % 10000 == 0) {
            LogUtils.info(getClass(),
                    String.format("[%d] Completed iteration. [time=%d][elapsed=%d sec]",
                            iteration, (System.currentTimeMillis() - st), (System.currentTimeMillis() - starttime) / 1000));
        }
        if (iteration > 0 && iteration % lastPrintCount == 0) {
            OutputPrinter.print(reader.cache(), connections, iteration, null);
            lastPrintCount *= (1 + Math.log10(iteration / 10000f));
        }
        return lastPrintCount;
    }

    private List<Ring> detectRings() {
        List<Ring> rings = new ArrayList<>();
        Map<Integer, Point> passed = new HashMap<>();
        short ring = 0;
        for (Connections.Connection connection : connections.connections().values()) {
            if (connection.isComplete()) continue;
            Point point = connection.point();
            if (passed.containsKey(point.sequence())) continue;
            Ring r = findRing(ring, point, passed);
            if (r != null) {
                rings.add(r);
                ring++;
            }
        }
        for (Point point : reader.cache().points()) {
            if (passed.containsKey(point.sequence())) continue;
            Ring r = findRing(ring, point, passed);
            if (r != null) {
                rings.add(r);
                ring++;
            }
        }
        LogUtils.info(getClass(), String.format("#rings = %d", ring));
        if (ring > 1) {
            detectLevels(rings);
        }
        return rings;
    }


    private void detectLevels(List<Ring> rings) {
        for (int ii = 0; ii < rings.size(); ii++) {
            Ring o = rings.get(ii);
            if (!o.isClosed()) continue;
            Point[] polygon = o.getPolygon();
            for (int jj = 0; jj < rings.size(); jj++) {
                if (ii == jj) continue;
                Ring i = rings.get(jj);
                Path p = i.ring().get(0);
                Point point = p.A();
                if (point == null) {
                    point = p.B();
                }
                if (GFG.isInside(polygon, polygon.length, point)) {
                    Ring e = i.enclosing();
                    short l = (short) (o.level() + 1);
                    if (e == null || e.level() < l) {
                        i.enclosing(o);
                    }
                    i.level(l);
                }
            }
        }
    }

    private boolean hasLocalEquilibrium() {
        Map<Integer, Point> check = new HashMap<>();
        Point prevp = reader.cache().points()[0];
        Connections.Connection connection = connections.get(prevp);
        Path path = connection.connections()[0];
        if (path == null) {
            path = connection.connections()[1];
        }
        check.put(prevp.sequence(), prevp);
        while (true) {
            Point target = path.getTarget(prevp);
            if (check.containsKey(target.sequence())) {
                break;
            }
            connection = connections.get(target);
            if (connection.connections()[0] == null && connection.connections()[1] == null) {
                throw new RuntimeException(String.format("Both connections are NULL. [connection=%s]", connection));
            }
            if (connection.connections()[0] != null
                    && !connection.connections()[0].getTarget(target).equals(prevp)) {
                path = connection.connections()[0];
            } else if (connection.connections()[1] != null) {
                path = connection.connections()[1];
            }
            check.put(target.sequence(), target);
            prevp = target;
            if (path == null) {
                break;
            }
        }
        if (check.size() != reader.cache().size()) {
            LogUtils.info(getClass(), String.format("Expected Size = %d, Loop Size = %d", reader.cache().size(), check.size()));
            return true;
        }
        return false;
    }

    private Ring findRing(short ring, Point start, Map<Integer, Point> passed) {
        Ring r = new Ring(ring);
        start.ring(ring);

        Point prevp = start;
        Path patho = null;
        Connections.Connection connection = connections.get(prevp);
        Path path = connection.connections()[0];
        if (path == null) {
            path = connection.connections()[1];
        } else {
            patho = connection.connections()[1];
        }
        if (path == null) return null;

        r.add(path);
        passed.put(prevp.sequence(), prevp);
        boolean both = (patho != null);
        while (true) {
            Point target = path.getTarget(prevp);
            if (passed.containsKey(target.sequence())) {
                if (both) {
                    path = patho;
                    prevp = start;
                    both = false;
                    continue;
                }
                break;
            }
            target.ring(ring);
            connection = connections.get(target);
            if (connection.connections()[0] == null && connection.connections()[1] == null) {
                throw new RuntimeException(String.format("Both connections are NULL. [connection=%s]", connection));
            }
            if (connection.connections()[0] != null
                    && !connection.connections()[0].getTarget(target).equals(prevp)) {
                path = connection.connections()[0];
            } else if (connection.connections()[1] != null) {
                path = connection.connections()[1];
            }
            passed.put(target.sequence(), target);
            prevp = target;
            if (path == null) {
                r.isClosed(false);
                break;
            }
            r.add(path);
        }
        return r;
    }

    public static void main(String... argv) {
        try {
            Runner r = new Runner();
            JCommander.newBuilder().addObject(r).build().parse(argv);
            r.run();
        } catch (Throwable t) {
            LogUtils.error(Runner.class, t);
            t.printStackTrace();
        }
    }
}
