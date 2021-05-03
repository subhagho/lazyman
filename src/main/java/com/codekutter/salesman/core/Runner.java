package com.codekutter.salesman.core;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Connections;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.moeaframework.problem.tsplib.DataType;

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

            connections = new Connections(reader.getNodeCount());
            iterator = new RunIterator(reader.cache(), connections);

            int iteration = 0;
            long st = System.currentTimeMillis();
            while (true) {
                run(reader, iteration);
                if (connections.reachedClosure()) break;
                iteration++;
            }
            LogUtils.info(getClass(), String.format("Reached equilibrium : [#iterations=%d][time=%d]", iteration, (System.currentTimeMillis() - st)));
        } catch (Exception ex) {
            LogUtils.error(getClass(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void run(TSPDataReader reader, int iteration) {
        LogUtils.info(getClass(), String.format("[%d] Running iteration...", iteration));
        long st = System.currentTimeMillis();
        for (int ii = 0; ii < reader.getNodeCount(); ii++) {
            Point p = reader.cache().points()[ii];
            Connections.Connection paths = connections.get(p);
            if (paths == null || !paths.isComplete()) {
                iterator.run(iteration, p, ii);
            }
        }
        LogUtils.info(getClass(),
                String.format("[%d] Completed iteration. [time=%d]", iteration, (System.currentTimeMillis() - st)));
        if (iteration > 0 && iteration % 100 == 0)
            OutputPrinter.print(reader.cache(), connections, iteration);
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
