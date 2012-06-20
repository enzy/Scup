package cz.matejsimek.scup;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Desktop;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

/**
 * Scup - Simple screenshot & file uploader <p>Easily upload screenshot or files
 * to FTP server and copy its URL address to clipboard.
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class Scup {

    /**
     * Simple new version checking with incremental number
     */
    final static int VERSION = 1;
    //
    public static Clipboard clipboard;
    public static TrayIcon trayIcon;
    public static Point virtualOrigin;
    public static Dimension virtualSize;
    private static Preferences prefs;
    /**
     * 16x16 app icon
     */
    public static BufferedImage iconImage = null;
    /**
     * User configuration keys
     */
    public final static String KEY_FTP_SERVER = "FTP_SERVER";
    public final static String KEY_FTP_USERNAME = "FTP_USERNAME";
    public final static String KEY_FTP_PASSWORD = "FTP_PASSWORD";
    public final static String KEY_DIRECTORY = "FTP_DIRECTORY";
    public final static String KEY_URL = "URL";
    public final static String KEY_UPLOAD = "UPLOAD";
    public final static String KEY_MONITOR_ALL = "MONITOR_ALL";
    public final static String KEY_INITIAL_SETTINGS = "INITIAL_SETTINGS";
    /**
     * FTP configuration variables
     */
    private static String FTP_SERVER, FTP_USERNAME, FTP_PASSWORD, FTP_DIRECTORY, URL;
    /**
     * Flag which enable upload to FTP server
     */
    public static boolean UPLOAD;
    /**
     * Flag which enable capture images from all sources, not only printscreen
     */
    public static boolean MONITOR_ALL;
    /**
     * Flag indicates initial settings
     */
    private static boolean INITIAL_SETTINGS;
    /**
     * Tray Popup menu items
     */
    private static final CheckboxMenuItem uploadEnabledCheckBox = new CheckboxMenuItem("Upload to FTP");
    private static final CheckboxMenuItem monitorAllCheckBox = new CheckboxMenuItem("Monitor all");

    /**
     * Startup initialization, then endless Thread sleep
     *
     * @param args not used yet
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
	// Set system windows theme and load default icon
	try {
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    iconImage = ImageIO.read(Scup.class.getResource("/icon.png"));
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	// Read configuration
	prefs = Preferences.userNodeForPackage(cz.matejsimek.scup.Scup.class);
	readConfiguration();
	// Init tray icon
	initTray();
	// Detect virtual space
	detectVirtualDimensions();
	// Get system clipboard and asign event handler to it
	clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	clipboard.addFlavorListener(new ClipboardChangeListener(clipboard, virtualSize));
	// Force users to download new version
	checkForUpdates();
	// Show configuration form on startup until first save
	if (INITIAL_SETTINGS) {
	    new SettingsForm().setVisible(true);
	}

	// Endless program run, events are handled in EDT thread
	while (true) {
	    Thread.sleep(Long.MAX_VALUE);
	}
    }

    /**
     * Simple new version checking with incremental number under on url
     */
    static private void checkForUpdates() {
	System.out.println("Checking for updates...");

	try {
	    final URI projectURL = new URI("https://github.com/enzy/Scup");
	    //final URL url = new URL("http://localhost/Scup/version.txt");
	    final URL url = new URL("https://raw.github.com/enzy/Scup/master/version.txt");
	    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

	    String inputLine = in.readLine();
	    if (inputLine != null) {
		int remoteVersion = Integer.parseInt(inputLine);
		System.out.println("Found version " + remoteVersion + ", your is " + VERSION);

		if (remoteVersion > VERSION) {
		    System.out.println("New version available! Get it at: " + projectURL);
		    // Create option dialog
		    JOptionPane pane = new JOptionPane("New version is available. Download it now?", JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION);
		    JDialog dialog = pane.createDialog(new JFrame(), "Scup - Update check");
		    dialog.setIconImage(iconImage);
		    dialog.setAlwaysOnTop(true);
		    dialog.setVisible(true);
		    // Get user choice
		    Object obj = pane.getValue();
		    // Open browser on project page on yes and close program
		    if (obj != null && !obj.equals(JOptionPane.UNINITIALIZED_VALUE) && (Integer) obj == 0) {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			    Desktop.getDesktop().browse(projectURL);
			    System.exit(0);
			}
		    }
		} else {
		    System.out.println("You have the latest version.");
		}
	    }

	    in.close();

	} catch (Exception ex) {
	    System.err.println("Check for updates failed.");
	    ex.printStackTrace();
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
	    // Different trayicon sizes, prefer downscalling
	    String icoVersion;
	    int icoWidth = tray.getTrayIconSize().width;
	    if (icoWidth <= 16) {
		icoVersion = "";
	    } else if (icoWidth <= 24) {
		icoVersion = "24";
	    } else if (icoWidth <= 32) {
		icoVersion = "32";
	    } else if (icoWidth <= 48) {
		icoVersion = "48";
	    } else if (icoWidth <= 64) {
		icoVersion = "64";
	    } else if (icoWidth <= 96) {
		icoVersion = "96";
	    } else if (icoWidth <= 128) {
		icoVersion = "128";
	    } else if (icoWidth <= 256) {
		icoVersion = "256";
	    } else {
		icoVersion = "512";
	    }
	    // Load tray icon
	    try {
		trayIcon = new TrayIcon(ImageIO.read(Scup.class.getResource("/icon" + icoVersion + ".png")), "Scup v0.1");
		// @TODO enable trayIcon.setImageAutoSize(true); after some real test
	    } catch (IOException ex) {
		System.err.println("IOException: TrayIcon could not be added.");
		System.exit(1);
	    }
	    // Add tray icon to system tray
	    try {
		tray.add(trayIcon);
	    } catch (AWTException e) {
		System.err.println("AWTException: TrayIcon could not be added.");
		System.exit(1);
	    }
	    // Build popup menu showed on trayicon right click (on Windows)
	    PopupMenu popup = new PopupMenu();

	    MenuItem exitItem = new MenuItem("Exit");
	    MenuItem settingsItem = new MenuItem("Settings...");

	    popup.add(settingsItem);
	    popup.add(uploadEnabledCheckBox);
	    popup.add(monitorAllCheckBox);
	    popup.addSeparator();
	    popup.add(exitItem);
	    // Add popup to tray
	    trayIcon.setPopupMenu(popup);
	    // Set default flags
	    uploadEnabledCheckBox.setState(UPLOAD);
	    monitorAllCheckBox.setState(MONITOR_ALL);

	    // Add listener to uploadEnabledCheckBox
	    uploadEnabledCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    int chxbx = e.getStateChange();
		    if (chxbx == ItemEvent.SELECTED) {
			UPLOAD = true;
		    } else {
			UPLOAD = false;
		    }
		    prefs.putBoolean(KEY_UPLOAD, UPLOAD);
		}
	    });

	    // Add listener to monitorAllCheckBox
	    monitorAllCheckBox.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    int chxbx = e.getStateChange();
		    if (chxbx == ItemEvent.SELECTED) {
			MONITOR_ALL = true;
		    } else {
			MONITOR_ALL = false;
		    }
		    prefs.putBoolean(KEY_MONITOR_ALL, MONITOR_ALL);
		}
	    });

	    // Add listener to exitItem.
	    exitItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    tray.remove(trayIcon);
		    System.exit(0);
		}
	    });

	    // Add listener to settingsItem.
	    settingsItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    new SettingsForm().setVisible(true);
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
     * FTP_DIRECTORY, URL, UPLOAD, MONITOR_ALL</code>
     *
     * @param filename to read configuration from
     */
    static private void readConfiguration(/*String filename*/) {
	// Load config
	FTP_SERVER = prefs.get(KEY_FTP_SERVER, "localhost");
	FTP_USERNAME = prefs.get(KEY_FTP_USERNAME, "anonymous");
	FTP_PASSWORD = prefs.get(KEY_FTP_PASSWORD, "");
	FTP_DIRECTORY = prefs.get(KEY_DIRECTORY, "");
	URL = prefs.get(KEY_URL, "http://localhost");
	UPLOAD = prefs.getBoolean(KEY_UPLOAD, false);
	MONITOR_ALL = prefs.getBoolean(KEY_MONITOR_ALL, true);
	INITIAL_SETTINGS = prefs.getBoolean(KEY_INITIAL_SETTINGS, true);
    }

    public static void reloadConfiguration() {
	readConfiguration();
	uploadEnabledCheckBox.setState(UPLOAD);
	monitorAllCheckBox.setState(MONITOR_ALL);
    }

    /**
     * Whole image handling process - display, crop, save on disk, transfer to
     * FTP, copy its URL to clipboard
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