package com.codekutter.lazyman.v2.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class IndexedPath {
    private int index;
    private Path path;
    private Path next;
}
