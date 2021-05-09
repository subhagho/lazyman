package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Ring;
import lombok.NonNull;

import java.util.List;

public class RingProcessor {
    public RunIterator process(@NonNull List<Ring> rings, @NonNull String name) {
        int pcount = 0;
        for (Ring ring : rings) {
            if (ring.isClosed()) {
                pcount++;
            } else {
                pcount += 2;
            }
        }
        TSPDataMap map = new TSPDataMap();
        map.init(name, pcount);


        return null;
    }
}
