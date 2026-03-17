from __future__ import annotations

import ast
import csv
import json
from dataclasses import dataclass, field
from io import StringIO
from pathlib import Path

from lazyman.tsplib.enums import EdgeWeightFormat, EdgeWeightType, ProblemType
from lazyman.tsplib.errors import TSPLibParseError
from lazyman.tsplib.models import Node, TSPLibProblem


@dataclass(frozen=True)
class CityCoordinatePattern:
    x_template: str
    y_template: str
    start_index: int = 1

    def x_column(self, index: int) -> str:
        return self.x_template.format(index=index)

    def y_column(self, index: int) -> str:
        return self.y_template.format(index=index)


@dataclass
class TSPLibCSVConfig:
    name_column: str | None = None
    dimension_column: str | None = None
    comment_column: str | None = None
    coordinates_column: str | None = None
    distance_matrix_column: str | None = None
    tour_column: str | None = None
    city_coordinate_pattern: CityCoordinatePattern | None = None
    metadata_columns: list[str] = field(default_factory=list)
    auto_metadata: bool = False
    problem_type: ProblemType = ProblemType.TSP
    edge_weight_type: EdgeWeightType | None = None
    node_id_start: int = 1
    fallback_name_prefix: str = "csv_instance"
    delimiter: str = ","


def read_csv_file(path: str | Path, config: TSPLibCSVConfig) -> list[TSPLibProblem]:
    file_path = Path(path)
    return read_csv_text(file_path.read_text(encoding="utf-8"), config=config, source=str(file_path))


def read_csv_text(
    text: str,
    *,
    config: TSPLibCSVConfig,
    source: str = "<memory>",
) -> list[TSPLibProblem]:
    reader = csv.DictReader(StringIO(text), delimiter=config.delimiter)
    if reader.fieldnames is None:
        raise TSPLibParseError("CSV input is missing a header row", section="CSV")

    _validate_mapped_columns(reader.fieldnames, config)

    problems: list[TSPLibProblem] = []
    for line_number, row in enumerate(reader, start=2):
        problems.append(_row_to_problem(row, config=config, line_number=line_number, source=source))

    return problems


def _validate_mapped_columns(fieldnames: list[str], config: TSPLibCSVConfig) -> None:
    required_columns = [
        config.name_column,
        config.dimension_column,
        config.comment_column,
        config.coordinates_column,
        config.distance_matrix_column,
        config.tour_column,
    ]
    required_columns.extend(config.metadata_columns)

    for column in required_columns:
        if column is None:
            continue
        if column not in fieldnames:
            raise TSPLibParseError(f"mapped CSV column '{column}' does not exist", section="CSV")

    pattern = config.city_coordinate_pattern
    if pattern is not None:
        first_x = pattern.x_column(pattern.start_index)
        first_y = pattern.y_column(pattern.start_index)
        if first_x not in fieldnames or first_y not in fieldnames:
            raise TSPLibParseError(
                "city coordinate pattern does not match CSV header columns",
                section="CSV",
            )


def _row_to_problem(
    row: dict[str, str | None],
    *,
    config: TSPLibCSVConfig,
    line_number: int,
    source: str,
) -> TSPLibProblem:
    name = _read_name(row, config=config, line_number=line_number)
    dimension = _read_dimension(row, config=config, line_number=line_number)
    comment = _read_optional_text(row, config.comment_column)
    nodes = _read_nodes(row, config=config, dimension=dimension, line_number=line_number)
    weight_matrix = _read_distance_matrix(row, config=config, line_number=line_number)
    tours = _read_tours(row, config=config, line_number=line_number)

    if dimension is None:
        if nodes:
            dimension = len(nodes)
        elif weight_matrix is not None:
            dimension = len(weight_matrix)
        else:
            raise TSPLibParseError(
                "unable to infer dimension from CSV row",
                line_number=line_number,
                section="CSV",
            )

    edge_weight_type = config.edge_weight_type
    if edge_weight_type is None:
        if weight_matrix is not None:
            edge_weight_type = EdgeWeightType.EXPLICIT
        elif nodes and any(node.z is not None for node in nodes):
            edge_weight_type = EdgeWeightType.EUC_3D
        elif nodes:
            edge_weight_type = EdgeWeightType.EUC_2D

    edge_weight_format = EdgeWeightFormat.FULL_MATRIX if weight_matrix is not None else None
    metadata = _read_metadata(row, config=config)
    metadata["csv_source"] = source

    return TSPLibProblem(
        name=name,
        problem_type=config.problem_type,
        dimension=dimension,
        comment=comment,
        edge_weight_type=edge_weight_type,
        edge_weight_format=edge_weight_format,
        nodes=nodes,
        weight_matrix=weight_matrix,
        tours=tours,
        metadata=metadata,
    )


