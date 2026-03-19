from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np

from lazyman.viz3d import (
    SparseFillConfig,
    VisualizationPipelineConfig,
    run_visualization_pipeline,
)


def generate_sparse_demo_volume(
    *,
    shape: tuple[int, int, int] = (56, 56, 56),
    keep_ratio: float = 0.08,
    seed: int = 7,
) -> np.ndarray:
    if len(shape) != 3 or any(size <= 0 for size in shape):
        raise ValueError("shape must be a 3-tuple of positive integers")
    if not (0.0 < keep_ratio <= 1.0):
        raise ValueError("keep_ratio must be in (0.0, 1.0]")

    rng = np.random.default_rng(seed)
    x = np.linspace(-1.0, 1.0, shape[0])
    y = np.linspace(-1.0, 1.0, shape[1])
    z = np.linspace(-1.0, 1.0, shape[2])
    xx, yy, zz = np.meshgrid(x, y, z, indexing="ij")

    g1 = np.exp(-((xx + 0.35) ** 2 + (yy - 0.1) ** 2 + (zz + 0.15) ** 2) / 0.08)
    g2 = np.exp(-((xx - 0.25) ** 2 + (yy + 0.3) ** 2 + (zz - 0.2) ** 2) / 0.05)
    wave = 0.2 * (np.sin(6 * xx) * np.cos(5 * yy) * np.sin(4 * zz))
    dense = g1 + 0.8 * g2 + wave

    dense = (dense - dense.min()) / (dense.max() - dense.min())
    sparse = dense.copy()

    keep_mask = rng.random(shape) < keep_ratio
    sparse[~keep_mask] = np.nan

    # Add some zero placeholders to mimic sparse pipelines that encode "missing" as zero.
    zero_mask = (~keep_mask) & (rng.random(shape) < 0.2)
    sparse[zero_mask] = 0.0
    return sparse


def run_demo(
    *,
    shape: tuple[int, int, int] = (56, 56, 56),
    keep_ratio: float = 0.08,
    seed: int = 7,
    show: bool = True,
    output_mesh: str | None = None,
    save_volume: str | None = None,
) -> None:
    volume = generate_sparse_demo_volume(shape=shape, keep_ratio=keep_ratio, seed=seed)

    if save_volume is not None:
        volume_path = Path(save_volume)
        np.save(volume_path, volume)
        print(f"Saved demo volume to {volume_path}")

    config = VisualizationPipelineConfig(
        fill=SparseFillConfig(
            method="linear",
            gaussian_sigma=1.4,
            treat_zeros_as_missing=True,
            missing_value=np.nan,
        ),
        level=0.45,
        taubin_iterations=35,
        taubin_pass_band=0.1,
        show_original_wireframe=True,
    )

    result = run_visualization_pipeline(volume, config=config, show=show)

    if output_mesh is not None:
        mesh_path = Path(output_mesh)
        result.smoothed_mesh.save(mesh_path)
        print(f"Saved smoothed mesh to {mesh_path}")

    print(
        f"Demo complete: level={result.level:.3f}, "
        f"vertices={len(result.vertices)}, faces={len(result.faces)}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate sparse 3D demo data and visualize it.")
    parser.add_argument("--shape", default="56,56,56", help="3D shape as x,y,z")
    parser.add_argument("--keep-ratio", type=float, default=0.08, help="Fraction of voxels kept from dense data")
    parser.add_argument("--seed", type=int, default=7)
    parser.add_argument("--no-show", action="store_true", help="Run pipeline without opening a viewer window")
    parser.add_argument("--output-mesh", default=None, help="Optional output mesh path (e.g. demo.ply)")
    parser.add_argument("--save-volume", default=None, help="Optional output path to save generated .npy volume")
    args = parser.parse_args()

    shape = _parse_shape(args.shape)
    run_demo(
        shape=shape,
        keep_ratio=args.keep_ratio,
        seed=args.seed,
        show=not args.no_show,
        output_mesh=args.output_mesh,
        save_volume=args.save_volume,
    )
    return 0


def _parse_shape(value: str) -> tuple[int, int, int]:
    try:
        parts = [int(item.strip()) for item in value.split(",")]
    except ValueError as exc:
        raise ValueError("shape must be comma-separated integers, e.g. 56,56,56") from exc
    if len(parts) != 3 or any(part <= 0 for part in parts):
        raise ValueError("shape must contain exactly 3 positive integers")
    return parts[0], parts[1], parts[2]


if __name__ == "__main__":
    raise SystemExit(main())
