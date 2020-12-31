package com.codekutter.salesman.core.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.openhft.chronicle.bytes.BytesMarshallable;

@Getter
@Setter
@Accessors(fluent = true)
public class Point implements BytesMarshallable {
    private int sequence;
    private double X;
    private double Y;

    @Override
    public String toString() {
        return "Point{" +
                "sequence=" + sequence +
                ", X=" + X +
                ", Y=" + Y +
                '}';
    }
}
