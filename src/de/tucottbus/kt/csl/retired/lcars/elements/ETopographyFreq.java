package de.tucottbus.kt.csl.retired.lcars.elements;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.TimerTask;
import java.util.Vector;

import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.EImage;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.swt.ColorMeta;
import de.tucottbus.kt.lcars.swt.ImageMeta;

/**
 * A simple topographic map with a background image, a sector grid, point shaped
 * objects and a cursor brace.
 * 
 * <p>
 * The layout anchor point of this contributor is the top left corner specified
 * by the constructor's <code>lBounds</code> parameter. The layout will not
 * exceed this bounding rectangle.
 * </p>
 * 
 * @author Matthias Wolff
 * @author Martin Birth
 */
@Deprecated
public class ETopographyFreq extends ElementContributor {
  // Common topography fields
  private final Rectangle lBounds;
  private Rectangle2D.Double pBounds;
  private String pUnit;
  private final int style;
  private AffineTransform pTx;

  // Grid fields
  private int gridStyle;
  private Point2D.Double pGridMajor;
  private float gridMajorAlpha;
  @SuppressWarnings("unused")
  private Point2D.Double pGridMinor;
  @SuppressWarnings("unused")
  private float gridMinorAlpha;

  // Points fields
  private final Vector<ERect> points;

  // Map image fields
  private ImageMeta imf = new ImageMeta.None();
  @SuppressWarnings("unused")
  private Rectangle2D.Float mapImageBounds;
  @SuppressWarnings("unused")
  private int mapStyle;

  // Cursor fields
  private final Vector<EElement> cursor;
  private int cursorSize;
  private Point2D.Double cursorPos;

  private static final String TT_CURSORSLIDE = "CURSORSLIDE";

  double logUpperBound;
  double logLowerBound;

  /**
   * Creates a new topographic map contributor.
   * 
   * @param x
   *          The x-coordinate of the upper left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the upper left corner (in LCARS panel pixels).
   * @param w
   *          The width (in LCARS panel pixels).
   * @param h
   *          The height (in LCARS panel pixels).
   * @param style
   *          The LCARS style (see class {@link LCARS}). Only the font and color
   *          attributes will be used.
   */
  public ETopographyFreq(int x, int y, int w, int h, int style) {
    super(x, y);
    this.lBounds = new Rectangle(x, y, w, h);
    this.style = style & (LCARS.ES_FONT | LCARS.ES_COLOR);
    this.points = new Vector<ERect>();
    this.cursor = new Vector<EElement>();
    this.gridStyle = -1;
    this.gridMajorAlpha = 0.3f;
    this.gridMinorAlpha = 0.3f;
    this.mapStyle = -1;
  }

  /**
   * Returns the logical bounds of this topographic map.
   */
  public Rectangle getLogicalBounds() {
    return lBounds;
  }

  /**
   * Sets the physical bounds of this topographic map.
   * 
   * @param bounds
   *          the bounding rectangle in physical units (left, top, width,
   *          height)
   * @param unit
   *          the name of the physical unit, e.g. "m"
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   */
  public void setPhysicalBounds(Rectangle2D.Double bounds, String unit,
      boolean layout) {
    this.pUnit = unit;
    if (this.pBounds != null && this.pBounds.equals(bounds))
      return;
    this.pBounds = bounds;
    logUpperBound = Math.log10(pBounds.getHeight());
    logLowerBound = Math.log10(pBounds.getY());

    // Compute scale and inflate physical bounds to match the logical aspect
    // ratio
    double scale;
    double scaleX = lBounds.getWidth() / pBounds.getWidth();
    double scaleY = lBounds.getHeight() / pBounds.getHeight();
    if (scaleX > scaleY) {
      double inflateX = (lBounds.width - scaleY * pBounds.width) / scaleY;
      pBounds.x -= inflateX / 2;
      pBounds.width += inflateX;
      scale = scaleY;
    } else {
      double inflateY = (lBounds.height - scaleX * pBounds.height) / scaleX;
      pBounds.y -= inflateY / 2;
      pBounds.height += inflateY;
      scale = scaleX;
    }

    // Compute placement transform and relayout
    pTx = new AffineTransform();
    pTx.scale(scale, -scale);
    pTx.translate(-pBounds.x, -pBounds.y - pBounds.height);
    if (layout)
      layout();
  }

