package com.codekutter.salesman.core;

import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.PathComparator;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.moeaframework.problem.tsplib.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        DistanceTable dt = data.getDistanceTable();
        if (dt instanceof NodeCoordinates) {
            NodeCoordinates coords = (NodeCoordinates) dt;
            for (int ii = 1; ii <= coords.size(); ii++) {
                Node node = coords.get(ii);
                if (node != null) {
                    Point point = new Point();
                    point.sequence(ii);
                    point.X(node.getPosition()[0]);
                    point.Y(node.getPosition()[1]);

                    cache.points()[ii - 1] = point;
                }
            }
            for (int ii = 0; ii < cache.points().length; ii++) {
                for (int jj = 0; jj < cache.points().length; jj++) {
                    if (jj <= ii) continue;
                    String key = cache.getKey(ii, jj);
                    Path path = new Path();
                    path.compute(cache.points()[ii], cache.points()[jj]);
                    cache.cache().put(key, path);
                    LogUtils.debug(getClass(), String.format("Added path [sequence=%d][index=%d][path=%s]", ii, jj, path));
                }
            }
        }
    }

    public List<Path> getSortedPaths(int sequence) {
        Preconditions.checkArgument(sequence >= 0 && sequence < cache.points().length);

        ArrayList<Path> paths = new ArrayList<>(cache.points().length - 1);
        for (int ii = 0; ii < cache.points().length; ii++) {
            if (ii == sequence) continue;
            String key = cache.getKey(sequence, ii);
            Path p = cache.cache().get(key);
            if (p != null) {
                paths.add(p);
            }
        }
        if (!paths.isEmpty())
            paths.sort(new PathComparator());
        return paths;
    }

    public int getNodeCount() {
        if (cache != null && cache.points() != null) {
            return cache.points().length;
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        if (cache != null) cache.close();
        cache = null;
        data = null;
    }
}
