(TeX-add-style-hook "PoissonNoise_manual"
 (lambda ()
    (LaTeX-add-bibliographies
     "refs")
    (LaTeX-add-labels
     "sec:intro")
    (TeX-run-style-hooks
     "color"
     "listings"
     "graphicx"
     "amssymb"
     "amsmath"
     "inputenc"
     "ansinew"
     "latex2e"
     "scrartcl10"
     "scrartcl")))

