package de.tucottbus.kt.csl.lcars.components.threeDim;

import j3d.examples.particles.shapes.FuzzBallFactory;

import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.GraphicsConfiguration;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.Enumeration;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.Node;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.lcars.components.threeDim.mouse.PickTranslateBehaviorNew;
import de.tucottbus.kt.csl.lcars.messages.IObservable;
import de.tucottbus.kt.csl.lcars.messages.IObserver;
import de.tucottbus.kt.csl.lcars.messages.NewBeamformerMsg;
import de.tucottbus.kt.csl.lcars.messages.NewCeilingMsg;
import de.tucottbus.kt.csl.lcars.messages.NewCube3dViewMsg;
import de.tucottbus.kt.csl.lcars.messages.NewSlicePositionMsg;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * 
 * This class is to generate a cube model that corresponds to the Cognitive Systems Model.
 * 
 * @author Martin Birth
 *
 */
public class Cube3d extends Cube3dObjects implements IObserver {
  // ################### static fields ###################
  
  /**
   * Initial angle of the viewpoint
   */
  private final int START_ANGLE = 38; // degree
  
  /**
   * Dimensions of the cube
   */
  private final Vector3f CUBE_DIM = new Vector3f(1.0f, .6f, 1.0f);
  
  /**
   * Default viewpoint
   */
  private final Vector3f DEFAULT_VIEW = new Vector3f(-0.1f, -.5f, 4.2f);
  
  private static Canvas3D canvas3d;
  
  //################### non-static fields ###################
  
  private SimpleUniverse simpleUni;
  private final BoundingSphere bigBounds = new BoundingSphere(new Point3d(),500);
  
  private Vector3f lightDirection;
  
  private BranchGroup sliderBranchGroup;

  private final TransformGroup ceilingArrayTg = new TransformGroup();
  private final TransformGroup targetPointTg = new TransformGroup();
  private final TransformGroup slicesTg = new TransformGroup();
  private final TransformGroup sliceSliderTg = new TransformGroup();
  private final TransformGroup fuzzyPointTg = new TransformGroup();
  
  private final Switch targetSwitch = new Switch();
  private final Switch slicesSwitch = new Switch();
  private final Switch sliderSwitch = new Switch();
  private final Switch tvArraySwitch = new Switch();
  private final Switch ceilingArraySwitch = new Switch();
  
  private final FuzzBallFactory fuzzy = new FuzzBallFactory(0.17f, 0f, TARGET_COLOR, new Color3f(0.2f, 0.2f, 0.2f),1);
  
  private final Point3d targetPoint = new Point3d(0,0,0);
  private final Point3d slicePos = DEFAULT_SLICE_POS;
  
  private Transform3D viewPointMatrix;
  
  @SuppressWarnings("unused")
  private final IObservable observable;
  
 // Point3d leftManualEyeInImagePlate = new Point3d(0.142, 0.135, 0.4572);
 // Point3d rightManualEyeInImagePlate = new Point3d(0.208, 0.135, 0.4572);
  
  /**
   * Constructor
   * @param observable - set the IObservable interface for communication
   */
  public Cube3d(IObservable observable) {
    this.observable=observable;
    GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
    canvas3d = new Canvas3D(config);
  }
  
  /**
   * Get a default movable 3d canvas of the cube3d model
   * 
   * @return
   *     Canvas3D - room model
   */
  public Canvas3D getDefaultCanvas3d() {
    return getCanvas3d(DEFAULT_VIEW, START_ANGLE, true);
  }
  
  /**
   * Get a default non-movable 3d canvas of the cube3d model
   * 
   * @return
   *      Canvas3D - room model
   */
  public Canvas3D getCanvas3d() {
    return getCanvas3d(DEFAULT_VIEW, START_ANGLE, false);
  }
  
  /**
   * Get a projection screen image from canvas.
   * 
   * @param canvas
   * @return
   *       BufferedImage
   */
  public static BufferedImage getImage(Canvas canvas) {
    Robot r;
    try {
      r = new Robot();
      return r.createScreenCapture(new java.awt.Rectangle(
          (int) canvas3d.getLocationOnScreen().getX(), (int) canvas3d
                  .getLocationOnScreen().getY(), canvas3d.getBounds().width,
                  canvas3d.getBounds().height));
    } catch (AWTException e) {
      Log.err("Error on image creation", e);
    }
    return null;
  }
  
