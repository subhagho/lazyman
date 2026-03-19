from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import TYPE_CHECKING

import numpy as np
from scipy.interpolate import griddata
from scipy.ndimage import gaussian_filter
from skimage.measure import marching_cubes

if TYPE_CHECKING:
    import pyvista as pv


@dataclass(frozen=True)
class SparseFillConfig:
    method: str = "linear"
    gaussian_sigma: float = 1.0
    treat_zeros_as_missing: bool = False
    missing_value: float | None = np.nan


@dataclass(frozen=True)
class VisualizationPipelineConfig:
    fill: SparseFillConfig = SparseFillConfig()
    level: float | None = None
    spacing: tuple[float, float, float] = (1.0, 1.0, 1.0)
    step_size: int = 1
    taubin_iterations: int = 30
    taubin_pass_band: float = 0.1
    show_original_wireframe: bool = True


@dataclass(frozen=True)
class VisualizationPipelineResult:
    filled_volume: np.ndarray
    level: float
    vertices: np.ndarray
    faces: np.ndarray
    raw_mesh: "pv.PolyData"
    smoothed_mesh: "pv.PolyData"


def fill_sparse_volume(volume: np.ndarray, config: SparseFillConfig = SparseFillConfig()) -> np.ndarray:
    matrix = np.asarray(volume, dtype=float)
    _validate_volume(matrix)
    _validate_fill_config(config)

    missing_mask = _build_missing_mask(matrix, config)
    if missing_mask.all():
        raise ValueError("input volume cannot be fully missing")

    if not missing_mask.any():
        return _apply_optional_gaussian(matrix, sigma=config.gaussian_sigma)

    known_coords = np.argwhere(~missing_mask)
    known_values = matrix[~missing_mask]
    all_coords = np.indices(matrix.shape).reshape(3, -1).T

    filled = griddata(
        points=known_coords,
        values=known_values,
        xi=all_coords,
        method=config.method,
        fill_value=np.nan,
    ).reshape(matrix.shape)

    nan_mask = np.isnan(filled)
    if nan_mask.any():
        nearest = griddata(
            points=known_coords,
            values=known_values,
            xi=all_coords,
            method="nearest",
        ).reshape(matrix.shape)
        filled[nan_mask] = nearest[nan_mask]

    return _apply_optional_gaussian(filled, sigma=config.gaussian_sigma)


def extract_surface(
    volume: np.ndarray,
    *,
    level: float | None = None,
    spacing: tuple[float, float, float] = (1.0, 1.0, 1.0),
    step_size: int = 1,
) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, float]:
    matrix = np.asarray(volume, dtype=float)
    _validate_volume(matrix)

    min_value = float(np.min(matrix))
    max_value = float(np.max(matrix))
    if min_value == max_value:
        raise ValueError("marching cubes requires non-constant input values")

    chosen_level = float(level) if level is not None else (min_value + max_value) / 2.0
    if not (min_value < chosen_level < max_value):
        raise ValueError("level must be strictly between min and max values in the volume")

    vertices, faces, normals, values = marching_cubes(
        matrix,
        level=chosen_level,
        spacing=spacing,
        step_size=step_size,
        allow_degenerate=False,
    )
    return vertices, faces, normals, values, chosen_level


def create_pyvista_mesh(vertices: np.ndarray, faces: np.ndarray) -> "pv.PolyData":
    pv = _import_pyvista()
    triangle_faces = np.c_[np.full((faces.shape[0], 1), 3), faces].astype(np.int64)
    return pv.PolyData(vertices, triangle_faces)


def smooth_mesh_taubin(
    mesh: "pv.PolyData",
    *,
    iterations: int = 30,
    pass_band: float = 0.1,
) -> "pv.PolyData":
    if iterations <= 0:
        raise ValueError("iterations must be positive")
    if not (0.0 < pass_band < 2.0):
        raise ValueError("pass_band must be in (0.0, 2.0)")

    return mesh.smooth_taubin(n_iter=iterations, pass_band=pass_band)


def show_mesh_interactive(
    mesh: "pv.PolyData",
    *,
    title: str = "LazyMan 3D Surface Viewer",
    original_mesh: "pv.PolyData | None" = None,
) -> None:
    pv = _import_pyvista()

    plotter = pv.Plotter(window_size=(1200, 800), title=title)
    plotter.set_background("#0b0f14")

    if original_mesh is not None:
        plotter.add_mesh(
            original_mesh,
            color="lightgray",
            style="wireframe",
            opacity=0.25,
            line_width=1,
            label="Raw Surface",
        )

    plotter.add_mesh(mesh, color="#4cc9f0", smooth_shading=True, label="Taubin Smoothed")
    plotter.add_axes()
    plotter.add_legend()
    plotter.show()


