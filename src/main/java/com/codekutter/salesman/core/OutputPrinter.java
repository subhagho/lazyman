package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.common.RunInfo;
import com.codekutter.salesman.core.model.Point;
import lombok.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class OutputPrinter {
    public static final String DIR_ITERATION_OUTPUT = "iterations";

    public static void print(@NonNull PointIndexOut out, int iteration) {
        if (!LogUtils.isDebugEnabled()) return;
        try {
            String dir = Config.get().runInfo().createOutputDir(DIR_ITERATION_OUTPUT);
            String file = String.format("iteration_%d.tsv", iteration);
            File fout = new File(String.format("%s/%s", dir, file));
            try (FileOutputStream fos = new FileOutputStream(fout)) {
                printHeader(fos, iteration, out.size() + 1);
                for (Integer key : out.outPoints().keySet()) {
                    PointIndexOut.PointInfo[] points = out.getPoints(key);
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(key);
                    for (int jj = 0; jj < out.size(); jj++) {
                        buffer.append("\t");
                        PointIndexOut.PointInfo pi = findPoint(jj, points);
                        if (pi == null) buffer.append("XXXXXXXXX");
                        else {
                            buffer.append(String.format("%09.4f", pi.distance()));
                        }
                    }
                    buffer.append("\n");
                    fos.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            LogUtils.info(OutputPrinter.class,
                    String.format("Written output file [iteration=%d][path=%s]", iteration, fout.getAbsolutePath()));
        } catch (Throwable ex) {
            LogUtils.error(OutputPrinter.class, ex);
        }
    }

    private static PointIndexOut.PointInfo findPoint(int sequence, PointIndexOut.PointInfo[] points) {
        if (points != null) {
            for (PointIndexOut.PointInfo pi : points) {
                if (pi != null && pi.sequence() == sequence) return pi;
            }
        }
        return null;
    }

    private static void printHeader(FileOutputStream fos, int iteration, int size) throws IOException {
        StringBuffer buff = new StringBuffer();
        RunInfo ri = Config.get().runInfo();
        buff.append(String.format("RUN ID\t%s\n", ri.runId()));
        buff.append(String.format("DATE\t%s\n", new Date().toString()));
        buff.append(String.format("ITERATION\t%d\n", iteration));

        for (int ii = 0; ii < size; ii++) {
            if (ii != 0) buff.append("\t");
            buff.append(ii);
        }
        buff.append("\n");

        fos.write(buff.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void printTotalDistance(String datafile,
                                          PointIndexOut indexOut,
                                          TSPDataMap dataMap,
                                          int iterations, long time) throws IOException {
        try {
            String dir = Config.get().runInfo().createOutputDir(DIR_ITERATION_OUTPUT);
            File fout = new File(String.format("%s/final.out", dir));
            try (FileOutputStream fos = new FileOutputStream(fout)) {
                StringBuffer header = new StringBuffer();
                header.append(String.format("DATA FILE: %s\n", datafile));
                header.append(String.format("RUN ID: %s\n", Config.get().runInfo().runId()));
                header.append(String.format("RUN TIME: %d\n", time));
                header.append(String.format("# ITERATIONS: %d\n", iterations));
                StringBuffer route = new StringBuffer();
                route.append("Route Details:\n").append("-----------------------------------------------------\n");
                route.append("Sequence").append("\t").append("[X,Y]").append("\t").append("Distance").append("\n");
                double distance = 0;

                PointIndexOut.PointInfo[] startp = indexOut.getPoints(1);
                PointIndexOut.PointInfo[] prevp = startp;
                int prevIndex = 0;
                Point ps = dataMap.points()[startp[0].sequence()];
                route.append(startp[0].sequence()).append("\t").append(String.format("[%f,%f]", ps.X(), ps.Y())).append("\t").append("0.0").append("\n");
                while (true) {
                    PointIndexOut.PointInfo[] points = indexOut.getPoints(prevp[prevIndex].sequence());
                    PointIndexOut.PointInfo pi = null;
                    int matchIndex = -1;
                    if (prevp[prevIndex].equalsTo(points[0])) {
                        matchIndex = 0;
                    } else if (prevp[prevIndex].equalsTo(points[1])) {
                        matchIndex = 1;
                    }
                    if (matchIndex < 0)
                        throw new Exception(String.format("Index mismatch. [sequence=%s]", prevp[prevIndex].sequence()));
                    pi = points[matchIndex];
                    int nextIndex = (matchIndex == 0 ? 1 : 0);
                    PointIndexOut.PointInfo pn = points[nextIndex];
                    if (pn.equalsTo(startp[1])) {
                        double d = dataMap.getLength(startp[1].sequence(), pn.sequence());
                        if (d <= 0)
                            throw new Exception(String.format("[%d->%d] Invalid distance.", startp[1].sequence(), pn.sequence()));
                        distance += d;
                        break;
                    }
                    double d = dataMap.getLength(pi.sequence(), prevp[prevIndex].sequence());
                    if (d <= 0)
                        throw new Exception(String.format("[%d->%d] Invalid distance.", startp[1].sequence(), pn.sequence()));
                    distance += d;
                    ps = dataMap.points()[pi.sequence()];
                    route.append(pi.sequence()).append("\t").append(String.format("[%f,%f]", ps.X(), ps.Y())).append("\t").append(d).append("\n");
                }
                header.append(String.format("TOTAL DISTANCE: %f\n\n", distance));

                fos.write(header.toString().getBytes(StandardCharsets.UTF_8));
                fos.write(route.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ex) {
            LogUtils.error(OutputPrinter.class, ex);
        }
    }
}
