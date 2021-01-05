package com.codekutter.salesman.core;

import com.codekutter.salesman.core.model.Path;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public class PointIndexOut {
    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class PointInfo {
        private int sequence;
        private double distance;

        public boolean equalsTo(@NonNull PointInfo target) {
            return sequence == target.sequence;
        }
    }
    
    private final Map<Integer, PointInfo[]> outPoints;

    public PointIndexOut(int size) {
        Preconditions.checkArgument(size > 0);
        outPoints = new HashMap<>(size);
    }

    public int size() {
        if (outPoints != null) return outPoints.size();
        return 0;
    }

    public PointInfo[] put(int sequence) throws IndexOutOfBoundsException {
        Preconditions.checkArgument(sequence > 0);
        if (outPoints.containsKey(sequence))
            throw new IndexOutOfBoundsException(String.format("Sequence already added. [sequence=%d]", sequence));
        PointInfo[] pis = new PointInfo[2];
        pis[0] = null;
        pis[1] = null;
        outPoints.put(sequence, pis);

        return pis;
    }

    public void put(int sequence, @NonNull Path path) throws IndexOutOfBoundsException {
        Preconditions.checkArgument(sequence > 0);
        if (!outPoints.containsKey(sequence)) {
            put(sequence);
        }
        PointInfo[] pis = outPoints.get(sequence);
        if (pis[0] != null && pis[1] != null)
            throw new IndexOutOfBoundsException(String.format("Sequence already full. [sequence=%d]", sequence));
        int findex = -1;
        if (pis[0] == null) findex = 0;
        else findex = 1;

        PointInfo pi = new PointInfo();
        int target = -1;
        if (sequence == path.A().sequence()) {
            target = path.B().sequence();
        } else {
            target = path.A().sequence();
        }

        pi.sequence(target);
        pi.distance(path.distance());
        pis[findex] = pi;
    }

    public boolean remove(int sequence, int target) throws IndexOutOfBoundsException {
        Preconditions.checkArgument(sequence > 0);
        Preconditions.checkArgument(target > 0 && target != sequence);
        PointInfo[] pis = outPoints.get(sequence);
        if (pis == null) {
            throw new IndexOutOfBoundsException(String.format("Sequence not added. [sequence=%d]", sequence));
        }
        if (pis[0].sequence() == target) {
            pis[0] = null;
            return true;
        } else if (pis[1].sequence() == target) {
            pis[1] = null;
            return true;
        }
        return false;
    }

    public void put(int sequence, int replace, @NonNull Path path) throws IndexOutOfBoundsException {
        Preconditions.checkArgument(sequence > 0);
        Preconditions.checkArgument(path.A().sequence() == sequence || path.B().sequence() == sequence);

        boolean added = false;
        PointInfo pi = new PointInfo();
        int target = -1;
        if (sequence == path.A().sequence()) {
            target = path.B().sequence();
        } else {
            target = path.A().sequence();
        }

        pi.sequence(target);
        pi.distance(path.distance());
        PointInfo[] array = outPoints.get(sequence);
        if (array == null) {
            array = new PointInfo[2];
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

    public PointInfo[] getPoints(int sequence) {
        if (outPoints.containsKey(sequence)) {
            return outPoints.get(sequence);
        }
        return null;
    }
}
