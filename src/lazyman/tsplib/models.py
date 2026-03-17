from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from lazyman.tsplib.enums import EdgeWeightFormat, EdgeWeightType, ProblemType


class Node(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: int = Field(gt=0)
    x: float | None = None
    y: float | None = None
    z: float | None = None

    @model_validator(mode="after")
    def validate_coordinates(self) -> "Node":
        has_xy = self.x is not None and self.y is not None
        has_xyz = has_xy and self.z is not None

        if not has_xy and self.z is None:
            raise ValueError("node must include at least x and y coordinates")

        if self.z is not None and not has_xyz:
            raise ValueError("z coordinate requires x and y coordinates")

        if (self.x is None) != (self.y is None):
            raise ValueError("x and y coordinates must be provided together")

        return self


class TSPLibProblem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: str
    problem_type: ProblemType
    dimension: int = Field(gt=0)
    comment: str | None = None
    edge_weight_type: EdgeWeightType | None = None
    edge_weight_format: EdgeWeightFormat | None = None
    nodes: list[Node] = Field(default_factory=list)
    display_nodes: list[Node] = Field(default_factory=list)
    weight_matrix: list[list[float]] | None = None
    fixed_edges: list[tuple[int, int]] = Field(default_factory=list)
    tours: list[list[int]] = Field(default_factory=list)
    metadata: dict[str, str] = Field(default_factory=dict)

    @field_validator("weight_matrix")
    @classmethod
    def validate_weight_matrix(cls, value: list[list[float]] | None) -> list[list[float]] | None:
        if value is None:
            return None

        width = len(value)
        for row in value:
            if len(row) != width:
                raise ValueError("weight_matrix must be square")

        return value

    @model_validator(mode="after")
    def validate_consistency(self) -> "TSPLibProblem":
        if self.edge_weight_type == EdgeWeightType.EXPLICIT and self.edge_weight_format is None:
            raise ValueError("edge_weight_format is required for explicit edge weights")

        if self.nodes:
            self._validate_node_collection(self.nodes, "nodes")

        if self.display_nodes:
            self._validate_node_collection(self.display_nodes, "display_nodes")

        if self.weight_matrix is not None and len(self.weight_matrix) != self.dimension:
            raise ValueError("weight_matrix row count must match dimension")

        for row in self.weight_matrix or []:
            if len(row) != self.dimension:
                raise ValueError("weight_matrix column count must match dimension")

        if self.problem_type in {ProblemType.TSP, ProblemType.ATSP}:
            if not self.nodes and self.weight_matrix is None:
                raise ValueError("TSP and ATSP problems require nodes or a weight matrix")

        if self.problem_type == ProblemType.TOUR and not self.tours:
            raise ValueError("TOUR problems require at least one tour")

        return self

    def _validate_node_collection(self, nodes: list[Node], field_name: str) -> None:
        if len(nodes) != self.dimension:
            raise ValueError(f"{field_name} count must match dimension")

        ids = [node.id for node in nodes]
        if len(ids) != len(set(ids)):
            raise ValueError(f"{field_name} IDs must be unique")
