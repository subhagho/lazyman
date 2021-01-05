package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
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
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public class TSPDataMap implements Closeable {
    private static final String AVG_KEY_SAMPLE = "XXXXX::YYYYY";
    private static final int AVG_VALUE_SIZE = 256;

    @Setter(AccessLevel.NONE)
    private String filename;
    @Setter(AccessLevel.NONE)
    private Map<Integer, Map<Integer, Path>> cache;
    @Setter(AccessLevel.NONE)
    private Point[] points;

    public void init(@NonNull String name, boolean reset, int size) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(size > 0);

        String fname = Config.get().setupMapFile(name, reset);
        cache = new HashMap<>(size);
        points = new Point[size];
    }

    public Path get(int ii, int jj) {
        if (cache.containsKey(ii)) {
            Map<Integer, Path> map = cache.get(ii);
            if (map != null) return map.get(jj);
        }
        return null;
    }

    public Path put(int ii, int jj, @NonNull Path path) {
        Preconditions.checkArgument(ii > 0);
        Preconditions.checkArgument(jj > 0);
        putTo(ii, jj, path);
        putTo(jj, ii, path);
        return path;
    }

    private void putTo(int index, int target, Path path) {
        Map<Integer, Path> map = cache.get(index);
        if (map == null) {
            map = new HashMap<>();
            cache.put(index, map);
        }
        map.put(target, path);
    }

    public double getDistance(int s1, int s2) {
        if (cache.containsKey(s1)) {
            Map<Integer, Path> map = cache.get(s1);
            Path p = map.get(s2);
            if (p != null) return p.distance();
        }
        return -1;
    }

    public double getLength(int s1, int s2) {
        if (cache.containsKey(s1)) {
            Map<Integer, Path> map = cache.get(s1);
            Path p = map.get(s2);
            if (p != null) return p.length();
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        if (cache != null) {
            for (Integer k : cache.keySet()) {
                Map<Integer, Path> map = cache.get(k);
                if (map != null) map.clear();
            }
            cache.clear();
        }
        cache = null;
        points = null;
    }
}