def _read_name(row: dict[str, str | None], *, config: TSPLibCSVConfig, line_number: int) -> str:
    if config.name_column is None:
        return f"{config.fallback_name_prefix}_{line_number - 1}"

    value = _read_optional_text(row, config.name_column)
    if value is None:
        raise TSPLibParseError(
            f"column '{config.name_column}' is empty",
            line_number=line_number,
            section="CSV",
        )
    return value


def _read_dimension(
    row: dict[str, str | None],
    *,
    config: TSPLibCSVConfig,
    line_number: int,
) -> int | None:
    if config.dimension_column is None:
        return None

    value = _read_optional_text(row, config.dimension_column)
    if value is None:
        raise TSPLibParseError(
            f"column '{config.dimension_column}' is empty",
            line_number=line_number,
            section="CSV",
        )

    try:
        parsed = int(value)
    except ValueError as exc:
        raise TSPLibParseError(
            f"column '{config.dimension_column}' must be an integer",
            line_number=line_number,
            section="CSV",
        ) from exc

    if parsed <= 0:
        raise TSPLibParseError(
            f"column '{config.dimension_column}' must be positive",
            line_number=line_number,
            section="CSV",
        )

    return parsed


def _read_nodes(
    row: dict[str, str | None],
    *,
    config: TSPLibCSVConfig,
    dimension: int | None,
    line_number: int,
) -> list[Node]:
    if config.coordinates_column is not None:
        return _read_nodes_from_coordinates_column(row, config=config, line_number=line_number)

    if config.city_coordinate_pattern is not None:
        return _read_nodes_from_city_pattern(
            row,
            pattern=config.city_coordinate_pattern,
            dimension=dimension,
            node_id_start=config.node_id_start,
            line_number=line_number,
        )

    return []


def _read_nodes_from_coordinates_column(
    row: dict[str, str | None],
    *,
    config: TSPLibCSVConfig,
    line_number: int,
) -> list[Node]:
    raw_value = _read_optional_text(row, config.coordinates_column)
    if raw_value is None:
        return []

    coordinates = _parse_structured_value(raw_value, line_number=line_number, column=config.coordinates_column)
    if not isinstance(coordinates, list):
        raise TSPLibParseError(
            f"column '{config.coordinates_column}' must contain a list",
            line_number=line_number,
            section="CSV",
        )

    nodes: list[Node] = []
    for offset, value in enumerate(coordinates):
        if not isinstance(value, list) or len(value) not in {2, 3}:
            raise TSPLibParseError(
                f"column '{config.coordinates_column}' must contain [x, y] or [x, y, z] items",
                line_number=line_number,
                section="CSV",
            )

        try:
            numeric = [float(part) for part in value]
        except (TypeError, ValueError) as exc:
            raise TSPLibParseError(
                f"column '{config.coordinates_column}' includes non-numeric coordinates",
                line_number=line_number,
                section="CSV",
            ) from exc

        node_kwargs = {
            "id": config.node_id_start + offset,
            "x": numeric[0],
            "y": numeric[1],
        }
        if len(numeric) == 3:
            node_kwargs["z"] = numeric[2]

        nodes.append(Node(**node_kwargs))

    return nodes


def _read_nodes_from_city_pattern(
    row: dict[str, str | None],
    *,
    pattern: CityCoordinatePattern,
    dimension: int | None,
    node_id_start: int,
    line_number: int,
) -> list[Node]:
    nodes: list[Node] = []
    index = pattern.start_index

    while True:
        if dimension is not None and len(nodes) >= dimension:
            break

        x_column = pattern.x_column(index)
        y_column = pattern.y_column(index)

        x_raw = _read_optional_text(row, x_column)
        y_raw = _read_optional_text(row, y_column)

        if x_raw is None and y_raw is None:
            break

        if x_raw is None or y_raw is None:
            raise TSPLibParseError(
                f"incomplete city coordinates at index {index}",
                line_number=line_number,
                section="CSV",
            )

        try:
            x = float(x_raw)
            y = float(y_raw)
        except ValueError as exc:
            raise TSPLibParseError(
                f"city coordinate columns at index {index} must be numeric",
                line_number=line_number,
                section="CSV",
            ) from exc

        nodes.append(Node(id=node_id_start + len(nodes), x=x, y=y))
        index += 1

    return nodes


