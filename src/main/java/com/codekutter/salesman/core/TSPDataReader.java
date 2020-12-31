package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.moeaframework.problem.tsplib.DataType;
import org.moeaframework.problem.tsplib.Node;
import org.moeaframework.problem.tsplib.NodeCoordinates;
import org.moeaframework.problem.tsplib.TSPInstance;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

@Getter
@Accessors(fluent = true)
public class TSPDataReader implements Closeable {
    private String name;
    private final String filename;
    private final DataType dataType;
    private TSPInstance data;
    private TSPDataMap cache;

    public TSPDataReader(@NonNull String filename, @NonNull DataType dataType) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(filename));
        this.filename = filename;
        this.dataType = dataType;
    }

    public TSPInstance read() throws IOException {
        File fi = new File(filename);
        if (!fi.exists()) {
            throw new IOException(String.format("File not found. [path=%s]", fi.getAbsolutePath()));
        }
        data = new TSPInstance(fi);
        if (data.getDataType() != dataType) {
            throw new IOException(String.format("Invalid file Data type. [path=%s][type=%s][expected type=%s]",
                    fi.getAbsolutePath(), data.getDataType().name(), dataType.name()));
        }
        name = fi.getName();
        return data;
    }

    public void load() throws Exception {
        Preconditions.checkArgument(data != null);

        if (cache != null) {
            cache.close();
        }
        cache = new TSPDataMap();
        cache.init(name, true, data.getDimension());

        NodeCoordinates coords = data.getDisplayData();
        for (int ii = 0; ii < coords.size(); ii++) {
            Node node = coords.get(ii);
            if (node != null) {
                Point point = new Point();
                point.sequence(ii);
                point.X(node.getPosition()[0]);
                point.Y(node.getPosition()[1]);

                cache.points()[ii] = point;
            }
        }
        for (int ii = 0; ii < cache.points().length; ii++) {
            for (int jj = 0; jj < cache.points().length; jj++) {
                if (ii <= jj) continue;
                String key = cache.getKey(ii, jj);
                Path path = new Path();
                path.compute(cache.points()[ii], cache.points()[jj]);
                cache.cache().put(key, path);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (cache != null) cache.close();
        cache = null;
        data = null;
    }
}