  /**
   * Returns the physical bounds of this topographic map.
   */
  public Rectangle2D.Double getPhysicalBounds() {
    return pBounds;
  }

  /**
   * Returns the physical unit.
   */
  public String getPhysicalUnit() {
    return pUnit;
  }

  /**
   * Sets a new topographic map image, e.g. a satellite photo.
   * 
   * @param imageFile
   *          the image file
   * @param bounds
   *          the physicals bounds of the area showed on the image
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   */
  public void setMapImage(ImageMeta imageMeta, Rectangle2D.Float bounds,
      boolean layout) {
    this.imf = imageMeta != null ? imageMeta : new ImageMeta.None();
    this.mapImageBounds = bounds;
    if (layout)
      layout();
  }

  /**
   * Sets the map image's color style.
   * 
   * @param style
   *          The new color style (see class {@link LCARS}). Only the color
   *          attributes will be used. A value of -1 turns off the styling and
   *          makes the image being used as supplied (this is the default
   *          behavior).
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   */
  public void setMapStyle(int style, boolean layout) {
    this.mapStyle = style & LCARS.ES_COLOR;
    if (layout)
      layout();
  }

  /**
   * Sets grid intervals.
   * 
   * @param major
   *          major grid intervals in physical units
   * @param minor
   *          -- reserved, must be <code>null</code> --
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   */
  public void setGrid(Point2D.Double major, Point2D.Double minor, boolean layout) {
    this.pGridMajor = major;
    this.pGridMinor = minor;
    if (layout)
      layout();
  }

  /**
   * Sets the grid's color style, font style and opacity.
   * 
   * @param style
   *          The new grid style (see class {@link LCARS}). Only the color and
   *          font attributes will be used. A value of -1 sets the grid's style
   *          to the topography's style.
   * @param majorAlpha
   *          The opacity of the major grid, 0.0f (transparent) through 1.0f
   *          (opaque).
   * @param minorAlpha
   *          -- reserved, must be 0.f
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   */
  public void setGridStyle(int style, float majorAlpha, float minorAlpha,
      boolean layout) {
    this.gridStyle = style;
    this.gridMajorAlpha = majorAlpha;
    this.gridMinorAlpha = minorAlpha;
    if (layout)
      layout();
  }

  /**
   * Creates a cursor. If there already is a cursor, the old cursor will be
   * destroyed and a new cursor will be created.
   * 
   * @param size
   *          the size of the cursor
   * @param linewidth
   *          the width of the lines
   * @param textStyle
   *          the label text style
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   * @see #removeCursor(boolean)
   * @see #hasCursor()
   * @see #setCursorPos(float, float, String)
   * @see #slideCursor(float, float, long, String)
   */
  public void setCursor(int size, int linewidth, int textStyle, boolean layout) {
    EElbo e;
    size /= 2;
    this.cursorSize = size;
    int size1 = (int) (0.4 * size);
    int size2 = (int) (0.5 * size);
    textStyle = (style & (~LCARS.ES_FONT)) | (textStyle & LCARS.ES_FONT)
        | LCARS.ES_STATIC | LCARS.ES_LABEL_W;

    cursor.clear();
    e = new EElbo(null, this.x, this.y, size1, size, this.style
        | LCARS.ES_STATIC | LCARS.ES_SHAPE_NW, null);
    e.setArmWidths(linewidth, linewidth);
    e.setArcWidths(size2, size1);
    cursor.add(e);
    e = new EElbo(null, this.x, this.y + size, size1, size, this.style
        | LCARS.ES_STATIC | LCARS.ES_SHAPE_SW, null);
    e.setArmWidths(linewidth, linewidth);
    e.setArcWidths(size2, size1);
    cursor.add(e);
    e = new EElbo(null, this.x + 2 * size - size1, this.y, size1, size,
        this.style | LCARS.ES_STATIC | LCARS.ES_SHAPE_NE, null);
    e.setArmWidths(linewidth, linewidth);
    e.setArcWidths(size2, size1);
    cursor.add(e);
    e = new EElbo(null, this.x + 2 * size - size1, this.y + size, size1, size,
        this.style | LCARS.ES_STATIC | LCARS.ES_SHAPE_SE, null);
    e.setArmWidths(linewidth, linewidth);
    e.setArcWidths(size2, size1);
    cursor.add(e);
    cursor.add(new ERect(null, this.x + 2 * size,
        this.y + size - linewidth / 2, size1, linewidth, this.style
            | LCARS.ES_STATIC, null));
    cursor.add(new ELabel(null, this.x + 2 * size + size1 + 3, this.y,
        2 * size, 2 * size, textStyle, null));

    setCursorPos(0f, 0f, "");
    if (layout)
      layout();
  }

