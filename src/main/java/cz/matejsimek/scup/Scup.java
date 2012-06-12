package cz.matejsimek.scup;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

public class Scup {

    private static Clipboard clipboard;

    public static void main(String[] args) throws InterruptedException {
	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.addFlavorListener(new ClipboardChangeListener());

	// Endless program run
	while (true) {
	    Thread.sleep(100);
	}
    }

    static void processImage(Image img) {
	System.out.println("Processing image...");
	System.out.println("Image: " + img.getWidth(null) + "x" + img.getHeight(null));
	// TODO crop image
	saveImageToFile(img);
    }

    static void processFiles(List<File> files) {
	System.out.println("Processing files...");
	for (int i = 0; i < files.size(); i++) {
	    System.out.println("File " + i + ": " + files.get(i).getName());
	}
    }

    static class ClipboardChangeListener implements FlavorListener {

	@Override
	public void flavorsChanged(FlavorEvent e) {
	    System.out.println("Clipboard changed " + e.getSource() + " " + e.toString());

	    try {
		if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
		    Image image = (Image) clipboard.getData(DataFlavor.imageFlavor);
		    clearClipboard();
		    Scup.processImage(image);
		}
		if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
		    List<File> files = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
		    clearClipboard();
		    Scup.processFiles(files);
		}
	    } catch(NullPointerException npe){
		// Clipboard content is null
	    } catch(IllegalStateException ise){
		// Clipboard is unavailable
	    } catch (UnsupportedFlavorException ufe) {
		// Cliboard content is unsupported
	    } catch (IOException ioe) {
		// Clipboard content is unreadable
		ioe.printStackTrace();
	    }
	}

	static void clearClipboard() {
	    System.out.println("Clearing clipboard...");
	    try {
		clipboard.setContents(new Transferable() {
		    @Override
		    public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[0];
		    }

		    @Override
		    public boolean isDataFlavorSupported(DataFlavor df) {
			return false;
		    }

		    @Override
		    public Object getTransferData(DataFlavor df) throws UnsupportedFlavorException, IOException {
			throw new UnsupportedOperationException("Not supported yet.");
		    }
		}, null);
	    } catch (IllegalStateException e) {
		System.err.println("Can't clear clipboard!");
	    }
	}
    }

    static File saveImageToFile(Image img) {
	try {
	    File outputfile = new File(System.currentTimeMillis() + ".png");
	    ImageIO.write((BufferedImage) img, "png", outputfile);
	    return outputfile;
	} catch (IOException ex) {
	    System.err.println("Can't write image to file!");
	}
	return null;
    }

//    public void takeScreenshot() {
//	Graphics2D imageGraphics = null;
//	try {
//	    Robot robot = new Robot();
//	    GraphicsDevice currentDevice = MouseInfo.getPointerInfo().getDevice();
//	    BufferedImage exportImage = robot.createScreenCapture(currentDevice.getDefaultConfiguration().getBounds());
//
//	    imageGraphics = (Graphics2D) exportImage.getGraphics();
//	    File screenshotFile = new File("./CurrentMonitorScreenshot-" + System.currentTimeMillis() + ".png");
//	    ImageIO.write(exportImage, "png", screenshotFile);
//	    System.out.println("Screenshot successfully captured to '" + screenshotFile.getCanonicalPath() + "'!");
//	} catch (Exception exp) {
//	    exp.printStackTrace();
//	} finally {
//	    imageGraphics.dispose();
//	}
//    }
}