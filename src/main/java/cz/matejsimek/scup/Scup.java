package cz.matejsimek.scup;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;

/**
 * Scup - Simple screenshot & file uploader <p>Easily upload screenshot or files
 * to FTP server and copy its URL address to clipboard.
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class Scup {

    public static Clipboard clipboard;
    public static TrayIcon trayIcon;
    public static Point virtualOrigin;
    public static Dimension virtualSize;
    // User configuration
    private static Properties config;
    private static boolean configError = false;
    /**
     * FTP configuration variables
     */
    private static String FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY, URL;
    /**
     * Flag which enable upload to FTP server
     */
    public static boolean UPLOAD = false;
    /**
     * Flag which enable capture images from all sources, not only printscreen
     */
    public static boolean MONITOR_ALL = true;

    /**
     * Startup initialization, then endless Thread sleep
     *
     * @param args not used yet
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
	// Read configuration
	readConfiguration("config.properties");
	// Init tray icon
	initTray();
	// Detect virtual space
	detectVirtualDimensions();
	// Get system clipboard and asign event handler to it
	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.addFlavorListener(new ClipboardChangeListener(clipboard, virtualSize));

	// Endless program run, events are handled in EDT thread
	while (true) {
	    Thread.sleep(Long.MAX_VALUE);
	}
    }

    /**
     * Detect dimensions of virtual space and save them to
     * <code>Dimension virtualSize</code> and
     * <code>Point virtualOrigin</code>
     */
    static private void detectVirtualDimensions() {
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

	virtualOrigin = vBounds.getLocation();
	virtualSize = vBounds.getSize();
    }

    /**
     * Place app icon into system tray, build popupmenu and attach event
     * handlers to items
     */
    static private void initTray() {
	if (SystemTray.isSupported()) {
	    final SystemTray tray = SystemTray.getSystemTray();
	    // @TODO Different trayicon sizes based on SystemTray.getTrayIconSize()
	    trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage("resources/icon.png"), "Scup v0.1");
	    //trayIcon.setImageAutoSize(true);

	    try {
		tray.add(trayIcon);
	    } catch (AWTException e) {
		System.err.println("TrayIcon could not be added.");
		System.exit(1);
	    }

	    PopupMenu popup = new PopupMenu();

	    final CheckboxMenuItem uploadEnabledCheckBox = new CheckboxMenuItem("Upload to FTP");
	    final CheckboxMenuItem monitorAllCheckBox = new CheckboxMenuItem("Monitor all");
	    MenuItem exitItem = new MenuItem("Exit");
	    popup.add(uploadEnabledCheckBox);
	    popup.add(monitorAllCheckBox);
	    popup.addSeparator();
	    popup.add(exitItem);

	    trayIcon.setPopupMenu(popup);

	    uploadEnabledCheckBox.setState(UPLOAD);
	    uploadEnabledCheckBox.setEnabled(!configError);
	    monitorAllCheckBox.setState(MONITOR_ALL);

	    // Add listener to uploadEnabledCheckBox.
	    uploadEnabledCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    int chxbx = e.getStateChange();
		    if (chxbx == ItemEvent.SELECTED) {
			UPLOAD = true;
		    } else {
			UPLOAD = false;
		    }
		}
	    });

	    // Add listener to uploadEnabledCheckBox.
	    monitorAllCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    int chxbx = e.getStateChange();
		    if (chxbx == ItemEvent.SELECTED) {
			MONITOR_ALL = true;
		    } else {
			MONITOR_ALL = false;
		    }
		}
	    });

	    // Add listener to exitItem.
	    exitItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    tray.remove(trayIcon);
		    System.exit(0);
		}
	    });

	    trayIcon.displayMessage("Scup", "I am here to serve", TrayIcon.MessageType.NONE);
	} else {
	    System.err.println("SystemTray is not supported");
	}
    }

    /**
     * Fills class varibles:
     * <code>FTP_SERVER, FTP_USERNAME, FTP_PASSWORD,
     * FTP_DIRECTORY, URL, UPLOAD, MONITOR_ALL</code> <p>Sets flag
     * <code>configError</code> in case of error
     *
     * @param filename to read configuration from
     */
    static private void readConfiguration(String filename) {
	File configFile = new File(filename);

	try {
	    config = new Properties();
	    FileInputStream fis = new FileInputStream(configFile);
	    config.load(fis);
	    fis.close();

	    FTP_SERVER = config.getProperty("FTP_SERVER", "localhost");
	    FTP_USERNAME = config.getProperty("FTP_USERNAME", "anonymous");
	    FTP_PASSWORD = config.getProperty("FTP_PASSWORD", "");
	    FTP_DIRECTORY = config.getProperty("FTP_DIRECTORY", "");
	    URL = config.getProperty("URL");
	    UPLOAD = Boolean.parseBoolean(config.getProperty("UPLOAD", "true"));
	    MONITOR_ALL = Boolean.parseBoolean(config.getProperty("MONITOR_ALL", "true"));

	} catch (FileNotFoundException nfex) {
	    System.err.println("Configuration file config.properties doesn't exist, please create one.");
	    UPLOAD = false;
	    configError = true;
	} catch (IOException ioex) {
	    System.err.println("Can't read from configuration file!");
	    UPLOAD = false;
	    configError = true;
	}
    }

    /**
     * Whole image handling process - display, crop, save on disk, transfer to FTP, copy its URL to clipboard
     *
     * @param image to process
     * @param cropImage should be image cropped?
     * @param device to display image on
     */
    static void processImage(BufferedImage image, boolean cropImage, GraphicsDevice device) {
	System.out.println("Processing image...");
	System.out.println("Image: " + image.getWidth() + "x" + image.getHeight());

	if (cropImage) {
	    image = cropImage(image, device);
	}
	if (image == null) {
	    System.out.println("Image is empty, canceling");
	    return;
	}

	File imageFile = saveImageToFile(image);
	image.flush();
	image = null;

	if (UPLOAD) {
	    // Transer image to FTP
	    System.out.println("Uploading image to FTP server...");
	    // FTP file upload service
	    FileUpload fileupload = new FileUpload(FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY);
	    if (fileupload.uploadFile(imageFile, imageFile.getName())) {
		System.out.println("Upload done");
		// Generate URL
		String url = (URL.endsWith("/") ? URL : URL + "/") + imageFile.getName();
		System.out.println(url);
		// Copy URL to clipboard
		try {
		    clipboard.setContents(new StringSelection(url), null);
		} catch (IllegalStateException e) {
		    System.err.println("Can't set clipboard, sorry!");
		}
		// Notify me about it
		trayIcon.displayMessage("Image uploaded", url, TrayIcon.MessageType.INFO);
	    } else {
		System.err.println("Upload failed");
		trayIcon.displayMessage("Upload failed", "I can not serve, sorry", TrayIcon.MessageType.ERROR);
	    }
	    // Don't keep copy of already uploaded image
	    imageFile.delete();
	} else {
	    // Copy URL to clipboard
	    try {
		clipboard.setContents(new StringSelection(imageFile.getAbsolutePath()), null);
	    } catch (IllegalStateException e) {
		System.err.println("Can't set clipboard, sorry!");
	    }
	    // Notify user about it
	    System.out.println("Image saved " + imageFile.getAbsolutePath());
	    trayIcon.displayMessage("Image saved", imageFile.getAbsolutePath(), TrayIcon.MessageType.INFO);
	}

	imageFile = null;
    }

    /**
     * Display image full screen and crop it by user celection
     *
     * @param image to crop
     * @param device to display image on
     * @return cropped image or null in case of crop cancel
     */
    static BufferedImage cropImage(BufferedImage image, GraphicsDevice device) {
	CountDownLatch framerun = new CountDownLatch(1);
	FullscreenFrame fullscreenFrame = new FullscreenFrame(framerun, image);
	fullscreenFrame.setVisible(true);

	if (device.isFullScreenSupported()) {
	    device.setFullScreenWindow(fullscreenFrame);
	} else {
	    System.err.println("FullScreen is not supported");
	}

	try {
	    framerun.await();
	} catch (InterruptedException ex) {
	    ex.printStackTrace();
	}

	// When its closed, get cropped image
	if (fullscreenFrame.isImageCropped()) {
	    image = fullscreenFrame.getCroppedImage();
	} else {
	    image = null;
	}
	fullscreenFrame.dispose();

	return image;
    }

    /**
     * @TODO, actually only prints file list
     *
     * @param files
     */
    static void processFiles(List<File> files) {
	System.out.println("Processing files...");
	for (int i = 0; i < files.size(); i++) {
	    System.out.println("File " + i + ": " + files.get(i).getName());
	}
	System.err.println("Not supported feature yet, stay tuned!");
    }

    /**
     * Save image to PNG file named by its content hash into current directory
     *
     * @param img
     * @return
     */
    static File saveImageToFile(BufferedImage img) {
	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    BufferedOutputStream bos = new BufferedOutputStream(baos);
	    ImageIO.write(img, "png", bos);
	    // Calculate hash for filename and cut hash to smaller size
	    String hash = generateHash(baos.toByteArray());
	    String filename = hash.substring(0, 10);
	    // Close streams
	    bos.flush();
	    bos.close();
	    baos.close();

	    //
	    System.out.println("Saving image: " + filename + ".png");
	    File outputfile = new File(filename + ".png");
	    BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(outputfile));
	    ImageIO.write(img, "png", bos2);
	    bos2.flush();
	    bos2.close();

	    return outputfile;

	} catch (IOException ex) {
	    System.err.println("Can't write image to file!");
	}
	return null;
    }

    /**
     * Generate SHA has from given data
     *
     * @param data Array of bytes to calculate hash from
     * @return SHA hash from data or currentTimeMillis in case of error
     */
    static String generateHash(byte[] data) {
	try {
	    MessageDigest md = MessageDigest.getInstance("SHA");
	    md.update(data);

	    Formatter formatter = new Formatter();
	    for (byte b : md.digest()) {
		formatter.format("%02x", b);
	    }

	    return formatter.toString();

	} catch (NoSuchAlgorithmException ex) {
	    System.err.println("Can't load digest algorithm!");
	}

	return Long.toString(System.currentTimeMillis());
    }
}