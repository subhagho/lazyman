package com.codekutter.lazyman.v2;

import com.codekutter.lazyman.common.LogUtils;
import com.codekutter.lazyman.v2.model.Path;
import com.codekutter.lazyman.v2.model.Point;
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
public class DataReader implements Closeable {
    private String name;
    private final String filename;
    private final DataType dataType;
    private TSPInstance data;
    private Cache cache;
    private List<Tour> tours;

    public DataReader(@NonNull String filename, @NonNull DataType dataType) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(filename));
        this.filename = filename;
        this.dataType = dataType;
    }

    public List<Tour> readTours(@NonNull String filename) throws IOException {
        File fi = new File(filename);
        if (!fi.exists()) {
            throw new IOException(String.format("File not found. [path=%s]", fi.getAbsolutePath()));
        }
        TSPInstance td = new TSPInstance(fi);
        tours = td.getTours();
        if (tours == null || tours.isEmpty()) {
            throw new IOException(String.format("No tours loaded from file. [file=%s]", fi.getAbsolutePath()));
        }
        LogUtils.info(getClass(), String.format("Loaded tour file. [file=%s][#tours=%d]",
                fi.getAbsolutePath(), tours.size()));
        return tours;
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

        cache = new Cache();
        cache.init(data.getDimension());

        DistanceTable dt = data.getDistanceTable();
        if (dt instanceof NodeCoordinates) {
            NodeCoordinates coords = (NodeCoordinates) dt;
            readNodeCoordinates(coords);
        } else if (dt instanceof EdgeWeightMatrix) {
            readEdgeWeightMatrix((EdgeWeightMatrix) dt);
        }
    }

    private void readEdgeWeightMatrix(EdgeWeightMatrix matrix) throws Exception {
        int[] nodes = matrix.listNodes();
        if (nodes != null && nodes.length > 0) {
            for (int ii = 1; ii <= nodes.length; ii++) {
                double X = -1;
                double Y = -1;
                if (data.getDisplayData() != null) {
                    Node node = data.getDisplayData().get(ii);
                    if (node != null) {
                        X = node.getPosition()[0];
                        Y = node.getPosition()[1];
                    }
                }
                Point p = cache.add(ii - 1, X, Y);
            }
            for (int ii = 0; ii < cache.size(); ii++) {
                for (int jj = 0; jj < cache.size(); jj++) {
                    if (ii == jj) continue;
                    double length = matrix.getDistanceBetween(ii + 1, jj + 1);
                    Path path = cache.add(ii, jj, length);
                    LogUtils.debug(getClass(), String.format("Added path [sequence=%d][index=%d][path=%s]", ii, jj, path));
                }
            }
        }
    }

    private void readNodeCoordinates(NodeCoordinates coords) throws Exception {
        for (int ii = 1; ii <= coords.size(); ii++) {
            Node node = coords.get(ii);
            if (node != null) {
                double X = node.getPosition()[0];
                double Y = node.getPosition()[1];

                Point p = cache.add(ii - 1, X, Y);
            }
        }
        for (int ii = 0; ii < cache.size(); ii++) {
            LogUtils.info(getClass(), String.format("Loading path. [sequence=%d]...", ii));
            for (int jj = 0; jj < cache.size(); jj++) {
                if (jj <= ii) continue;
                Point p1 = cache.get(ii);
                Point p2 = cache.get(jj);

                Path path = cache.add(p1.sequence(), p2.sequence(), p1.distance(p2));
                LogUtils.debug(getClass(), String.format("Added path [sequence=%d][index=%d][path=%s]", ii, jj, path));
            }
        }
    }

    public int getNodeCount() {
        return cache.size();
    }

    public double getOptDistance() {
        if (tours != null && !tours.isEmpty()) {
            double distance = Double.MAX_VALUE;
            for (Tour tour : tours) {
                double d = tour.distance(data);
                if (d < distance) {
                    distance = d;
                }
            }
            return distance;
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        cache = null;
        data = null;
    }
}
