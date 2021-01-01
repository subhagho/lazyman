package com.codekutter.salesman.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;

@Getter
@Setter
public class Runner {
    @Parameter(names = {"--config", "-c"}, description = "Configuration Properties file.")
    private String config;
    @Parameter(names = {"--data", "-d"}, description = "TSP Input Data file.")
    private String tspData;
    @Parameter(names = {"--type", "-t"}, description = "TSP File type.")
    private String tspDataType;

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
        } catch (Exception ex) {
            LogUtils.error(getClass(), ex);
            throw new RuntimeException(ex);
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
