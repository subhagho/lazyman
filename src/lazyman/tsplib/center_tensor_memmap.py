from __future__ import annotations

from dataclasses import dataclass
import json
from pathlib import Path
from typing import TYPE_CHECKING, Literal

from lazyman.tsplib.center_tensor import DistanceMatrix, Pair

if TYPE_CHECKING:
    import numpy as np


MemmapMode = Literal["r", "r+", "w+", "c"]


@dataclass(frozen=True)
class CenterValueIndex:
    root_path: Path
    center: int
    size: int
    all_values: "np.ndarray"
    all_pairs: "np.ndarray"
    y_offsets: "np.ndarray"
    y_values: "np.ndarray"
    y_pairs: "np.ndarray"
    z_offsets: "np.ndarray"
    z_values: "np.ndarray"
    z_pairs: "np.ndarray"


def create_center_tensor_memmap(
    path: str | Path,
    distance_matrix: DistanceMatrix,
    *,
    dtype: str = "float32",
    mode: MemmapMode = "w+",
) -> "np.memmap":
    """
    Materialize the center tensor to a dense NumPy memmap on disk.

    Semantics match ``build_center_distance_tensor``:
    - tensor[x, y, z] = d(x,y) + d(x,z) for valid triples
    - tensor[x, y, z] = -1 when x == y, x == z, or y == z
    """
    np = _import_numpy()

    distance_array = np.asarray(distance_matrix, dtype=np.float64)
    size = _validate_distance_matrix(distance_array)
    target_path = Path(path)

    tensor = np.memmap(target_path, dtype=dtype, mode=mode, shape=(size, size, size))
    indexes = np.arange(size)
    same_index_mask = indexes[:, None] == indexes[None, :]

    for center in range(size):
        row = distance_array[center]
        center_slice = row[:, None] + row[None, :]

        invalid_mask = (
            (indexes[:, None] == center)
            | (indexes[None, :] == center)
            | same_index_mask
        )
        center_slice[invalid_mask] = -1.0
        tensor[center, :, :] = center_slice.astype(dtype, copy=False)

    tensor.flush()
    return tensor


def open_center_tensor_memmap(
    path: str | Path,
    shape: tuple[int, int, int],
    *,
    dtype: str = "float32",
    mode: MemmapMode = "r+",
) -> "np.memmap":
    np = _import_numpy()
    _validate_tensor_shape(shape)
    return np.memmap(Path(path), dtype=dtype, mode=mode, shape=shape)


def lowest_values_by_center_memmap(tensor: "np.ndarray") -> list[float]:
    np = _import_numpy()
    if tensor.ndim != 3:
        raise ValueError("tensor must be 3-dimensional")
    if tensor.shape[0] != tensor.shape[1] or tensor.shape[0] != tensor.shape[2]:
        raise ValueError("tensor must be cubic")

    lowest_values: list[float] = []
    for center in range(tensor.shape[0]):
        center_slice = tensor[center]
        positive_values = center_slice[center_slice > 0]
        if positive_values.size == 0:
            raise ValueError(f"no positive tensor value found for center index {center}")
        lowest_values.append(float(np.min(positive_values)))

    return lowest_values