  /**
   * Removes the cursor. The method does nothing if there is no cursor.
   * 
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible)
   * @see #setCursor(int, int, int, boolean)
   * @see #hasCursor()
   * @see #setCursorPos(float, float, String)
   * @see #slideCursor(float, float, long, String)
   */
  public void removeCursor(boolean layout) {
    if (!hasCursor())
      return;
    cursor.clear();
    if (layout)
      layout();
  }

  /**
   * Determines if this map has a cursor.
   * 
   * @see #setCursor(int, int, int, boolean)
   * @see #removeCursor(boolean)
   * @see #setCursorPos(float, float, String)
   * @see #slideCursor(float, float, long, String)
   */
  public boolean hasCursor() {
    return cursor != null && cursor.size() > 0;
  }

  /**
   * Sets the label of the cursor. The method does nothing if there is no
   * cursor.
   * 
   * @param label
   *          The new cursor label. If <code>null</code>, the method does
   *          nothing.
   */
  public void setCursorLabel(String label) {
    if (!hasCursor())
      return;
    if (label == null)
      return;
    for (int i = 0; i < cursor.size(); i++) {
      EElement e = cursor.get(i);
      if (e instanceof ELabel)
        ((ELabel) e).setLabel(label);
    }
  }

  /**
   * Instantly moves the cursor to a new target position. The method does
   * nothing if there is no cursor.
   * 
   * @param x
   *          physical x-coordinate of target position
   * @param y
   *          physical x-coordinate of target position
   * @param label
   *          the new cursor label or <code>null</code> to keep the current
   *          label
   * @see #slideCursor(float, float, long, String)
   * @see #setCursor(int, int, int, boolean)
   * @see #removeCursor(boolean)
   * @see #hasCursor()
   */
  public synchronized void setCursorPos(double x, double y, String label) {
    if (cursor == null || cursor.isEmpty())
      return;
    Point p = pToL(x, y);
    Rectangle r = cursor.get(0).getBounds();
    int cx = r.x;
    int cy = r.y;
    for (int i = 0; i < cursor.size(); i++) {
      EElement e = cursor.get(i);
      if (cursorPos == null || !equalPos(new Point2D.Double(x, y), cursorPos)) {
        r = e.getBounds();
        r.x = p.x + (r.x - cx) - cursorSize + this.x;
        r.y = p.y + (r.y - cy) - cursorSize + this.y;
        e.setBounds(r);
      }
      if (i == 4)
        e.setColor("".equals(label) ? new ColorMeta(0, true) : null);
      if (e instanceof ELabel && label != null)
        ((ELabel) e).setLabel(label);
    }
    this.cursorPos = new Point2D.Double(x, y);
  }

