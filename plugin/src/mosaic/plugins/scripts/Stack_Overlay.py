import os
from java.awt import Color, GridLayout
from javax.swing import JPanel, JComboBox, JLabel, JFrame, JScrollPane, JColorChooser, JButton, JSeparator, SwingConstants, SpinnerNumberModel, JSpinner, BorderFactory, JCheckBox
from net.miginfocom.swing import MigLayout

from ij import IJ, WindowManager, ImagePlus, ImageStack
from ij.process import Blitter
from script.imglib.math import Multiply, Difference, Subtract, Xor, Add, Or, Min
from script.imglib.color import Red, Green, Blue, RGBA
from mpicbg.imglib.image.display.imagej import ImageJFunctions as IJF


def AWTColorToArray(color):
    return [color.getRed(), color.getGreen(), color.getBlue()]


class StackOverlay:
    def __init__(self):
        self.frame = None
        self.overlayColorPreviewLabel = None
        self.showStackOverlayWindow()
        self.overlayColor = None

    def onQuit(self, e):
        print "Exiting..."
        self.frame.dispose()
        
    def showColorChooser(self, e):
        colorChooser = JColorChooser()
        self.overlayColor = colorChooser.showDialog(self.frame, "Choose color", Color.red)
        self.overlayColorPreviewLabel.setBackground(self.overlayColor)

    def showStackOverlayWindow(self):
        all = JPanel()
        all.setLayout(MigLayout())

        self.imageIDs = WindowManager.getIDList()
        self.imageNames = []

        if self.imageIDs is None:
            IJ.error("No open images", "Stack Overlay requires at least one image to be already open.")
            return

        for i in self.imageIDs:
            self.imageNames.append(WindowManager.getImage(i).getTitle())

        self.baseImageBox = JComboBox(self.imageNames)
        baseImageBoxLabel = JLabel("Base image")
        self.baseImageBox.setSelectedIndex(0)
        all.add(baseImageBoxLabel)
        all.add(self.baseImageBox, "wrap")

        self.overlayImageBox = JComboBox(self.imageNames)
        overlayImageBoxLabel = JLabel("Overlay image")
        if len(self.imageNames) > 1:
            self.overlayImageBox.setSelectedIndex(1)

        all.add(overlayImageBoxLabel)
        all.add(self.overlayImageBox, "wrap")

        all.add(JSeparator(SwingConstants.HORIZONTAL), "span, wrap")

        overlayStyleFrame = JPanel()
        overlayStyleFrame.setLayout(MigLayout())
        overlayStyleFrame.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Overlay Style"), BorderFactory.createEmptyBorder(5,5,5,5)))

        colorLabel = JLabel("Overlay color")
        self.overlayColorPreviewLabel = JLabel("           ")
        self.overlayColorPreviewLabel.setBorder(BorderFactory.createEmptyBorder(0,0,1,0))
        self.overlayColorPreviewLabel.setOpaque(True)
        self.overlayColorPreviewLabel.setBackground(Color.red)
        self.overlayColor = Color.red
        colorPicker = JColorChooser()
        colorPicker.setPreviewPanel(self.overlayColorPreviewLabel)
        colorButton = JButton("Select color...", actionPerformed=self.showColorChooser)

        opacityLabel = JLabel("Overlay opacity (%)")
        opacitySpinnerModel = SpinnerNumberModel(100, 0, 100, 1)
        self.opacitySpinner = JSpinner(opacitySpinnerModel)

        overlayStyleFrame.add(colorLabel)
        overlayStyleFrame.add(self.overlayColorPreviewLabel)
        overlayStyleFrame.add(colorButton, "wrap")

        overlayStyleFrame.add(opacityLabel)
        overlayStyleFrame.add(self.opacitySpinner, "wrap")
        

        all.add(overlayStyleFrame, "span, wrap")
        
        self.virtualStackCheckbox = JCheckBox("Use Virtual Stack", True)
        all.add(self.virtualStackCheckbox, "span, wrap")

        # TODO: add non-thermonuclear cancel button functionality
        overlayCancelButton = JButton("Cancel", actionPerformed=self.onQuit)
        overlayStartButton = JButton("Overlay images", actionPerformed=self.overlayImages)
        
        all.add(overlayCancelButton, "gapleft push")
        all.add(overlayStartButton, "gapleft push")

        self.frame = JFrame("Stack Overlay")
        self.frame.getContentPane().add(JScrollPane(all))
        self.frame.pack()
        self.frame.setLocationRelativeTo(None)
        self.frame.setVisible(True)
        
    def overlayImages(self, e):
        impBase = WindowManager.getImage(self.imageIDs[self.baseImageBox.getSelectedIndex()])
        refBase = impBase.getStack().getProcessor(1)
        
        impOverlay = WindowManager.getImage(self.imageIDs[self.overlayImageBox.getSelectedIndex()])
        refOverlay = impOverlay.getStack().getProcessor(1)
        
        print "Overlaying for stack sizes " + str(impBase.getStackSize()) + "/" + str(impOverlay.getStackSize()) + "..."
        
        stack = None
        
        if self.virtualStackCheckbox.isSelected():
            stack = OverlayVirtualStack()
            stack.overlayOpacity = float(self.opacitySpinner.getValue())/100.0
            stack.overlayColor = AWTColorToArray(self.overlayColorPreviewLabel.getBackground())
            stack.base = impBase
            stack.overlay = impOverlay

            ImagePlus("Stack Overlay from " + self.imageNames[self.baseImageBox.getSelectedIndex()] + " and " + self.imageNames[self.overlayImageBox.getSelectedIndex()], stack).show()
        else:
            IJ.error("Not implemented yet", "Using normal stacks is not implemented yet. Please use the Virtual Stack option.")


def blendImages(base, overlay, mode="screen"):
    print type(base), type(overlay)
    if mode == "screen":
        image = None
    if mode == "multiply":
        image = Multiply(base, overlay)
    if mode == "add":
        image = Add(base, overlay)
    if mode == "difference":
        image = Difference(base, overlay)

    return image

class OverlayVirtualStack(VirtualStack):
    def __init__(self):
        self.last = None
        self.overlayColor = [255, 255, 255]
        self.overlayOpacity = 1.0
        self.blendMode = "screen"
        self.base = None
        self.overlay = None

    def getProcessor(self, i):
        overlay = IJF.wrap(ImagePlus("", self.overlay.getStack().getProcessor(i)))
        base = self.base.getStack().getProcessor(i).convertToRGB()
        R = Min(overlay, self.overlayColor[0])
        G = Min(overlay, self.overlayColor[1])
        B = Min(overlay, self.overlayColor[2])

        print "Opacity is " + str(self.overlayOpacity)

        overlayrgb = IJF.copyToImagePlus(RGBA(R, G, B, self.overlayOpacity).asImage())
        base.copyBits(overlayrgb.getProcessor(), 0, 0, Blitter.COPY_ZERO_TRANSPARENT)
        baseImage = IJF.wrap(ImagePlus("", base))
        self.last = IJF.displayAsVirtualStack(baseImage).getProcessor()
        return self.last

    def getSize(self):
        return self.base.getStackSize()

    def getSliceLabel(self, i):
        return str(i)

    def getWidth(self):
        self.last.getWidth()

    def getHeight(self):
        self.last.getHeight()

    def getPixels(self, i):
        return self.getProcessor(i).getPixels()
    def setPixels(self, pixels, i):
        pass

stackOverlay = StackOverlay()

print "Done."
