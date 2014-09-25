package mosaic.region_competition;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import mosaic.core.ipc.InterPluginCSV;
import mosaic.core.utils.ImgTest;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.core.utils.ShellCommand;
import mosaic.plugins.Region_Competition;
import mosaic.region_competition.output.RCOutput;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

public class Jtest 
{	
	@Test
	public void segmentation() 
	{
		Segmentation BG = new Region_Competition();
		MosaicUtils.<RCOutput>testPlugin(BG,"Region_Competition",RCOutput.class);
	}
}
