package com.codekutter.salesman.core.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public class Ring {
    private final short number;
    private List<Path> ring = new ArrayList<>();
    private boolean isClosed = true;

    public Ring(short number) {
        this.number = number;
    }

    public Ring add(@NonNull Path connection) {
        ring.add(connection);
        return this;
    }
}
