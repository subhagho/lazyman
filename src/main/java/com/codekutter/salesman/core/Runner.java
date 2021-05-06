package com.codekutter.salesman.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;

import java.util.HashMap;
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
    @Setter(AccessLevel.NONE)
    private TSPDataReader reader;
    @Setter(AccessLevel.NONE)
    private Connections connections;
    @Setter(AccessLevel.NONE)
    private ClosedRunIterator iterator;

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
            iterator = new ClosedRunIterator(reader.cache(), connections);

            int iteration = 0;
            int lastPrintCount = 10000;
            long st = System.currentTimeMillis();
            while (true) {
                lastPrintCount = run(reader, iteration, lastPrintCount, st);
                if (connections.reachedClosure())  {
                    for (int ii = 0; ii < reader.getNodeCount(); ii++) {
                        Point p = reader.cache().points()[ii];
                        iterator.checkOptimal(p, ii);
                    }
                    if (connections.reachedClosure()) {
                        break;
                    }
                }
                iteration++;
            }
            if (hasLocalEquilibrium()) {
                LogUtils.info(getClass(), "Local equilibrium detected...");
                detectRings();
            }
            LogUtils.info(getClass(), String.format("Reached equilibrium : [#iterations=%d][time=%d]", iteration, (System.currentTimeMillis() - st)));
            OutputPrinter.print(reader.cache(), connections, iteration);
        } catch (Exception ex) {
            LogUtils.error(getClass(), ex);
            throw new RuntimeException(ex);
        }
    }

    private int run(TSPDataReader reader, int iteration, int lastPrintCount, long starttime) {
        long st = System.currentTimeMillis();
        for (int ii = 0; ii < reader.getNodeCount(); ii++) {
            Point p = reader.cache().points()[ii];
            iterator.run(iteration, p, ii);
        }
        if (iteration % 10000 == 0) {
            LogUtils.info(getClass(),
                    String.format("[%d] Completed iteration. [time=%d][elapsed=%d sec]",
                            iteration, (System.currentTimeMillis() - st), (System.currentTimeMillis() - starttime) / 1000));
        }
        if (iteration > 0 && iteration % lastPrintCount == 0) {
            OutputPrinter.print(reader.cache(), connections, iteration);
            lastPrintCount *= (1 + Math.log10(iteration / 10000f));
        }
        return lastPrintCount;
    }

    private void detectRings() {
        Map<Integer, Point> passed = new HashMap<>();
        short ring = 0;
        for (Point point : reader.cache().points()) {
            if (passed.containsKey(point.sequence())) continue;
            findRing(ring, point, passed);
            ring++;
        }
        LogUtils.info(getClass(), String.format("#rings = %d", ring));
    }

    private boolean hasLocalEquilibrium() {
        Map<Integer, Point> check = new HashMap<>();
        Point prevp = reader.cache().points()[0];
        Connections.Connection connection = connections.get(prevp);
        Path path = connection.connections()[0];
        check.put(prevp.sequence(), prevp);
        while (true) {
            Point target = path.getTarget(prevp);
            if (check.containsKey(target.sequence())) {
                break;
            }
            connection = connections.get(target);
            if (!connection.connections()[0].getTarget(target).equals(prevp)) {
                path = connection.connections()[0];
            } else {
                path = connection.connections()[1];
            }
            check.put(target.sequence(), target);
            prevp = target;
        }
        if (check.size() != reader.cache().size()) {
            LogUtils.info(getClass(), String.format("Expected Size = %d, Loop Size = %d", reader.cache().size(), check.size()));
            return true;
        }
        return false;
    }

    private void findRing(short ring, Point start, Map<Integer, Point> passed) {
        start.ring(ring);

        Point prevp = start;
        Connections.Connection connection = connections.get(prevp);
        Path path = connection.connections()[0];
        passed.put(prevp.sequence(), prevp);
        while (true) {
            Point target = path.getTarget(prevp);
            if (passed.containsKey(target.sequence())) {
                break;
            }
            target.ring(ring);
            connection = connections.get(target);
            if (!connection.connections()[0].getTarget(target).equals(prevp)) {
                path = connection.connections()[0];
            } else {
                path = connection.connections()[1];
            }
            passed.put(target.sequence(), target);
            prevp = target;
        }
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
