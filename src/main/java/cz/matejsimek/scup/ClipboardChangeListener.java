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

public class ClipboardChangeListener implements FlavorListener {

    private Clipboard clipboard;
    private Dimension virtualSize;

    public ClipboardChangeListener(Clipboard clipboard, Dimension virtualSize) {
	this.clipboard = clipboard;
	this.virtualSize = virtualSize;
    }

    @Override
    public void flavorsChanged(FlavorEvent e) {
	//System.out.println("Clipboard changed " + e.getSource() + " " + e.toString());

	try {
	    if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
		final BufferedImage image = (BufferedImage) clipboard.getData(DataFlavor.imageFlavor);
		final BufferedImage newImage;
		final GraphicsDevice currentDevice = MouseInfo.getPointerInfo().getDevice();

		this.clearClipboard();

		if (image.getWidth() < virtualSize.width || image.getHeight() < virtualSize.height) {
		    System.out.println("Custom image in clipboard");

		    Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
			    Scup.processImage(image, false, currentDevice);
			    image.flush();
			    System.gc();
			}
		    });
		    t.start();
		} else {
		    Robot robot = new Robot();
		    newImage = robot.createScreenCapture(currentDevice.getDefaultConfiguration().getBounds());
		    Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
			    Scup.processImage(newImage, true, currentDevice);
			    newImage.flush();
			    System.gc();
			}
		    });
		    t.start();
		    image.flush();
		}

	    }
	    if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
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
	    clearClipboard();
	}
    }
}
