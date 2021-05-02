package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Connections;
import lombok.NonNull;

public class RunIterator {
    private final TSPDataMap data;
    private final Connections connections;

    public RunIterator(@NonNull TSPDataMap data, @NonNull Connections connections) {
        this.data = data;
        this.connections = connections;
    }

    public void run(int iteration, int index) {

    }
}
