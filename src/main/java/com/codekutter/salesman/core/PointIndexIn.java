package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Path;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public class PointIndexIn {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class PointInfo {
        private int sequence;
        private double distance;
    }

    private final Map<Integer, List<PointInfo>> inPoints;

    public PointIndexIn(int size) {
        Preconditions.checkArgument(size > 0);
        inPoints = new HashMap<>(size);
    }

    public int size() {
        return inPoints.size();
    }

    public List<PointInfo> find(int sequence) {
        Preconditions.checkArgument(sequence > 0);
        if (inPoints.containsKey(sequence)) {
            return inPoints.get(sequence);
        }
        return null;
    }

    public void remove(@NonNull Path path) throws IllegalArgumentException {
        if (!remove(path.A().sequence(), path.B().sequence())) {
            throw new IllegalArgumentException(
                    String.format("No path found for [source=%d][target=%d]",
                            path.A().sequence(), path.B().sequence()));
        }
        if (!remove(path.B().sequence(), path.A().sequence())) {
            throw new IllegalArgumentException(
                    String.format("No path found for [source=%d][target=%d]",
                            path.B().sequence(), path.A().sequence()));
        }
    }

    public void add(@NonNull Path path) {
        add(path.A().sequence(), path.B().sequence(), path.distance());
        add(path.B().sequence(), path.A().sequence(), path.distance());
    }

    private void add(int sequence, int target, double distance) {
        PointInfo pi = new PointInfo();
        pi.sequence = target;
        pi.distance = distance;

        List<PointInfo> points = inPoints.get(sequence);
        if (points == null) {
            points = new ArrayList<>();
            inPoints.put(sequence, points);
        }
        points.add(pi);
    }

    private boolean remove(int sequence, int target) {
        Preconditions.checkArgument(sequence > 0);
        Preconditions.checkArgument(target > 0);
        if (inPoints != null && inPoints.containsKey(sequence)) {
            List<PointInfo> points = inPoints.get(sequence);
            if (points != null) {
                for (PointInfo pi : points) {
                    if (pi != null && pi.sequence == target)
                        return true;
                }
            }
        }
        return false;
    }

    public boolean clear(int sequence) {
        Preconditions.checkArgument(sequence > 0);
        if (inPoints != null && inPoints.containsKey(sequence)) {
            List<PointInfo> points = inPoints.get(sequence);
            if (points != null) {
                points.clear();
                return true;
            }
        }
        return false;
    }
}
