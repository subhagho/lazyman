import pytest

from lazyman.tsplib import (
    EdgeWeightFormat,
    EdgeWeightType,
    GeometryCompletionConfig,
    Node,
    ProblemType,
    TSPLibProblem,
    complete_problem_geometry,
)


def test_complete_distances_from_coordinates():
    problem = TSPLibProblem(
        name="coords_only",
        problem_type=ProblemType.TSP,
        dimension=3,
        nodes=[
            Node(id=1, x=0.0, y=0.0),
            Node(id=2, x=3.0, y=0.0),
            Node(id=3, x=0.0, y=4.0),
        ],
    )

    completed = complete_problem_geometry(problem)

    assert completed.weight_matrix is not None
    assert completed.edge_weight_type == EdgeWeightType.EUC_2D
    assert completed.edge_weight_format == EdgeWeightFormat.FULL_MATRIX
    assert completed.weight_matrix[0][1] == pytest.approx(3.0)
    assert completed.weight_matrix[0][2] == pytest.approx(4.0)
    assert completed.weight_matrix[1][2] == pytest.approx(5.0)


def test_complete_coordinates_from_distances_are_normalized():
    problem = TSPLibProblem(
        name="dist_only",
        problem_type=ProblemType.TSP,
        dimension=4,
        edge_weight_type=EdgeWeightType.EXPLICIT,
        edge_weight_format=EdgeWeightFormat.FULL_MATRIX,
        weight_matrix=[
            [0.0, 1.0, 1.0, 1.414213562],
            [1.0, 0.0, 1.414213562, 1.0],
            [1.0, 1.414213562, 0.0, 1.0],
            [1.414213562, 1.0, 1.0, 0.0],
        ],
    )

    completed = complete_problem_geometry(
        problem,
        GeometryCompletionConfig(coordinate_dim=2, normalize_coordinates=True),
    )

    assert len(completed.nodes) == 4
    for node in completed.nodes:
        assert 0.0 <= node.x <= 1.0
        assert 0.0 <= node.y <= 1.0
        assert node.z is None


def test_complete_coordinates_raises_for_asymmetric_matrix_by_default():
    problem = TSPLibProblem(
        name="atsp_like",
        problem_type=ProblemType.ATSP,
        dimension=3,
        edge_weight_type=EdgeWeightType.EXPLICIT,
        edge_weight_format=EdgeWeightFormat.FULL_MATRIX,
        weight_matrix=[
            [0.0, 1.0, 2.0],
            [3.0, 0.0, 4.0],
            [2.0, 1.0, 0.0],
        ],
    )

    with pytest.raises(ValueError, match="symmetric"):
        complete_problem_geometry(problem)


def test_complete_coordinates_can_symmetrize_asymmetric_matrix():
    problem = TSPLibProblem(
        name="atsp_like",
        problem_type=ProblemType.ATSP,
        dimension=3,
        edge_weight_type=EdgeWeightType.EXPLICIT,
        edge_weight_format=EdgeWeightFormat.FULL_MATRIX,
        weight_matrix=[
            [0.0, 1.0, 2.0],
            [3.0, 0.0, 4.0],
            [2.0, 1.0, 0.0],
        ],
    )

    completed = complete_problem_geometry(
        problem,
        GeometryCompletionConfig(asymmetric_strategy="symmetrize"),
    )

    assert len(completed.nodes) == 3
