from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from lazyman.tsplib.enums import EdgeWeightFormat, EdgeWeightType, ProblemType
from lazyman.tsplib.errors import TSPLibParseError
from lazyman.tsplib.models import Node, TSPLibProblem

SECTION_NAMES = {
    "NODE_COORD_SECTION",
    "DISPLAY_DATA_SECTION",
    "EDGE_WEIGHT_SECTION",
    "FIXED_EDGES_SECTION",
    "TOUR_SECTION",
}

KNOWN_HEADERS = {
    "NAME",
    "TYPE",
    "COMMENT",
    "DIMENSION",
    "EDGE_WEIGHT_TYPE",
    "EDGE_WEIGHT_FORMAT",
}


@dataclass
class RawDocument:
    headers: dict[str, str] = field(default_factory=dict)
    comments: list[str] = field(default_factory=list)
    sections: dict[str, list[tuple[int, str]]] = field(default_factory=dict)


def read_file(path: str | Path) -> TSPLibProblem:
    file_path = Path(path)
    return read_text(file_path.read_text(encoding="utf-8"), source=str(file_path))


def read_text(text: str, *, source: str = "<memory>") -> TSPLibProblem:
    raw = _parse_document(text, source=source)

    name = _require_header(raw, "NAME")
    problem_type = _parse_enum(_require_header(raw, "TYPE"), ProblemType, source=source)
    dimension = _parse_positive_int(_require_header(raw, "DIMENSION"), header="DIMENSION", source=source)
    edge_weight_type = _parse_optional_enum(raw.headers.get("EDGE_WEIGHT_TYPE"), EdgeWeightType, source=source)
    edge_weight_format = _parse_optional_enum(raw.headers.get("EDGE_WEIGHT_FORMAT"), EdgeWeightFormat, source=source)

    if (
        problem_type == ProblemType.ATSP
        and edge_weight_type == EdgeWeightType.EXPLICIT
        and edge_weight_format not in {None, EdgeWeightFormat.FULL_MATRIX}
    ):
        raise TSPLibParseError(
            "ATSP explicit problems must use FULL_MATRIX",
            section="EDGE_WEIGHT_SECTION",
        )

    nodes = _parse_node_section(raw.sections.get("NODE_COORD_SECTION"), section="NODE_COORD_SECTION")
    display_nodes = _parse_node_section(raw.sections.get("DISPLAY_DATA_SECTION"), section="DISPLAY_DATA_SECTION")
    fixed_edges = _parse_fixed_edges(raw.sections.get("FIXED_EDGES_SECTION"))
    tours = _parse_tours(raw.sections.get("TOUR_SECTION"))
    weight_matrix = _parse_edge_weight_section(
        raw.sections.get("EDGE_WEIGHT_SECTION"),
        dimension=dimension,
        edge_weight_format=edge_weight_format,
    )

    metadata = {
        key: value
        for key, value in raw.headers.items()
        if key not in KNOWN_HEADERS
    }

    return TSPLibProblem(
        name=name,
        problem_type=problem_type,
        dimension=dimension,
        comment="\n".join(raw.comments) or None,
        edge_weight_type=edge_weight_type,
        edge_weight_format=edge_weight_format,
        nodes=nodes,
        display_nodes=display_nodes,
        weight_matrix=weight_matrix,
        fixed_edges=fixed_edges,
        tours=tours,
        metadata=metadata,
    )


def _parse_document(text: str, *, source: str) -> RawDocument:
    raw = RawDocument()
    current_section: str | None = None

    for line_number, raw_line in enumerate(text.splitlines(), start=1):
        line = raw_line.strip()
        if not line:
            continue

        if line == "EOF":
            current_section = None
            break

        upper_line = line.upper()
        if upper_line in SECTION_NAMES:
            current_section = upper_line
            raw.sections.setdefault(current_section, [])
            continue

        if upper_line.endswith("_SECTION"):
            raise TSPLibParseError(f"unsupported section '{upper_line}'", line_number=line_number)

        if current_section is not None:
            raw.sections[current_section].append((line_number, line))
            continue

        if ":" not in line:
            raise TSPLibParseError(
                f"expected header or section marker in {source}",
                line_number=line_number,
            )

        key, value = line.split(":", 1)
        normalized_key = key.strip().upper()
        normalized_value = value.strip()
        if normalized_key == "COMMENT":
            raw.comments.append(normalized_value)
        else:
            raw.headers[normalized_key] = normalized_value

    return raw


