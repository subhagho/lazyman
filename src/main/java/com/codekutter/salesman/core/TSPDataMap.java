package com.codekutter.salesman.core;

import com.codekutter.salesman.common.Config;
import com.codekutter.salesman.common.LogUtils;
import com.codekutter.salesman.core.model.Path;
import com.codekutter.salesman.core.model.Point;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import jdk.internal.joptsimple.internal.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.openhft.chronicle.map.ChronicleMap;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(fluent = true)
public class TSPDataMap implements Closeable {
    private static final String AVG_KEY_SAMPLE = "XXXXX::YYYYY";

    @Setter(AccessLevel.NONE)
    private String filename;
    @Setter(AccessLevel.NONE)
    private ChronicleMap<CharSequence, Path> cache;
    @Setter(AccessLevel.NONE)
    private Point[] points;

    public void init(@NonNull String name, boolean reset, int size) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
        Preconditions.checkArgument(size > 0);

        String fname = Config.get().setupMapFile(name, reset);
        long csize = (long) size * (size - 1);
        cache = ChronicleMap
                .of(CharSequence.class, Path.class)
                .name(name)
                .averageKey(AVG_KEY_SAMPLE)
                .entries(csize)
                .createOrRecoverPersistedTo(new File(fname));
        LogUtils.debug(getClass(), String.format("Initialized Chronicle Map. [file=%s][size=%d]", fname, csize));
        points = new Point[size];
    }

    @JsonIgnore
    public String getKey(int s1, int s2) {
        if (s1 <= s2) {
            return String.format("%d:%d", s1, s2);
        }
        return String.format("%d:%d", s2, s1);
    }

    @Override
    public void close() throws IOException {
        if (cache != null) {
            cache.close();
        }
        cache = null;
        points = null;
    }
}
