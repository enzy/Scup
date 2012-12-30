package cz.matejsimek.scup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * FTP file uploading based on {@link FTPClient} and {@link FTPSClient}
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class FileUpload {

  private FTPClient client;
  private FTPSClient sclient;
  private FileInputStream fis;
  private String server, username, password, remotePath;

  private FileUpload() {
	client = new FTPClient();
	sclient = new FTPSClient();
	fis = null;
  }

  /**
   * Prepare FTP client for connection
   *
   * @param server FTP server URL
   * @param username
   * @param password
   * @param remotePath folder where files will be uploaded
   */
  public FileUpload(String server, String username, String password, String remotePath) {
	this();
	this.server = server;
	this.username = username;
	this.password = password;
	this.remotePath = remotePath;
  }

  /**
   * Upload local file to remote FTP server with given file name under
   * <code>remotePath</code>
   *
   * @param file local file
   * @param fileName remote file name
   * @return isUploaded which indicates success of upload
   */
  public boolean uploadFile(File file, String fileName) {
	boolean isUploaded = false;

	try {
	  client.connect(server);

	  int reply = client.getReplyCode();
	  if (!FTPReply.isPositiveCompletion(reply)) {
		client.disconnect();
		System.err.println("FTP server refused connection.");
		return false;
	  }

	  if (!client.login(username, password)) {
		client.logout();
		client.disconnect();
		System.err.println("Login failed.");
		return false;
	  }

	  client.enterRemotePassiveMode();
	  client.enterLocalPassiveMode();
	  client.setFileType(FTP.BINARY_FILE_TYPE);

	  fis = new FileInputStream(file);
	  isUploaded = client.storeFile((remotePath.endsWith("/") ? remotePath : remotePath + "/") + fileName, fis);

	  client.logout();

	} catch (IOException e) {
	  System.err.println("FTP: IOException");
	  System.err.println(e.getLocalizedMessage());
	  return false;
	} finally {
	  try {
		if (fis != null) {
		  fis.close();
		}
		client.disconnect();
	  } catch (IOException e) {
		e.printStackTrace();
	  }
	}

	return isUploaded;
  }
}
