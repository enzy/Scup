package cz.matejsimek.scup;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Clipboard listener which decide what to do with clipboard content
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class ClipboardChangeListener extends Thread {

  /**
   * Data source
   */
  private Clipboard clipboard;
  /**
   * Dimension of virtual desktop needed to decide from what source image is
   */
  private Dimension virtualSize;
  /**
   * Indicates multiple display setup
   */
  private boolean multipleDisplays = false;

  @Override
  public void run() {
	Scup.setClipboard("");
	System.out.println("Starting clipboard listener...");

	BufferedImage oldImage = null;
	List<File> oldFiles = null;

	while (true) {

	  try {
		// Text in clipboard idicates free way to clear old references
		if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
		  if (oldImage != null) {
			oldImage.flush();
		  }
		  oldImage = null;
		  oldFiles = null;
		} // Compare old image with new one from clipboard if its available
		else if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
		  BufferedImage newImage = (BufferedImage) clipboard.getData(DataFlavor.imageFlavor);
		  clipboard.setContents(new StringSelection(""), null);

		  if (!newImage.equals(oldImage)) {
			System.out.println("New image detected in clipboard");
			if (oldImage != null) {
			  oldImage.flush();
			}
			oldImage = newImage;
			processImageContent(newImage);
		  } else {
			newImage.flush();
			newImage = null;
		  }

		} // Compare old file list with new one from clipboard if its available
		else if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
		  List<File> newFiles = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
		  if (!newFiles.equals(oldFiles)) {
			System.out.println("New files detected in clipboard");
			oldFiles = newFiles;
			processFileContent(newFiles);
		  } else {
			newFiles = null;
		  }
		}
	  } catch (Exception ex) {
		ex.printStackTrace();
	  }

	  synchronized (this) {
		try {
		  this.sleep(500);
		} catch (InterruptedException ex) {
		  ex.printStackTrace();
		}
	  }
	}
  }

  /**
   * Detect dimensions of virtual space and save them to
   * <code>Dimension virtualSize</code> and
   * <code>Point virtualOrigin</code>
   */
  void detectVirtualDimensions() {
	GraphicsEnvironment ge;
	ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	Rectangle vBounds = new Rectangle();
	GraphicsDevice[] gdArray = ge.getScreenDevices();

	for (int i = 0; i < gdArray.length; i++) {
	  GraphicsDevice gd = gdArray[i];
	  GraphicsConfiguration[] gcArray = gd.getConfigurations();

	  for (int j = 0; j < gcArray.length; j++) {
		vBounds = vBounds.union(gcArray[j].getBounds());
	  }
	}
	multipleDisplays = gdArray.length > 1 ? true : false;
	virtualSize = vBounds.getSize();
  }

  /**
   *
   * @param clipboard Data source
   * @param monitorAll Capture images from all sources, not only printscreen
   */
  public ClipboardChangeListener(Clipboard clipboard) {
	this.clipboard = clipboard;
  }

  /**
   * Basic image content handling - determinates image source and
   *
   * @param image
   */
  public void processImageContent(BufferedImage image) {
	GraphicsDevice currentDevice = MouseInfo.getPointerInfo().getDevice();
	detectVirtualDimensions();

	// Decide from what source is image in clipboard based on its dimensions
	if (image.getWidth() < virtualSize.width || image.getHeight() < virtualSize.height) {
	  // Custom source
	  if (Scup.MONITOR_ALL) {
		Scup.processImage(image, false, currentDevice);
	  } else {
		System.out.println("Skipping image, Monitor all is disabled.");
	  }
	  image.flush();
	  image = null;

	} else {
	  if (multipleDisplays) {
		try {
		  image.flush();
		  image = null;
		  // Full print screen in clipboard, make screen again but only for active device
		  Robot robot = new Robot();
		  BufferedImage newImage = robot.createScreenCapture(currentDevice.getDefaultConfiguration().getBounds());
		  Scup.processImage(newImage, true, currentDevice);
		  newImage.flush();
		  newImage = null;
		} catch (AWTException ex) {
		  ex.printStackTrace();
		}
	  } else {
		Scup.processImage(image, true, currentDevice);
		image.flush();
		image = null;
	  }
	}

	System.gc();
  }

  /**
   * Basic file content handling (only calls Scup.processFiles when
   * Scup.MONITOR_ALL is true)
   *
   * @param files
   */
  public void processFileContent(List<File> files) {
	if (Scup.MONITOR_ALL) {
	  Scup.processFiles(files);
	} else {
	  System.out.println("Skippign files, Monitor all is disabled.");
	}
  }
}
