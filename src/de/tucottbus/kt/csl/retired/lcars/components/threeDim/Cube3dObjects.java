package de.tucottbus.kt.csl.retired.lcars.components.threeDim;

import java.awt.Canvas;
import java.net.URL;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Texture;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point2f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.selman.java3d.Cuboid;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.image.TextureLoader;

import de.tucottbus.kt.lcars.logging.Log;
import j3d.examples.particles.shapes.FuzzBallFactory;

/**
 * This abstract class contains all objects that appear in the cube3d class.
 * @author Martin Birth
 *
 */
@Deprecated
public abstract class Cube3dObjects {
  protected final String FLOOR_PT_NAME = "floorPoint";
  
  public final static String SLIDER_X = "sliderX";
  public final static String SLIDER_Y = "sliderY";
  public final static String SLIDER_Z = "sliderZ";
  
  /*
   * SLIDER_X_POS[0] - start point
   * SLIDER_X_POS[0] - end point
   */
  protected final static Point3d[] SLIDER_X_POS = {new Point3d(.95f, 0.65f, -1.3f), new Point3d(-1.05f, 0.65f, -1.3f)};
  protected final static Point3d[] SLIDER_Y_POS = {new Point3d(1.15f, -0.6f, 1f), new Point3d(1.15f, -0.6f, -1f)};
  protected final static Point3d[] SLIDER_Z_POS = {new Point3d(-1f, -0.6f, 1.15f), new Point3d(-1f, 0.6f, 1.15f)};
  
  private final Color3f SLICE_COLOR = new Color3f(0f, 0.5f, 0.8f);
  protected final Color3f TARGET_COLOR = new Color3f(0.75f, 0.0f, 0.0f);
  
  protected final Point3d DEFAULT_SLICE_POS = new Point3d(0, 0, 0.2);
  
  private final String ARRAY_1 = "array1.jpg";
  private final String ARRAY_2 = "array2.jpg";
  private final String PATH = "/main/resources/";
  
  private final double transAngle = Math.toRadians(90);
  
  /**
   * To get the resource location of a file.
   * 
   * @param file
   *          Desired file.
   * @return URL of the desired file.
   */
  private URL getUrl(String file) {
    URL url = this.getClass().getResource(PATH+file);
    if (url==null)
      Log.err("Cannot find resource file "+file+".");
    return url;
  }
  
  /**
   * Get an empty space lattice model without microphone fields
   * 
   * @param dim
   *          Vector3f
   * @return BranchGroup
   */
  protected BranchGroup getBlank3dCubeBranchGroup(Vector3f dim) {
    BranchGroup cubeBg = new BranchGroup();

    Transform3D cubeTransform3d = new Transform3D();
    cubeTransform3d.setTranslation(new Vector3f(0, 0, 0));
    TransformGroup cubeTransformGroup = new TransformGroup(cubeTransform3d);
    cubeTransformGroup.addChild(getBlank3dCube(dim));

    cubeBg.addChild(cubeTransformGroup);
    cubeBg.compile();
    return cubeBg;
  }
  
  /**
   * Creates a transparent cube, which will serve as a space model.
   * 
   * @return Node of object.
   */
  private Node getBlank3dCube(Vector3f dim) {
    Appearance cubeApp = new Appearance();

    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.BLENDED);
    ta.setTransparency(0f);
    cubeApp.setTransparencyAttributes(ta);

    // set colors
    Color3f objColor = new Color3f(0.5f, 0.5f, 0.5f);
    ColoringAttributes ca = new ColoringAttributes();
    ca.setColor(objColor);
    cubeApp.setColoringAttributes(ca);

    // set line attributes
    LineAttributes lineAttr = new LineAttributes();
    lineAttr.setLineWidth(1f);
    lineAttr.setLinePattern(LineAttributes.PATTERN_SOLID);
    lineAttr.setLineAntialiasingEnable(true);
    cubeApp.setLineAttributes(lineAttr);

    // Set up the polygon attributes
    PolygonAttributes polyAttr = new PolygonAttributes();
    polyAttr.setPolygonMode(PolygonAttributes.POLYGON_LINE);
    polyAttr.setCullFace(PolygonAttributes.CULL_NONE);
    cubeApp.setPolygonAttributes(polyAttr);

