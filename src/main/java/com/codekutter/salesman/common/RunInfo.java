package com.codekutter.salesman.common;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
public class RunInfo {
    private String rootDir;
    private String runId;
    private long startTime;

    public void init(@NonNull String wd) {
        Date date = Calendar.getInstance().getTime();
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss");
        String datePath = df.format(date);
        String path = String.format("%s/%s", wd, datePath);
        File di = new File(path);
        if (!di.exists()) {
            if (!di.mkdirs()) {
                throw new RuntimeException(String.format("Error creating output folder. [path=%s]", di.getAbsolutePath()));
            }
        }
        runId = UUID.randomUUID().toString();
        startTime = date.getTime();
        rootDir = di.getAbsolutePath();
    }

    public String createOutputDir(@NonNull String name) throws IOException {
        File di = new File(String.format("%s/%s", rootDir, name));
        if (!di.exists()) {
            if (!di.mkdirs()) {
                throw new IOException(String.format("Failed to create output folder. [path=%s]", di.getAbsolutePath()));
            }
        }
        return di.getAbsolutePath();
    }
}
