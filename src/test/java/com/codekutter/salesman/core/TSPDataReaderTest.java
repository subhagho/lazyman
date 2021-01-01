package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.moeaframework.problem.tsplib.DataType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TSPDataReaderTest {
    private static final String TSP_SOURCE_PATH = "src/test/resources/data/att48.tsp";
    private static final String CONFIG_FILE = "src/test/resources/salesman-test.properties";

    @BeforeAll
    static void beforeAll() {
        try {
            Config.init(CONFIG_FILE);
        } catch (Throwable t) {
            LogUtils.error(TSPDataReaderTest.class, t);
            throw t;
        }
    }

    @Test
    void read() {
        try {
            TSPDataReader reader = new TSPDataReader(TSP_SOURCE_PATH, DataType.TSP);
            reader.read();
            reader.load();

            LogUtils.debug(TSPDataReader.class, String.format("Read TSP Data. [path=%s]", TSP_SOURCE_PATH));

            List<Path> paths = reader.getSortedPaths(10);
            assertNotNull(paths);
            assertFalse(paths.isEmpty());

            for (Path p : paths) {
                LogUtils.debug(getClass(), String.format("Distance = %f", p.distance()));
            }
        } catch (Throwable t) {
            LogUtils.error(TSPDataReader.class, t);
            fail(t);
        }
    }
}