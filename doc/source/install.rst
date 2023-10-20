========================
Installation
========================


Required software
=================

MosaicSuite requires ImageJ2 or Fiji and Java 8 or greater to work.

- How to install Java?
    Please download it and install from `official Java site <https://www.oracle.com/java/technologies/>`__.
- How to install Fiji/ImageJ2?
    You can download it from `official Fiji site <http://fiji.sc/>`__.


Installation of MosaicSuite plugin
==================================

1. Open Fiji or ImageJ2
#. Run ``Help > Update Fiji`` command (or in case ImageJ2 ``Help > Update...``)
#. Click on ``Manage update sites``
#. Find there and mark ``MOSAIC ToolSuite``
#. Apply changes and Fiji should automatically download latest release of MosaicSuite
#. Restart Fiji (as required) and after restart all functionality by MosaicSuite can be found in ``Plugins > Mosaic`` menu.


.. important::

    If you are using very old Java 6 or because any reason you need to install MosaicSuite manually
    please refer for detailed instructions `old MosaicSuite site <http://sbalzarini-lab.org/?q=downloads/imageJ>`_.

Hints
=====

If you are working with large images - both 2D and 3D - please consider
increasing amount of memory (and number of threads) accessible for ImageJ/Fiji via
```" Edit > Options > Memory and Threads..."```.

Default values are good for regular machines (like laptops with 8/16 GB of RAM) but if you are using a machine
with 32GB+ of memory you can increase this value a lot (just leave few GB of RAM for operating system and other running applications).
