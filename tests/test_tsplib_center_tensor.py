import pytest

from lazyman.tsplib import (
    build_center_distance_tensor,
    lowest_values_by_center,
    next_larger_point_index,
    pair_distance_sum,
    two_pair_distance_components,
    two_pair_distance_sums,
)


def _sample_distance_matrix() -> list[list[float]]:
    return [
        [0.0, 2.0, 3.0, 5.0],
        [2.0, 0.0, 4.0, 6.0],
        [3.0, 4.0, 0.0, 7.0],
        [5.0, 6.0, 7.0, 0.0],
    ]


def test_build_center_distance_tensor_values():
    matrix = _sample_distance_matrix()

    tensor = build_center_distance_tensor(matrix)

    assert len(tensor) == 4
    assert len(tensor[0]) == 4
    assert len(tensor[0][0]) == 4

    # three distinct points => computed sum
    assert tensor[1][0][2] == pytest.approx(6.0)  # 2 + 4
    # x == y => invalid
    assert tensor[1][1][2] == pytest.approx(-1.0)
    # x == z => invalid
    assert tensor[3][1][3] == pytest.approx(-1.0)
    # y == z => invalid
    assert tensor[0][2][2] == pytest.approx(-1.0)
    # x == y == z => invalid
    assert tensor[2][2][2] == pytest.approx(-1.0)


def test_two_pair_distance_sums():
    matrix = _sample_distance_matrix()

    first, second = two_pair_distance_sums(matrix, center=0, pair_one=(1, 2), pair_two=(2, 3))

    assert first == pytest.approx(5.0)  # 2 + 3
    assert second == pytest.approx(8.0)  # 3 + 5


def test_two_pair_distance_components():
    matrix = _sample_distance_matrix()

    first, second = two_pair_distance_components(matrix, center=2, pair_one=(0, 1), pair_two=(1, 3))

    assert first[0] == pytest.approx(3.0)
    assert first[1] == pytest.approx(4.0)
    assert second[0] == pytest.approx(4.0)
    assert second[1] == pytest.approx(7.0)


def test_lowest_values_by_center():
    matrix = _sample_distance_matrix()

    values = lowest_values_by_center(matrix)

    assert values == pytest.approx([5.0, 6.0, 7.0, 11.0])


def test_next_larger_point_index():
    matrix = _sample_distance_matrix()

    # center=0, (1,2) => 2 + 3 = 5, next larger valid pair is (1,3) => 7
    pair = next_larger_point_index(matrix, center=0, y_index=1, z_index=2)

    assert pair == (1, 3)


def test_next_larger_point_index_returns_none_at_maximum():
    matrix = _sample_distance_matrix()

    pair = next_larger_point_index(matrix, center=3, y_index=1, z_index=2)

    assert pair is None


def test_next_larger_point_index_rejects_invalid_current_cell():
    matrix = _sample_distance_matrix()

    with pytest.raises(ValueError, match="invalid"):
        next_larger_point_index(matrix, center=0, y_index=0, z_index=2)


def test_pair_distance_sum_out_of_range_raises():
    matrix = _sample_distance_matrix()

    with pytest.raises(IndexError):
        pair_distance_sum(matrix, center=10, pair=(1, 2))

    with pytest.raises(IndexError):
        pair_distance_sum(matrix, center=1, pair=(1, 9))


def test_build_center_distance_tensor_rejects_non_square_matrix():
    with pytest.raises(ValueError, match="square"):
        build_center_distance_tensor([[0.0, 1.0], [1.0]])
