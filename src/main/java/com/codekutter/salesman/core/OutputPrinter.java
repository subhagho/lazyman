package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.common.RunInfo;
import lombok.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class OutputPrinter {
    public static final String DIR_ITERATION_OUTPUT = "iterations";

    public static void print(@NonNull PointIndexIn out, int iteration) {
        if (!LogUtils.isDebugEnabled()) return;
        try {
            String dir = Config.get().runInfo().createOutputDir(DIR_ITERATION_OUTPUT);
            String file = String.format("iteration_%d.out", iteration);
            File fout = new File(String.format("%s/%s", dir, file));
            try (FileOutputStream fos = new FileOutputStream(fout)) {
                printHeader(fos, iteration, out.size());

                for (int ii = 0; ii < out.size(); ii++) {
                    List<PointIndexIn.PointInfo> points = out.find(ii);
                    StringBuffer buffer = new StringBuffer(ii);
                    for (int jj = 0; jj < out.size(); jj++) {
                        buffer.append(",");
                        PointIndexIn.PointInfo pi = findPoint(jj, points);
                        if (pi == null) buffer.append("X");
                        else {
                            buffer.append(pi.distance());
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

    private static PointIndexIn.PointInfo findPoint(int sequence, List<PointIndexIn.PointInfo> points) {
        if (points != null) {
            for (PointIndexIn.PointInfo pi : points) {
                if (pi.sequence() == sequence) return pi;
            }
        }
        return null;
    }

    private static void printHeader(FileOutputStream fos, int iteration, int size) throws IOException {
        StringBuffer buff = new StringBuffer();
        RunInfo ri = Config.get().runInfo();
        buff.append(String.format("RUN ID=%s\n", ri.runId()));
        buff.append(String.format("DATE=%s\n", new Date().toString()));
        buff.append(String.format("ITERATION=%d\n", iteration));

        for (int ii = 0; ii < size; ii++) {
            if (ii != 0) buff.append(",");
            buff.append(ii);
        }
        buff.append(",").append(size);

        fos.write(buff.toString().getBytes(StandardCharsets.UTF_8));
    }
}
