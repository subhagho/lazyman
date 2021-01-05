package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.moeaframework.problem.tsplib.DataType;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TSPDataReaderTest {
    // private static final String TSP_SOURCE_PATH = "src/test/resources/data/att48.tsp";
    private static final String TSP_SOURCE_PATH = "src/test/resources/data/rl5934.tsp";
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
            int csize = reader.getNodeCount();
            assertTrue(csize > 0);
            Random rnd = new Random(System.currentTimeMillis());

            long ctime = System.currentTimeMillis();
            for (int ii = 0; ii < 5; ii++) {
                int index = rnd.nextInt(csize);
                List<Path> paths = reader.getSortedPaths(index);
                assertNotNull(paths);
                assertFalse(paths.isEmpty());

                for (Path p : paths) {
                    LogUtils.debug(getClass(), String.format("[sequence=%d] Distance = %f", index, p.distance()));
                }
            }
            LogUtils.info(getClass(), String.format("Time to fetch 5 path lists = %d msec", (System.currentTimeMillis() - ctime)));
        } catch (Throwable t) {
            LogUtils.error(TSPDataReader.class, t);
            fail(t);
        }
    }
}