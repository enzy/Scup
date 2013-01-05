package cz.matejsimek.scup;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Dropbox file uploading based on Dropbox SDK
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class DropboxUpload {

  final static private String APP_KEY = "zjphszw97j58mlq";
  final static private String APP_SECRET = "vjzbym5o71u173w";
  final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
  private String user_key, user_secret;
  private WebAuthSession session;
  private DropboxAPI<WebAuthSession> mDBApi;

  /**
   * Authenticate Scup app on Dropbox webpage and receive API access token
   *
   * @throws DropboxException
   */
  private void authenticateUser() throws DropboxException {
	System.out.println("Dropbox authentication...");
	session.unlink();
	WebAuthInfo authInfo = session.getAuthInfo();
	RequestTokenPair pair = authInfo.requestTokenPair;
	String url = authInfo.url;

	try {
	  Desktop.getDesktop().browse(new URL(url).toURI());
	} catch (Exception ex) {
	  ex.printStackTrace();
	}

	JOptionPane pane = new JOptionPane("Browser will launch on Dropbox authentication page. \nOnce you have allowed Scup to access, press OK to continue.", JOptionPane.PLAIN_MESSAGE);
	JDialog dialog = pane.createDialog(new JFrame(), "Scup - Dropbox authentication");
	dialog.setIconImage(Scup.iconImage);
	dialog.setAlwaysOnTop(true);
	dialog.setVisible(true);

	session.retrieveWebAccessToken(pair);
	AccessTokenPair tokens = session.getAccessTokenPair();

	this.user_key = tokens.key;
	this.user_secret = tokens.secret;

	// Start Dropbox session
	mDBApi = new DropboxAPI<WebAuthSession>(session);
  }

  /**
   *
   * @return API token key
   */
  public String getKey() {
	return this.user_key;
  }

  /**
   *
   * @return API token secret
   */
  public String getSecret() {
	return this.user_secret;
  }

  /**
   * Invokes connection to Dropbox API
   *
   * @param key User API token key
   * @param secret User API token secret
   * @throws DropboxException
   */
  public DropboxUpload(String key, String secret) throws DropboxException {
	AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
	session = new WebAuthSession(appKeys, ACCESS_TYPE);
	this.user_key = key;
	this.user_secret = secret;

	// Authenticate Scup app when its neccesarry
	if (key.isEmpty() || secret.isEmpty()) {
	  authenticateUser();
	} // Otherwise already authenticated in past
	else {
	  session.setAccessTokenPair(new AccessTokenPair(key, secret));
	  // Start Dropbox session
	  mDBApi = new DropboxAPI<WebAuthSession>(session);
	}
  }

  /**
   * Uploads given file to Dropbox, handles unlink exception with new
   * authentication
   *
   * @param file file to upload
   * @param fileName name of file on Dropbox
   * @return
   */
  public String uploadFile(File file, String fileName) {
	FileInputStream fis = null;
	try {
	  fis = new FileInputStream(file);
	  Entry newEntry = mDBApi.putFileOverwrite(fileName, fis, file.length(), null);
	  fis.close();

	  return mDBApi.share(newEntry.path).url;

	} // When user removes Scup application from dropbox, unlink happens, dont give up!
	catch (DropboxUnlinkedException duex) {
	  try {
		fis.close();
		authenticateUser();
		return uploadFile(file, fileName);
	  } catch (Exception ex) {
		ex.printStackTrace();
	  }
	} // And many other errors we may ignore
	catch (Exception ex) {
	  ex.printStackTrace();
	}
	// Close file strem
	if (fis != null) {
	  try {
		fis.close();
	  } catch (IOException ex) {
		ex.printStackTrace();
	  }
	}

	return null;

  }
}