    return new Cuboid(dim.getX(), dim.getY(), dim.getZ(), cubeApp);
  }
  
  /**
   * Create a BranchGroup that reflects the grid to room floor
   * 
   * @return BranchGroup
   */
  protected BranchGroup getFloorGridBranchGroup() {
    BranchGroup rootBg = new BranchGroup();
    float w = 2f / 23f;
    for (float y = -1f; y < 1; y += w)
      rootBg.addChild(getLineShape(new Point3f(y, -0.6f, -1), new Point3f(y,
          -0.6f, 1)));

    for (float x = -1f; x < 1; x += w)
      rootBg.addChild(getLineShape(new Point3f(-1, -0.6f, x), new Point3f(1,
          -0.6f, x)));

    rootBg.compile();
    return rootBg;
  }
  
  /**
   * Get a line shape object
   * 
   * @param startPt
   *          Point3f - start point of the line
   * @param endPt
   *          Point3f - end point of the line
   * @return Shape3D
   */
  protected Shape3D getLineShape(Point3f startPt, Point3f endPt) {
    Appearance gridApp = new Appearance();
    ColoringAttributes ca = new ColoringAttributes(
        new Color3f(0.5f, 0.5f, 0.5f), ColoringAttributes.SHADE_GOURAUD);
    gridApp.setColoringAttributes(ca);

    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.BLEND_ZERO);
    ta.setTransparency(0.6f);
    gridApp.setTransparencyAttributes(ta);

    LineAttributes lineAttr = new LineAttributes();
    lineAttr.setLineWidth(2f);
    lineAttr.setLinePattern(LineAttributes.PATTERN_SOLID);
    lineAttr.setLineAntialiasingEnable(true);
    gridApp.setLineAttributes(lineAttr);

    Point3f[] plaPts = new Point3f[2];
    plaPts[0] = startPt;
    plaPts[1] = endPt;
    LineArray pla = new LineArray(2, LineArray.COORDINATES);
    pla.setCoordinates(0, plaPts);
    Shape3D plShape = new Shape3D(pla, gridApp);

    return plShape;
  }
  
  /**
   * Getting a TransformGroup of a single Point
   * 
   * @param radius
   *          float - radius of the point
   * @return
   *        TransformGroup
   */
  protected TransformGroup getFloorPoint(float radius) {
    float height = 0.001f;
    int polygons = 50;
    
    Appearance pointApp = new Appearance();

    // set transparency attributes
    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.NICEST);
    ta.setTransparency(0.7f);
    pointApp.setTransparencyAttributes(ta);

    ColoringAttributes ca = new ColoringAttributes();
    ca.setColor(TARGET_COLOR);
    ca.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
    pointApp.setColoringAttributes(ca);

    Cylinder cyl = new Cylinder(radius, height, Cylinder.BOTTOM, polygons, 1,
        pointApp);

    Transform3D pointTransform3d = new Transform3D();
    pointTransform3d.rotX(transAngle);
    pointTransform3d.rotY(transAngle);
    pointTransform3d.setTranslation(new Vector3f(0f, -0.6f, 0f));
    TransformGroup pointTransformGroup = new TransformGroup(pointTransform3d);
    pointTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    pointTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    pointTransformGroup.addChild(cyl);
    pointTransformGroup.setName(FLOOR_PT_NAME);

    return pointTransformGroup;
  }
  
  /**
   * Get the BranchGroup of the TV node
   * 
   * @return BranchGroup
   */
  protected BranchGroup getTvArrayBranchGroup(Canvas canvas3d, Switch tvArraySwitch) {
    BranchGroup bg = new BranchGroup();
    tvArraySwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    tvArraySwitch.addChild(getInvisibleNode());
    tvArraySwitch.addChild(getTvNode(canvas3d));
    tvArraySwitch.setWhichChild(1);
    
    Transform3D boxTransform3d = new Transform3D();
    boxTransform3d.setTranslation(new Vector3f(0.0f, 0.1f, -0.95f));
    TransformGroup boxTransformGroup = new TransformGroup(boxTransform3d);
    boxTransformGroup.addChild(tvArraySwitch);
    bg.addChild(boxTransformGroup);
    
    bg.compile();
    return bg;
  }
  
  /**
   * Creates a flat square that will serve as a TV object.
   * 
   * @return Node of the object.
   */
  @SuppressWarnings("deprecation")
  protected Node getTvNode(Canvas canvas3d) {
    Appearance boxApp = new Appearance();
    boxApp.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
    boxApp.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
    
    // set transparency attributes
    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.BLENDED);
    ta.setTransparency(0.3f);
    boxApp.setTransparencyAttributes(ta);

    // loading texture
    URL imgUrl = getUrl(ARRAY_1);
    if (imgUrl != null) {
      TextureLoader array1Tex = new TextureLoader(imgUrl, new String("RGB"),
          TextureLoader.BY_REFERENCE, canvas3d);
      boxApp.setTexture(array1Tex.getTexture());
    }

    float[] verts = {
        // front face
        0.6f, -0.3f, -0.04f, 0.6f, 0.3f, -0.04f, -0.6f,
        0.3f,
        -0.04f,
        -0.6f,
        -0.3f,
        -0.04f,

        // back face
        -0.6f, -0.3f, -0.05f, -0.6f, 0.3f, -0.05f, 0.6f, 0.3f,
        -0.05f,
        0.6f,
        -0.3f,
        -0.05f,

        // right face
        0.6f, -0.3f, -0.05f, 0.6f, 0.3f, -0.05f, 0.6f, 0.3f, -0.04f,
        0.6f,
        -0.3f,
        -0.04f,

        // left face
        -0.6f, -0.3f, -0.04f, -0.6f, 0.3f, -0.04f, -0.6f, 0.3f, -0.05f, -0.6f,
        -0.3f,
        -0.05f,

        // top face
        0.6f, 0.3f, -0.04f, 0.6f, 0.3f, -0.05f, -0.6f, 0.3f, -0.05f, -0.6f,
        0.3f, -0.04f,

        // bottom face
        -0.6f, -0.3f, -0.04f, -0.6f, -0.3f, -0.05f, 0.6f, -0.3f, -0.05f, 0.6f,
        -0.3f, -0.04f, };
    QuadArray q = new QuadArray(24, IndexedQuadArray.COORDINATES
        | IndexedQuadArray.TEXTURE_COORDINATE_2);
    q.setCoordinates(0, verts);

    q.setTextureCoordinate(0, new Point2f(1.0f, 0.0f));
    q.setTextureCoordinate(1, new Point2f(1.0f, 1.0f));
    q.setTextureCoordinate(2, new Point2f(0.0f, 1.0f));
    q.setTextureCoordinate(3, new Point2f(0.0f, 0.0f));

    q.setTextureCoordinate(4, new Point2f(1.0f, 0.0f));
    q.setTextureCoordinate(5, new Point2f(1.0f, 1.0f));
    q.setTextureCoordinate(6, new Point2f(0.0f, 1.0f));
    q.setTextureCoordinate(7, new Point2f(0.0f, 0.0f));

    // culling mode
    PolygonAttributes attr = new PolygonAttributes();
    attr.setCullFace(PolygonAttributes.CULL_BACK);
    boxApp.setPolygonAttributes(attr);

    return new Shape3D(q, boxApp);
  }
  
  /**
   * Get the ceiling microphone array BranchGroup.
   * 
   * @return BranchGroup
   */
  protected BranchGroup getCeilingArrayBranchGroup(Canvas canvas3d, Switch ceilingArraySwitch, TransformGroup ceilingArrayTg) {
    BranchGroup arrayBg = new BranchGroup();
    
    ceilingArraySwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    ceilingArraySwitch.addChild(getInvisibleNode());
    ceilingArraySwitch.addChild(getCeilingArray(canvas3d));
    ceilingArraySwitch.setWhichChild(1);
    
    Transform3D cylinTransform3d = new Transform3D();
    cylinTransform3d.rotX(Math.toRadians(-8.4));
    cylinTransform3d.setTranslation(new Vector3f(0.0f, 0.6f, 0.0f));
    
    ceilingArrayTg.setTransform(cylinTransform3d);
    ceilingArrayTg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    ceilingArrayTg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    ceilingArrayTg.addChild(ceilingArraySwitch);
    
    arrayBg.setCapability(BranchGroup.ALLOW_DETACH);
    arrayBg.addChild(ceilingArrayTg);
    
    arrayBg.compile();
    return arrayBg;
  }
  
  /**
   * Creates a shallow cylinder that serves as a ceiling structure.
   * 
   * @return Node of the object.
   */
  protected Node getCeilingArray(Canvas canvas3d) {
    Appearance cylinApp = new Appearance();
    cylinApp.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
    cylinApp.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);

    // set transparency attributes
    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.BLEND_ZERO);
    ta.setTransparency(0.4f);
    cylinApp.setTransparencyAttributes(ta);

    // loading texture
    URL imgUrl = getUrl(ARRAY_2);
    if (imgUrl != null) {
      TextureLoader array2Tex = new TextureLoader(imgUrl, new String("RGB"),
          TextureLoader.BY_REFERENCE | TextureLoader.Y_UP, canvas3d);
      Texture texture = array2Tex.getTexture();
      texture.setBoundaryModeS(Texture.WRAP);
      texture.setBoundaryModeT(Texture.WRAP);
      texture.setBoundaryColor(new Color4f(.0f, .0f, .0f, .0f));
      cylinApp.setTexture(texture);
    }

    return new Cylinder(.5f, .01f, Cylinder.GENERATE_NORMALS
        | Cylinder.BOTTOM, 80, 1, cylinApp);
  }
  
  /**
   * Get an invisible node object.
   * @return Node
   */
  protected Node getInvisibleNode(){
    Appearance app = new Appearance();
    RenderingAttributes ren = new RenderingAttributes();
    ren.setVisible(false);
    app.setRenderingAttributes(ren);
    
    return new Box(0f, 0f, 0f, Box.GENERATE_NORMALS,app);
  }
  
  /**
   * Get a BranchGroup of all room slices.
   * 
   * @return
   *        BranchGroup
   */
  protected BranchGroup getSlicesBranchGroup(Switch slicesSwitch, TransformGroup slicesTg){
    BranchGroup sliceBg = new BranchGroup();
    
    slicesSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    slicesSwitch.addChild(getInvisibleNode());
    
    slicesTg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    slicesTg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    
    // build slices
    slicesTg.addChild(getSlice(SLIDER_X, new Vector3f(1f, .6f, .001f), new Vector3f(0f, 0f, 0f)));
    slicesTg.addChild(getSlice(SLIDER_Y, new Vector3f(1f, .001f, .6f), new Vector3f(0f, 0f, 0f)));
    slicesTg.addChild(getSlice(SLIDER_Z, new Vector3f(1f, .001f, 1f), new Vector3f(0f, (float) DEFAULT_SLICE_POS.getZ(), 0f)));
        
    // switcher
    slicesSwitch.addChild(slicesTg);
    slicesSwitch.setWhichChild(1);
    
    sliceBg.setCapability(BranchGroup.ALLOW_DETACH);
    sliceBg.addChild(slicesSwitch);
    sliceBg.compile();
    return sliceBg;
  }
  
  /**
   * Get a single room slice.
   * 
   * @param name
   *          String - name of the object
   * @param dim
   *          Vector3f
   * @param trans
   *          Vector3f
   * @return
   *        TransformGroup
   */
  protected TransformGroup getSlice(String name, Vector3f dim, Vector3f trans) {
    return getBoxTransformGroup(name, SLICE_COLOR, dim, trans);
  }
  
  /**
   * Get a single slider for better the target representation.
   * 
   * @param name
   *          String
   * @param trans
   * @return
   */
  protected TransformGroup getTargetSlider(String name, Vector3f trans) {
    Vector3f dim = new Vector3f(.03f, .008f, .008f);
    return getBoxTransformGroup(name, TARGET_COLOR, dim, trans);
  }
  
  /**
   * 
   * @param name
   *          String - name of the object
   * @param color
   *          Color3f
   * @param dim
   *          Vector3f
   * @param trans
   *          Vector3f
   * @return
   */
  protected TransformGroup getBoxTransformGroup(String name, Color3f color,
      Vector3f dim, Vector3f trans) {
    Transform3D sliderTransform3d = new Transform3D();

    if (name.contains(SLIDER_X))
      sliderTransform3d.rotY(transAngle);
    else if (name.contains(SLIDER_Y))
      sliderTransform3d.rotX(transAngle);
    else if (name.contains(SLIDER_Z))
      sliderTransform3d.rotY(transAngle);

    sliderTransform3d.setTranslation(trans);
    TransformGroup transformGroup = new TransformGroup(sliderTransform3d);
    transformGroup.setName(name);
    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    transformGroup.addChild(getBoxNode(dim, color, name));

    return transformGroup;
  }
  
  /**
   * Building slider object.
   * 
   * @return Node of the object.
   */
  protected Node getBoxNode(Vector3f dim, Color3f color, String name) {
    Appearance boxApp = new Appearance();

    // set transparency attributes
    TransparencyAttributes ta = new TransparencyAttributes();
    ta.setTransparencyMode(TransparencyAttributes.NICEST);
    ta.setTransparency(0.4f);
    boxApp.setTransparencyAttributes(ta);

    // setting color attributes
    ColoringAttributes ca = new ColoringAttributes();
    ca.setColor(color);
    ca.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
    boxApp.setColoringAttributes(ca);

    Box box = new Box(dim.getX(), dim.getY(), dim.getZ(), boxApp);
    box.setUserData(name);
    return box;
  }

  /**
   * Get a fuzzy ball for canvas
   * @param sphereTransformGroup
   * @param posVector
   * @param fuzzy
   * @return
   */
  protected TransformGroup getFuzzyTransformGroup(TransformGroup sphereTransformGroup, Vector3f posVector, FuzzBallFactory fuzzy)
  {
    Transform3D sphereTransform3d = new Transform3D();
    sphereTransform3d.setTranslation(posVector);
    sphereTransformGroup.setTransform(sphereTransform3d);
    sphereTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    sphereTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    sphereTransformGroup.setName("FuzzySphere");
    sphereTransformGroup.addChild(fuzzy.createShape());
    
    return sphereTransformGroup;
  }
  
  /**
   * Branchgroup for target point and mini slider
   * @param targetSwitch
   * @param targetPointTg
   * @return BranchGroup
   */
  protected BranchGroup getTargetBranchGroup(Switch targetSwitch, TransformGroup targetPointTg, TransformGroup fuzzyTrans) {
    BranchGroup targetBg = new BranchGroup();

    targetSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    targetSwitch.addChild(getInvisibleNode());

    // target point
    targetPointTg.addChild(getFloorPoint(0.04f));
    targetPointTg.addChild(fuzzyTrans);

    // build sliders
    targetPointTg.addChild(getTargetSlider(SLIDER_X, new Vector3f( 0.0f, -0.6f, 0.98f)));
    targetPointTg.addChild(getTargetSlider(SLIDER_Y, new Vector3f( 0.98f, -0.6f, 0.0f)));
    targetPointTg.addChild(getTargetSlider(SLIDER_Z, new Vector3f(-1.0f,  0.0f, 0.98f)));
    
    targetSwitch.addChild(targetPointTg);
    targetSwitch.setWhichChild(0);
    
    targetBg.setCapability(BranchGroup.ALLOW_DETACH);
    targetBg.addChild(targetSwitch);
    
    targetBg.compile();
    return targetBg;
  }
  
  /**
   * Get a slider line
   * 
   * @param startPt
   *          Point3f - start point of the line
   * @param endPt
   *          Point3f - end point of the line
   * @return Shape3D
   */
  protected Shape3D getSliderLineShape(Point3d startPt, Point3d endPt, float thickness, Color3f color) {
    Appearance gridApp = new Appearance();
    ColoringAttributes ca = new ColoringAttributes(
        color, ColoringAttributes.SHADE_FLAT);
    gridApp.setColoringAttributes(ca);

    LineAttributes lineAttr = new LineAttributes();
    lineAttr.setLineWidth(thickness);
    lineAttr.setLinePattern(LineAttributes.PATTERN_SOLID);
    lineAttr.setLineAntialiasingEnable(true);
    gridApp.setLineAttributes(lineAttr);

    Point3d[] plaPts = new Point3d[2];
    plaPts[0] = startPt;
    plaPts[1] = endPt;
    LineArray pla = new LineArray(2, LineArray.COORDINATES);
    pla.setCoordinates(0, plaPts);
    Shape3D plShape = new Shape3D(pla, gridApp);

    return plShape;
  }
  
  /**
   * Get the slider positions according to the transformation group name.
   * @param tg - TransformationGroup
   * @return Point3d[] - p[0] start point, p[1] end point
   */
  public static Point3d[] getSliderPosFromTg(TransformGroup tg){
    if(tg.getName()==SLIDER_X){
      return SLIDER_X_POS;
    } else if(tg.getName()==SLIDER_Y){
      return SLIDER_Y_POS;
    } else if(tg.getName()==SLIDER_Z){
      return SLIDER_Z_POS;
    } else {
      return null;
    }
  }
  
  /**
   * Get the slider positions according to the transformation group
   * name and transformed to the virtual world.
   * @param tg
   * @return
   */
  public static Point3d[] getSliderVirtualPosFromTg(TransformGroup tg){
    Point3d[] slider = getSliderPosFromTg(tg);
    
    Transform3D currentTransform = new Transform3D();
    tg.getLocalToVworld(currentTransform);
    
    currentTransform.transform(slider[0]);
    currentTransform.transform(slider[1]);
    
    return slider;
  }
}