def build_center_value_index_memmap(
    tensor: "np.ndarray",
    center: int,
    *,
    path: str | Path | None = None,
    mode: MemmapMode = "w+",
) -> CenterValueIndex:
    """
    Build a disk-backed sorted lookup index for one center slice.

    The index supports:
    1. unconstrained next-larger searches for ``(x, ?, ?)``
    2. next-larger searches with fixed ``y`` for ``(x, y, ?)``
    3. next-larger searches with fixed ``z`` for ``(x, ?, z)``
    """
    _validate_tensor(tensor)
    _validate_center_index(tensor, center)

    center_slice = tensor[center]
    valid_y, valid_z = _import_numpy().nonzero(center_slice > 0)
    if valid_y.size == 0:
        raise ValueError(f"no positive tensor value found for center index {center}")

    size = int(tensor.shape[0])
    root_path = _resolve_center_index_root(path, tensor)
    center_dir = _center_index_dir(root_path, center)
    center_dir.mkdir(parents=True, exist_ok=True)

    valid_values = center_slice[valid_y, valid_z]
    all_values, all_pairs = _build_lookup(valid_values, valid_y, valid_z)
    y_offsets, y_values, y_pairs = _build_grouped_lookup(
        valid_values,
        group_indexes=valid_y,
        tie_breaker_indexes=valid_z,
        pair_y_indexes=valid_y,
        pair_z_indexes=valid_z,
        size=size,
    )
    z_offsets, z_values, z_pairs = _build_grouped_lookup(
        valid_values,
        group_indexes=valid_z,
        tie_breaker_indexes=valid_y,
        pair_y_indexes=valid_y,
        pair_z_indexes=valid_z,
        size=size,
    )

    all_values_mm = _write_array_memmap(center_dir / "all_values.dat", all_values, mode=mode)
    all_pairs_mm = _write_array_memmap(center_dir / "all_pairs.dat", all_pairs, mode=mode)
    y_offsets_mm = _write_array_memmap(center_dir / "y_offsets.dat", y_offsets, mode=mode)
    y_values_mm = _write_array_memmap(center_dir / "y_values.dat", y_values, mode=mode)
    y_pairs_mm = _write_array_memmap(center_dir / "y_pairs.dat", y_pairs, mode=mode)
    z_offsets_mm = _write_array_memmap(center_dir / "z_offsets.dat", z_offsets, mode=mode)
    z_values_mm = _write_array_memmap(center_dir / "z_values.dat", z_values, mode=mode)
    z_pairs_mm = _write_array_memmap(center_dir / "z_pairs.dat", z_pairs, mode=mode)

    _write_center_index_metadata(
        center_dir,
        size=size,
        all_len=int(all_values.shape[0]),
        y_len=int(y_values.shape[0]),
        z_len=int(z_values.shape[0]),
        value_dtype=all_values.dtype.str,
        pair_dtype=all_pairs.dtype.str,
        offset_dtype=y_offsets.dtype.str,
    )

    return CenterValueIndex(
        root_path=root_path,
        center=center,
        size=size,
        all_values=all_values_mm,
        all_pairs=all_pairs_mm,
        y_offsets=y_offsets_mm,
        y_values=y_values_mm,
        y_pairs=y_pairs_mm,
        z_offsets=z_offsets_mm,
        z_values=z_values_mm,
        z_pairs=z_pairs_mm,
    )


def open_center_value_index_memmap(
    path: str | Path,
    center: int,
    *,
    mode: MemmapMode = "r",
) -> CenterValueIndex:
    np = _import_numpy()
    root_path = Path(path)
    center_dir = _center_index_dir(root_path, center)
    metadata = _read_center_index_metadata(center_dir)

    value_dtype = np.dtype(metadata["value_dtype"])
    pair_dtype = np.dtype(metadata["pair_dtype"])
    offset_dtype = np.dtype(metadata["offset_dtype"])
    all_len = int(metadata["all_len"])
    y_len = int(metadata["y_len"])
    z_len = int(metadata["z_len"])
    size = int(metadata["size"])

    return CenterValueIndex(
        root_path=root_path,
        center=center,
        size=size,
        all_values=np.memmap(center_dir / "all_values.dat", dtype=value_dtype, mode=mode, shape=(all_len,)),
        all_pairs=np.memmap(center_dir / "all_pairs.dat", dtype=pair_dtype, mode=mode, shape=(all_len, 2)),
        y_offsets=np.memmap(center_dir / "y_offsets.dat", dtype=offset_dtype, mode=mode, shape=(size + 1,)),
        y_values=np.memmap(center_dir / "y_values.dat", dtype=value_dtype, mode=mode, shape=(y_len,)),
        y_pairs=np.memmap(center_dir / "y_pairs.dat", dtype=pair_dtype, mode=mode, shape=(y_len, 2)),
        z_offsets=np.memmap(center_dir / "z_offsets.dat", dtype=offset_dtype, mode=mode, shape=(size + 1,)),
        z_values=np.memmap(center_dir / "z_values.dat", dtype=value_dtype, mode=mode, shape=(z_len,)),
        z_pairs=np.memmap(center_dir / "z_pairs.dat", dtype=pair_dtype, mode=mode, shape=(z_len, 2)),
    )


def next_larger_point_index_memmap(
    tensor: "np.ndarray",
    center: int,
    y_index: int,
    z_index: int,
    *,
    center_index: CenterValueIndex | None = None,
    index_path: str | Path | None = None,
    constraint: Literal["any", "fixed_y", "fixed_z"] = "any",
) -> Pair | None:
    """
    Return the (y, z) pair for the smallest valid tensor value that is
    strictly larger than ``tensor[center, y_index, z_index]``.

    ``constraint="fixed_y"`` restricts the search to ``(center, y_index, ?)``.
    ``constraint="fixed_z"`` restricts the search to ``(center, ?, z_index)``.

    Returns ``None`` when no larger valid value exists for the given center.
    """
    _validate_tensor(tensor)
    _validate_tensor_indexes(tensor, center, y_index, z_index)

    center_slice = tensor[center]
    current_value = float(center_slice[y_index, z_index])
    if current_value <= 0:
        raise ValueError("current tensor cell is invalid")

    if center_index is None:
        root_path = _resolve_center_index_root(index_path, tensor)
        if _center_index_metadata_path(root_path, center).exists():
            center_index = open_center_value_index_memmap(root_path, center, mode="r")
        else:
            center_index = build_center_value_index_memmap(
                tensor,
                center,
                path=root_path,
                mode="w+",
            )
    else:
        _validate_center_value_index(center_index)

    lookup = _lookup_for_constraint(center_index, y_index, z_index, constraint)
    return _next_larger_in_lookup(current_value, lookup)


