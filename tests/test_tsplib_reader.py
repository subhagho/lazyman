import pytest
from pydantic import ValidationError

from lazyman.tsplib import (
    EdgeWeightFormat,
    EdgeWeightType,
    ProblemType,
    TSPLibParseError,
    read_text,
)


def test_read_euc_2d_problem():
    problem = read_text(
        """
        NAME: square
        TYPE: TSP
        COMMENT: sample euclidean instance
        DIMENSION: 3
        EDGE_WEIGHT_TYPE: EUC_2D
        NODE_COORD_SECTION
        1 0 0
        2 1 0
        3 1 1
        EOF
        """
    )

    assert problem.name == "square"
    assert problem.problem_type == ProblemType.TSP
    assert problem.edge_weight_type == EdgeWeightType.EUC_2D
    assert problem.dimension == 3
    assert problem.comment == "sample euclidean instance"
    assert problem.weight_matrix is None
    assert [node.id for node in problem.nodes] == [1, 2, 3]
    assert problem.nodes[2].y == 1.0


def test_read_explicit_full_matrix_problem():
    problem = read_text(
        """
        NAME: matrix
        TYPE: TSP
        DIMENSION: 3
        EDGE_WEIGHT_TYPE: EXPLICIT
        EDGE_WEIGHT_FORMAT: FULL_MATRIX
        EDGE_WEIGHT_SECTION
        0 1 2
        1 0 3
        2 3 0
        EOF
        """
    )

    assert problem.edge_weight_type == EdgeWeightType.EXPLICIT
    assert problem.edge_weight_format == EdgeWeightFormat.FULL_MATRIX
    assert problem.weight_matrix == [
        [0.0, 1.0, 2.0],
        [1.0, 0.0, 3.0],
        [2.0, 3.0, 0.0],
    ]


def test_upper_row_is_normalized_to_full_matrix():
    problem = read_text(
        """
        NAME: triangle
        TYPE: TSP
        DIMENSION: 4
        EDGE_WEIGHT_TYPE: EXPLICIT
        EDGE_WEIGHT_FORMAT: UPPER_ROW
        EDGE_WEIGHT_SECTION
        1 2 3
        4 5
        6
        EOF
        """
    )

    assert problem.weight_matrix == [
        [0.0, 1.0, 2.0, 3.0],
        [1.0, 0.0, 4.0, 5.0],
        [2.0, 4.0, 0.0, 6.0],
        [3.0, 5.0, 6.0, 0.0],
    ]


def test_duplicate_node_ids_fail_validation():
    with pytest.raises(ValidationError):
        read_text(
            """
            NAME: duplicate
            TYPE: TSP
            DIMENSION: 2
            EDGE_WEIGHT_TYPE: EUC_2D
            NODE_COORD_SECTION
            1 0 0
            1 1 1
            EOF
            """
        )


def test_missing_edge_weight_format_raises_parse_error():
    with pytest.raises(TSPLibParseError):
        read_text(
            """
            NAME: incomplete
            TYPE: TSP
            DIMENSION: 2
            EDGE_WEIGHT_TYPE: EXPLICIT
            EDGE_WEIGHT_SECTION
            0 1
            1 0
            EOF
            """
        )


def test_dimension_mismatch_fails_validation():
    with pytest.raises(ValidationError):
        read_text(
            """
            NAME: mismatch
            TYPE: TSP
            DIMENSION: 3
            EDGE_WEIGHT_TYPE: EUC_2D
            NODE_COORD_SECTION
            1 0 0
            2 1 1
            EOF
            """
        )
