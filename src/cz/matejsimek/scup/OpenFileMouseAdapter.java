package cz.matejsimek.scup;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * Stores file path/url and calls Scup.openOnFile or Scup.openBrowserOn on mouse click
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class OpenFileMouseAdapter extends MouseAdapter {

  private String path;
  private boolean isLocalFile;

  public OpenFileMouseAdapter(String path, boolean isLocalFile) {
	this.path = path;
	this.isLocalFile = isLocalFile;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
	if (e.isControlDown()) {
	  if (isLocalFile) {
		Scup.openOnFile(path);
	  } else {
		Scup.openBrowserOn(URI.create(path));
	  }
	}
  }
}
