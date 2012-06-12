package cz.matejsimek.scup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

public class FileUpload {

    private FTPClient client;
    private FTPSClient sclient;
    private FileInputStream fis;
    private String server, username, password, remotePath;

    public FileUpload() {
	client = new FTPClient();
	sclient = new FTPSClient();
	fis = null;
    }

    public FileUpload(String server, String username, String password, String remotePath) {
	this();
	this.server = server;
	this.username = username;
	this.password = password;
	this.remotePath = remotePath;
    }

    public boolean uploadFile(File file, String fileName) {
	boolean isUploaded = false;

	try {
	    client.connect(server);
	    System.out.print(client.getReplyString());

	    int reply = client.getReplyCode();
	    if (!FTPReply.isPositiveCompletion(reply)) {
		client.disconnect();
		System.err.println("FTP server refused connection.");
		System.exit(1);
	    }

	    if (!client.login(username, password)) {
		client.logout();
		client.disconnect();
		System.err.println("Login failed.");
		System.exit(1);
	    }
	    System.out.print(client.getReplyString());
	    System.out.println("Remote system is " + client.getSystemType());

	    client.enterRemotePassiveMode();
	    client.enterLocalPassiveMode();
	    client.setFileType(FTP.BINARY_FILE_TYPE);

	    fis = new FileInputStream(file);
	    isUploaded = client.storeFile((remotePath.endsWith("/") ? remotePath : remotePath + "/") + fileName, fis);

	    client.logout();

	} catch (IOException e) {
	    System.err.println("IOException");
	    System.err.println(e.getLocalizedMessage());
	    //e.printStackTrace();
	    System.exit(1);
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
