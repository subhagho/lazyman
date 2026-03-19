from __future__ import annotations

from typing import TypeAlias

DistanceMatrix: TypeAlias = list[list[float]]
Pair: TypeAlias = tuple[int, int]
PairComponents: TypeAlias = tuple[tuple[float, float], tuple[float, float]]


def build_center_distance_tensor(distance_matrix: DistanceMatrix) -> list[list[list[float]]]:
    """
    Build tensor T where T[x][y][z] = d(x,y) + d(x,z) for distinct points.

    Invalid triples are marked with -1:

    - T[x][y][z] = -1 when x == y
    - T[x][y][z] = -1 when x == z
    - T[x][y][z] = -1 when y == z
    """
    size = _validate_distance_matrix(distance_matrix)

    tensor: list[list[list[float]]] = []
    for center in range(size):
        by_y: list[list[float]] = []
        for y_index in range(size):
            by_z: list[float] = []
            for z_index in range(size):
                if y_index == center or z_index == center or y_index == z_index:
                    value = -1.0
                else:
                    value = distance_matrix[center][y_index] + distance_matrix[center][z_index]
                by_z.append(value)
            by_y.append(by_z)
        tensor.append(by_y)
    return tensor


def lowest_values_by_center(distance_matrix: DistanceMatrix) -> list[float]:
    """
    Return the minimum positive tensor value for each center index x.

    Equivalent to:
    min(tensor[x][y][z] for all y, z if tensor[x][y][z] > 0)
    """
    tensor = build_center_distance_tensor(distance_matrix)
    lowest_values: list[float] = []

    for center, center_slice in enumerate(tensor):
        positive_values = [
            value
            for row in center_slice
            for value in row
            if value > 0
        ]
        if not positive_values:
            raise ValueError(f"no positive tensor value found for center index {center}")
        lowest_values.append(min(positive_values))

    return lowest_values


def next_larger_point_index(
    distance_matrix: DistanceMatrix,
    center: int,
    y_index: int,
    z_index: int,
) -> Pair | None:
    """
    Return the (y, z) index pair for the smallest valid tensor value
    that is strictly larger than tensor[center][y_index][z_index].

    Returns ``None`` when no larger valid value exists for the given center.
    """
    tensor = build_center_distance_tensor(distance_matrix)
    _validate_tensor_indexes(tensor, center, y_index, z_index)

    current_value = tensor[center][y_index][z_index]
    if current_value <= 0:
        raise ValueError("current tensor cell is invalid")

    best_pair: Pair | None = None
    best_value: float | None = None

    for candidate_y, row in enumerate(tensor[center]):
        for candidate_z, value in enumerate(row):
            if value <= current_value:
                continue
            if best_value is None or value < best_value:
                best_value = value
                best_pair = (candidate_y, candidate_z)
            elif value == best_value and best_pair is not None:
                candidate_pair = (candidate_y, candidate_z)
                if candidate_pair < best_pair:
                    best_pair = candidate_pair

    return best_pair


def pair_distance_sum(distance_matrix: DistanceMatrix, center: int, pair: Pair) -> float:
    """Return d(center, y) + d(center, z) for a single (y, z) pair."""
    y_index, z_index = pair
    return _distance(distance_matrix, center, y_index) + _distance(distance_matrix, center, z_index)


def two_pair_distance_sums(
    distance_matrix: DistanceMatrix,
    center: int,
    pair_one: Pair,
    pair_two: Pair,
) -> tuple[float, float]:
    """
    Return:
    1. d(center, y1) + d(center, z1)
    2. d(center, y2) + d(center, z2)
    """
    return (
        pair_distance_sum(distance_matrix, center, pair_one),
        pair_distance_sum(distance_matrix, center, pair_two),
    )


def two_pair_distance_components(
    distance_matrix: DistanceMatrix,
    center: int,
    pair_one: Pair,
    pair_two: Pair,
) -> PairComponents:
    """
    Return:
    1. (d(center, y1), d(center, z1))
    2. (d(center, y2), d(center, z2))
    """
    y1, z1 = pair_one
    y2, z2 = pair_two
    return (
        (_distance(distance_matrix, center, y1), _distance(distance_matrix, center, z1)),
        (_distance(distance_matrix, center, y2), _distance(distance_matrix, center, z2)),
    )


def _validate_distance_matrix(distance_matrix: DistanceMatrix) -> int:
    if not distance_matrix:
        raise ValueError("distance matrix cannot be empty")

    size = len(distance_matrix)
    for row in distance_matrix:
        if len(row) != size:
            raise ValueError("distance matrix must be square")
    return size


def _distance(distance_matrix: DistanceMatrix, row: int, column: int) -> float:
    size = len(distance_matrix)
    if row < 0 or row >= size:
        raise IndexError(f"center index out of range: {row}")
    if column < 0 or column >= size:
        raise IndexError(f"node index out of range: {column}")
    return distance_matrix[row][column]


def _validate_tensor_indexes(
    tensor: list[list[list[float]]],
    center: int,
    y_index: int,
    z_index: int,
) -> None:
    size = len(tensor)
    if center < 0 or center >= size:
        raise IndexError(f"center index out of range: {center}")
    if y_index < 0 or y_index >= size:
        raise IndexError(f"node index out of range: {y_index}")
    if z_index < 0 or z_index >= size:
        raise IndexError(f"node index out of range: {z_index}")
