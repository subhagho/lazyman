package com.codekutter.lazyman.v2.utils;

import com.codekutter.lazyman.common.Config;
import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.common.RunInfo;
import com.codekutter.lazyman.v2.RunIteration;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
import lombok.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class OutputPrinter {
    public static final String DIR_ITERATION_OUTPUT = "iterations";

    public static String print(@NonNull RunIteration run) throws Exception {
        try {
            String dir = Config.get().runInfo().createOutputDir(DIR_ITERATION_OUTPUT);
            String file = String.format("iteration_%d.tsv", run.iteration());
            File fout = new File(String.format("%s/%s", dir, file));
            double totalDistance = 0;
            int unbalanced = 0;
            try (FileOutputStream fos = new FileOutputStream(fout)) {
                printHeader(fos, run);
                for (Point point : run.points()) {
                    StringBuffer buffer = new StringBuffer();
                    if (!point.isConnected()) {
                        unbalanced++;
                        buffer.append("**\t");
                    } else {
                        buffer.append("--\t");
                    }
                    buffer.append(point.sequence() + 1);
                    for (Point pn : run.points()) {
                        buffer.append("\t");
                        if (pn.equals(point)) {
                            buffer.append("XX");
                        } else {
                            Path p = point.isConnectedTo(pn);
                            if (p != null) {
                                totalDistance += p.actualLength();
                                buffer.append("YY");
                            } else {
                                buffer.append("--");
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
                    String.format("Written output file [iteration=%d][path=%s]", run.iteration(), fout.getAbsolutePath()));
            return fout.getAbsolutePath();
        } catch (Throwable ex) {
            LogUtils.error(OutputPrinter.class, ex);
            throw ex;
        }
    }

    private static void printHeader(FileOutputStream fos, RunIteration run) throws IOException {
        StringBuffer buff = new StringBuffer();
        RunInfo ri = Config.get().runInfo();
        buff.append(String.format("RUN ID\t%s\n", ri.runId()));
        buff.append(String.format("DATE\t%s\n", new Date().toString()));
        buff.append(String.format("ITERATION\t%d\n\n", run.iteration()));

        StringBuffer th = new StringBuffer("[points]\t[state]");
        for (Point point : run.points()) {
            th.append("\t").append(point.sequence() + 1);
        }
        buff.append(th);
        buff.append("\n");

        fos.write(buff.toString().getBytes(StandardCharsets.UTF_8));
    }
}
