from __future__ import annotations

from enum import Enum


class ProblemType(str, Enum):
    TSP = "TSP"
    ATSP = "ATSP"
    TOUR = "TOUR"


class EdgeWeightType(str, Enum):
    EXPLICIT = "EXPLICIT"
    EUC_2D = "EUC_2D"
    EUC_3D = "EUC_3D"
    MAX_2D = "MAX_2D"
    MAX_3D = "MAX_3D"
    MAN_2D = "MAN_2D"
    MAN_3D = "MAN_3D"
    CEIL_2D = "CEIL_2D"
    GEO = "GEO"
    ATT = "ATT"
    XRAY1 = "XRAY1"
    XRAY2 = "XRAY2"
    SPECIAL = "SPECIAL"


class EdgeWeightFormat(str, Enum):
    FULL_MATRIX = "FULL_MATRIX"
    UPPER_ROW = "UPPER_ROW"
    LOWER_ROW = "LOWER_ROW"
    UPPER_DIAG_ROW = "UPPER_DIAG_ROW"
    LOWER_DIAG_ROW = "LOWER_DIAG_ROW"
    UPPER_COL = "UPPER_COL"
    LOWER_COL = "LOWER_COL"
    UPPER_DIAG_COL = "UPPER_DIAG_COL"
    LOWER_DIAG_COL = "LOWER_DIAG_COL"
