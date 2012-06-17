package cz.matejsimek.scup;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.MouseInfo;
import java.awt.Robot;
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

/**
 * Clipboard listener which decide what to do with clipboard content
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class ClipboardChangeListener implements FlavorListener {

    /**
     * Data source
     */
    private Clipboard clipboard;
    /**
     * Dimension of virtual desktop needed to decide from what source image is
     */
    private Dimension virtualSize;

    /**
     *
     * @param clipboard Data source
     * @param virtualSize Dimension of virtual desktop needed to decide from what source image is
     * @param monitorAll Capture images from all sources, not only printscreen
     */
    public ClipboardChangeListener(Clipboard clipboard, Dimension virtualSize) {
	this.clipboard = clipboard;
	this.virtualSize = virtualSize;
    }

    /**
     * Clipboard change event handler
     * @param e FlavorEvent
     */
    @Override
    public void flavorsChanged(FlavorEvent e) {
	//System.out.println("Clipboard changed " + e.getSource() + " " + e.toString());

	try {
	    if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
		// Image detected in clipboard, lets capture it!
		final BufferedImage image = (BufferedImage) clipboard.getData(DataFlavor.imageFlavor);
		final BufferedImage newImage;
		final GraphicsDevice currentDevice = MouseInfo.getPointerInfo().getDevice();
		// Needed for change detection to work
		this.clearClipboard();

		// Decide from what source is image in clipboard on its dimensions
		if (image.getWidth() < virtualSize.width || image.getHeight() < virtualSize.height) {
		    // Custom source
		    if (Scup.MONITOR_ALL) {
			// Current thread is EDT, start another for image processing
			Thread t = new Thread(new Runnable() {
			    @Override
			    public void run() {
				Scup.processImage(image, false, currentDevice);
				// Try to release all image resources
				image.getGraphics().dispose();
				image.flush();
				System.gc();
			    }
			});
			t.start();
		    } else {
			// Try to release all image resources
			image.getGraphics().dispose();
			image.flush();
			System.gc();
		    }
		} else {
		    // Full print screen in clipboard, make screen again but only for active device
		    Robot robot = new Robot();
		    newImage = robot.createScreenCapture(currentDevice.getDefaultConfiguration().getBounds());
		    // Current thread is EDT, start another for image processing
		    Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
			    Scup.processImage(newImage, true, currentDevice);
			    // Try to release all image resources
			    newImage.getGraphics().dispose();
			    newImage.flush();
			    System.gc();
			}
		    });
		    t.start();
		    // Try to release all image resources
		    image.flush();
		}

	    }
	    if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
		// Files detected in clipboard, lets capture them!
		List<File> files = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
		this.clearClipboard();
		Scup.processFiles(files);
	    }

	} catch (Exception ex) {
	    ex.printStackTrace();
	    clearClipboard();
	}

	System.gc();
    }

    /**
     * Strong clipboard cleaning needed for change detection to work, otherwise flavorsChanged is not called
     */
    public void clearClipboard() {
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
	    try {
		Thread.sleep(10);
	    } catch (InterruptedException ex1) {
	    }
	    clearClipboard();
	}
    }
}
