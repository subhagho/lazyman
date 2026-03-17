import csv
from io import StringIO
from pathlib import Path

import pytest

from lazyman.tsplib import CityCoordinatePattern, TSPLibCSVConfig, read_csv_text

TSPLIB_DOWNLOADS_DIR = Path.home() / "Downloads" / "TSPLIB"
TSP_DATASET_PATH = TSPLIB_DOWNLOADS_DIR / "tsp_dataset.csv"
TSP_INSTANCES_PATH = TSPLIB_DOWNLOADS_DIR / "tsp_instances_dataset.csv"


def _single_row_csv(path: Path) -> str:
    with path.open(newline="", encoding="utf-8") as file_obj:
        reader = csv.DictReader(file_obj)
        row = next(reader)
        fieldnames = reader.fieldnames

    if fieldnames is None:
        raise ValueError(f"CSV has no headers: {path}")

    buffer = StringIO()
    writer = csv.DictWriter(buffer, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerow(row)
    return buffer.getvalue()


@pytest.mark.skipif(not TSP_DATASET_PATH.exists(), reason="missing ~/Downloads/TSPLIB/tsp_dataset.csv")
def test_reads_downloaded_tsp_dataset_row():
    config = TSPLibCSVConfig(
        name_column="instance_id",
        dimension_column="num_cities",
        coordinates_column="city_coordinates",
        distance_matrix_column="distance_matrix",
        metadata_columns=["best_route", "total_distance"],
    )

    problems = read_csv_text(
        _single_row_csv(TSP_DATASET_PATH),
        config=config,
        source=str(TSP_DATASET_PATH),
    )

    assert len(problems) == 1
    problem = problems[0]
    assert problem.name
    assert problem.dimension > 0
    assert len(problem.nodes) == problem.dimension
    assert problem.weight_matrix is not None
    assert len(problem.weight_matrix) == problem.dimension


@pytest.mark.skipif(not TSP_INSTANCES_PATH.exists(), reason="missing ~/Downloads/TSPLIB/tsp_instances_dataset.csv")
def test_reads_downloaded_tsp_instances_row():
    config = TSPLibCSVConfig(
        name_column="TSP_Instance",
        dimension_column="Num_Cities",
        city_coordinate_pattern=CityCoordinatePattern(
            x_template="City_{index}_X",
            y_template="City_{index}_Y",
        ),
        metadata_columns=["Total_Distance", "Best_Route_Category"],
    )

    problems = read_csv_text(
        _single_row_csv(TSP_INSTANCES_PATH),
        config=config,
        source=str(TSP_INSTANCES_PATH),
    )

    assert len(problems) == 1
    problem = problems[0]
    assert problem.name
    assert problem.dimension > 0
    assert len(problem.nodes) == problem.dimension
    assert problem.weight_matrix is None