def _build_lookup(
    values: "np.ndarray",
    y_indexes: "np.ndarray",
    z_indexes: "np.ndarray",
) -> tuple["np.ndarray", "np.ndarray"]:
    np = _import_numpy()
    order = np.lexsort((z_indexes, y_indexes, values))
    sorted_values = values[order]
    sorted_pairs = np.column_stack((y_indexes[order], z_indexes[order]))
    unique_mask = np.empty(sorted_values.shape[0], dtype=bool)
    unique_mask[0] = True
    unique_mask[1:] = sorted_values[1:] != sorted_values[:-1]

    return (
        sorted_values[unique_mask],
        sorted_pairs[unique_mask].astype(np.int32, copy=False),
    )


def _build_grouped_lookup(
    values: "np.ndarray",
    group_indexes: "np.ndarray",
    tie_breaker_indexes: "np.ndarray",
    pair_y_indexes: "np.ndarray",
    pair_z_indexes: "np.ndarray",
    *,
    size: int,
) -> tuple["np.ndarray", "np.ndarray", "np.ndarray"]:
    np = _import_numpy()
    order = np.lexsort((tie_breaker_indexes, values, group_indexes))
    sorted_groups = group_indexes[order]
    sorted_values = values[order]
    sorted_pairs = np.column_stack((pair_y_indexes[order], pair_z_indexes[order]))

    unique_mask = np.empty(sorted_values.shape[0], dtype=bool)
    unique_mask[0] = True
    unique_mask[1:] = (
        (sorted_groups[1:] != sorted_groups[:-1])
        | (sorted_values[1:] != sorted_values[:-1])
    )

    reduced_groups = sorted_groups[unique_mask]
    reduced_values = sorted_values[unique_mask]
    reduced_pairs = sorted_pairs[unique_mask].astype(np.int32, copy=False)

    counts = np.bincount(reduced_groups.astype(np.int64, copy=False), minlength=size)
    offsets = np.empty(size + 1, dtype=np.int64)
    offsets[0] = 0
    offsets[1:] = np.cumsum(counts, dtype=np.int64)
    return (offsets, reduced_values, reduced_pairs)


def _lookup_for_constraint(
    center_index: CenterValueIndex,
    y_index: int,
    z_index: int,
    constraint: Literal["any", "fixed_y", "fixed_z"],
) -> tuple["np.ndarray", "np.ndarray"]:
    if constraint == "any":
        return (center_index.all_values, center_index.all_pairs)
    if constraint == "fixed_y":
        return _slice_grouped_lookup(
            center_index.y_offsets,
            center_index.y_values,
            center_index.y_pairs,
            y_index,
        )
    if constraint == "fixed_z":
        return _slice_grouped_lookup(
            center_index.z_offsets,
            center_index.z_values,
            center_index.z_pairs,
            z_index,
        )
    raise ValueError(f"unsupported constraint: {constraint}")


def _slice_grouped_lookup(
    offsets: "np.ndarray",
    values: "np.ndarray",
    pairs: "np.ndarray",
    group_index: int,
) -> tuple["np.ndarray", "np.ndarray"]:
    start = int(offsets[group_index])
    end = int(offsets[group_index + 1])
    return (values[start:end], pairs[start:end])


def _next_larger_in_lookup(
    current_value: float,
    lookup: tuple["np.ndarray", "np.ndarray"],
) -> Pair | None:
    np = _import_numpy()
    sorted_values, sorted_pairs = lookup
    next_position = int(np.searchsorted(sorted_values, current_value, side="right"))
    if next_position >= int(sorted_values.shape[0]):
        return None

    next_y, next_z = sorted_pairs[next_position]
    return (int(next_y), int(next_z))


def _validate_distance_matrix(distance_matrix: "np.ndarray") -> int:
    if distance_matrix.ndim != 2:
        raise ValueError("distance matrix must be 2-dimensional")
    if distance_matrix.shape[0] == 0:
        raise ValueError("distance matrix cannot be empty")
    if distance_matrix.shape[0] != distance_matrix.shape[1]:
        raise ValueError("distance matrix must be square")
    return int(distance_matrix.shape[0])


