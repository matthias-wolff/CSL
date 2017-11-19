package de.tucottbus.kt.csl.retired.lcars;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Vector;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.micarray3d.AMicArray3DPart;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.csl.lcars.elements.EMicrophoneArraySelector;
import de.tucottbus.kt.csl.retired.lcars.components.threeDim.Cube3d;
import de.tucottbus.kt.csl.retired.lcars.components.threeDim.Viewer3d;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot;
import de.tucottbus.kt.csl.retired.lcars.elements.EBufferedImage;
import de.tucottbus.kt.csl.retired.lcars.elements.ECanvas3D;
import de.tucottbus.kt.csl.retired.lcars.elements.EPositionerFreq;
import de.tucottbus.kt.csl.retired.lcars.elements.ESensitivityPlot;
import de.tucottbus.kt.csl.retired.lcars.geometry.GSensitivityPlot;
import de.tucottbus.kt.csl.retired.lcars.messages.IObservable;
import de.tucottbus.kt.csl.retired.lcars.messages.IObserver;
import de.tucottbus.kt.csl.retired.lcars.messages.NewBeamformerMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewCeilingMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewCube3dViewMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewFrequencyMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewSlicePositionMsg;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.MainPanel;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.Screen;
import de.tucottbus.kt.lcars.contributors.EPositioner;
import de.tucottbus.kt.lcars.contributors.IPositionListener;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcars.net.NetUtils;
import de.tucottbus.kt.lcars.swt.ColorMeta;
import de.tucottbus.kt.lcars.swt.ImageMeta;

/**
 * GUI panel for beamforming processing.
 * @author Martin Birth
 *
 */
@Deprecated
public class BeamformerPanel extends Panel implements IObservable {
  
  /**
   * Panel name
   */
  private final String PANEL_TITLE = "BEAMFORMER PANEL";
  
  /**
   * Color scheme of this panel
   */
  private final int   COLOR_SCHEME = LCARS.CS_MULTIDISP;
  
  /**
   * spacing between buttons = 3 pt
   */
  private final int SPACEING = 3;
  
  /**
   * Error color
   */
  private final ColorMeta cError  = new ColorMeta(0x00FF0066,false);
  
  // observable objects
  private boolean changed = false;
  private Vector<IObserver> obs;
  
  // configuration
  private float freq;
  
  // panel elements
  private SensitivityPlot sensitivity;
  private Cube3d cube;
  private static Viewer3d viewer3d;
  
  private ERect eRectSliceMode;
  private ERect eRectTargetData;
  private ERect eRectTarget;
  private ERect eRectTargetReset;
  private ERect eRectSliceData;
  private ERect eRectSpacer;
  private ERect eRectSlices;
  private ERect eRectCeilArray;
  private ERect eRectTvArray;
  private ERect eRectSlicesReset;
  private ERect eRectAutoSlicer;
  private ERect eCalibrate;
  private ERect eRectRot;
  private ERect eRectReset;
  
  private ERect eRectMicArray1;
  private ERect eRectMicArray2;
  
  private ERect eRectMA1NQuater;
  private ERect eRectMA1NHalf;
  private ERect eRectMA1N23;
  private ERect eRectMA1NFull;
  private ERect eRectMA2NQuater;
  private ERect eRectMA2NHalf;
  private ERect eRectMA2N23;
  private ERect eRectMA2NFull;
  
  private ELabel eLabelHor;
  private ELabel eLabelVertLat;
  private ELabel eLabelVerFr;
  private ELabel eLabelTh;
  
  private EElbo[] micArr1Elbo;
  private EElbo[] micArr2Elbo;
  private EElbo[] horiEElbo;
  private EElbo[] vertLatEElbo;
  private EElbo[] vertFroEElbo;
  private EElbo[] eElboScale;
  private ERect[] lineGroup;
  private EElbo[] thFrame;
  private ELabel[] eLabelsScale;
  
  private EBufferedImage imgScale;
  private ECanvas3D canvas;
  private ECanvas3D canvasViewer;
  
  private ESensitivityPlot sensitivityPlotHo;
  private ESensitivityPlot sensitivityPlotVertSi;
  private ESensitivityPlot sensitivityPlotVertFr;
  
  private EMicrophoneArraySelector eA1tTopo;
  private EMicrophoneArraySelector eA2tTopo;
  private EPositioner eA2tPoser;
  private EPositioner eA2tPoser2;
  private EPositionerFreq freqPoser;
  
  private BeamformerPanel beamformerPanel;
  
  /**
   * Constructor method
   * @param screen - IScreen
   * @see MainPanel
   * @see IScreen
   */
  public BeamformerPanel(IScreen screen) {
    super(screen);
  }
  