def _require_header(raw: RawDocument, key: str) -> str:
    value = raw.headers.get(key)
    if value is None:
        raise TSPLibParseError(f"missing required header '{key}'")
    return value


def _parse_enum(value: str, enum_type: type[ProblemType | EdgeWeightType | EdgeWeightFormat], *, source: str):
    try:
        return enum_type(value.strip().upper())
    except ValueError as exc:
        raise TSPLibParseError(f"unsupported value '{value}' in {source}") from exc


def _parse_optional_enum(
    value: str | None,
    enum_type: type[EdgeWeightType | EdgeWeightFormat],
    *,
    source: str,
):
    if value is None:
        return None
    return _parse_enum(value, enum_type, source=source)


def _parse_positive_int(value: str, *, header: str, source: str) -> int:
    try:
        parsed = int(value)
    except ValueError as exc:
        raise TSPLibParseError(f"invalid integer for header '{header}' in {source}") from exc

    if parsed <= 0:
        raise TSPLibParseError(f"header '{header}' must be positive")

    return parsed


def _parse_node_section(
    lines: list[tuple[int, str]] | None,
    *,
    section: str,
) -> list[Node]:
    if not lines:
        return []

    nodes: list[Node] = []
    coordinate_width: int | None = None

    for line_number, line in lines:
        tokens = line.split()
        if len(tokens) not in {3, 4}:
            raise TSPLibParseError(
                "node coordinate lines must contain id x y or id x y z",
                line_number=line_number,
                section=section,
            )

        if coordinate_width is None:
            coordinate_width = len(tokens)
        elif coordinate_width != len(tokens):
            raise TSPLibParseError(
                "mixed coordinate dimensionality is not supported",
                line_number=line_number,
                section=section,
            )

        try:
            node_id = int(tokens[0])
            coordinates = [float(token) for token in tokens[1:]]
        except ValueError as exc:
            raise TSPLibParseError(
                "invalid numeric value in coordinate section",
                line_number=line_number,
                section=section,
            ) from exc

        node_kwargs = {
            "id": node_id,
            "x": coordinates[0],
            "y": coordinates[1],
        }
        if len(coordinates) == 3:
            node_kwargs["z"] = coordinates[2]

        nodes.append(Node(**node_kwargs))

    return nodes


def _parse_edge_weight_section(
    lines: list[tuple[int, str]] | None,
    *,
    dimension: int,
    edge_weight_format: EdgeWeightFormat | None,
) -> list[list[float]] | None:
    if not lines:
        return None

    if edge_weight_format is None:
        raise TSPLibParseError(
            "EDGE_WEIGHT_FORMAT is required when EDGE_WEIGHT_SECTION is present",
            section="EDGE_WEIGHT_SECTION",
        )

    values: list[float] = []
    for line_number, line in lines:
        for token in line.split():
            try:
                values.append(float(token))
            except ValueError as exc:
                raise TSPLibParseError(
                    f"invalid edge weight '{token}'",
                    line_number=line_number,
                    section="EDGE_WEIGHT_SECTION",
                ) from exc

    return _expand_edge_weights(values, dimension=dimension, edge_weight_format=edge_weight_format)


