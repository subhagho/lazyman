package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public class TSPDataMap implements Closeable {
    @Setter(AccessLevel.NONE)
    private int size;
    @Setter(AccessLevel.NONE)
    private String filename;
    @Setter(AccessLevel.NONE)
    private Map<Integer, Path[]> cache;
    @Setter(AccessLevel.NONE)
    private Point[] points;

    public void init(@NonNull String name, int size) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(size > 0);

        cache = new HashMap<>(size);
        points = new Point[size];
        this.size = size;
    }

    public Path get(int ii, int index) {
        if (cache.containsKey(ii)) {
            return cache.get(ii)[index];
        }
        return null;
    }

    public Path put(int ii, int jj, @NonNull Path path) {
        Preconditions.checkArgument(ii >= 0);
        Preconditions.checkArgument(jj >= 0);
        putTo(ii, jj, path);
        putTo(jj, ii, path);
        return path;
    }

    public void postLoad() {
        Path.SortByLength sorter = new Path.SortByLength();
        for (int ii = 0; ii < size; ii++) {
            Path[] paths = cache.get(ii);
            if (paths != null) {
                Arrays.sort(paths, sorter);
            }
        }
    }

    public Path[] get(int sequence) {
        return cache.get(sequence);
    }

    private void putTo(int index, int target, Path path) {
        Path[] paths = cache.get(index);
        if (paths == null) {
            paths = new Path[size];
            cache.put(index, paths);
        }
        paths[target] = path;
    }

    public double getDistance(int s1, int s2) {
        if (cache.containsKey(s1)) {
            Path[] paths = cache.get(s1);
            if (paths != null) {
                Path p = find(paths, s2);
                if (p != null) return p.distance();
            }
        }
        return -1;
    }

    public double getLength(int s1, int s2) {
        if (cache.containsKey(s1)) {
            Path[] paths = cache.get(s1);
            if (paths != null) {
                Path p = find(paths, s2);
                if (p != null) return p.length();
            }
        }
        return -1;
    }

    private Path find(Path[] paths, int sequence) {
        for (Path p : paths) {
            if (p.A().sequence() == sequence || p.B().sequence() == sequence) return p;
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (cache != null) {
            cache.clear();
        }
        cache = null;
        points = null;
    }
}
