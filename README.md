# LazyMan
Attempts to solve the traveling salesman problem

## Python project setup

Create a virtual environment and install the package in editable mode:

```bash
python -m venv .venv
. .venv/bin/activate
python -m pip install -e .[dev]
```

Run the package:

```bash
python -m lazyman
```

Or use the installed console script:

```bash
lazyman
```

Run tests:

```bash
pytest
```

## 3D Visualization Pipeline

Install visualization dependencies:

```bash
python -m pip install -e .[dev,viz]
```

Run the desktop pipeline on a `.npy` 3D matrix:

```bash
python -m lazyman.viz3d path/to/volume.npy
```

Pipeline stages:

1. Sparse fill via interpolation (`linear` or `nearest`)
2. Optional Gaussian smoothing
3. Isosurface extraction with marching cubes
4. Taubin mesh smoothing
5. Interactive desktop rendering (PyVista/VTK)

Example with explicit options:

```bash
python -m lazyman.viz3d path/to/volume.npy \
  --fill-method linear \
  --gaussian-sigma 1.5 \
  --level 0.4 \
  --taubin-iterations 40 \
  --taubin-pass-band 0.1 \
  --output-mesh surface.ply
```

Generate a synthetic sparse 3D demo dataset and open the viewer:

```bash
python -m lazyman.demo.demo_viz3d
```

Run demo headless and export mesh:

```bash
python -m lazyman.demo.demo_viz3d --no-show --output-mesh demo_surface.ply
```

## TSPLIB Geometry Completion

Derive missing geometry for a `TSPLibProblem`:

- If only coordinates are present, generate a full Euclidean distance matrix.
- If only a distance matrix is present, generate normalized coordinates via classical MDS.

```python
from lazyman.tsplib import (
    GeometryCompletionConfig,
    complete_problem_geometry,
)

completed = complete_problem_geometry(problem)

# Optional settings for distance->coordinates
completed = complete_problem_geometry(
    problem,
    GeometryCompletionConfig(
        coordinate_dim=3,
        normalize_coordinates=True,
        asymmetric_strategy="symmetrize",
    ),
)
```