  /**
   * Get a canvas of the cube3d model
   * 
   * @param viewPoint - eye position
   * @param angle - rotation angle
   * @param movable - set true for movable 3d canvas
   * @return
   *        Canvas3D - room model
   */
  public Canvas3D getCanvas3d(Vector3f viewPoint, float angle, boolean movable) {
    // Assign the current view Platform OrbitBehavior the object
    ViewingPlatform viewingPlatform = new ViewingPlatform();
    viewingPlatform.getViewPlatform().setActivationRadius(300f);
    
    // Set default positioning of the viewer
    TransformGroup viewTransform = viewingPlatform.getViewPlatformTransform();
    viewPointMatrix = getViewingTransform(viewPoint,angle);
    viewTransform.setTransform(viewPointMatrix);
    
    Viewer viewer = new Viewer(canvas3d);
    View view = viewer.getView();
    //view.setSceneAntialiasingEnable(true);
    view.setBackClipDistance(300);
    view.setPhysicalBody(new PhysicalBody());
    view.setPhysicalEnvironment(new PhysicalEnvironment());
    view.setViewPolicy(View.NOMINAL_HEAD);
    view.setProjectionPolicy(View.PERSPECTIVE_PROJECTION);
    view.setSceneAntialiasingEnable(true);
    
    simpleUni = new SimpleUniverse(viewingPlatform, viewer);

    lightDirection = viewPoint;
    lightDirection.setZ(-lightDirection.getZ());
    
    // adding content
    BranchGroup root = getRootBranchGroup();
    simpleUni.addBranchGraph(root);
    
    if(movable){
      viewingPlatform.setViewPlatformBehavior(getMovableOrbitBehavior());
      sliderSwitch.setWhichChild(0);
      slicesSwitch.setWhichChild(0);
    } else {
      viewingPlatform.setViewPlatformBehavior(getNonMovableOrbitBehavior());
      sliderSwitch.setWhichChild(1);
      slicesSwitch.setWhichChild(1);
    }
    //BufferedImage capture = getImage(canvas3d);
    return canvas3d;
  }
  
  /**
   * Get the positions of the room slices.
   * <br><br>
   * Point3d.getX() - position of the X slice <br>
   * Point3d.getY() - position of the Y slice <br>
   * Point3d.getZ() - position of the Z slice 
   * 
   * @return
   *      Point3d - position of the three slices
   */
  public Point3d getSlicePosition(){
    Point3d point = new Point3d();
    point.setX(220*slicePos.getX());
    point.setY(220*slicePos.getY());
    point.setZ(240/1.2*slicePos.getZ()+120);
    return point;
  }
  
  /**
   * 
   * @return
   */
  public Transform3D getViewPointMatrix(){
    return viewPointMatrix;
  }
  
  /**
   * Create OrbitBehavior to interact with the universe
   * 
   * @return OrbitBehavior
   */
  private OrbitBehavior getMovableOrbitBehavior() {
    OrbitBehavior orbit = new OrbitBehavior(canvas3d, OrbitBehavior.REVERSE_ALL
        | OrbitBehavior.MOUSE_MOTION_LISTENER | OrbitBehavior.STOP_ZOOM | OrbitBehavior.DISABLE_TRANSLATE | OrbitBehavior.DISABLE_ZOOM);
    orbit.setSchedulingBounds(bigBounds);
    orbit.setMinRadius(1.2);
    return orbit;
  }
  
  /**
   * Create OrbitBehavior to disable the movement of the universe
   * 
   * @return OrbitBehavior
   */
  private OrbitBehavior getNonMovableOrbitBehavior(){
    OrbitBehavior orbit = new OrbitBehavior(canvas3d, OrbitBehavior.DISABLE_ROTATE | OrbitBehavior.DISABLE_TRANSLATE | OrbitBehavior.DISABLE_ZOOM);
    orbit.setSchedulingBounds(bigBounds);
    orbit.setMinRadius(1.2);
    return orbit;
  }
  
  /**
   * Merge all transformations to get the starting position of the cube
   * 
   * @param viewPoint - eye position
   * @param angle - rotation angle
   * 
   * @return Transform3D
   */
  private Transform3D getViewingTransform(Vector3f viewPoint, float angle){
    Transform3D transform = new Transform3D();
    Transform3D zRotTrans = new Transform3D();
    Transform3D zoomTrans = new Transform3D();
    zoomTrans.setTranslation(viewPoint);
    zRotTrans.rotX(-Math.toRadians(30));
    transform.rotY(Math.toRadians(angle));
    transform.mul(zRotTrans);
    transform.mul(zoomTrans);
    return transform;
  }
  