  /**
   * Slides the cursor to a new target position. The method does nothing if
   * there is no cursor.
   * 
   * @param x
   *          physical x-coordinate of the target position
   * @param y
   *          physical y-coordinate of the target position
   * @param time
   *          time to target in milliseconds
   * @param label
   *          the cursor label to display when arriving at the target
   * @see #setCursorPos(float, float, String)
   * @see #setCursor(int, int, int, boolean)
   * @see #removeCursor(boolean)
   * @see #hasCursor()
   */
  public synchronized void slideCursor(double x, double y, long time, String label) {
    if (getPanel() == null) {
      setCursorPos(x, y, label);
      return;
    }
    if (cursorPos != null && equalPos(new Point2D.Double(x, y), cursorPos)) {
      // Not moving
      setCursorPos(x, y, label);
      return;
    }
    int steps = Math.round(time / 40);
    long period = time / steps;
    CursorSlideTask tt = new CursorSlideTask(steps, new Point2D.Double(x, y),
        label);
    scheduleTimerTask(tt, TT_CURSORSLIDE, period, period);
  }

  /**
   * Adds a point shaped object to the map.
   * 
   * @param pos
   *          The position of in physical units.
   * @param radius
   *          The radius in panel pixels.
   * @param style
   *          The LCARS element style.
   * @param data
   *          Custom data to be associated with the point (can be
   *          <code>null</code>).
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible).
   * @see #addPoint(java.awt.geom.Point2D.Float, int, int, boolean)
   * @see #movePoint(ERect, java.awt.geom.Point2D.Float)
   * @see #getPoints()
   */
  public ERect addPoint(Point2D.Double pos, int radius, int style, Object data,
      boolean layout) {
    Point lPos = pToL(pos);
    ERect e = new ERect(null, lPos.x - radius + this.x, lPos.y - radius
        + this.y, 2 * radius, 2 * radius, style | LCARS.ES_RECT_RND, null);
    e.setData(data);
    e.addEEventListener(this);
    points.add(e);
    if (layout)
      layout();
    return e;
  }

  /**
   * Adds a point shaped object to the map.
   * 
   * @param pos
   *          The position of in physical units.
   * @param radius
   *          The radius in panel pixels.
   * @param style
   *          The LCARS element style.
   * @param layout
   *          <code>true</code> to recompute the layout (this will make the
   *          changes visible).
   * @see #addPoint(java.awt.geom.Point2D.Float, int, int, Object, boolean)
   * @see #movePoint(ERect, java.awt.geom.Point2D.Float)
   * @see #getPoints()
   */
  public ERect addPoint(Point2D.Double pos, int radius, int style, boolean layout) {
    return addPoint(pos, radius, style, null, layout);
  }

  /**
   * Returns the points in this topography. The returned vector is a copy,
   * modifying it has no effect on the internal data.
   * 
   * @see #addPoint(java.awt.geom.Point2D.Float, int, int, boolean)
   * @see #addPoint(java.awt.geom.Point2D.Float, int, int, Object, boolean)
   * @see #movePoint(ERect, java.awt.geom.Point2D.Float)
   */
  public Vector<ERect> getPoints() {
    return new Vector<ERect>(points);
  }

  /**
   * Instantly moves a point to a new position on the map. If
   * <code>ePoint</code> is <code>null</code> or no point on the map, the method
   * does nothing.
   * 
   * @param ePoint
   *          The point (return value of
   *          {@link #addPoint(java.awt.geom.Point2D.Float, int, int, Object, boolean)
   *          addPoint(...)}).
   * @param pos
   *          The new position of in physical units.
   * @see #addPoint(java.awt.geom.Point2D.Float, int, int, boolean)
   * @see #addPoint(java.awt.geom.Point2D.Float, int, int, Object, boolean)
   * @see #getPoints()
   */
  public void movePoint(ERect ePoint, Point2D.Double pos) {
    if (pos == null)
      return;
    if (points.indexOf(ePoint) < 0)
      return;
    Rectangle lBounds = ePoint.getBounds();
    Point lPos = pToL(pos);
    lBounds.x = lPos.x - lBounds.width / 2 + this.x;
    lBounds.y = lPos.y - lBounds.height / 2 + this.y;
    ePoint.setBounds(lBounds);
  }