def _read_distance_matrix(
    row: dict[str, str | None],
    *,
    config: TSPLibCSVConfig,
    line_number: int,
) -> list[list[float]] | None:
    if config.distance_matrix_column is None:
        return None

    raw_value = _read_optional_text(row, config.distance_matrix_column)
    if raw_value is None:
        return None

    matrix = _parse_structured_value(raw_value, line_number=line_number, column=config.distance_matrix_column)
    if not isinstance(matrix, list):
        raise TSPLibParseError(
            f"column '{config.distance_matrix_column}' must contain a matrix list",
            line_number=line_number,
            section="CSV",
        )

    normalized: list[list[float]] = []
    width: int | None = None
    for row_values in matrix:
        if not isinstance(row_values, list):
            raise TSPLibParseError(
                f"column '{config.distance_matrix_column}' must contain nested lists",
                line_number=line_number,
                section="CSV",
            )

        try:
            numeric_row = [float(value) for value in row_values]
        except (TypeError, ValueError) as exc:
            raise TSPLibParseError(
                f"column '{config.distance_matrix_column}' includes non-numeric values",
                line_number=line_number,
                section="CSV",
            ) from exc

        if width is None:
            width = len(numeric_row)
        elif width != len(numeric_row):
            raise TSPLibParseError(
                f"column '{config.distance_matrix_column}' must be rectangular",
                line_number=line_number,
                section="CSV",
            )

        normalized.append(numeric_row)

    return normalized


def _read_tours(
    row: dict[str, str | None],
    *,
    config: TSPLibCSVConfig,
    line_number: int,
) -> list[list[int]]:
    if config.tour_column is None:
        return []

    raw_value = _read_optional_text(row, config.tour_column)
    if raw_value is None:
        return []

    parsed = _parse_structured_value(raw_value, line_number=line_number, column=config.tour_column)
    if not isinstance(parsed, list):
        raise TSPLibParseError(
            f"column '{config.tour_column}' must contain a list",
            line_number=line_number,
            section="CSV",
        )

    if parsed and all(isinstance(item, int) for item in parsed):
        return [parsed]

    tours: list[list[int]] = []
    for tour in parsed:
        if not isinstance(tour, list) or not all(isinstance(item, int) for item in tour):
            raise TSPLibParseError(
                f"column '{config.tour_column}' must contain [int] or [[int], ...]",
                line_number=line_number,
                section="CSV",
            )
        tours.append(tour)

    return tours


def _read_metadata(row: dict[str, str | None], *, config: TSPLibCSVConfig) -> dict[str, str]:
    metadata: dict[str, str] = {}

    for column in config.metadata_columns:
        value = _read_optional_text(row, column)
        if value is not None:
            metadata[column] = value

    if not config.auto_metadata:
        return metadata

    excluded = {
        config.name_column,
        config.dimension_column,
        config.comment_column,
        config.coordinates_column,
        config.distance_matrix_column,
        config.tour_column,
    }
    excluded.update(config.metadata_columns)
    excluded.discard(None)

    for column, raw_value in row.items():
        if column in excluded:
            continue
        value = _normalize_text(raw_value)
        if value is not None:
            metadata[column] = value

    return metadata


def _read_optional_text(row: dict[str, str | None], column: str | None) -> str | None:
    if column is None:
        return None
    return _normalize_text(row.get(column))


def _normalize_text(value: str | None) -> str | None:
    if value is None:
        return None
    stripped = value.strip()
    if not stripped:
        return None
    return stripped


def _parse_structured_value(raw_value: str, *, line_number: int, column: str | None) -> list | dict:
    try:
        return json.loads(raw_value)
    except json.JSONDecodeError:
        try:
            parsed = ast.literal_eval(raw_value)
        except (SyntaxError, ValueError) as exc:
            raise TSPLibParseError(
                f"unable to parse structured value in column '{column}'",
                line_number=line_number,
                section="CSV",
            ) from exc
        if not isinstance(parsed, (list, dict)):
            raise TSPLibParseError(
                f"column '{column}' must encode list or dict values",
                line_number=line_number,
                section="CSV",
            )
        return parsed