def run_visualization_pipeline(
    volume: np.ndarray,
    *,
    config: VisualizationPipelineConfig = VisualizationPipelineConfig(),
    show: bool = True,
) -> VisualizationPipelineResult:
    filled = fill_sparse_volume(volume, config.fill)
    vertices, faces, _, _, chosen_level = extract_surface(
        filled,
        level=config.level,
        spacing=config.spacing,
        step_size=config.step_size,
    )
    raw_mesh = create_pyvista_mesh(vertices, faces)
    smoothed_mesh = smooth_mesh_taubin(
        raw_mesh,
        iterations=config.taubin_iterations,
        pass_band=config.taubin_pass_band,
    )

    if show:
        original_mesh = raw_mesh if config.show_original_wireframe else None
        show_mesh_interactive(smoothed_mesh, original_mesh=original_mesh)

    return VisualizationPipelineResult(
        filled_volume=filled,
        level=chosen_level,
        vertices=vertices,
        faces=faces,
        raw_mesh=raw_mesh,
        smoothed_mesh=smoothed_mesh,
    )


def main() -> int:
    parser = _build_parser()
    args = parser.parse_args()

    input_path = Path(args.input)
    volume = np.load(input_path)

    config = VisualizationPipelineConfig(
        fill=SparseFillConfig(
            method=args.fill_method,
            gaussian_sigma=args.gaussian_sigma,
            treat_zeros_as_missing=args.zero_is_missing,
            missing_value=np.nan if args.missing_value == "nan" else float(args.missing_value),
        ),
        level=args.level,
        spacing=_parse_spacing(args.spacing),
        step_size=args.step_size,
        taubin_iterations=args.taubin_iterations,
        taubin_pass_band=args.taubin_pass_band,
        show_original_wireframe=not args.hide_raw_wireframe,
    )

    result = run_visualization_pipeline(volume, config=config, show=not args.no_show)

    if args.output_mesh is not None:
        result.smoothed_mesh.save(args.output_mesh)
        print(f"Saved smoothed mesh to {args.output_mesh}")

    print(
        f"Pipeline finished: level={result.level:.4f}, "
        f"vertices={len(result.vertices)}, faces={len(result.faces)}"
    )
    return 0


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="3D volume visualization pipeline for sparse matrices.")
    parser.add_argument("input", help="Path to a .npy file containing a 3D matrix")
    parser.add_argument("--fill-method", default="linear", choices=("linear", "nearest"))
    parser.add_argument("--gaussian-sigma", type=float, default=1.0)
    parser.add_argument("--zero-is-missing", action="store_true")
    parser.add_argument("--missing-value", default="nan")
    parser.add_argument("--level", type=float, default=None)
    parser.add_argument("--spacing", default="1,1,1")
    parser.add_argument("--step-size", type=int, default=1)
    parser.add_argument("--taubin-iterations", type=int, default=30)
    parser.add_argument("--taubin-pass-band", type=float, default=0.1)
    parser.add_argument("--hide-raw-wireframe", action="store_true")
    parser.add_argument("--no-show", action="store_true")
    parser.add_argument("--output-mesh", default=None, help="Optional output mesh path, e.g., surface.ply")
    return parser


def _parse_spacing(value: str) -> tuple[float, float, float]:
    try:
        parts = [float(item.strip()) for item in value.split(",")]
    except ValueError as exc:
        raise ValueError("spacing must be comma-separated floats, e.g. 1,1,1") from exc
    if len(parts) != 3:
        raise ValueError("spacing must contain exactly 3 values")
    return parts[0], parts[1], parts[2]


def _validate_volume(volume: np.ndarray) -> None:
    if volume.ndim != 3:
        raise ValueError(f"expected a 3D matrix, received {volume.ndim}D")


def _validate_fill_config(config: SparseFillConfig) -> None:
    if config.method not in {"linear", "nearest"}:
        raise ValueError("fill method must be either 'linear' or 'nearest'")
    if config.gaussian_sigma < 0:
        raise ValueError("gaussian_sigma must be non-negative")


def _build_missing_mask(volume: np.ndarray, config: SparseFillConfig) -> np.ndarray:
    if config.missing_value is None:
        missing_mask = np.zeros_like(volume, dtype=bool)
    elif np.isnan(config.missing_value):
        missing_mask = np.isnan(volume)
    else:
        missing_mask = volume == config.missing_value

    if config.treat_zeros_as_missing:
        missing_mask = missing_mask | (volume == 0.0)

    return missing_mask


def _apply_optional_gaussian(volume: np.ndarray, *, sigma: float) -> np.ndarray:
    if sigma <= 0:
        return volume
    return gaussian_filter(volume, sigma=sigma)


def _import_pyvista():
    try:
        import pyvista as pv
    except ImportError as exc:
        raise ImportError(
            "pyvista is required for interactive viewing. Install with: pip install -e .[viz]"
        ) from exc
    return pv


if __name__ == "__main__":
    raise SystemExit(main())
