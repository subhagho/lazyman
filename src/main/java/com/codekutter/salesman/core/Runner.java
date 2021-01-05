package com.codekutter.salesman.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;

import java.io.IOException;
import java.util.List;

@Getter
@Setter
public class Runner {
    @Parameter(names = {"--config", "-c"}, description = "Configuration Properties file.")
    private String config;
    @Parameter(names = {"--data", "-d"}, description = "TSP Input Data file.")
    private String tspData;
    @Parameter(names = {"--type", "-t"}, description = "TSP File type.")
    private String tspDataType;
    private PointIndexOut indexOut;

    private void run() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(config));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(tspData));

        try {
            Config.init(config);
            DataType type = DataType.TSP;
            if (!Strings.isNullOrEmpty(tspDataType)) {
                type = DataType.valueOf(tspDataType);
            }
            TSPDataReader reader = new TSPDataReader(tspData, type);
            reader.read();
            reader.load();

            int iteration = 0;
            long st = System.currentTimeMillis();
            while (true) {
                run(reader, iteration);
                if (checkComplete(reader, iteration, st)) break;
                iteration++;
            }
        } catch (Exception ex) {
            LogUtils.error(getClass(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void run(TSPDataReader reader, int iteration) {
        LogUtils.info(getClass(), String.format("[%d] Running iteration...", iteration));
        long st = System.currentTimeMillis();
        checkStarving(reader, iteration);
        checkOverProvisioned(reader, iteration);
        LogUtils.info(getClass(),
                String.format("[%d] Completed iteration. [time=%d]", iteration, (System.currentTimeMillis() - st)));
        OutputPrinter.print(indexOut, iteration);
    }

    private void checkStarving(TSPDataReader reader, int iteration) {
        for (int ii = 0; ii < reader.cache().points().length; ii++) {
            Point pp = reader.cache().points()[ii];
            PointIndexOut.PointInfo[] pointsOut = indexOut.getPoints(pp.sequence());
            if (pointsOut == null || pointsOut[0] == null || pointsOut[1] == null) {
                List<Path> paths = reader.getSortedPaths(pp.sequence());
                Preconditions.checkState(paths != null && !paths.isEmpty());

            }
        }
    }

    private PointIndexOut.PointInfo[] reservePaths(int sequence,
                                                   PointIndexOut.PointInfo[] points,
                                                   List<Path> paths,
                                                   Point pp,
                                                   TSPDataReader reader) {
        if (points == null) {
            points = indexOut.put(sequence);
        }
        if (points[0] == null) {
            for (int ii = 0; ii < paths.size(); ii++) {
                Path path = paths.get(ii);
                if (ignorePath(points, 1, path))
                    continue;
                if (bidForPath(path, ii, paths, points[0])) {
                    indexOut.put(points[0].sequence(), path);
                    break;
                }
            }
        }
        return points;
    }

    private boolean ignorePath(PointIndexOut.PointInfo[] points, int index, Path path) {
        if (points[index] != null
                && (points[index].sequence() == path.A().sequence() || points[index].sequence() == path.B().sequence())) {
            return false;
        }
        return true;
    }

    private boolean bidForPath(Path path, int index, List<Path> paths, PointIndexOut.PointInfo point) {
        Point target = path.getTarget(point.sequence());
        Preconditions.checkState(target != null);
        PointIndexOut.PointInfo[] inps = indexOut.getPoints(target.sequence());
        if (inps == null || inps[0] == null || inps[1] == null) {
            indexOut.put(point.sequence(), path);
            indexOut.put(target.sequence(), path);
            return true;
        } else if (index == paths.size() - 1) {

        } else {

            for (PointIndexOut.PointInfo pi : inps) {

            }
        }
        return false;
    }

    private void checkOverProvisioned(TSPDataReader reader, int iteration) {

    }

    private boolean checkComplete(TSPDataReader reader, int iteration, long startTime) throws IOException {
        for (int ii = 0; ii < reader.cache().points().length; ii++) {
            Point pp = reader.cache().points()[ii];
            PointIndexOut.PointInfo[] pin = indexOut.getPoints(ii);
            if (pin != null && pin.length != 2) return false;
        }
        OutputPrinter.printTotalDistance(tspData, indexOut, reader.cache(), iteration + 1,
                (System.currentTimeMillis() - startTime));
        return true;
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