  @Override
  public void init() {
    super.init();
    setColorScheme(COLOR_SCHEME);
    
    obs = new Vector<IObserver>();
    try
    {
      cube = new Cube3d(this);
      viewer3d = new Viewer3d();
      addObserver(cube);
    }
    catch (UnsatisfiedLinkError e)
    {
      Log.err("j3dcore*.dll's not found. Add <CSL-project-dir>/natives/<machinetype> to java.library.path!");
      Log.err("In Eclipse:");
      Log.err("- Right-click <CSL-project-dir>/src and select \"Build Path|Configure Build Path...\"");
      Log.err("- Select \"Native library location\" and press [Edit...]");
      Log.err("- Press [Workspace...] and select folder <CSL-project-dir>/natives/<machinetype>");
      Log.err("Error details:", e);
    }
    
    freq = 1000;
    sensitivity = SensitivityPlot.getInstance();
    beamformerPanel = this;
    
    int menuWidth = 144;
    
    // ########################################################################
    // ############################### headline ###############################
    // ########################################################################
    
    int dy = 20;
    int dx = 70;
    int headHeight = 80;
    
    add(new ERect(this,0,dy,dx,headHeight,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_NE|LCARS.EC_ELBOUP,null));
    
    dx+=160;
    ELabel eLabelTitle = new ELabel(this,dx+SPACEING,dy-17,300,headHeight,LCARS.EC_HEADLINE|LCARS.EF_HEAD1|LCARS.ES_LABEL_NE|LCARS.ES_STATIC,PANEL_TITLE);
    setTitleLabel(eLabelTitle);
    
    dx+=312;
    int w = 764;
    int h = headHeight+120;
    EElbo elbLeftHead = new EElbo(this,dx,dy,w,h,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_NE|LCARS.ES_LABEL_SW|LCARS.ES_STATIC,"SENSITIVITY\nVISUALISATION");
    elbLeftHead.setArmWidths(menuWidth,headHeight); elbLeftHead.setArcWidths(70,90);
    add(elbLeftHead);
    
    dx+=SPACEING+w;
    w=485;
    EElbo elbLeftHead2 = new EElbo(this,dx,dy,w,h,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_NW|LCARS.ES_LABEL_SE|LCARS.ES_STATIC,"ARRAY\nCONTROL");
    elbLeftHead2.setArmWidths(menuWidth,headHeight); elbLeftHead2.setArcWidths(70,90);
    add(elbLeftHead2);
    
    EValue eValueHead = new EValue(this,dx+w,dy,105,headHeight,LCARS.EC_ELBOUP|LCARS.ES_RECT_RND_E|LCARS.ES_VALUE_W,null);
    eValueHead.setValueMargin(45);
    eValueHead.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        try {
          getScreen().exit();
        } catch (RemoteException e) {
          Log.err(e.getMessage(), e);
        }
      }
    });
    add(eValueHead);
    
    // ########################################################################
    // ############################### left menu ##############################
    // ########################################################################
    
    dx = 1162;
    int dyl = 223;
    eRectSliceMode = new ERect(this,dx,dyl,menuWidth,50,LCARS.CS_PRIMARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"MODE: MANUAL");
    eRectSliceMode.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        if(ee.el.isSelected()){
          ee.el.setLabel("MODE: AUTO");
          DoAEstimator.getInstance().setAutoMode(true);
          setElboStyle(horiEElbo, LCARS.CS_PRIMARY|LCARS.ES_SELECTED);
          setElboStyle(vertLatEElbo, LCARS.CS_PRIMARY|LCARS.ES_SELECTED);
          setElboStyle(vertFroEElbo, LCARS.CS_PRIMARY|LCARS.ES_SELECTED);
        } else {
          ee.el.setLabel("MODE: MANUAL");
          DoAEstimator.getInstance().setAutoMode(false);
          setElboStyle(horiEElbo, LCARS.EC_ELBOUP);
          setElboStyle(vertLatEElbo, LCARS.EC_ELBOUP);
          setElboStyle(vertFroEElbo, LCARS.EC_ELBOUP);
        }
      }
    });
    DoAEstimator.getInstance().setAutoMode(false);
    eRectSliceMode.setSelected(DoAEstimator.getInstance().isAutoMode());
    add(eRectSliceMode);

    dyl+=50+SPACEING;
    eRectTarget = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"TARGET");
    eRectTarget.setSelected(!eRectTarget.isSelected());
    eRectTarget.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        setChanged();
        notifyObservers(new NewBeamformerMsg(MicArrayState.getCurrent(), ee.el.isSelected()));
      }
    });
    add(eRectTarget);
    
    dyl += 50+SPACEING;
    eRectTargetReset = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"TARGET RESET");
    eRectTargetReset.setSelected(!eRectTarget.isSelected());
    eRectTargetReset.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        if(DoAEstimator.getInstance().isAutoMode()==false){
          DoAEstimator.getInstance().setTargetSource(DoAEstimator.DEFAULT_TARGET);
          MicArrayState state = MicArrayState.getCurrent();
          setChanged();
          notifyObservers(new NewBeamformerMsg(state, eRectTarget.isSelected()));
          Point3d t = state.target;
          setChanged();
          notifyObservers(new NewSlicePositionMsg(t.getX(),t.getY(),t.getZ(), ee.el.isSelected(), false));
        }
      }
    });
    add(eRectTargetReset);
    
    dyl += 50+SPACEING;
    eRectCeilArray = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"CEIL ARRAY");
    eRectCeilArray.addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        setChanged();
        notifyObservers(new NewCeilingMsg(MicArrayState.getCurrent(), ee.el.isSelected()));
      }
    });
    add(eRectCeilArray);
    
    dyl += 50+SPACEING;
    eRectTvArray = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"TV ARRAY");
    eRectTvArray.addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        setChanged();
        notifyObservers(new NewCube3dViewMsg(null, ee.el.isSelected(), null));
      }
    });
    add(eRectTvArray);
    
    dyl += 50+SPACEING;
    eRectSlices = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"SLICES");
    eRectSlices.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        if(ee.el.isSelected()){
          setElboStyle(horiEElbo, LCARS.EC_ELBOUP);
          setElboStyle(vertLatEElbo, LCARS.EC_ELBOUP);
          setElboStyle(vertFroEElbo, LCARS.EC_ELBOUP);
          for(ERect r:lineGroup)
            r.setAlpha(1);
        } else {
          setElboStyle(horiEElbo, LCARS.EC_ELBOUP | LCARS.ES_DISABLED);
          setElboStyle(vertLatEElbo, LCARS.EC_ELBOUP | LCARS.ES_DISABLED);
          setElboStyle(vertFroEElbo, LCARS.EC_ELBOUP | LCARS.ES_DISABLED);
          for(ERect r:lineGroup)
            r.setAlpha(0);
        }
        setChanged();
        notifyObservers(new NewSlicePositionMsg(cube.getSlicePosition().getX(),cube.getSlicePosition().getY(),cube.getSlicePosition().getZ(), ee.el.isSelected(),false));

        setChanged();
        notifyObservers(new NewCube3dViewMsg(null, null, !ee.el.isSelected()));
      }
    });
    add(eRectSlices);
    
    dyl += 50+SPACEING;
    eRectSlicesReset = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"SLICES RESET");
    eRectSlicesReset.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        MicArrayState state = MicArrayState.getCurrent();
        setChanged();
        notifyObservers(new NewSlicePositionMsg(state.target.x,state.target.y,state.target.z, true, true));
      }
    });
    add(eRectSlicesReset);
    
    dyl += 50+SPACEING;
    eRectAutoSlicer = new ERect(this,dx,dyl,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_W|LCARS.ES_SHAPE_NW,"AUTO SLICE");
    eRectAutoSlicer.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        if(ee.el.isSelected()){
          ee.el.setLabel("AUTO SLICE");
        } else {
          ee.el.setLabel("MANUAL SLICE");
        }
        MicArrayState state = MicArrayState.getCurrent();
        setChanged();
        notifyObservers(new NewSlicePositionMsg(state.target.x, state.target.y, state.target.z, true, false));
      }
    });
    add(eRectAutoSlicer);

    dyl+=50+SPACEING;
    ERect eRectOpenCl = new ERect(this,dx,dyl,menuWidth,40,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NW|LCARS.ES_STATIC,null);
    add(eRectOpenCl);
    if(!sensitivity.isOpenCLActive()){
      eRectOpenCl.setLabel("OPENCL: OFF");
    } else {
      eRectOpenCl.setLabel("OPENCL: ON");
    }
    dyl+=40;
    
    ERect eRectTargetDataString = new ERect(this,dx,dyl,menuWidth,30,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NW|LCARS.ES_STATIC,"TARGET (X/Y/Z):");
    add(eRectTargetDataString);
    dyl+=30;
    
    eRectTargetData = new ERect(this,dx,dyl,menuWidth,40,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NW|LCARS.ES_STATIC,null);
    add(eRectTargetData);
    dyl+=40;
    
    ERect eRectSliceDataString = new ERect(this,dx,dyl,menuWidth,30,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NW|LCARS.ES_STATIC,"SLICES (X/Y/Z):");
    add(eRectSliceDataString);
    dyl+=30;
    
    eRectSliceData = new ERect(this,dx,dyl,menuWidth,40,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NW|LCARS.ES_STATIC,null);
    add(eRectSliceData);
    
    dyl+=40;
    eRectSpacer = new ERect(this,dx,dyl,menuWidth,80,LCARS.ES_SHAPE_NW,null);
    eRectSpacer.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee)
      {
        ee.el.setSelected(!ee.el.isSelected());
        if(ee.el.isSelected()){
          viewer3d.startPlugin();
          setDiableToAll(!ee.el.isSelected());
          cube.setVisible(false);
          canvas.removeFromPanel();
          eA2tPoser2.addToPanel(beamformerPanel);
          eRectReset.setVisible(true);
          eRectRot.setVisible(true);
          for (EElbo elb : thFrame) {
            elb.setVisible(true);
          }
          canvasViewer = new ECanvas3D(0, 100, 950, 950, viewer3d.getCanvas());
          canvasViewer.addToPanel(beamformerPanel);
          viewer3d.setFrequency(freq);
        } else {
          if(!canvas.isDisplayed()){
            cube.setVisible(true);
            canvas.addToPanel(beamformerPanel);
          }
          if(canvasViewer!=null && canvasViewer.isDisplayed()){
            viewer3d.stopAnimation();
            canvasViewer.removeFromPanel();
            eA2tPoser2.removeFromPanel();
          }
          setDiableToAll(!ee.el.isSelected());
          eRectReset.setVisible(false);
          eRectRot.setVisible(false);
          for (EElbo elb : thFrame) {
            elb.setVisible(false);
          }
        }
      }
    });
    add(eRectSpacer);
    
    dyl+=80;
    w=420;
    h=157;
    int elbH = 38;
    EElbo elbLeft2 = new EElbo(this,dx,dyl,w,h,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NW|LCARS.ES_STATIC,null);
    elbLeft2.setArmWidths(menuWidth,elbH); elbLeft2.setArcWidths(170,90);
    add(elbLeft2);

    EValue eValue = new EValue(this,dx+w,dyl+h-elbH,300,elbH,LCARS.EC_ELBOUP|LCARS.ES_STATIC|LCARS.ES_RECT_RND_E|LCARS.ES_VALUE_W,null);
    eValue.setValue(NetUtils.getHostName().toUpperCase());
    eValue.setValueMargin(160);
    add(eValue); 
    
    // ########################################################################
    // ############################## right menu ##############################
    // ########################################################################
    
    dx+=menuWidth+SPACEING;
    int dyr = 223;
        
    eRectMicArray1 = new ERect(this,dx,dyr,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE,"TV MIC ARRAY");
    eRectMicArray1.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        disablePresetModeArray1(!ee.el.isSelected());
        if(!ee.el.isSelected()) setElboStyle(micArr1Elbo,LCARS.EC_PRIMARY | LCARS.ES_DISABLED);
        else setElboStyle(micArr1Elbo,LCARS.EC_ELBOUP);
        try {
          MicArrayViewer.getInstance().setActive(ee.el.isSelected());
        } catch (HardwareException e) {
          Log.err(e.getMessage(),e);
        }
      }
    });
    add(eRectMicArray1);
    
    dyr+=50+SPACEING;
    eRectMicArray2 = new ERect(this,dx,dyr,menuWidth,50,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE,"CEIL MIC ARRAY");
    eRectMicArray2.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        disablePresetModeArray2(!ee.el.isSelected());
        if(!ee.el.isSelected()) setElboStyle(micArr2Elbo,LCARS.EC_PRIMARY | LCARS.ES_DISABLED);
        else setElboStyle(micArr2Elbo,LCARS.EC_ELBOUP);
        try {
          MicArrayCeiling.getInstance().setActive(ee.el.isSelected());
        } catch (HardwareException e) {
          Log.err(e.getMessage(),e);
        }
      }
    });
    add(eRectMicArray2);
    
    dyr+=50+SPACEING;
    eCalibrate = new ERect(this,dx,dyr,menuWidth,50,LCARS.EC_PRIMARY|LCARS.ES_SHAPE_NW| LCARS.ES_LABEL_NE,"CALIBRATE");
    eCalibrate.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        try {
          MicArray3D.getInstance().calibrate();
        }
        catch (Exception e){
          Log.err(e.getMessage(), e);
        };
      }
    });
    add(eCalibrate);
    
    dyr+=50+SPACEING;
    ERect eRectLcars = new ERect(this,dx,dyr,menuWidth,156,LCARS.EC_ELBOLO|LCARS.ES_LABEL_NW|LCARS.ES_SELECTED,"LCARS");
    eRectLcars.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        try
        {
          getScreen().setPanel(null);
        } 
        catch (Exception e)
        {
          Log.err("Failed to remove panel from screen.", e);
        }
      }
    });
    add(eRectLcars);
    
    dyr += 156+SPACEING;
    w=550;
    h=315-156;
    EElbo elbRight2 = new EElbo(this,dx,dyr,w,h,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE|LCARS.ES_STATIC,null);
    elbRight2.setArmWidths(menuWidth,22); elbRight2.setArcWidths(0,90);
    add(elbRight2);

    add(new ERect(this,dx+w,dyr+h-22,22,22,LCARS.ES_STATIC|LCARS.ES_RECT_RND_E,null));
    
    dyr+=h+SPACEING;
    ERect eRectData = new ERect(this,dx,dyr,menuWidth,93,LCARS.EC_ELBOUP|LCARS.ES_LABEL_NE|LCARS.ES_STATIC,"MIC ARRAY\nPRESETS");
    add(eRectData);
    
    dyr+=93+SPACEING;
    w = 550;
    h = 224;
    EElbo elbRight3 = new EElbo(this,dx,dyr,w,h,LCARS.EF_SMALL|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE|LCARS.ES_STATIC,null);
    elbRight3.setArmWidths(menuWidth,22); elbRight3.setArcWidths(80,90);
    add(elbRight3);
    (new Thread(){
      @Override
      public void run() {
        if (getScreen() instanceof Screen) {
          elbRight3.setLabel("PNL: LOCAL");
          return;
        }
        elbRight3.setLabel("PNL: "+NetUtils.getHostName().toUpperCase());
      }
    }).start();
    
    add(new ERect(this,dx+w,dyr+h-22,22,22,LCARS.ES_STATIC|LCARS.ES_RECT_RND_E,null));

    // ########################################################################
    // ########################### slices section #############################
    // ########################################################################
    
    int posX = 80;
    int posY = 150;
    int roomWidth = 440;
    int roomHeight = 250;
    int lineStyle = LCARS.EC_ELBOUP|LCARS.ES_DISABLED|LCARS.ES_STATIC;
    int scaleTextStyle = LCARS.EF_SMALL|LCARS.ES_STATIC|LCARS.EC_TEXT|LCARS.ES_LABEL_NE;
    
    // horizontal sensitivity plot with frame and lines
    sensitivityPlotHo = new ESensitivityPlot(this, this, posX, posY, roomWidth, roomWidth, cube.getSlicePosition().getZ(), MicArrayState.getCurrent(), freq, GSensitivityPlot.ES_XY);
    addObserver(sensitivityPlotHo);
    sensitivityPlotHo.setStatic(false);
    sensitivityPlotHo.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        if(!eRectSliceMode.isSelected()){
          MicArrayState state=MicArrayState.getCurrent();
          Point3d target =state.target;
          target.setX(-(roomWidth/2)+ee.pt.getX());
          target.setY(roomWidth-((roomWidth/2)+ee.pt.getY()));
          DoAEstimator.getInstance().setTargetSource(target);
          setChanged();
          notifyObservers(new NewBeamformerMsg(state,eRectTarget.isSelected()));
          if(eRectAutoSlicer.isSelected()) {
            setChanged();
            notifyObservers(new NewSlicePositionMsg(state.target.x, state.target.y, state.target.z, eRectSlices.isSelected(), false));
          }
        }
      }
    });
    add(sensitivityPlotHo);
    
    horiEElbo = getElboFrame(posX-15,posY-20,460,466,10,LCARS.EC_ELBOUP);
    eLabelHor = new ELabel(this,posX+107,posY-20,0,0,scaleTextStyle,"HORIZONTAL SLICE");
    add(eLabelHor);
    int lineThickness = 2;
    int lxx=posX-45;
    int lyy=posY+245;
    lineGroup = new ERect[11];
    lineGroup[0]=new ERect(this,lxx,lyy,30,lineThickness,lineStyle,null);
    lineGroup[1]=new ERect(this,lxx,lyy+2,lineThickness,238,lineStyle,null);lyy+=240;
    lineGroup[2]=new ERect(this,lxx,lyy,570,lineThickness,lineStyle,null);lxx+=570;
    lineGroup[3]=new ERect(this,lxx,lyy,lineThickness,198,lineStyle,null);lyy+=198;
    lineGroup[4]=new ERect(this,lxx,lyy,100,lineThickness,lineStyle,null);
    
    // lateral vertical sensitivity plot with frame and lines
    sensitivityPlotVertSi = new ESensitivityPlot(this, this, posX+roomWidth+100, posY, roomHeight, roomWidth, cube.getSlicePosition().getX(), MicArrayState.getCurrent(), freq, GSensitivityPlot.ES_YZ);
    sensitivityPlotVertSi.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        if(!eRectSliceMode.isSelected()){
          MicArrayState state = MicArrayState.getCurrent();
          Point3d target = state.target;
          target.setZ(ee.pt.getX());
          target.setY(roomWidth-((roomWidth/2)+ee.pt.getY()));
          DoAEstimator.getInstance().setTargetSource(target);
          setChanged();
          notifyObservers(new NewBeamformerMsg(state,eRectTarget.isSelected()));
          if(eRectAutoSlicer.isSelected()) {
            setChanged();
            notifyObservers(new NewSlicePositionMsg(state.target.x, state.target.y, state.target.z, eRectSlices.isSelected(), false));
          }
        }
      }
    });
    addObserver(sensitivityPlotVertSi);
    add(sensitivityPlotVertSi);
    
    vertLatEElbo = getElboFrame(posX+526,posY-20,268,466,10,LCARS.EC_ELBOUP);
    eLabelVertLat = new ELabel(this,posX+685,posY-20,0,0,scaleTextStyle,"VERTICAL LATERAL SLICE");
    add(eLabelVertLat);
    lxx=posX+520+284;
    lyy=posY+245;
    lineGroup[5]=new ERect(this,lxx,lyy,30,lineThickness,lineStyle,null);lxx+=30;
    lineGroup[6]=new ERect(this,lxx,lyy,lineThickness,233,lineStyle,null);lyy+=233;
    lineGroup[7]=new ERect(this,lxx,lyy,117,lineThickness,lineStyle,null);
    
    // frequency slider
    int maxFreq = 7000;
    getElboFrame(posX+880,posY+10,140,420,10,lineStyle);
    add(new ELabel(this,posX+963,posY+10,0,0,scaleTextStyle,"FREQUENCY"));
    Rectangle sBounds = new Rectangle(posX+897,posY+45,60,380);
    Rectangle2D.Double pBounds = new Rectangle2D.Double(0,10,0,maxFreq);
    freqPoser = new EPositionerFreq(freq,sBounds,pBounds,"Hz ");
    pBounds = new Rectangle2D.Double(0,0,0,maxFreq);
    freqPoser.setConstraints(pBounds,true);
    freqPoser.addPositionListener(new IPositionListener() {
      @Override
      public void positionChanging(java.awt.geom.Point2D.Float position) {
        positionChanged(position);
      }

      @Override
      public void positionChanged(java.awt.geom.Point2D.Float position) {
        freq=(float) position.getY();
        setChanged();
        notifyObservers(new NewFrequencyMsg(freq));
        // Feed frequency slider
        Point2D.Double p2d = new Point2D.Double();
        p2d.setLocation(0, freq);
        freqPoser.setActualPos(p2d);
      }
    });
    freqPoser.addToPanel(this);
    
    // vertical front sensitivity plot with frame and lines
    posY+=530;
    sensitivityPlotVertFr = new ESensitivityPlot(this, this, posX, posY, roomWidth, roomHeight, cube.getSlicePosition().getY(), MicArrayState.getCurrent(), freq, GSensitivityPlot.ES_XZ);
    sensitivityPlotVertFr.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        if(!eRectSliceMode.isSelected()){
          MicArrayState state=MicArrayState.getCurrent();
          Point3d target = state.target;
          target.setX(-(roomWidth/2)+ee.pt.getX());
          target.setZ(roomHeight-ee.pt.getY());
          DoAEstimator.getInstance().setTargetSource(target);
          setChanged();
          notifyObservers(new NewBeamformerMsg(state,eRectTarget.isSelected()));
          if(eRectAutoSlicer.isSelected()) {
            setChanged();
            notifyObservers(new NewSlicePositionMsg(state.target.x, state.target.y, state.target.z, eRectSlices.isSelected(), false));
          }
        }
      }
    });
    addObserver(sensitivityPlotVertFr);
    add(sensitivityPlotVertFr);
    
    vertFroEElbo = getElboFrame(posX-15,posY-20,460,276,10,LCARS.EC_ELBOUP);
    eLabelVerFr = new ELabel(this,posX+133,posY-20,0,0,scaleTextStyle,"VERTICAL FRONT SLICE");
    add(eLabelVerFr);
    lxx=posX+455;
    lyy=posY+125;
    lineGroup[8]=new ERect(this,lxx,lyy,40,lineThickness,lineStyle,null);lxx+=40;
    lineGroup[9]=new ERect(this,lxx,lyy,lineThickness,254,lineStyle,null);lyy+=254;
    lineGroup[10]=new ERect(this,lxx,lyy,86,lineThickness,lineStyle,null);
    for(ERect r:lineGroup)
      add(r);
    
    // dB scale with frame
    int posXX = posX;
    int posYY = posY+320;
    imgScale = new EBufferedImage(this, posXX, posYY, LCARS.ES_STATIC, sensitivity.getHorizontalScaleImage(440, 20));
    add(imgScale);
    
    eElboScale = getElboFrame(posX-15,posYY-21,460,68,10,lineStyle);
    eLabelsScale = new ELabel[9];
    eLabelsScale[0] = new ELabel(this,posX+35,posYY-21,0,0,scaleTextStyle,"SCALE");
    posYY+=23;
    eLabelsScale[1] = new ELabel(this,posXX+25,posYY+20,0,0,scaleTextStyle,"[dB]");
    int textStyle = LCARS.EF_TINY|LCARS.ES_STATIC|LCARS.EC_TEXT|LCARS.ES_LABEL_NE;
    eLabelsScale[2] = new ELabel(this,posXX+2,posYY,0,0,textStyle,"0");
    eLabelsScale[3] = new ELabel(this,posXX+50,posYY,0,0,textStyle,"-6");
    eLabelsScale[4] = new ELabel(this,posXX+100,posYY,0,0,textStyle,"-12");
    eLabelsScale[5] = new ELabel(this,posXX+170,posYY,0,0,textStyle,"-18");
    eLabelsScale[6] = new ELabel(this,posXX+260,posYY,0,0,textStyle,"-24");
    eLabelsScale[7] = new ELabel(this,posXX+350,posYY,0,0,textStyle,"-30");
    eLabelsScale[8] = new ELabel(this,posXX+444,posYY,0,0,textStyle,"-36");
    for (ELabel el : eLabelsScale) {
      add(el);
    }
    
    // cube
    canvas = new ECanvas3D(posX+530, posY-50, 520, 520, cube.getCanvas3d());
    canvas.addToPanel(this);
        
    // ########################################################################
    // ######################## microphone array section ######################
    // ########################################################################
    
    sBounds = new Rectangle(1520,150,300,183);
    Rectangle2D.Float pBounds2 = new Rectangle2D.Float(-90,-55,180,110);
    eA1tTopo = new EMicrophoneArraySelector(MicArrayViewer.getInstance(),MicArrayState.getCurrent(),sBounds,pBounds2);
    pBounds2 = new Rectangle2D.Float(-80,-40,100,50);
    ImageMeta.Resource imr = new ImageMeta.Resource("csl/resources/MicrophoneArray1_small.png");
    eA1tTopo.setMapImage(imr,pBounds2,false);
    eA1tTopo.addToPanel(this);
    micArr1Elbo = getElboFrame((int)sBounds.getX()-20, (int)sBounds.getY()-20, (int)sBounds.getWidth()+30, (int)sBounds.getHeight()+40, 10, LCARS.EC_ELBOUP);
    add(new ELabel(this,(int)sBounds.getX()+73,(int)sBounds.getY()+(int)sBounds.getHeight()+18,0,0,textStyle,"VIEWER MIC ARRAY"));
    
    sBounds = new Rectangle(1520,420,183,183);
    pBounds2 = new Rectangle2D.Float(-105,-105,210,210);
    eA2tTopo = new EMicrophoneArraySelector(MicArrayCeiling.getInstance(),MicArrayState.getCurrent(),sBounds,pBounds2);
    imr = new ImageMeta.Resource("csl/resources/MicrophoneArray2_small.png");
    eA2tTopo.setMapImage(imr,pBounds2,false);
    eA2tTopo.addToPanel(this);
    micArr2Elbo = getElboFrame((int)sBounds.getX()-20, (int)sBounds.getY()-30, (int)sBounds.getWidth()+30, (int)sBounds.getHeight()+60, 10, LCARS.EC_ELBOUP);
    add(new ELabel(this,(int)sBounds.getX()+75,(int)sBounds.getY()+(int)sBounds.getHeight()+28,0,0,textStyle,"CEILING MIC ARRAY"));
    
    int x = 1750;
    int y = 380;
    EValue eA2tPosValue = new EValue(this,x+8,y+17,33,20,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_STATIC|LCARS.EF_TINY,null);
    eA2tPosValue.setValueMargin(0);
    add(eA2tPosValue);
    
    ERect eA2tPosLock = new ERect(this,x+42,y+17,45,20,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.EF_TINY,"LOCK");
    add(eA2tPosLock);
    
    sBounds = new Rectangle(x+8,y+52,80,193);
    pBounds2 = new Rectangle2D.Float(-35,-157,70,279);
    eA2tPoser = new EPositioner(sBounds,pBounds2,"cm ");
    pBounds2 = new Rectangle2D.Float(0,-140,0,249);
    eA2tPoser.setConstraints(pBounds2,true);
    eA2tPoser.setControls(eA2tPosLock,eA2tPosValue);
    eA2tPoser.addPositionListener(new IPositionListener() {
      @Override
      public void positionChanging(java.awt.geom.Point2D.Float position) {
      }

      @Override
      public void positionChanged(java.awt.geom.Point2D.Float position) {
        setPosition((float)position.getY());
      }
    });
    eA2tPoser.setLock(true);
    eA2tPoser.addToPanel(this);
    
    
    // ########################################################################
    // ############################ presets section ###########################
    // ########################################################################
    
    int xPre = 1500;
    int yPre = 760;
    
    add(new ELabel(this, xPre, yPre-23, 0, 0, LCARS.ES_LABEL_W|LCARS.ES_STATIC|LCARS.EC_TEXT, "VIEWER MIC ARRAY"));
    eRectMA1NQuater = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "1/4");
    eRectMA1NQuater.addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray1(0);
      }
    });
    add(eRectMA1NQuater);
    
    xPre+=90;
    eRectMA1NHalf = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "1/2");
    eRectMA1NHalf .addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray1(1);
      }
    });
    add(eRectMA1NHalf);
    
    xPre+=90;
    eRectMA1N23 = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "2/3");
    eRectMA1N23 .addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray1(2);
      }
    });
    add(eRectMA1N23);
    
    xPre+=90;
    eRectMA1NFull = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "FULL");
    eRectMA1NFull .addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray1(3);
      }
    });
    add(eRectMA1NFull);
    setPresetModeArray1(3);
    
    xPre=1500;
    yPre+=120;
    add(new ELabel(this, xPre, yPre-23, 0, 0, LCARS.ES_LABEL_W|LCARS.ES_STATIC|LCARS.EC_TEXT, "CEILING MIC ARRAY"));
    eRectMA2NQuater = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "1/4");
    eRectMA2NQuater.addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray2(0);
      }
    });
    add(eRectMA2NQuater);
    
    xPre+=90;
    eRectMA2NHalf = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "1/2");
    eRectMA2NHalf .addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray2(1);
      }
    });
    add(eRectMA2NHalf);
    
    xPre+=90;
    eRectMA2N23 = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "2/3");
    eRectMA2N23 .addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray2(2);
      }
    });
    add(eRectMA2N23);
    
    xPre+=90;
    eRectMA2NFull = new ERect(this, xPre, yPre, 80, 58, LCARS.EC_PRIMARY|LCARS.ES_LABEL_C|LCARS.ES_SHAPE_NW, "FULL");
    eRectMA2NFull .addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee) {
        setPresetModeArray2(3);
      }
    });
    add(eRectMA2NFull);
    setPresetModeArray2(3);
    
    // ########################################################################
    
    sBounds = new Rectangle(970,650,80,200);
    Rectangle2D.Float pBounds3 = new Rectangle2D.Float(0,0,10,180);
    eA2tPoser2 = new EPositioner(sBounds,pBounds3,"TH");
    pBounds3 = new Rectangle2D.Float(0,0,0,180);
    eA2tPoser2.setConstraints(pBounds3,true);
    eA2tPoser2.addPositionListener(new IPositionListener() {
      @Override
      public void positionChanging(java.awt.geom.Point2D.Float position) {
      }

      @Override
      public void positionChanged(java.awt.geom.Point2D.Float position) {
        viewer3d.setThreshold((int)Math.round(position.getY()));
      }
    });
    Point2D.Float thres = new Point2D.Float();
    thres.setLocation(0, viewer3d.getThreshold());
    eA2tPoser2.setTargetPos(thres, true, true);
    
    eLabelTh = new ELabel(this, sBounds.x+72,sBounds.y-20, 0, 0, scaleTextStyle,"THRESHOLD");
    add(eLabelTh);
    
    thFrame = getElboFrame(sBounds.x-10,sBounds.y-20,sBounds.width+55,sBounds.height+30,10,lineStyle);
    for (EElbo elb : thFrame) {
      elb.setVisible(false);
    }
    
    eRectRot = new ERect(this,1000,900,100,58,LCARS.EC_PRIMARY|LCARS.ES_RECT_RND|LCARS.ES_LABEL_C,"ROTATE");
    eRectRot.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
        ee.el.setSelected(!ee.el.isSelected());
        if(ee.el.isSelected())
          viewer3d.startAnimation();
        else
          viewer3d.stopAnimation();
      }
    });
    add(eRectRot);
    eRectRot.setVisible(false);
    
    eRectReset = new ERect(this,1000,980,100,58,LCARS.EC_SECONDARY|LCARS.ES_RECT_RND|LCARS.ES_LABEL_C,"RESET");
    eRectReset.addEEventListener(new EEventListenerAdapter() {
      @Override
      public void touchDown(EEvent ee) {
          viewer3d.resetView();
      }
    });
    add(eRectReset);
    eRectReset.setVisible(false);
  }

  /**
   * Set position of the motor slider for the ceiling array.
   * @param position - float value
   */
  protected void setPosition(float position) {
    Point3d point=MicArrayCeiling.getInstance().getPosition();
    point.setY(position);
    try {
      MicArrayCeiling.getInstance().setPosition(point);
    } catch (IllegalArgumentException e) {
      Log.err(e.getMessage(), e);
    } catch (HardwareException e) {
      Log.err(e.getMessage(), e);
    }
  }
  
  /**
   * Creating a EElbo[] frame
   * @param x - int, x position of the frame
   * @param y - int, y position of the frame
   * @param width - int, width value of the frame
   * @param height - int, height value of the frame
   * @param frameThickness - int, thickness of the frame
   * @param style - int, style
   * @return EElbo[] array with 4 elements of EElbo
   */
  private EElbo[] getElboFrame(int x, int y, int width, int height, int frameThickness, int style){
    EElbo[] eFrame = new EElbo[4];
    height/=2;
    eFrame[0] = new EElbo(this,x,y,frameThickness,height,style|LCARS.ES_SHAPE_NW,null);
    eFrame[1] = new EElbo(this,x,y+height,frameThickness,height+14,style|LCARS.ES_SHAPE_SW,null);
    eFrame[2] = new EElbo(this,x+width,y,frameThickness,height,style|LCARS.ES_SHAPE_NE,null);
    eFrame[3] = new EElbo(this,x+width,y+height,frameThickness,height+14,style|LCARS.ES_SHAPE_SE,null);
    
    for(int i=0;i<eFrame.length;i++){
      eFrame[i].setArmWidths(4,18);
      eFrame[i].setArcWidths(10,5);
      add(eFrame[i]);
    }
    return eFrame;
  }
  
  /**
   * Setting or updating the style of a EElbo[] frame
   * @param eFrame - EElbo[]
   * @param style - int value
   */
  private void setElboStyle(EElbo[] eFrame, int style){
    if(eFrame.length<0 || eFrame.length>4) return;
    
    eFrame[0].setStyle(style|LCARS.ES_SHAPE_NW);
    eFrame[1].setStyle(style|LCARS.ES_SHAPE_SW);
    eFrame[2].setStyle(style|LCARS.ES_SHAPE_NE);
    eFrame[3].setStyle(style|LCARS.ES_SHAPE_SE);
  }
  
  private void hideElbo(EElbo[] eFrame, boolean visible){
    if(eFrame.length<0 || eFrame.length>4) return;
    
    eFrame[0].setVisible(visible);
    eFrame[1].setVisible(visible);
    eFrame[2].setVisible(visible);
    eFrame[3].setVisible(visible);
  }
  
  @Override
  public void stop() {
    
    if (canvas.isDisplayed())
      canvas.removeFromPanel();

    super.stop();
  }
  
  @Override
  protected void fps10() {
    eRectSlicesReset.setDisabled(!eRectSlices.isSelected());
    MicArrayState state = MicArrayState.getCurrent();
    eRectTargetData.setLabel(""+Math.round(state.target.getX())+"  "+Math.round(state.target.getY())+"  "+Math.round(state.target.getZ()));
    
    if(!eRectSliceMode.isSelected()){
      sensitivityPlotHo.setDisabled(false);
      sensitivityPlotVertSi.setDisabled(false);
      sensitivityPlotVertFr.setDisabled(false);
      eRectTargetReset.setDisabled(false);
      eRectTargetReset.setSelected(true);
    } else {
      sensitivityPlotHo.setDisabled(true);
      sensitivityPlotVertSi.setDisabled(true);
      sensitivityPlotVertFr.setDisabled(true);
      eRectTargetReset.setDisabled(true);
      eRectTargetReset.setSelected(false);
    }
    
    // Feed motor position
    Point3d p3d = MicArrayCeiling.getInstance().getPosition();
    Point2D.Float p2d = new Point2D.Float();
    p2d.setLocation(p3d.x, p3d.y);
    eA2tPoser.setActualPos(Double.isNaN(p2d.y)?null:p2d);
    setChanged();
    notifyObservers(new NewCeilingMsg(MicArrayState.getCurrent(),eRectCeilArray.isSelected()));
    
    Point2D.Float thres = new Point2D.Float();
    thres.setLocation(0, viewer3d.getThreshold());
    eA2tPoser2.setActualPos(thres);
  }
  
  @Override
  protected void fps1() {
    setChanged();
    notifyObservers(new NewBeamformerMsg(MicArrayState.getCurrent(),eRectTarget.isSelected()));
    
    String xSlice = String.format(Locale.ENGLISH,"%03.1f",cube.getSlicePosition().getX());
    String ySlice = String.format(Locale.ENGLISH,"%03.1f",cube.getSlicePosition().getY());
    String zSlice = String.format(Locale.ENGLISH,"%03.1f",cube.getSlicePosition().getZ());
    eRectSliceData.setLabel(""+xSlice+"  "+ySlice+"  "+zSlice);
  }
  
  /**
   * Update all LEDs of a single microphone array panel.
   * @param partArray - AMicArray3DPart
   * @param topo - EMicrophoneArraySelector
   */
  private void updateMicArrayTopo(AMicArray3DPart partArray, EMicrophoneArraySelector topo){
    for (ERect e : topo.getPoints()) {
      int micId = (Integer)e.getData();
      e.setColor(partArray.getLedController().getColor(micId-partArray.getMinMicId()));
    }
    topo.updateCursor();
  }
  
  @Override
  protected void fps25()
  {
    // Reflect calibration state
    MicArray3D ma3d = MicArray3D.getInstance();
    eCalibrate.setColor(ma3d.isCalibrated()?null:cError);
    eCalibrate.setBlinking(!ma3d.isCalibrated());
    
    // Feed microphone array topography
    updateMicArrayTopo(MicArrayViewer.getInstance(), eA1tTopo);
    updateMicArrayTopo(MicArrayCeiling.getInstance(), eA2tTopo);
    
    MicArrayState state = MicArrayState.getCurrent();
    if(eRectAutoSlicer.isSelected()) {
      setChanged();
      notifyObservers(new NewSlicePositionMsg(state.target.x, state.target.y, state.target.z, eRectSlices.isSelected(), false));
    }
    
    // - on entire panel if currently displayed array is muted
    try
    {
      setColorScheme(!ma3d.isActive()?LCARS.CS_REDALERT:LCARS.CS_MULTIDISP);
    }
    catch (Exception e) {
      Log.err(e.getMessage(), e);
    }
    
    presetChangerArray1();
    presetChangerArray2();
  }
  
  private void setDiableToAll(boolean disable){
    eRectTarget.setDisabled(!disable);
    eRectSlices.setDisabled(!disable);
    eRectSliceMode.setDisabled(!disable);
    eRectTargetReset.setDisabled(!disable);
    eRectSlicesReset.setDisabled(!disable);
    eRectAutoSlicer.setDisabled(!disable);
    eRectTvArray.setDisabled(!disable);
    eRectCeilArray.setDisabled(!disable);
    sensitivityPlotHo.setDisabled(!disable);
    sensitivityPlotVertSi.setDisabled(!disable);
    sensitivityPlotVertFr.setDisabled(!disable);
    
    hideElbo(horiEElbo, disable);
    hideElbo(vertLatEElbo, disable);
    hideElbo(vertFroEElbo, disable);
    
    eLabelHor.setVisible(disable);
    eLabelVertLat.setVisible(disable);
    eLabelVerFr.setVisible(disable);
    
    imgScale.setVisible(disable);
    
    for(EElbo elb : eElboScale)
      elb.setVisible(disable);
    
    for(ELabel el : eLabelsScale)
      el.setVisible(disable);
    
    for(ERect e : lineGroup)
      e.setAlpha((disable)?1:0);
    
    sensitivityPlotHo.setVisible(disable);
    sensitivityPlotVertSi.setVisible(disable);
    sensitivityPlotVertFr.setVisible(disable);
    
    freqPoser.setLock(!disable);
  }
  
  public static void dispose(){
    viewer3d.stopAnimation();
  }
  
  private void presetChangerArray1(){
    if(eRectMicArray1.isSelected()){
      MicArrayViewer viewer = MicArrayViewer.getInstance();
      int mode = getPresetModeArray1();
      try {
        viewer.setMicArrayPreset(mode);
      } catch (HardwareException e) {
        Log.err(e.getMessage());
      }
    }
  }
  
  private void presetChangerArray2(){
    if(eRectMicArray2.isSelected()){
      MicArrayCeiling ceil = MicArrayCeiling.getInstance();
      int mode = getPresetModeArray2();
      try {
        ceil.setMicArrayPreset(mode);
      } catch (HardwareException e) {
        Log.err(e.getMessage());
      }
    }
  }
  
  /**
   * Set the preset mode of Array 1
   * @param mode
   */
  private void setPresetModeArray1(int mode) {
    eRectMA1NQuater.setSelected(mode==0);
    eRectMA1NHalf.setSelected(mode==1);
    eRectMA1N23.setSelected(mode==2);
    eRectMA1NFull.setSelected(mode==3);
  }
  
  /**
   * 
   * @param disabled
   */
  private void disablePresetModeArray1(boolean disabled) {
    eRectMA1NQuater.setDisabled(disabled);
    eRectMA1NHalf.setDisabled(disabled);
    eRectMA1N23.setDisabled(disabled);
    eRectMA1NFull.setDisabled(disabled);
  }
  
  /**
   * 
   * @param disabled
   */
  private void disablePresetModeArray2(boolean disabled) {
    eRectMA2NQuater.setDisabled(disabled);
    eRectMA2NHalf.setDisabled(disabled);
    eRectMA2N23.setDisabled(disabled);
    eRectMA2NFull.setDisabled(disabled);
  }

  /**
   * Get the preset mode of Array 1
   */
  private int getPresetModeArray1(){
    if (eRectMA1NQuater.isSelected()) return 0;
    if (eRectMA1NHalf.isSelected()) return 1;
    if (eRectMA1N23.isSelected()) return 2;
    if (eRectMA1NFull.isSelected()) return 3;
    else return -1;
  }
  
  /**
   * Set the preset mode of Array 1
   * @param mode
   */
  private void setPresetModeArray2(int mode) {
    eRectMA2NQuater.setSelected(mode==0);
    eRectMA2NHalf.setSelected(mode==1);
    eRectMA2N23.setSelected(mode==2);
    eRectMA2NFull.setSelected(mode==3);
  }

  /**
   * Get the preset mode of Array 1
   */
  private int getPresetModeArray2(){
    if (eRectMA2NQuater.isSelected()) return 0;
    if (eRectMA2NHalf.isSelected()) return 1;
    if (eRectMA2N23.isSelected()) return 2;
    if (eRectMA2NFull.isSelected()) return 3;
    else return -1;
  }
  
  /* *****************************************************************************
   * My own observable implementation.
   * Reason: there is no multiple extends in java.
   * ***************************************************************************** 
   */
    
  @Override
  public synchronized void addObserver(IObserver o) {
    if (o == null)
      return;
    if (!obs.contains(o)) {
      obs.addElement(o);
    }
  }

  @Override
  public synchronized void deleteObserver(IObserver o) {
    obs.removeElement(o);
  }

  @Override
  public void notifyObservers() {
    notifyObservers(null);
  }

  @Override
  public void notifyObservers(Object arg) {
    Object[] arrLocal;
    
    synchronized (this) {

      if (!changed)
        return;
      arrLocal = obs.toArray();
      clearChanged();
    }

    for(Object local : arrLocal)
      ((IObserver) local).update(this, arg);
  }

  @Override
  public synchronized void deleteObservers() {
    obs.removeAllElements();
  }

  @Override
  public synchronized void setChanged() {
    changed = true;
  }

  protected synchronized void clearChanged() {
    changed = false;
  }

  @Override
  public synchronized boolean hasChanged() {
    return changed;
  }

  @Override
  public synchronized int countObservers() {
    return obs.size();
  }
  
  /**
   * Convenience method: Runs the test panel.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",BeamformerPanel.class.getName());
    CSL.main(args);
  }

}
