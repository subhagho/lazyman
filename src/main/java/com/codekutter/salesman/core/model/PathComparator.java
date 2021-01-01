package com.codekutter.salesman.core.model;

import java.util.Comparator;

public class PathComparator implements Comparator<Path> {
    @Override
    public int compare(Path o1, Path o2) {
        return o1.compareTo(o2);
    }
}
