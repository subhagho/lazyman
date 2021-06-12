package com.codekutter.salesman.core.model;

import com.codekutter.salesman.core.TSPDataMap;
import lombok.NonNull;

import java.util.List;

public class OpenRing extends Ring {
    public OpenRing(short number) {
        super(number);
    }

    public OpenRing(@NonNull Ring source) {
        super(source, false);
    }

    @Override
    public void computeConnections(@NonNull Connections connections, @NonNull TSPDataMap data, @NonNull List<Ring> rings) {

    }
}
