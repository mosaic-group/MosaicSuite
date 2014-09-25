(TeX-add-style-hook "manual"
 (lambda ()
    (LaTeX-add-bibliographies
     "refs")
    (LaTeX-add-labels
     "sec:intro"
     "sec:pluginstart"
     "sec:visualizing"
     "sec:results")
    (TeX-run-style-hooks
     "graphicx"
     "amssymb"
     "amsmath"
     "inputenc"
     "ansinew"
     "latex2e"
     "scrartcl10"
     "scrartcl")))

