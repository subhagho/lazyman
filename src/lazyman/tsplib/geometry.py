from __future__ import annotations

from dataclasses import dataclass
from math import dist

from lazyman.tsplib.enums import EdgeWeightFormat, EdgeWeightType
from lazyman.tsplib.models import Node, TSPLibProblem


@dataclass(frozen=True)
class GeometryCompletionConfig:
    coordinate_dim: int = 2
    normalize_coordinates: bool = True
    asymmetric_strategy: str = "raise"
    symmetry_tolerance: float = 1e-8


def complete_problem_geometry(
    problem: TSPLibProblem,
    config: GeometryCompletionConfig = GeometryCompletionConfig(),
) -> TSPLibProblem:
    if config.coordinate_dim not in {2, 3}:
        raise ValueError("coordinate_dim must be 2 or 3")
    if config.asymmetric_strategy not in {"raise", "symmetrize"}:
        raise ValueError("asymmetric_strategy must be 'raise' or 'symmetrize'")
    if config.symmetry_tolerance < 0:
        raise ValueError("symmetry_tolerance must be non-negative")

    has_nodes = bool(problem.nodes)
    has_distances = problem.weight_matrix is not None

    if has_nodes and has_distances:
        return problem

    if has_nodes:
        return _complete_distances_from_nodes(problem)

    if has_distances:
        return _complete_nodes_from_distances(problem, config)

    raise ValueError("problem must include either nodes or weight_matrix")


def _complete_distances_from_nodes(problem: TSPLibProblem) -> TSPLibProblem:
    points = [_node_to_point(node) for node in problem.nodes]
    matrix = [[dist(left, right) for right in points] for left in points]

    edge_weight_type = problem.edge_weight_type
    if edge_weight_type is None:
        has_z = any(node.z is not None for node in problem.nodes)
        edge_weight_type = EdgeWeightType.EUC_3D if has_z else EdgeWeightType.EUC_2D

    return problem.model_copy(
        update={
            "weight_matrix": matrix,
            "edge_weight_format": EdgeWeightFormat.FULL_MATRIX,
            "edge_weight_type": edge_weight_type,
        }
    )


def _complete_nodes_from_distances(
    problem: TSPLibProblem,
    config: GeometryCompletionConfig,
) -> TSPLibProblem:
    matrix = problem.weight_matrix
    if matrix is None:
        raise ValueError("weight_matrix is required to derive coordinates")

    coordinates = _coordinates_from_distance_matrix(
        matrix,
        target_dim=config.coordinate_dim,
        normalize=config.normalize_coordinates,
        asymmetric_strategy=config.asymmetric_strategy,
        symmetry_tolerance=config.symmetry_tolerance,
    )
    nodes = _nodes_from_coordinate_rows(coordinates)

    edge_weight_type = problem.edge_weight_type
    if edge_weight_type is None:
        edge_weight_type = EdgeWeightType.EUC_3D if config.coordinate_dim == 3 else EdgeWeightType.EUC_2D

    return problem.model_copy(
        update={
            "nodes": nodes,
            "edge_weight_type": edge_weight_type,
        }
    )


def _coordinates_from_distance_matrix(
    matrix: list[list[float]],
    *,
    target_dim: int,
    normalize: bool,
    asymmetric_strategy: str,
    symmetry_tolerance: float,
) -> list[list[float]]:
    np = _import_numpy()

    distance_array = np.array(matrix, dtype=float)
    _validate_square_matrix(distance_array)

    if asymmetric_strategy == "raise":
        if not np.allclose(distance_array, distance_array.T, atol=symmetry_tolerance):
            raise ValueError("distance matrix must be symmetric to derive Euclidean coordinates")
    else:
        distance_array = (distance_array + distance_array.T) / 2.0

    if np.any(distance_array < 0):
        raise ValueError("distance matrix cannot contain negative values")

    n = distance_array.shape[0]
    if n < 2:
        raise ValueError("distance matrix must contain at least 2 points")

    squared = distance_array**2
    centering = np.eye(n) - np.ones((n, n)) / n
    gram = -0.5 * centering @ squared @ centering

    eigenvalues, eigenvectors = np.linalg.eigh(gram)
    indices = np.argsort(eigenvalues)[::-1]
    eigenvalues = eigenvalues[indices]
    eigenvectors = eigenvectors[:, indices]

    positive = eigenvalues > 1e-12
    if not np.any(positive):
        raise ValueError("could not derive coordinates from distance matrix")

    selected_values = eigenvalues[positive][:target_dim]
    selected_vectors = eigenvectors[:, positive][:, :target_dim]
    coords = selected_vectors * np.sqrt(selected_values)

    if coords.shape[1] < target_dim:
        pad_width = target_dim - coords.shape[1]
        coords = np.hstack((coords, np.zeros((n, pad_width))))

    if normalize:
        coords = _normalize_coordinate_axes(coords)

    return coords.tolist()


def _nodes_from_coordinate_rows(rows: list[list[float]]) -> list[Node]:
    nodes: list[Node] = []
    for index, row in enumerate(rows, start=1):
        node_kwargs = {
            "id": index,
            "x": float(row[0]),
            "y": float(row[1]),
        }
        if len(row) >= 3:
            node_kwargs["z"] = float(row[2])
        nodes.append(Node(**node_kwargs))
    return nodes


def _node_to_point(node: Node) -> tuple[float, ...]:
    if node.z is None:
        return (float(node.x), float(node.y))
    return (float(node.x), float(node.y), float(node.z))


def _normalize_coordinate_axes(coords):
    np = _import_numpy()

    mins = coords.min(axis=0)
    maxs = coords.max(axis=0)
    spans = maxs - mins
    safe_spans = np.where(spans > 0.0, spans, 1.0)
    normalized = (coords - mins) / safe_spans
    constant_axes = spans <= 0.0
    if np.any(constant_axes):
        normalized[:, constant_axes] = 0.0
    return normalized


def _validate_square_matrix(matrix) -> None:
    if matrix.ndim != 2:
        raise ValueError("distance matrix must be 2-dimensional")
    if matrix.shape[0] != matrix.shape[1]:
        raise ValueError("distance matrix must be square")


def _import_numpy():
    try:
        import numpy as np
    except ImportError as exc:
        raise ImportError(
            "numpy is required for deriving coordinates from distances. "
            "Install with: pip install numpy"
        ) from exc
    return np