def _validate_tensor_shape(shape: tuple[int, int, int]) -> None:
    if len(shape) != 3:
        raise ValueError("shape must contain exactly 3 dimensions")
    if shape[0] <= 0 or shape[1] <= 0 or shape[2] <= 0:
        raise ValueError("shape dimensions must be positive")
    if not (shape[0] == shape[1] == shape[2]):
        raise ValueError("tensor shape must be cubic")


def _validate_tensor(tensor: "np.ndarray") -> None:
    if tensor.ndim != 3:
        raise ValueError("tensor must be 3-dimensional")
    _validate_tensor_shape(tensor.shape)


def _validate_center_index(tensor: "np.ndarray", center: int) -> None:
    size = int(tensor.shape[0])
    if center < 0 or center >= size:
        raise IndexError(f"center index out of range: {center}")


def _validate_tensor_indexes(
    tensor: "np.ndarray",
    center: int,
    y_index: int,
    z_index: int,
) -> None:
    size = int(tensor.shape[0])
    _validate_center_index(tensor, center)
    if y_index < 0 or y_index >= size:
        raise IndexError(f"node index out of range: {y_index}")
    if z_index < 0 or z_index >= size:
        raise IndexError(f"node index out of range: {z_index}")


def _validate_center_value_index(center_index: CenterValueIndex) -> None:
    if center_index.size <= 0:
        raise ValueError("center index size must be positive")
    _validate_lookup_arrays(center_index.all_values, center_index.all_pairs)
    _validate_offset_array(center_index.y_offsets, center_index.size)
    _validate_lookup_arrays(center_index.y_values, center_index.y_pairs)
    _validate_offset_array(center_index.z_offsets, center_index.size)
    _validate_lookup_arrays(center_index.z_values, center_index.z_pairs)


def _validate_lookup_arrays(
    sorted_values: "np.ndarray",
    sorted_pairs: "np.ndarray",
) -> None:
    if sorted_values.ndim != 1:
        raise ValueError("center index values must be 1-dimensional")
    if sorted_pairs.ndim != 2 or sorted_pairs.shape[1] != 2:
        raise ValueError("center index pairs must have shape (n, 2)")
    if sorted_values.shape[0] != sorted_pairs.shape[0]:
        raise ValueError("center index values and pairs must have matching lengths")


def _validate_offset_array(offsets: "np.ndarray", size: int) -> None:
    if offsets.ndim != 1:
        raise ValueError("center index offsets must be 1-dimensional")
    if offsets.shape[0] != size + 1:
        raise ValueError("center index offsets must have length size + 1")


def _resolve_center_index_root(
    path: str | Path | None,
    tensor: "np.ndarray",
) -> Path:
    if path is not None:
        return Path(path)

    filename = getattr(tensor, "filename", None)
    if filename is None:
        raise ValueError("index path is required when tensor is not file-backed")

    tensor_path = Path(filename)
    return tensor_path.with_name(f"{tensor_path.name}.center_index")


def _center_index_dir(root_path: str | Path, center: int) -> Path:
    return Path(root_path) / f"center_{center}"


def _center_index_metadata_path(root_path: str | Path, center: int) -> Path:
    return _center_index_dir(root_path, center) / "meta.json"


def _write_center_index_metadata(
    center_dir: Path,
    *,
    size: int,
    all_len: int,
    y_len: int,
    z_len: int,
    value_dtype: str,
    pair_dtype: str,
    offset_dtype: str,
) -> None:
    metadata = {
        "size": size,
        "all_len": all_len,
        "y_len": y_len,
        "z_len": z_len,
        "value_dtype": value_dtype,
        "pair_dtype": pair_dtype,
        "offset_dtype": offset_dtype,
    }
    with (center_dir / "meta.json").open("w", encoding="utf-8") as metadata_file:
        json.dump(metadata, metadata_file)


def _read_center_index_metadata(center_dir: Path) -> dict[str, int | str]:
    metadata_path = center_dir / "meta.json"
    with metadata_path.open("r", encoding="utf-8") as metadata_file:
        return json.load(metadata_file)


def _write_array_memmap(
    path: Path,
    array: "np.ndarray",
    *,
    mode: MemmapMode,
) -> "np.memmap":
    np = _import_numpy()
    memmap = np.memmap(path, dtype=array.dtype, mode=mode, shape=array.shape)
    memmap[...] = array
    memmap.flush()
    return memmap


def _import_numpy():
    try:
        import numpy as np
    except ImportError as exc:
        raise ImportError(
            "numpy is required for memmap-backed center tensors. "
            "Install with: pip install -e .[viz]"
        ) from exc
    return np
