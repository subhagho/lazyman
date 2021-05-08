package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.common.RunInfo;
import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.codekutter.salesman.core.model.Ring;
import lombok.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class OutputPrinter {
    public static final String DIR_ITERATION_OUTPUT = "iterations";

    public static void print(@NonNull TSPDataMap cache,
                             @NonNull Connections out, int iteration, List<Ring> rings) {
        if (!LogUtils.isDebugEnabled()) return;
        try {
            String dir = Config.get().runInfo().createOutputDir(DIR_ITERATION_OUTPUT);
            String file = String.format("iteration_%d.tsv", iteration);
            File fout = new File(String.format("%s/%s", dir, file));
            double totalDistance = 0;
            int unbalanced = 0;
            try (FileOutputStream fos = new FileOutputStream(fout)) {
                printHeader(fos, iteration, cache);
                for (int ii = 0; ii < cache.size(); ii++) {
                    Point p = cache.points()[ii];
                    Connections.Connection connection = out.get(p);
                    StringBuffer buffer = new StringBuffer();
                    if (!connection.isComplete()) {
                        unbalanced++;
                        buffer.append("**");
                    }
                    buffer.append(p.print()).append("\t").append(p.ring());
                    for (int jj = 0; jj < cache.size(); jj++) {
                        buffer.append("\t");
                        if (ii != jj) {
                            Path path = connection.hasSequence(jj);
                            if (path != null) {
                                buffer.append(path.actualLength());
                                totalDistance += path.actualLength();
                            }
                        }
                    }
                    buffer.append("\n");
                    fos.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
                }
                fos.write(String.format("Total Distance:\t%f\n", totalDistance / 2).getBytes(StandardCharsets.UTF_8));
                fos.write(String.format("Unbalanced Count:\t%d\n", unbalanced).getBytes(StandardCharsets.UTF_8));
            }
            LogUtils.info(OutputPrinter.class,
                    String.format("Written output file [iteration=%d][path=%s]", iteration, fout.getAbsolutePath()));
            if (rings != null && !rings.isEmpty()) {
                dir = String.format("%s/rings", dir);
                File d = new File(dir);
                if (!d.exists()) {
                    d.mkdirs();
                }
                for (Ring ring : rings) {
                    printRing(ring, d);
                }
            }
        } catch (Throwable ex) {
            LogUtils.error(OutputPrinter.class, ex);
        }
    }

    private static void printRing(Ring ring, File dir) throws IOException {
        String fname = String.format("%s/ring_%d.tsv", dir.getAbsolutePath(), ring.number());
        try (FileOutputStream fos = new FileOutputStream(fname)) {
            StringBuffer buffer = new StringBuffer();
            RunInfo ri = Config.get().runInfo();
            buffer.append(String.format("RUN ID\t%s\n", ri.runId()));
            buffer.append(String.format("DATE\t%s\n\n", new Date().toString()));
            buffer.append(String.format("RING\t%d\n", ring.number()));
            buffer.append(String.format("SIZE\t%d\n", ring.ring().size()));
            buffer.append(String.format("CLOSED\t%s\n\n", ring.isClosed()));

            for (Path path : ring.ring()) {
                buffer.append(String.format("%s\t%s\t%f\n", path.A(), path.B(), path.actualLength()));
            }
            fos.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void printHeader(FileOutputStream fos, int iteration, TSPDataMap cache) throws IOException {
        StringBuffer buff = new StringBuffer();
        RunInfo ri = Config.get().runInfo();
        buff.append(String.format("RUN ID\t%s\n", ri.runId()));
        buff.append(String.format("DATE\t%s\n", new Date().toString()));
        buff.append(String.format("ITERATION\t%d\n\n", iteration));

        StringBuffer th = new StringBuffer("[points]\t[ring]");
        Point[] points = cache.points();
        for (int ii = 0; ii < cache.size(); ii++) {
            th.append("\t").append(points[ii].print());
        }
        buff.append(th);
        buff.append("\n");

        fos.write(buff.toString().getBytes(StandardCharsets.UTF_8));
    }
}
