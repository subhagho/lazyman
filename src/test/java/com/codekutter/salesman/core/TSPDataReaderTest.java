package com.codekutter.salesman.core;

import com.codekutter.salesman.common.LogUtils;
import org.junit.jupiter.api.Test;
import org.moeaframework.problem.tsplib.DataType;

import static org.junit.jupiter.api.Assertions.*;

class TSPDataReaderTest {
    private static final String TSP_SOURCE_PATH = "src/test/resources/data/att48.tsp";

    @Test
    void read() {
        try {
            TSPDataReader reader = new TSPDataReader(TSP_SOURCE_PATH, DataType.TSP);
            reader.read();

            LogUtils.debug(TSPDataReader.class, String.format("Read TSP Data. [path=%s]", TSP_SOURCE_PATH));
        } catch (Throwable t) {
            LogUtils.error(TSPDataReader.class, t);
            fail(t);
        }
    }
}