  /**
   * Creates all objects in the scene, including the global illumination. In
   * addition, the mouse control is initialized.
   * 
   * @return Group of scene objects.
   */
  private BranchGroup getRootBranchGroup() {
    BranchGroup rootBg = new BranchGroup();
    
    // background and lightning
    rootBg.addChild(getBackgroundBranchGroup());
    rootBg.addChild(getAmbientLightBranchGroup());
    rootBg.addChild(getLightningBranchGroup());

    // room model
    rootBg.addChild(getBlank3dCubeBranchGroup(CUBE_DIM));
    rootBg.addChild(getFloorGridBranchGroup());
    rootBg.addChild(getTvArrayBranchGroup(canvas3d, tvArraySwitch));
    rootBg.addChild(getCeilingArrayBranchGroup(canvas3d, ceilingArraySwitch, ceilingArrayTg));

    // target point
    getFuzzyTransformGroup(fuzzyPointTg, new Vector3f(0,0,0), fuzzy);
    rootBg.addChild(getTargetBranchGroup(targetSwitch,targetPointTg,fuzzyPointTg));
    
    // build slices
    rootBg.addChild(getSlicesBranchGroup(slicesSwitch,slicesTg));
    rootBg.addChild(getSliceSliderBranchGroup());
    
    rootBg.compile();
    return rootBg;
  }
  
  /**
   * Method to set the background
   * 
   * @return BranchGroup
   */
  private BranchGroup getBackgroundBranchGroup() {
    BranchGroup backgroundBg = new BranchGroup();

    Background bg = new Background(new Color3f(0.0f, 0.0f, 0.0f));
    bg.setApplicationBounds(bigBounds);
    backgroundBg.addChild(bg);
    backgroundBg.compile();
    return backgroundBg;
  }

  /**
   * Create an ambient lightning
   * 
   * @return BranchGroupp
   */
  private BranchGroup getAmbientLightBranchGroup() {
    BranchGroup ambientBg = new BranchGroup();

    AmbientLight ambientLightNode = new AmbientLight(new Color3f(0.4f, 0.4f,
        0.4f));
    ambientLightNode.setInfluencingBounds(bigBounds);

    ambientBg.addChild(ambientLightNode);
    ambientBg.compile();
    return ambientBg;
  }

  /**
   * Method for setting the room illumination
   * 
   * @return BranchGroup
   */
  private BranchGroup getLightningBranchGroup() {
    BranchGroup lightBg = new BranchGroup();
    Color3f lightColor = new Color3f(1.0f, 1.0f, 0.8f);
    
    // Directional produce light and set scope
    DirectionalLight dLight = new DirectionalLight(lightColor, lightDirection);
    dLight.setInfluencingBounds(bigBounds);

    lightBg.addChild(dLight);
    lightBg.compile();
    return lightBg;
  }

  
  /**
   * Building slider object.
   * 
   * @return Node of the object.
   */
  private Node getSphereNode(float radius, Color3f color, String name) {
    Appearance sphereApp = new Appearance();

    // setting color attributes
    ColoringAttributes ca = new ColoringAttributes();
    ca.setColor(color);
    ca.setShadeModel(ColoringAttributes.NICEST);
    sphereApp.setColoringAttributes(ca);

    Sphere sp = new Sphere(radius,sphereApp);
    sp.setUserData(name);
    sp.setCapability(Sphere.ALLOW_PICKABLE_READ);
    sp.setCapability(Sphere.ALLOW_PICKABLE_WRITE);
    
    return sp;
  }
  
  /**
   * 
   * @param name
   * @param color
   * @param trans
   * @return
   */
  private TransformGroup getSphereTransformGroup(String name, Color3f color, Vector3f trans) {
    Transform3D transform = new Transform3D();
    float radius = .09f;
    
    if (name.contains(SLIDER_X)){
      radius+=.005f;
    }

    transform.setTranslation(trans);
    TransformGroup transformGroup = new TransformGroup(transform);
    transformGroup.setName(name);
    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    transformGroup.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
    transformGroup.addChild(getSphereNode(radius, color, name));
       
    return transformGroup;
  }
  
