import csv
import sys
from io import StringIO
from pathlib import Path

import pytest

from lazyman.tsplib import (
    TSPLibCSVConfig,
    build_center_value_index_memmap,
    create_center_tensor_memmap,
    lowest_values_by_center_memmap,
    next_center_value_index_entry,
    next_larger_point_index_memmap,
    open_center_value_index_memmap,
    open_center_tensor_memmap,
    read_csv_text,
)


def _sample_distance_matrix() -> list[list[float]]:
    return [
        [0.0, 2.0, 3.0, 5.0],
        [2.0, 0.0, 4.0, 6.0],
        [3.0, 4.0, 0.0, 7.0],
        [5.0, 6.0, 7.0, 0.0],
    ]


def _dataset_row_csv(instance_id: str) -> str:
    dataset_path = Path(__file__).parent / "data" / "tsp_dataset_sample.csv"
    csv.field_size_limit(sys.maxsize)

    with dataset_path.open(newline="", encoding="utf-8") as dataset_file:
        reader = csv.DictReader(dataset_file)
        fieldnames = reader.fieldnames
        assert fieldnames is not None

        for row in reader:
            if row["instance_id"] == instance_id:
                buffer = StringIO()
                writer = csv.DictWriter(buffer, fieldnames=fieldnames)
                writer.writeheader()
                writer.writerow(row)
                return buffer.getvalue()

    raise AssertionError(f"instance_id {instance_id} not found in {dataset_path}")


def test_create_center_tensor_memmap_and_reopen(tmp_path):
    np = pytest.importorskip("numpy")

    path = tmp_path / "center_tensor.dat"
    tensor = create_center_tensor_memmap(path, _sample_distance_matrix(), dtype="float32")

    assert tensor.shape == (4, 4, 4)
    assert tensor[1, 0, 2] == pytest.approx(6.0)
    assert tensor[1, 1, 2] == pytest.approx(-1.0)
    assert tensor[0, 2, 2] == pytest.approx(-1.0)

    reopened = open_center_tensor_memmap(path, shape=(4, 4, 4), dtype="float32", mode="r")
    assert isinstance(reopened, np.memmap)
    assert reopened[3, 0, 1] == pytest.approx(11.0)


