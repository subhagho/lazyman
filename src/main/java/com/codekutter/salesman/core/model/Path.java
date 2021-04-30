package com.codekutter.salesman.core.model;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.openhft.chronicle.bytes.BytesMarshallable;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Accessors(fluent = true)
public class Path implements BytesMarshallable, Comparable<Path> {
    @Setter(AccessLevel.NONE)
    private Point A;
    @Setter(AccessLevel.NONE)
    private Point B;
    private final double length;

    public Path(@NonNull Point A, @NonNull Point B, double length) {
        this.A = A;
        this.B = B;
        this.length = length;
    }

    public Path(@NonNull Point A, @NonNull Point B) {
        Preconditions.checkArgument(A.Y() != null && A.X() != null);
        Preconditions.checkArgument(B.Y() != null && B.X() != null);

        length = Math.sqrt(Math.pow((A.X() - B.X()), 2) + Math.pow((A.Y() - B.Y()), 2));
        this.A = A;
        this.B = B;
    }

    public double distance() {
        if (A != null && B != null) {
            double h = A.elevation() - B.elevation();

        }
        return -1;
    }

    public Point getTarget(@NonNull Point source) {
        if (A.sequence() == source.sequence()) {
            return B;
        } else if (B.sequence() == source.sequence()) {
            return A;
        }
        return null;
    }

    public Point getTarget(int source) {
        if (A.sequence() == source) {
            return B;
        } else if (B.sequence() == source) {
            return A;
        }
        return null;
    }

    @Override
    public int compareTo(@NotNull Path o) {
        return (int) (distance() - o.distance());
    }
}
