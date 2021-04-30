package com.codekutter.salesman.core.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.openhft.chronicle.bytes.BytesMarshallable;

@Getter
@Setter
@ToString
@Accessors(fluent = true)
public class Point implements BytesMarshallable {
    private int sequence;
    private Double X = null;
    private Double Y = null;
    private double elevation = 0;
}