def test_create_center_tensor_memmap_and_index_from_100_city_dataset_instance(tmp_path):
    np = pytest.importorskip("numpy")

    csv_text = _dataset_row_csv("56")
    csv.field_size_limit(sys.maxsize)
    [problem] = read_csv_text(
        csv_text,
        config=TSPLibCSVConfig(
            name_column="instance_id",
            dimension_column="num_cities",
            coordinates_column="city_coordinates",
            distance_matrix_column="distance_matrix",
            metadata_columns=["best_route", "total_distance"],
        ),
    )

    assert problem.dimension == 100
    assert problem.weight_matrix is not None

    tensor_path = tmp_path / "center_tensor_100.dat"
    index_path = tmp_path / "center_index_100"

    tensor = create_center_tensor_memmap(tensor_path, problem.weight_matrix, dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    assert isinstance(tensor, np.memmap)
    assert tensor.shape == (100, 100, 100)
    assert isinstance(center_index.all_values, np.memmap)
    assert center_index.size == 100
    assert center_index.all_values.shape[0] > 0
    assert center_index.all_pairs.shape[0] == center_index.all_values.shape[0]


def test_lowest_values_by_center_memmap(tmp_path):
    pytest.importorskip("numpy")

    path = tmp_path / "center_tensor.dat"
    tensor = create_center_tensor_memmap(path, _sample_distance_matrix(), dtype="float32")

    values = lowest_values_by_center_memmap(tensor)

    assert values == pytest.approx([5.0, 6.0, 7.0, 11.0])


def test_next_larger_point_index_memmap(tmp_path):
    pytest.importorskip("numpy")

    path = tmp_path / "center_tensor.dat"
    tensor = create_center_tensor_memmap(path, _sample_distance_matrix(), dtype="float32")

    pair = next_larger_point_index_memmap(tensor, center=0, y_index=1, z_index=2)

    assert pair == (1, 3)


def test_build_center_value_index_memmap(tmp_path):
    np = pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")

    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    assert isinstance(center_index.all_values, np.memmap)
    assert np.array_equal(center_index.all_values, np.array([5.0, 7.0, 8.0], dtype=np.float32))
    assert np.array_equal(center_index.all_pairs, np.array([[1, 2], [1, 3], [2, 3]], dtype=np.int32))
    assert np.array_equal(center_index.y_offsets, np.array([0, 0, 2, 4, 6], dtype=np.int64))
    assert np.array_equal(center_index.y_values, np.array([5.0, 7.0, 5.0, 8.0, 7.0, 8.0], dtype=np.float32))
    assert np.array_equal(
        center_index.y_pairs,
        np.array([[1, 2], [1, 3], [2, 1], [2, 3], [3, 1], [3, 2]], dtype=np.int32),
    )
    assert np.array_equal(center_index.z_offsets, np.array([0, 0, 2, 4, 6], dtype=np.int64))
    assert np.array_equal(center_index.z_values, np.array([5.0, 7.0, 5.0, 8.0, 7.0, 8.0], dtype=np.float32))
    assert np.array_equal(
        center_index.z_pairs,
        np.array([[2, 1], [3, 1], [1, 2], [3, 2], [1, 3], [2, 3]], dtype=np.int32),
    )


def test_open_center_value_index_memmap(tmp_path):
    np = pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    build_center_value_index_memmap(tensor, center=0, path=index_path)

    reopened = open_center_value_index_memmap(index_path, center=0, mode="r")

    assert isinstance(reopened.all_values, np.memmap)
    assert np.array_equal(reopened.all_values, np.array([5.0, 7.0, 8.0], dtype=np.float32))
    assert np.array_equal(reopened.z_offsets, np.array([0, 0, 2, 4, 6], dtype=np.int64))


def test_next_center_value_index_entry_all_values(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    assert next_center_value_index_entry(center_index, -1) == (5.0, (1, 2))
    assert next_center_value_index_entry(center_index, 0) == (7.0, (1, 3))
    assert next_center_value_index_entry(center_index, 2) is None


def test_next_center_value_index_entry_fixed_y(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    assert next_center_value_index_entry(center_index, -1, y_index=1) == (5.0, (1, 2))
    assert next_center_value_index_entry(center_index, 0, y_index=1) == (7.0, (1, 3))
    assert next_center_value_index_entry(center_index, 1, y_index=1) is None


def test_next_center_value_index_entry_fixed_z(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    assert next_center_value_index_entry(center_index, -1, z_index=3) == (7.0, (1, 3))
    assert next_center_value_index_entry(center_index, 0, z_index=3) == (8.0, (2, 3))
    assert next_center_value_index_entry(center_index, 1, z_index=3) is None


def test_next_center_value_index_entry_rejects_invalid_filters(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    with pytest.raises(ValueError, match="at most one"):
        next_center_value_index_entry(center_index, -1, y_index=1, z_index=2)

    with pytest.raises(ValueError, match=">= -1"):
        next_center_value_index_entry(center_index, -2)


def test_next_larger_point_index_memmap_uses_center_index(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    pair = next_larger_point_index_memmap(
        tensor,
        center=0,
        y_index=1,
        z_index=2,
        center_index=center_index,
    )

    assert pair == (1, 3)


def test_next_larger_point_index_memmap_with_fixed_y_constraint(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    pair = next_larger_point_index_memmap(
        tensor,
        center=0,
        y_index=1,
        z_index=2,
        center_index=center_index,
        constraint="fixed_y",
    )

    assert pair == (1, 3)


def test_next_larger_point_index_memmap_with_fixed_z_constraint(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    pair = next_larger_point_index_memmap(
        tensor,
        center=0,
        y_index=1,
        z_index=3,
        center_index=center_index,
        constraint="fixed_z",
    )

    assert pair == (2, 3)


def test_next_larger_point_index_memmap_fixed_constraint_returns_none(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    index_path = tmp_path / "center_index"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")
    center_index = build_center_value_index_memmap(tensor, center=0, path=index_path)

    pair = next_larger_point_index_memmap(
        tensor,
        center=0,
        y_index=1,
        z_index=3,
        center_index=center_index,
        constraint="fixed_y",
    )

    assert pair is None


def test_next_larger_point_index_memmap_returns_none_at_maximum(tmp_path):
    pytest.importorskip("numpy")

    path = tmp_path / "center_tensor.dat"
    tensor = create_center_tensor_memmap(path, _sample_distance_matrix(), dtype="float32")

    pair = next_larger_point_index_memmap(tensor, center=3, y_index=1, z_index=2)

    assert pair is None


def test_next_larger_point_index_memmap_builds_index_on_disk_by_default(tmp_path):
    pytest.importorskip("numpy")

    tensor_path = tmp_path / "center_tensor.dat"
    tensor = create_center_tensor_memmap(tensor_path, _sample_distance_matrix(), dtype="float32")

    pair = next_larger_point_index_memmap(
        tensor,
        center=0,
        y_index=1,
        z_index=2,
        index_path=tmp_path / "center_index",
    )

    assert pair == (1, 3)
    assert (tmp_path / "center_index" / "center_0" / "meta.json").exists()


def test_next_larger_point_index_memmap_rejects_invalid_current_cell(tmp_path):
    pytest.importorskip("numpy")

    path = tmp_path / "center_tensor.dat"
    tensor = create_center_tensor_memmap(path, _sample_distance_matrix(), dtype="float32")

    with pytest.raises(ValueError, match="invalid"):
        next_larger_point_index_memmap(tensor, center=0, y_index=0, z_index=2)


def test_open_center_tensor_memmap_rejects_non_cubic_shape(tmp_path):
    pytest.importorskip("numpy")

    path = tmp_path / "center_tensor.dat"
    create_center_tensor_memmap(path, _sample_distance_matrix(), dtype="float32")

    with pytest.raises(ValueError, match="cubic"):
        open_center_tensor_memmap(path, shape=(4, 4, 3), dtype="float32", mode="r")