  /**
   * Layout.
   */
  protected void layout() {
    if (getPanel() == null || pTx == null)
      return;
    
//  while (getElements().size() > 0)
//    remove(getElements().get(0));
    removeAll();

    Point tl = pToL(pBounds.x, pBounds.y + pBounds.height);
    Point br = pToL(pBounds.x + pBounds.width, pBounds.y);

    // Create map
    try {
      add(new EImage(null, 0, 0, LCARS.ES_STATIC, imf));
    } catch (IllegalStateException e) {}

    // Create points
    for (ERect point : points) {
      Rectangle r = point.getBounds();

      // Add an invisible circle for fat finger touching
      if (!point.isStatic() && (r.height < 38 || r.width < 38)) {
        int x = (int) Math.round(r.getCenterX());
        int y = (int) Math.round(r.getCenterY());
        ERect e = new ERect(null, x - 19, y - 19, 38, 38, 0, null);
        e.setColor(new ColorMeta(0, true));
        e.setData(point);
        e.addEEventListener(new EEventListenerAdapter() {
          @Override
          public void touchDown(EEvent ee) {
            ee.el = (ERect) ee.el.getData();
            fireEEvent(ee);
          }
        });
        add(e, false);
      }

      // Add the point itself
      add(point, false);
    }

    // Create major grid
    int gs = (this.style & LCARS.ES_COLOR) | LCARS.EF_TINY;
    if (this.gridStyle != -1)
      gs = this.gridStyle & (LCARS.ES_COLOR | LCARS.ES_FONT);

    ColorMeta color = LCARS.getColor(getPanel().getColorScheme(), gs);
    int alpha = (int) (255 * gridMajorAlpha);
    color = new ColorMeta(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    ERect e = null;
    if (pGridMajor != null) {
      int lx = pToL(0f, 0f).x;
      e = new ERect(null, lx - 1, tl.y - 1, 2, br.y - tl.y, gs
          | LCARS.ES_STATIC, null);
      e.setColor(color);
      add(e);

      // calculating the slider ticks
      int val = 0;
      int step = (int) pBounds.getY();
      while (val <= pBounds.getHeight()) {
        for (int i = 1; i < 10; i++) {
          if (val >= pBounds.getHeight()) break;
          val = Math.abs(step * i);
          double logVal = getDisplayPosition(val);
          int ly = pToL(0f, logVal).y;
          e = new ERect(null, tl.x - 1, ly - 1, br.x - tl.x, 2, gs | LCARS.ES_STATIC, null);
          e.setColor(color);
          add(e);

          if(i<5 || i==7) {
            int ls = gs | LCARS.ES_LABEL_NW | LCARS.ES_STATIC;
            ELabel l = new ELabel(null, (int) lBounds.getWidth() + 5, ly - 10, 50, 14, ls, ""+val);
            l.setColor(color);
            add(l);
          }
        }
        if (val >= pBounds.getHeight()) break;
        step *= 10;
      }
    }

    // Create the cursor
    String label = "";
    for (EElement el : cursor) {
      add(el, false);
      if (el instanceof ELabel)
        label = el.getLabel();
    }
    if (cursorPos != null) {
      double x = cursorPos.x;
      double y = cursorPos.y;
      cursorPos = null;
      setCursorPos(x, y, label);
    }

    if (getPanel() != null)
      getPanel().invalidate();
  }

  /**
   * Transforming a frequency value to LOG10 and 
   * normalizing this value to the slider bounds.
   * 
   * @param value - frequency value
   * @return
   *        double - normalized value
   */
  protected double getDisplayPosition(Number value) {
    double delta = logUpperBound - logLowerBound;
    double deltaV = Math.log10(value.doubleValue()) - logLowerBound;
    return (deltaV / delta)*pBounds.height;
  }
  
  /**
   * {@link #getDisplayPosition(Number value)}
   * @param pos
   *          Point2D
   * @return
   *         Point2D.Double
   */
  protected Point2D.Double getDisplayPosition(Point2D pos){
    return new Point2D.Double(0,getDisplayPosition(pos.getY()));
  }

  /**
   * Inverse function of {@link #getDisplayPosition(Number value)}
   * 
   * @param pos
   * @return
   *        double - frequency value
   */
  protected double getValueForDisplay(double pos) {
    double delta = logUpperBound - logLowerBound;
    return Math.pow(10,((pos/pBounds.height)*delta)+logLowerBound);
  }
  
  /**
   * {@link #getValueForDisplay(double pos)}
   * @param pos
   *          Point2D
   * @return
   *         Point2D.Double
   */
  protected Point2D.Double getValueForDisplay(Point2D pos){
    return new Point2D.Double(0,getValueForDisplay(pos.getY()));
  }

  // -- Overrides --

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.tucottbus.kt.lcars.contributors.ElementContributor#addToPanel(de.tucottbus
   * .kt.lcars.Panel)
   */
  @Override
  public void addToPanel(Panel panel) {
    super.addToPanel(panel);
    layout();
  }

