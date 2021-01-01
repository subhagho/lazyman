package com.codekutter.salesman.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

@Getter
@Accessors(fluent = true)
public class Config {
    public static final String DEFAULT_MAPS_DIR = "maps";

    private String workingDir;
    private String mapsDataDir;
    private Properties config;
    private RunInfo runInfo;

    private void load(@NonNull String filename) {
        try {
            config = new Properties();
            config.load(new FileInputStream(filename));

            initWorkingDir();
            runInfo = new RunInfo();
            runInfo.init(workingDir);
        } catch (Throwable t) {
            throw new RuntimeException("Error loading configuration.", t);
        }
    }

    private void initWorkingDir() throws Exception {
        String var = config.getProperty(Constants.CONFIG_WORKING_DIR);
        if (!Strings.isNullOrEmpty(var)) {
            workingDir = var;
        } else {
            workingDir = Constants.DEFAULT_WORKING_DIR;
        }
        File di = new File(workingDir);
        if (!di.exists()) {
            if (!di.mkdirs()) {
                throw new Exception(String.format("Error creating Working directory. [path=%s]", di.getAbsolutePath()));
            }
        }
        workingDir = di.getAbsolutePath();
        LogUtils.info(getClass(), String.format("Using Working Directory: %s%n", di.getAbsolutePath()));

        var = String.format("%s/%s", workingDir, DEFAULT_MAPS_DIR);
        di = new File(var);
        if (!di.exists()) {
            if (!di.mkdir()) {
                throw new Exception(String.format("Error creating Maps Data directory. [path=%s]", di.getAbsolutePath()));
            }
        }

        mapsDataDir = di.getAbsolutePath();
    }

    public String setupMapFile(@NonNull String mapname, boolean clear) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mapname));
        String fname = String.format("%s/%s", mapsDataDir, mapname);
        File f = new File(fname);
        if (clear) {
            if (f.exists()) {
                if (!f.delete()) {
                    throw new Exception(String.format("Failed to delete existing file. [path=%s]", f.getAbsolutePath()));
                }
            }
        }
        LogUtils.debug(getClass(), String.format("Setup Map file. [path=%s]", f.getAbsolutePath()));
        return f.getAbsolutePath();
    }

    private static final Config __instance = new Config();

    public static void init(@NonNull String configFile) {
        __instance.load(configFile);
    }

    public static Config get() {
        return __instance;
    }
}
