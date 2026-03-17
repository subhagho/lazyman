import pytest

from lazyman.tsplib import (
    CityCoordinatePattern,
    EdgeWeightFormat,
    EdgeWeightType,
    TSPLibCSVConfig,
    TSPLibParseError,
    read_csv_text,
)


def test_csv_reader_with_embedded_coordinate_and_matrix_columns():
    csv_text = """instance_id,num_cities,city_coordinates,distance_matrix,best_route,total_distance
sample_1,3,"[[0.0, 0.0], [1.0, 0.0], [1.0, 1.0]]","[[0, 1, 2], [1, 0, 3], [2, 3, 0]]","[1, 2, 3]",10.0
"""

    config = TSPLibCSVConfig(
        name_column="instance_id",
        dimension_column="num_cities",
        coordinates_column="city_coordinates",
        distance_matrix_column="distance_matrix",
        tour_column="best_route",
        metadata_columns=["total_distance"],
    )
    problems = read_csv_text(csv_text, config=config)

    assert len(problems) == 1
    problem = problems[0]

    assert problem.name == "sample_1"
    assert problem.dimension == 3
    assert problem.edge_weight_type == EdgeWeightType.EXPLICIT
    assert problem.edge_weight_format == EdgeWeightFormat.FULL_MATRIX
    assert [node.id for node in problem.nodes] == [1, 2, 3]
    assert problem.weight_matrix == [
        [0.0, 1.0, 2.0],
        [1.0, 0.0, 3.0],
        [2.0, 3.0, 0.0],
    ]
    assert problem.tours == [[1, 2, 3]]
    assert problem.metadata["total_distance"] == "10.0"


def test_csv_reader_with_wide_city_columns():
    csv_text = """TSP_Instance,Num_Cities,City_1_X,City_1_Y,City_2_X,City_2_Y,City_3_X,City_3_Y,Total_Distance
a280,3,10.5,20.0,40.0,50.0,60.0,70.0,1234.5
"""

    config = TSPLibCSVConfig(
        name_column="TSP_Instance",
        dimension_column="Num_Cities",
        city_coordinate_pattern=CityCoordinatePattern(
            x_template="City_{index}_X",
            y_template="City_{index}_Y",
        ),
        metadata_columns=["Total_Distance"],
    )
    problems = read_csv_text(csv_text, config=config)

    assert len(problems) == 1
    problem = problems[0]

    assert problem.name == "a280"
    assert problem.dimension == 3
    assert problem.edge_weight_type == EdgeWeightType.EUC_2D
    assert problem.edge_weight_format is None
    assert problem.weight_matrix is None
    assert [(node.x, node.y) for node in problem.nodes] == [
        (10.5, 20.0),
        (40.0, 50.0),
        (60.0, 70.0),
    ]
    assert problem.metadata["Total_Distance"] == "1234.5"


def test_csv_reader_fails_when_mapped_column_does_not_exist():
    csv_text = """id,n
x,2
"""

    config = TSPLibCSVConfig(
        name_column="id",
        dimension_column="num_cities",
    )

    with pytest.raises(TSPLibParseError):
        read_csv_text(csv_text, config=config)


def test_csv_reader_auto_metadata_includes_unmapped_columns():
    csv_text = """instance_id,num_cities,city_coordinates,label
item_1,2,"[[0, 0], [1, 1]]",train
"""

    config = TSPLibCSVConfig(
        name_column="instance_id",
        dimension_column="num_cities",
        coordinates_column="city_coordinates",
        auto_metadata=True,
    )
    problems = read_csv_text(csv_text, config=config)

    assert problems[0].metadata["label"] == "train"
