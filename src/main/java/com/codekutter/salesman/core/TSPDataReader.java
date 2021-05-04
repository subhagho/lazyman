package com.codekutter.salesman.core;

import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
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
        cache.init(name, data.getDimension());

        DistanceTable dt = data.getDistanceTable();
        if (dt instanceof NodeCoordinates) {
            NodeCoordinates coords = (NodeCoordinates) dt;
            readNodeCoordinates(coords);
        } else if (dt instanceof EdgeWeightMatrix) {
            readEdgeWeightMatrix((EdgeWeightMatrix) dt);
        }
        cache.postLoad();
    }

    private void readEdgeWeightMatrix(EdgeWeightMatrix matrix) {
        int[] nodes = matrix.listNodes();
        if (nodes != null && nodes.length > 0) {
            for (int ii = 1; ii <= nodes.length; ii++) {
                Point p = new Point();
                p.sequence(ii - 1);
                cache.points()[ii - 1] = p;
            }
            for (int ii = 0; ii < cache.points().length; ii++) {
                for (int jj = 0; jj < cache.points().length; jj++) {
                    if (ii == jj) continue;
                    double length = matrix.getDistanceBetween(ii + 1, jj + 1);
                    Path path = new Path(cache.points()[ii], cache.points()[jj], length);
                    cache.put(cache.points()[ii].sequence(), cache.points()[jj].sequence(), path);
                    LogUtils.debug(getClass(), String.format("Added path [sequence=%d][index=%d][path=%s]", ii, jj, path));
                }
            }
        }
    }

    private void readNodeCoordinates(NodeCoordinates coords) {
        for (int ii = 1; ii <= coords.size(); ii++) {
            Node node = coords.get(ii);
            if (node != null) {
                Point point = new Point();
                point.sequence(ii - 1);
                point.X(node.getPosition()[0]);
                point.Y(node.getPosition()[1]);

                cache.points()[ii - 1] = point;
            }
        }
        for (int ii = 0; ii < cache.points().length; ii++) {
            for (int jj = 0; jj < cache.points().length; jj++) {
                if (jj <= ii) continue;
                Path path = new Path(cache.points()[ii], cache.points()[jj]);
                cache.put(cache.points()[ii].sequence(), cache.points()[jj].sequence(), path);
                LogUtils.debug(getClass(), String.format("Added path [sequence=%d][index=%d][path=%s]", ii, jj, path));
            }
        }
    }

    public List<Path> getSortedPaths(int sequence) {
        Preconditions.checkArgument(sequence >= 0);

        ArrayList<Path> paths = new ArrayList<>(cache.points().length - 1);
        for (int ii = 0; ii < cache.points().length; ii++) {
            Point pp = cache.points()[ii];
            if (pp.sequence() == sequence) continue;
            Path p = cache.get(sequence, pp.sequence());
            if (p != null) {
                paths.add(p);
            }
        }
        if (!paths.isEmpty())
            paths.sort(new Path.PathComparator());
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