  /**
   * Get a single slider to move the slices.
   * 
   * @param name
   *          String
   * @param trans
   * @return
   */
  private TransformGroup getSliceSliderSphere(String name, Vector3f trans) {
    return getSphereTransformGroup(name, new Color3f(.8f,.4f,.4f), trans);
  }
  
  /**
   * 
   * @return
   */
  private BranchGroup getSliceSliderBranchGroup() {
    sliderSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    sliderSwitch.addChild(getInvisibleNode());
    
    sliderBranchGroup = new BranchGroup();
    BranchGroup sliceSliderSwitchBranchGroup = new BranchGroup();
    sliceSliderTg.setName("slider");
    
    float sliderThick = 3f;
    float lineThick = 1.8f;
    Color3f sliderColor = new Color3f(.6f, .6f, 1f);
    Color3f lineColor = new Color3f(.37647f, .37647f, .62745f);
    TransformGroup linesTransformGroup = new TransformGroup();
    
    // x slider
    linesTransformGroup.addChild(getSliderLineShape(Cube3dObjects.SLIDER_X_POS[0],Cube3dObjects.SLIDER_X_POS[1], sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(-1.05f, 0.625f, -1.3f), new Point3d(-1.05f, 0.675f, -1.3f), sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(.95f, 0.625f, -1.3f), new Point3d(.95f, 0.675f, -1.3f), sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(0, 0.65f, -1.3f), new Point3d(0, 0.65f, -2f), lineThick,lineColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(0, 0.65f, -2f), new Point3d(0, 1f, -1.905f), lineThick,lineColor));
    
    // y slider
    linesTransformGroup.addChild(getSliderLineShape(Cube3dObjects.SLIDER_Y_POS[0],Cube3dObjects.SLIDER_Y_POS[1], sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(1.125f, -0.6f, 1f), new Point3d(1.175f, -0.6f, 1f), sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(1.125f, -0.6f, -1f), new Point3d(1.175f, -0.6f, -1f), sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(1.15f, -0.6f, 0f), new Point3d(1.5f, -0.6f, 0f), lineThick,lineColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(1.5f, -0.6f, 0f), new Point3d(1.535f, -0.5f, 1f), lineThick,lineColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(1.535f, -0.5f, 1f), new Point3d(-1.2f, -0.6f, 3.01f), lineThick,lineColor));
    
    // z slider
    linesTransformGroup.addChild(getSliderLineShape(Cube3dObjects.SLIDER_Z_POS[0],Cube3dObjects.SLIDER_Z_POS[1], sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(-1f, -0.6f, 1.125f), new Point3d(-1f, -0.6f, 1.175f), sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(-1f, 0.6f, 1.125f), new Point3d(-1f, 0.6f, 1.175f), sliderThick,sliderColor));
    linesTransformGroup.addChild(getSliderLineShape(new Point3d(-1f, 0f, 1.15f), new Point3d(-1.5f, 0f, 1.55f), lineThick,lineColor));
    
    // build slider points
    sliceSliderTg.addChild(linesTransformGroup);
    
    sliderBranchGroup.addChild(getSliceSliderSphere(SLIDER_X, new Vector3f( 0f, 0.65f, -1.3f)));
    sliderBranchGroup.addChild(getSliceSliderSphere(SLIDER_Y, new Vector3f( 1.15f, -0.6f, 0f)));
    sliderBranchGroup.addChild(getSliceSliderSphere(SLIDER_Z, new Vector3f(-1f, 0.2f, 1.15f)));
    
    PickTranslateBehaviorNew sp = new PickTranslateBehaviorNew(sliderBranchGroup, canvas3d, bigBounds);
    sp.setTolerance(8f);
    sliderBranchGroup.addChild(sp);
    
    sliderBranchGroup.setName("slider_branchgroup");
    sliceSliderTg.addChild(sliderBranchGroup);
    
    sliderSwitch.addChild(sliceSliderTg);
    sliceSliderSwitchBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
    sliceSliderSwitchBranchGroup.addChild(sliderSwitch);
    
    sliceSliderSwitchBranchGroup.compile();
    return sliceSliderSwitchBranchGroup;
  }
  
