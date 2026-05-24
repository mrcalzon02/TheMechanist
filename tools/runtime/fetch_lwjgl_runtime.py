#!/usr/bin/env python3
"""Compatibility wrapper for the LWJGL runtime bootstrapper."""
from pathlib import Path
import runpy
import sys
bootstrap = Path(__file__).with_name("bootstrap_lwjgl_runtime.py")
sys.argv[0] = str(bootstrap)
runpy.run_path(str(bootstrap), run_name="__main__")
