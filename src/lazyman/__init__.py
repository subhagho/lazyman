"""LazyMan package."""

from lazyman.tsplib import read_file, read_text

__all__ = [
    "__version__",
    "read_file",
    "read_text",
]

try:
    from lazyman.viz3d import (
        SparseFillConfig,
        VisualizationPipelineConfig,
        extract_surface,
        fill_sparse_volume,
        run_visualization_pipeline,
    )
except ImportError:
    pass
else:
    __all__.extend(
        [
            "SparseFillConfig",
            "VisualizationPipelineConfig",
            "extract_surface",
            "fill_sparse_volume",
            "run_visualization_pipeline",
        ]
    )

__version__ = "0.1.0"