  /**
   * Update all elements related to the target point.
   * 
   * @param msg
   *         NewTargetPointMsg
   */
  private void updateTargetPoint(NewBeamformerMsg msg) {
    if(targetPointTg==null || targetPointTg.numChildren()<=0) return;
    
    if(msg.isTargetEnable()==false){
      targetSwitch.setWhichChild(0);
    } else {
      targetSwitch.setWhichChild(1);
      
      MicArrayState state = msg.getMicArrayState();
      Point3d targetPt = state.target;
      if(targetPt==null || targetPoint.equals(targetPt))
        return;
      
      targetPoint.setX(targetPt.getX()/220);
      targetPoint.setY(-targetPt.getY()/220);
      targetPoint.setZ((targetPt.getZ()-120)/(240/1.2));
      
      @SuppressWarnings("unchecked")
      Enumeration<TransformGroup> children = targetPointTg.getAllChildren();
      while(children.hasMoreElements()){
        TransformGroup tg = children.nextElement();
        Transform3D tempTransform = new Transform3D();
        Vector3f tempVector = new Vector3f();
        tg.getTransform(tempTransform);
        tempTransform.get(tempVector);
        
        if(tg.getName().contains(FLOOR_PT_NAME)){
          tempVector.x = (float) targetPoint.getX();
          tempVector.z = (float) targetPoint.getY();
        } else if(tg.getName().contains("FuzzySphere")){
          tempVector.x = (float) targetPoint.getX();
          tempVector.y = (float) targetPoint.getZ();
          tempVector.z = (float) targetPoint.getY();
        } else if(tg.getName().contains(SLIDER_X)){
          tempVector.x = (float) targetPoint.getX();
        } else if(tg.getName().contains(SLIDER_Y)){
          tempVector.z = (float) targetPoint.getY();
        } else if(tg.getName().contains(SLIDER_Z)){
          tempVector.y = (float) targetPoint.getZ();
        }
        
        tempTransform.setTranslation(tempVector);
        tg.setTransform(tempTransform);
      }
    }
  }
  
  /**
   * Updating the slice positions in the room model.
   * 
   * @param msg
   *         NewSlicePositionMsg
   */
  private void updateSlices(NewSlicePositionMsg msg) {
    if(slicesTg==null || slicesTg.numChildren()<=0 ) return;
    
    if(msg.isEnable()==false){
      simpleUni.getViewingPlatform().setViewPlatformBehavior(getMovableOrbitBehavior());
      slicesSwitch.setWhichChild(0);
      sliderSwitch.setWhichChild(0);
    } else {
      if(slicesSwitch.getWhichChild()==0){
        simpleUni.getViewingPlatform().setViewPlatformBehavior(getNonMovableOrbitBehavior());
        slicesSwitch.setWhichChild(1);
        sliderSwitch.setWhichChild(1);
      }
      
      if(msg.getXSlicePos()!=null){
        double xIn = msg.getXSlicePos()/220;
        slicePos.setX((xIn < -1) ? -1 : ((xIn > 1) ? 1 : xIn));
      }
      
      if(msg.getYSlicePos()!=null){
        double yIn = -msg.getYSlicePos()/220;
        slicePos.setY((yIn < -1) ? -1 : ((yIn > 1) ? 1 : yIn));
      }
      
      if(msg.getZSlicePos()!=null){
        double zIn = (msg.getZSlicePos()-120)/(240/1.2);
        slicePos.setZ((zIn < -0.6) ? -0.6 : ((zIn > 0.6) ? 0.6 : zIn));
      }
      
      if(msg.isReset()) slicePos.set(DEFAULT_SLICE_POS);
            
      @SuppressWarnings("unchecked")
      Enumeration<TransformGroup> sliceChildren = slicesTg.getAllChildren();
      while (sliceChildren.hasMoreElements()) {
        TransformGroup sliceTg = sliceChildren.nextElement();

        Transform3D tempTransform = new Transform3D();
        Vector3d actualPos = new Vector3d();
        sliceTg.getTransform(tempTransform);
        tempTransform.get(actualPos);

        if (sliceTg.getName() == null)
          continue;

        if (sliceTg.getName().contains(SLIDER_X)) {
          actualPos.setX(slicePos.getX());
        } else if (sliceTg.getName().contains(SLIDER_Y)) {
          actualPos.setZ(slicePos.getY());
        } else if (sliceTg.getName().contains(SLIDER_Z)) {
          actualPos.setY(slicePos.getZ());
        }
        
        tempTransform.setTranslation(actualPos);
        sliceTg.setTransform(tempTransform);
      }
      
      Enumeration<?> sliderChildren = sliceSliderTg.getAllChildren();
      while(sliderChildren.hasMoreElements()){
        Object nextChild = sliderChildren.nextElement();
        
        if (nextChild.toString().contains("slider_branchgroup")) {
          BranchGroup nextBg = (BranchGroup) nextChild;
          @SuppressWarnings("unchecked")
          Enumeration<BranchGroup> sliderBgChildren = nextBg.getAllChildren();
          while(sliderBgChildren.hasMoreElements()){
            Object o = sliderBgChildren.nextElement();
            if (o instanceof TransformGroup) {
              TransformGroup sliderTg = (TransformGroup) o;
              Transform3D slideTempTransform = new Transform3D();
  
              Vector3f tempVector2 = new Vector3f();
              sliderTg.getTransform(slideTempTransform);
              slideTempTransform.get(tempVector2);
  
              if (sliderTg.getName() == null)
                continue;
  
              if (sliderTg.getName().contains(SLIDER_X)) {
                tempVector2.setX((float) slicePos.getX());
              } else if (sliderTg.getName().contains(SLIDER_Y)) {
                tempVector2.setZ((float) slicePos.getY());
              } else if (sliderTg.getName().contains(SLIDER_Z)) {
                tempVector2.setY((float) slicePos.getZ());
              }
              
              slideTempTransform.setTranslation(tempVector2);
              sliderTg.setTransform(slideTempTransform);
            }
          }
        }
      }
      
    }
  }
  
