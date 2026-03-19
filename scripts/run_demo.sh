#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
VENV_PATH="$ROOT_DIR/.venv/bin/activate"

if [ ! -f "$VENV_PATH" ]; then
  echo "Missing virtual environment at $ROOT_DIR/.venv" >&2
  echo "Create it first: python -m venv .venv && . .venv/bin/activate && python -m pip install -e .[dev,viz]" >&2
  exit 1
fi

# shellcheck disable=SC1090
. "$VENV_PATH"

cd "$ROOT_DIR"
export PYTHONPATH="$ROOT_DIR/src${PYTHONPATH:+:$PYTHONPATH}"

if ! python -m pip --version >/dev/null 2>&1; then
  echo "pip is missing in .venv, bootstrapping with ensurepip ..." >&2
  if ! python -m ensurepip --upgrade >/dev/null 2>&1; then
    echo "Unable to bootstrap pip in .venv." >&2
    echo "Install venv tooling first (e.g. python3-venv) and recreate .venv." >&2
    exit 1
  fi
fi

if ! python - <<'PY' >/dev/null 2>&1
import pydantic
import numpy
import scipy
import skimage
import pyvista
PY
then
  echo "Installing missing Python dependencies into .venv ..." >&2
  python -m pip install -e ".[viz]" >/dev/null
fi

python -m lazyman.demo.demo_viz3d "$@"
