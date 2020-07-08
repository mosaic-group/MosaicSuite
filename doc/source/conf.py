# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
# import os
# import sys
# sys.path.insert(0, os.path.abspath('.'))


# -- Project information -----------------------------------------------------

project = 'MosaicSuite'
copyright = '2020, MOSAIC Group, Sbalzarini Lab, mosaic.mpi-cbg.de'
author = 'MOSAIC Group, Sbalzarini Lab'

# The full version, including alpha/beta/rc tags
release = '1.0.19'


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = ['recommonmark',
              'sphinx_rtd_theme']

source_suffix = ['.rst', '.md']

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = []


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
#html_theme = 'alabaster'
html_theme = "sphinx_rtd_theme"

html_theme_options = {
    'navigation_depth': 6
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']


#
# from sphinx.builders.latex import LaTeXBuilder
# LaTeXBuilder.supported_image_types = [
#     'image/svg+xml',
#     'image/tif',
#     'image/png',
#     'image/jpeg'
# ]

# Change the order of searching img files
# Thanks to that html will prefere (animated) gifs over png that are used
# in latexpdf target (without that change png was on the first place).
from sphinx.builders.html import StandaloneHTMLBuilder
StandaloneHTMLBuilder.    supported_image_types = [
    'image/svg+xml',
    'image/gif',
    'image/png',
    'image/jpeg'
]


# It will change alignemnt so images and text are always in order as it is in source
# doc files. Without it sometimes figures are moved to next page if not fitting in current.
latex_elements = {
#Figure placement within LaTeX paper
'figure_align': 'H'
}