  /**
   * Hide/show the TV array in model.
   * Changing the position of the viewer.
   * 
   * @param msg
   *         NewCub3dViewMsg
   */
  private void updateView(NewCube3dViewMsg msg){
    
    if(msg.isTvEnable()!=null){
      if(msg.isTvEnable()==false){
        tvArraySwitch.setWhichChild(0);
      } else {
        tvArraySwitch.setWhichChild(1);
      }
    }
    
    if(msg.getViewPosition()!=null){
      TransformGroup viewTransform = simpleUni.getViewingPlatform().getViewPlatformTransform();
      Transform3D transform = new Transform3D();
      transform.setTranslation(msg.getViewPosition());
      viewTransform.setTransform(transform);
    }
    
    if(msg.isMovable()!=null){
      if(msg.isMovable()==true){
        simpleUni.getViewingPlatform().setViewPlatformBehavior(getMovableOrbitBehavior());
        slicesSwitch.setWhichChild(0);
        sliderSwitch.setWhichChild(0);
      } else {
        ViewingPlatform vp = simpleUni.getViewingPlatform();
        vp.setViewPlatformBehavior(getNonMovableOrbitBehavior());
        TransformGroup viewTransform = vp.getViewPlatformTransform();
        viewTransform.setTransform(viewPointMatrix);
        slicesSwitch.setWhichChild(1);
        sliderSwitch.setWhichChild(1);
      }
    }
  }
  
  private void updateCeiling(NewCeilingMsg msg){
    if(ceilingArrayTg==null || ceilingArrayTg.numChildren()<=0) return;
    
    if(msg.isTrolleyEnable()==false){
      ceilingArraySwitch.setWhichChild(0);
    } else {
      ceilingArraySwitch.setWhichChild(1);
      Transform3D tempTransform = new Transform3D();
      ceilingArrayTg.getTransform(tempTransform);
      Vector3d tempVector = new Vector3d();
      tempTransform.get(tempVector);
      double tp = msg.getMicArrayState().trolleyPos;
      double min = -140;
      double max = 106;
      tp = (tp - min) / (max - min) * 1.5 + (-1);
      tempVector.z = -tp;
      tempTransform.setTranslation(tempVector);
      ceilingArrayTg.setTransform(tempTransform);
    }
  }
  
  /**
   * Returns true, if the canvas is visible
   * @return
   */
  public boolean isVisible(){
    return canvas3d.isVisible();
  }
  
  /**
   * Set this to false to hide canvas
   * @param visible, boolean
   */
  public void setVisible(boolean visible){
    canvas3d.setVisible(visible);
  }

  @Override
  public void update(IObservable o, Object arg) {
    if (arg == null)
      return;

    if (arg instanceof NewCeilingMsg)
      updateCeiling((NewCeilingMsg) arg);
    
    if (arg instanceof NewBeamformerMsg)
      updateTargetPoint((NewBeamformerMsg) arg);

    if (arg instanceof NewSlicePositionMsg)
      updateSlices((NewSlicePositionMsg) arg);

    if (arg instanceof NewCube3dViewMsg)
      updateView((NewCube3dViewMsg) arg);
  }

}
