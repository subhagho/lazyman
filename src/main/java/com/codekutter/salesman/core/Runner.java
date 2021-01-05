package com.codekutter.salesman.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.Constants;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;

import java.io.IOException;
import java.util.List;

@Getter
@Setter
public class Runner {
    @Parameter(names = {"--config", "-c"}, description = "Configuration Properties file.", required = true)
    private String config;
    @Parameter(names = {"--data", "-d"}, description = "TSP Input Data file.", required = true)
    private String tspData;
    @Parameter(names = {"--type", "-t"}, description = "TSP File type.", required = true)
    private String tspDataType;
    @Setter(AccessLevel.NONE)
    private PointIndexOut indexOut;
    @Setter(AccessLevel.NONE)
    private TSPDataReader reader;

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

            indexOut = new PointIndexOut(reader.getNodeCount());

            int iteration = 0;
            long st = System.currentTimeMillis();
            while (true) {
                run(reader, iteration);
                if (checkComplete(iteration, st)) break;
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
        checkStarving(iteration);
        LogUtils.info(getClass(),
                String.format("[%d] Completed iteration. [time=%d]", iteration, (System.currentTimeMillis() - st)));
        OutputPrinter.print(indexOut, iteration);
    }

    private void checkStarving(int iteration) {
        for (int ii = 0; ii < reader.cache().points().length; ii++) {
            Point pp = reader.cache().points()[ii];
            PointIndexOut.PointInfo[] pointsOut = indexOut.getPoints(pp.sequence());
            if (pointsOut == null || pointsOut[0] == null || pointsOut[1] == null) {
                List<Path> paths = reader.getSortedPaths(pp.sequence());
                Preconditions.checkState(paths != null && !paths.isEmpty());
                PointIndexOut.PointInfo[] ret = reservePaths(pp.sequence(), pointsOut, paths, pp);
            }
        }
    }

    private PointIndexOut.PointInfo[] reservePaths(int sequence,
                                                   PointIndexOut.PointInfo[] points,
                                                   List<Path> paths,
                                                   Point pp) {
        if (points == null) {
            points = indexOut.put(sequence);
        }
        if (points[0] == null) {
            for (int ii = 0; ii < paths.size(); ii++) {
                Path path = paths.get(ii);
                if (points[1] != null && ignorePath(points, 1, path))
                    continue;
                PointIndexOut.PointInfo pi = bidForPath(path, ii, paths, sequence);
                if (pi != null) {
                    break;
                }
            }
        }
        if (points[1] == null) {
            for (int ii = 0; ii < paths.size(); ii++) {
                Path path = paths.get(ii);
                if (points[0] != null && ignorePath(points, 0, path))
                    continue;
                PointIndexOut.PointInfo pi = bidForPath(path, ii, paths, sequence);
                if (pi != null) {
                    break;
                }
            }
        }
        return points;
    }

    private boolean ignorePath(PointIndexOut.PointInfo[] points, int index, Path path) {
        if (points[index] != null
                && (points[index].sequence() == path.A().sequence() || points[index].sequence() == path.B().sequence())) {
            return true;
        }
        return false;
    }

    private PointIndexOut.PointInfo bidForPath(Path path, int index, List<Path> paths, int sequence) {
        Point target = path.getTarget(sequence);
        Preconditions.checkState(target != null);
        PointIndexOut.PointInfo[] inps = indexOut.getPoints(target.sequence());
        if (inps == null || inps[0] == null || inps[1] == null) {
            PointIndexOut.PointInfo pp = indexOut.put(sequence, path);
            indexOut.put(target.sequence(), path);
            return pp;
        } else if (index == paths.size() - 1) {
            int pi = max(target, inps);
            int po = (pi == 0 ? 1 : 0);
            return reserve(target, inps, path, sequence, path.distance(), pi, po);
        } else {
            Path nextp = paths.get(index + 1);
            Preconditions.checkState(nextp != null);
            int pi = max(target, inps);
            int po = (pi == 0 ? 1 : 0);
            double dn = nextp.distance();
            if (inps[pi].distance() < dn) {
                return reserve(target, inps, path, sequence, dn, pi, po);
            } else if (inps[po].distance() < dn) {
                return reserve(target, inps, path, sequence, dn, po, pi);
            }
        }
        return null;
    }

    private PointIndexOut.PointInfo reserve(Point target,
                                            PointIndexOut.PointInfo[] points,
                                            Path path,
                                            int sequence,
                                            double distance,
                                            int index0,
                                            int index1) {
        PointIndexOut.PointInfo removed = indexOut.remove(target.sequence(), points[index0].sequence());
        Preconditions.checkState(removed != null);
        double h = Math.sqrt(Math.pow(distance, 2) - Math.pow(path.length(), 2));
        reader.updatePath(path, target, h);
        removed = indexOut.remove(target.sequence(), points[index1].sequence());
        Preconditions.checkState(removed != null);
        return indexOut.put(sequence, path);
    }

    private int max(Point point, PointIndexOut.PointInfo[] points) {
        Path p0 = reader.cache().get(point.sequence(), points[0].sequence());
        Preconditions.checkState(p0 != null);
        Path p1 = reader.cache().get(point.sequence(), points[1].sequence());
        Preconditions.checkState(p1 != null);
        if (p0.distance() >= p1.distance()) return 0;
        return 1;
    }

    private boolean checkComplete(int iteration, long startTime) throws IOException {
        for (int ii = 0; ii < reader.cache().points().length; ii++) {
            Point pp = reader.cache().points()[ii];
            PointIndexOut.PointInfo[] pin = indexOut.getPoints(ii);
            if (pin == null || pin[0] == null || pin[1] == null) return false;
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
