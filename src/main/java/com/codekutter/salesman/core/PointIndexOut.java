package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Path;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public class PointIndexOut {
    private final Map<Integer, PointIndexIn.PointInfo[]> outPoints;

    public PointIndexOut(int size) {
        Preconditions.checkArgument(size > 0);
        outPoints = new HashMap<>(size);
    }

    public int size() {
        if (outPoints != null) return outPoints.size();
        return 0;
    }

    public void put(int sequence, int replace, @NonNull Path path) throws IndexOutOfBoundsException {
        Preconditions.checkArgument(sequence > 0);
        Preconditions.checkArgument(path.A().sequence() == sequence || path.B().sequence() == sequence);

        boolean added = false;
        PointIndexIn.PointInfo pi = new PointIndexIn.PointInfo();
        int target = -1;
        if (sequence == path.A().sequence()) {
            target = path.B().sequence();
        } else {
            target = path.A().sequence();
        }

        pi.sequence(target);
        pi.distance(path.distance());
        PointIndexIn.PointInfo[] array = outPoints.get(sequence);
        if (array == null) {
            array = new PointIndexIn.PointInfo[2];
            outPoints.put(sequence, array);

            array[0] = pi;
            added = true;
        } else {
            if (array[1] == null) {
                array[1] = pi;
                added = true;
            }
        }
        if (!added) {
            if (array[0].sequence() == replace) {
                array[0] = pi;
                added = true;
            } else if (array[1].sequence() == replace) {
                array[1] = pi;
                added = true;
            }
        }
        if (!added) {
            throw new IndexOutOfBoundsException(String.format("[sequence=%d] Indexes filled and no replacement provided.", sequence));
        }
    }

    public PointIndexIn.PointInfo[] getPoints(int sequence) {
        if (outPoints.containsKey(sequence)) {
            return outPoints.get(sequence);
        }
        return null;
    }
}
