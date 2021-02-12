====
News
====

* **Discrete Region Sampling (DRS)**
    Discrete Region Sampling is a sampling version of well known Region Competition algorithm.
    It can be found in menu ``Segmentation > Discrete Region Sampling``.

    .. admonition:: Citation

        | *J. Cardinale*
        | Unsupervised Segmentation and Shape Posterior Estimation under Bayesian Image Models. PhD thesis, Diss. ETH No. 21026, MOSAIC Group, ETH Zurich, 2013.
        | `PDF <https://sbalzarini-lab.org/docs/Cardinale2013.pdf>`__

    *In order to ensure financial support for our project and allow further development of
    this software, please cite above publications in all your documents and manuscripts that
    made use of this software. Thanks a lot!*

* **Automatic optimal filament segmentation**

    The plugin can be used for a globally optimal filament segmentation of 2D images with
    previously unknown number of filaments. You can find plugin for segmentation in the menu
    ``Segmentation > Filament``. Presented solution can produce sub-pixel accuracy results
    and handle different types of image data from different microscopy modalities.
    The algorithm implemented in this plug-in is described in:

    .. admonition:: Citation

        | *X. Xiao, V. F. Geyer, H. Bowne-Anderson, J. Howard, and I. F. Sbalzarini.*
        | Automatic optimal filament segmentation with sub-pixel accuracy using generalized linear models and B-spline level-sets. Med. Image Anal., 32:157–172, 2016.
        | `PDF <https://sbalzarini-lab.org/docs/Xiao2016.pdf>`__

    *In order to ensure financial support for our project and allow further development of
    this software, please cite above publications in all your documents and manuscripts that
    made use of this software. Thanks a lot!*

* **Fast implicit curvature filters**

    Curvature filters provide geometric means of image filtering, denoising, and restoration.
    This amounts to solving a variational model, but the filters here implicitly do this, and
    are much faster. You find the filters in the menu ``Enhancement > Curvature Filters``.
    Currently, we implement Gauss curvature, Mean curvature, and Total Variation (TV) filters.
    The only parameters is the number of iterations, i.e., how many passes of the filter should
    be applied to the image. Else the filters are parameter free.
    A C++ implementation of these filters is also available `here <https://sbalzarini-lab.org/?q=downloads/curvaturefilters>`__.

    The algorithms implemented in this plug-in are described in:

    .. admonition:: Citation

        | Y. Gong and I. F. Sbalzarini.
        | Curvature filters efficiently reduce certain variational energies. IEEE Trans. Image Process., 26(4):1786–1798, 2017.
        | `PDF <https://sbalzarini-lab.org/docs/Gong2017.pdf>`__

    *In order to ensure financial support for our project and allow further development of
    this software, please cite above publications in all your documents and manuscripts that
    made use of this software. Thanks a lot!*

* **Image Naturalization**

    Image naturalization is an image enhancement technique that is based on gradient statistics
    of natural-scence images. The algorithm is completely parameter free. Simply open an image
    and choose ``Enhancement > Naturalization`` from the plugin menu. In fluorescence microscopy,
    image naturalization can be used for blind deconvolution, dehazing (removing scatter light),
    denoising, or contract enhancement. All just with one function! The "naturalness factor"
    displayed at the end tells you how close your original image was to a natural-scene one
    (1 meaning close, the farther from one the more different).

    The algorithm implemented in this plug-in is described in:

    .. admonition:: Citation

        | Y. Gong and I. F. Sbalzarini.
        | Image enhancement by gradient distribution specification. In Proc. ACCV, 12th Asian Conference on Computer Vision, Workshop on Emerging Topics in Image Enhancement and Restoration, pages w7–p3, Singapore, November 2014.
        | `PDF <https://sbalzarini-lab.org/docs/Gong2014.pdf>`__

    *In order to ensure financial support for our project and allow further development of
    this software, please cite above publications in all your documents and manuscripts that
    made use of this software. Thanks a lot!*

.. important::

    For information about previous news not listed here please refere to `old MosaicSuite site <https://sbalzarini-lab.org/?q=downloads/imageJ>`_.
