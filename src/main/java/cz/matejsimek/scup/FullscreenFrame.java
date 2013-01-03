package cz.matejsimek.scup;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

/**
 * Display given image and crop it to user selected area (by mouse drag)
 *
 * @author Matej Simek | www.matejsimek.cz
 */
public class FullscreenFrame extends JFrame {

  private CountDownLatch run;
  private BufferedImage image;
  private int imageWidth, imageHeight;
  private Point startPoint = new Point();
  private Point endPoint = new Point();
  private Point oldPoint = new Point();
  private Rectangle selectedRectangle = new Rectangle();
  private Rectangle areaRectangle;
  private boolean isCropped = false;

  /**
   * Initialize frame
   *
   * @param run for block calling Thread until frame is finished
   * @param image image to display and crop
   */
  public FullscreenFrame(CountDownLatch run, BufferedImage image) {
	this.run = run;
	this.image = image;
	imageWidth = image.getWidth();
	imageHeight = image.getHeight();
	areaRectangle = new Rectangle(0, 0, imageWidth - 1, imageHeight - 1);

	// Set frame parameteres
	setAlwaysOnTop(true);
	setUndecorated(true);
	setExtendedState(MAXIMIZED_BOTH);
	setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

	// Detect mouse events (for crop area)
	addMouseListener(new MouseAdapter() {
	  public void mousePressed(MouseEvent e) {
		startPoint = endPoint = oldPoint = e.getPoint();
		repaint();
	  }

	  public void mouseReleased(MouseEvent e) {
		crop();
		setVisible(false);
	  }
	});
	addMouseMotionListener(new MouseMotionAdapter() {
	  public void mouseDragged(MouseEvent e) {
		// Move with whole rectangle with CTRL key
		if (e.isControlDown()) {
		  int xmove = oldPoint.x - e.getPoint().x;
		  int ymove = oldPoint.y - e.getPoint().y;
		  startPoint.x -= xmove;
		  startPoint.y -= ymove;
		  endPoint.x -= xmove;
		  endPoint.y -= ymove;

		  oldPoint = e.getPoint();
		} // Move with endpoint
		else {
		  endPoint = e.getPoint();
		  oldPoint = endPoint;
		}
		repaint();
	  }
	});

	// Close the frame when the user presses escape
	KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
	Action escapeAction = new AbstractAction() {
	  @Override
	  public void actionPerformed(ActionEvent e) {
		setVisible(false);
	  }
	};
	getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
	getRootPane().getActionMap().put("ESCAPE", escapeAction);

	setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
  }

  /**
   *
   * @return cropped or original image
   */
  public BufferedImage getCroppedImage() {
	return image;
  }

  /**
   * Paint image, yellow window border and user selected area
   *
   * @param g Graphics
   */
  @Override
  public void paint(Graphics g) {
	Graphics2D g2d = (Graphics2D) g;

	g2d.drawImage(image, null, 0, 0);

	if (startPoint.x != endPoint.x || startPoint.y != endPoint.y) {
	  int x1 = (startPoint.x < endPoint.x) ? startPoint.x : endPoint.x;
	  int y1 = (startPoint.y < endPoint.y) ? startPoint.y : endPoint.y;
	  int x2 = (startPoint.x > endPoint.x) ? startPoint.x : endPoint.x;
	  int y2 = (startPoint.y > endPoint.y) ? startPoint.y : endPoint.y;
	  selectedRectangle.x = x1;
	  selectedRectangle.y = y1;
	  selectedRectangle.width = (x2 - x1) + 1;
	  selectedRectangle.height = (y2 - y1) + 1;
	  g2d.draw(selectedRectangle);
	}

	g2d.setColor(Color.YELLOW);
	g2d.draw(areaRectangle);
  }

  /**
   * Behavior like
   * <code>{@link JFrame}.setVisible(boolean b)</code>, but with onhide
   * CountDownLatch release
   *
   * @param b if false unblock CountDownLatch and hide itself
   */
  @Override
  public void setVisible(boolean b) {
	super.setVisible(b);

	if (!b) {
	  run.countDown();
	}
  }

  /**
   *
   * @return true if user already cropped image
   */
  public boolean isImageCropped() {
	return isCropped;
  }

  /**
   * Crop image from startPoint to endPoint and set flag isCropped
   */
  public void crop() {
	if (startPoint.equals(endPoint)) {
	  setVisible(false);
	}

	int x1 = (startPoint.x < endPoint.x) ? startPoint.x : endPoint.x;
	int y1 = (startPoint.y < endPoint.y) ? startPoint.y : endPoint.y;

	int x2 = (startPoint.x > endPoint.x) ? startPoint.x : endPoint.x;
	int y2 = (startPoint.y > endPoint.y) ? startPoint.y : endPoint.y;

	int width = (x2 - x1) + 1;
	int height = (y2 - y1) + 1;

	BufferedImage croppedImage = image.getSubimage(x1, y1, width, height);
	image.flush();
	image = croppedImage;
	croppedImage = null;

	isCropped = true;
  }
}
