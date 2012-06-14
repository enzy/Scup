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
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;

public class Scup {

    public static Clipboard clipboard;
    public static FileUpload fileupload;
    public static SystemTray tray;
    public static TrayIcon trayIcon;
    public static Point virtualOrigin;
    public static Dimension virtualSize;
    // User configuration
    private static String FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY, URL;
    // Runtime configuration
    public static boolean isUploadEnabled = true;

    public static void main(String[] args) throws InterruptedException, IOException {
	File configFile = new File("config.properties");
	if (configFile.exists()) {
	    Properties config = new Properties();
	    FileInputStream fis = new FileInputStream(configFile);
	    config.load(fis);
	    fis.close();
	    readConfiguration(config);
	} else {
	    System.err.println("Configuration file config.properties doesn't exist, please create one.");
	    isUploadEnabled = false;
	}

	initTray();
	detectVirtualDimensions();

	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.addFlavorListener(new ClipboardChangeListener(clipboard, virtualSize));

	fileupload = new FileUpload(FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY);

	// Endless program run
	while (true) {
	    Thread.sleep(100);
	}
    }

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

    static private void initTray() {
	if (SystemTray.isSupported()) {
	    tray = SystemTray.getSystemTray();
	    trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage("resources/icon.png"), "Scup v0.1");
	    //trayIcon.setImageAutoSize(true);

	    try {
		tray.add(trayIcon);
	    } catch (AWTException e) {
		System.err.println("TrayIcon could not be added.");
		System.exit(1);
	    }

	    PopupMenu popup = new PopupMenu();
	    final CheckboxMenuItem uploadEnabledCheckBox = new CheckboxMenuItem("Upload enabled");
	    MenuItem exitItem = new MenuItem("Exit");
	    popup.add(uploadEnabledCheckBox);
	    popup.addSeparator();
	    popup.add(exitItem);

	    trayIcon.setPopupMenu(popup);

	    uploadEnabledCheckBox.setState(isUploadEnabled);

	    // Add listener to uploadEnabledCheckBox.
	    uploadEnabledCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    int chxbx = e.getStateChange();
		    if (chxbx == ItemEvent.SELECTED) {
			isUploadEnabled = true;
			uploadEnabledCheckBox.setLabel("Upload enabled");
		    } else {
			isUploadEnabled = false;
			uploadEnabledCheckBox.setLabel("Upload disabled");
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

    static private void readConfiguration(Properties config) {
	FTP_SERVER = config.getProperty("FTP_SERVER");
	FTP_USERNAME = config.getProperty("FTP_USERNAME", "anonymous");
	FTP_PASSWORD = config.getProperty("FTP_PASSWORD", "");
	FTP_DIRECTORY = config.getProperty("FTP_DIRECTORY", "");
	URL = config.getProperty("URL");
    }

    static void processImage(BufferedImage img, boolean cropImage, GraphicsDevice device) {
	System.out.println("Processing image...");
	System.out.println("Image: " + img.getWidth() + "x" + img.getHeight());

	if (cropImage) {
	    img = cropImage(img, device);
	}
	if(img == null){
	    System.out.println("Image is empty, canceling");
	    return;
	}

	File imageFile = saveImageToFile(img);
	img.flush();
	img = null;

	if (isUploadEnabled) {
	    // Transer image to FTP
	    System.out.println("Uploading image to FTP server...");
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
	    // Notify me about it
	    System.out.println("Image saved " + imageFile.getAbsolutePath());
	    trayIcon.displayMessage("Image saved", imageFile.getAbsolutePath(), TrayIcon.MessageType.INFO);
	}

	imageFile = null;
    }

    static BufferedImage cropImage(BufferedImage img, GraphicsDevice device) {
	if (!device.isFullScreenSupported()) {
	    System.err.println("FullScreen is not supported");
	}

	CountDownLatch framerun = new CountDownLatch(1);
	FullscreenFrame fullscreenFrame = new FullscreenFrame(framerun, img);
	fullscreenFrame.setVisible(true);
	device.setFullScreenWindow(fullscreenFrame);

	try {
	    framerun.await();
	} catch (InterruptedException ex) {
	    ex.printStackTrace();
	}

	// When its closed, get cropped image
	if(fullscreenFrame.isImageCropped()){
	    img = fullscreenFrame.getCroppedImage();
	} else{
	    img = null;
	}
	fullscreenFrame.dispose();

	return img;
    }

    static void processFiles(List<File> files) {
	System.out.println("Processing files...");
	for (int i = 0; i < files.size(); i++) {
	    System.out.println("File " + i + ": " + files.get(i).getName());
	}
	System.err.println("Not supported feature yet, stay tuned!");
    }

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