def _expand_edge_weights(
    values: list[float],
    *,
    dimension: int,
    edge_weight_format: EdgeWeightFormat,
) -> list[list[float]]:
    matrix = [[0.0 for _ in range(dimension)] for _ in range(dimension)]

    if edge_weight_format == EdgeWeightFormat.FULL_MATRIX:
        expected = dimension * dimension
        if len(values) != expected:
            raise TSPLibParseError(
                f"FULL_MATRIX requires {expected} values, received {len(values)}",
                section="EDGE_WEIGHT_SECTION",
            )

        offset = 0
        for row_index in range(dimension):
            matrix[row_index] = values[offset : offset + dimension]
            offset += dimension
        return matrix

    indices = list(_iter_triangular_indices(dimension, edge_weight_format))
    if len(values) != len(indices):
        raise TSPLibParseError(
            f"{edge_weight_format.value} requires {len(indices)} values, received {len(values)}",
            section="EDGE_WEIGHT_SECTION",
        )

    for value, (row_index, column_index) in zip(values, indices):
        matrix[row_index][column_index] = value
        if row_index != column_index:
            matrix[column_index][row_index] = value

    return matrix


def _iter_triangular_indices(
    dimension: int,
    edge_weight_format: EdgeWeightFormat,
):
    if edge_weight_format == EdgeWeightFormat.UPPER_ROW:
        for row_index in range(dimension):
            for column_index in range(row_index + 1, dimension):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.LOWER_ROW:
        for row_index in range(dimension):
            for column_index in range(row_index):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.UPPER_DIAG_ROW:
        for row_index in range(dimension):
            for column_index in range(row_index, dimension):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.LOWER_DIAG_ROW:
        for row_index in range(dimension):
            for column_index in range(row_index + 1):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.UPPER_COL:
        for column_index in range(dimension):
            for row_index in range(column_index):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.LOWER_COL:
        for column_index in range(dimension):
            for row_index in range(column_index + 1, dimension):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.UPPER_DIAG_COL:
        for column_index in range(dimension):
            for row_index in range(column_index + 1):
                yield row_index, column_index
    elif edge_weight_format == EdgeWeightFormat.LOWER_DIAG_COL:
        for column_index in range(dimension):
            for row_index in range(column_index, dimension):
                yield row_index, column_index
    else:
        raise TSPLibParseError(
            f"unsupported EDGE_WEIGHT_FORMAT '{edge_weight_format.value}'",
            section="EDGE_WEIGHT_SECTION",
        )


def _parse_fixed_edges(lines: list[tuple[int, str]] | None) -> list[tuple[int, int]]:
    if not lines:
        return []

    values = _parse_int_tokens(lines, section="FIXED_EDGES_SECTION")
    pairs: list[tuple[int, int]] = []
    pending: list[int] = []

    for value in values:
        if value == -1:
            if pending:
                raise TSPLibParseError(
                    "FIXED_EDGES_SECTION must contain complete node pairs",
                    section="FIXED_EDGES_SECTION",
                )
            continue

        pending.append(value)
        if len(pending) == 2:
            pairs.append((pending[0], pending[1]))
            pending = []

    if pending:
        raise TSPLibParseError(
            "FIXED_EDGES_SECTION must contain complete node pairs",
            section="FIXED_EDGES_SECTION",
        )

    return pairs


def _parse_tours(lines: list[tuple[int, str]] | None) -> list[list[int]]:
    if not lines:
        return []

    values = _parse_int_tokens(lines, section="TOUR_SECTION")
    tours: list[list[int]] = []
    current_tour: list[int] = []

    for value in values:
        if value == -1:
            if current_tour:
                tours.append(current_tour)
                current_tour = []
            continue
        current_tour.append(value)

    if current_tour:
        tours.append(current_tour)

    return tours


def _parse_int_tokens(lines: list[tuple[int, str]], *, section: str) -> list[int]:
    values: list[int] = []
    for line_number, line in lines:
        for token in line.split():
            try:
                values.append(int(token))
            except ValueError as exc:
                raise TSPLibParseError(
                    f"invalid integer '{token}'",
                    line_number=line_number,
                    section=section,
                ) from exc
    return values
