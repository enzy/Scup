package cz.matejsimek.scup;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Clipboard listener which decide what to do with clipboard content
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class ClipboardChangeListener extends Thread implements ClipboardOwner {

  /**
   * Data source
   */
  private Clipboard clipboard;
  /**
   * Dimension of virtual desktop needed to decide from what source image is
   */
  private Dimension virtualSize;

  public void run() {
	System.out.println("Starting clipboard listener...");
	Transferable trans = clipboard.getContents(this);
	regainOwnership(trans);

	while (true) {
	  synchronized (this) {
		try {
		  this.wait();
		} catch (InterruptedException ex) {
		  ex.printStackTrace();
		}
	  }
	}
  }

  /**
   *
   * @param clipboard Data source
   * @param virtualSize Dimension of virtual desktop needed to decide from what
   * source image is
   * @param monitorAll Capture images from all sources, not only printscreen
   */
  public ClipboardChangeListener(Clipboard clipboard, Dimension virtualSize) {
	this.clipboard = clipboard;
	this.virtualSize = virtualSize;
  }

  /**
   * Whenever clipboard content is changed by other program, steal it back and process new data
   * @param clipboard
   * @param t
   */
  public void lostOwnership(Clipboard clipboard, Transferable t) {
	this.clipboard = clipboard;

	try {
	  this.sleep(200);
	} catch (Exception e) {
	  e.printStackTrace();
	}
	try {
	  Transferable contents = clipboard.getContents(this);
	  regainOwnership(contents);
	  processContents(contents);
	} catch (IllegalStateException e) {
	  System.out.println("Cannot gain acces to clipboard, trying again...");
	  try {
		this.sleep(100);
		lostOwnership(clipboard, t);
	  } catch (Exception ex) {
		ex.printStackTrace();
	  }
	}
  }

  /**
   * Steal ownership of clipboard by writing same data
   * @param contents Actual clipboard data
   */
  void regainOwnership(Transferable contents) {
	clipboard.setContents(contents, this);
  }

  /**
   * Decide what to do with clipboard content and invoke Scup static methods
   * @param contents Clipboard data
   */
  public void processContents(Transferable contents) {

	try {
	  if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
		// Image detected in clipboard, lets capture it!
		final BufferedImage image = (BufferedImage) contents.getTransferData(DataFlavor.imageFlavor);
		final BufferedImage newImage;
		final GraphicsDevice currentDevice = MouseInfo.getPointerInfo().getDevice();

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

	  } else if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
		// Files detected in clipboard, lets capture them!
		List<File> files = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
		Scup.processFiles(files);
	  }

	} catch (IllegalStateException ex) {
	  ex.printStackTrace();
	} catch (Exception ex) {
	  ex.printStackTrace();
	}
  }
}