  // -- Coordinate conversion --

  /**
   * Converts physical to logical coordinates.
   * 
   * @param point
   *          The physical coordinates.
   * @return The logical coordinates.
   */
  public Point pToL(Point2D.Double point) {
    if (pTx == null)
      return new Point(0, 0);
    Point2D p = pTx.transform(point, null);
    int x = (int) Math.round(p.getX());
    int y = (int) Math.round(p.getY());
    return new Point(x, y);
  }

  /**
   * Converts physical to logical coordinates.
   * 
   * @param x
   *          The physical x-coordinate.
   * @param y
   *          The physical y-coordinate.
   * @return The logical coordinates.
   */
  public Point pToL(double x, double y) {
    return pToL(new Point2D.Double(x, y));
  }

  /**
   * Converts logical tp physical coordinates.
   * 
   * @param point
   *          The logical coordinates.
   * @return The physical coordinates.
   */
  public Point2D lToP(Point point) {
    if (pTx == null)
      return new Point2D.Float(0, 0);
    try {
      return pTx.inverseTransform(point, null);
    } catch (NoninvertibleTransformException e) {
      return new Point2D.Float(0, 0);
    }
  }

  /**
   * Converts logical to physical coordinates.
   * 
   * @param x
   *          The logical x-coordinate.
   * @param y
   *          The logical y-coordinate.
   * @return The physical coordinates.
   */
  public Point2D lToP(int x, int y) {
    return lToP(new Point(x, y));
  }

  /**
   * Determines if to physical positions are equal. Two positions are equal if
   * and only of they are represented by the same LCARS panel pixel on the
   * topography.
   * 
   * @param pos1
   *          The first position (physical coordinates).
   * @param pos2
   *          The second position (physical coordinates).
   * @return <code>true</code> if the positions are equal, <code>false</code>
   *         otherwise.
   */
  public boolean equalPos(Point2D.Double pos1, Point2D.Double pos2) {
    Point l1 = pToL(pos1);
    Point l2 = pToL(pos2);
    return l1.x == l2.x && l1.y == l2.y;
  }

  // -- Animations --

  /**
   * Cursor sliding animation.
   */
  class CursorSlideTask extends TimerTask {
    private int steps;
    private final Point2D.Double target;
    private final String label;

    /**
     * Moves the cursor to the specified target coordinates.
     * 
     * @param steps
     *          number of steps to target
     * @param target
     *          physical coordinates to move the cursor to
     * @param targetLabel
     *          the cursor label to display when arrived at the target
     */
    public CursorSlideTask(int steps, Point2D.Double target, String targetLabel) {
      this.steps = steps;
      this.target = target;
      this.label = targetLabel;
    }

    @Override
    public void run() {
      if (steps > 0) {
        double x = cursorPos.x + (target.x - cursorPos.x) / steps;
        double y = cursorPos.y + (target.y - cursorPos.y) / steps;
        if (cursorPos != null && equalPos(new Point2D.Double(x, y), cursorPos)) {
          setCursorPos(target.x, target.y, label);
          cancel();
        } else {
          setCursorPos(x, y, "");
          steps--;
        }
      } else {
        setCursorPos(target.x, target.y, label);
        cancel();
      }
    }
  }
  
}
