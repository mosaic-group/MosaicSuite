=============
Poisson Noise
=============

Due to the descrete character of light, images acquired using photon counting devices
such as CCD cameras suffer from *photon counting noise*: During the exposure time of a
given time *T*, a photon might or might not hit the sensor (since it is a non-continuous
event). Such a process can be modeled using a Poisson distribution.

This ImageJ plugin inserts Poisson distributed noise into an image or an imagestack to simulate
the acquisition of an image. The noise realization is independent for each pixel. The
distribution parameter *λ* is set to the original intensity value.


Algorithm Description and Manual
================================

For better algorithm understanding please refer to `PDF <http://mosaic.mpi-cbg.de/Downloads/PoissonNoise_manual.pdf>`__.

