import pytest


def test_fill_sparse_volume_replaces_missing_values():
    np = pytest.importorskip("numpy")

    from lazyman.viz3d import SparseFillConfig, fill_sparse_volume

    matrix = np.full((8, 8, 8), np.nan, dtype=float)
    matrix[1, 1, 1] = 1.0
    matrix[6, 6, 6] = 3.0

    filled = fill_sparse_volume(
        matrix,
        SparseFillConfig(method="nearest", gaussian_sigma=0.0),
    )

    assert filled.shape == matrix.shape
    assert np.isfinite(filled).all()
    assert float(filled.min()) >= 1.0
    assert float(filled.max()) <= 3.0


def test_extract_surface_produces_triangles():
    np = pytest.importorskip("numpy")

    from lazyman.viz3d import extract_surface

    grid_x, grid_y, grid_z = np.indices((24, 24, 24))
    sphere = (
        (grid_x - 12.0) ** 2 + (grid_y - 12.0) ** 2 + (grid_z - 12.0) ** 2
    ) <= 7.0**2
    matrix = sphere.astype(float)

    vertices, faces, normals, values, level = extract_surface(matrix, level=0.5)

    assert level == 0.5
    assert vertices.shape[0] > 0
    assert faces.shape[0] > 0
    assert faces.shape[1] == 3
    assert normals.shape[0] == vertices.shape[0]
    assert values.shape[0] == vertices.shape[0]
