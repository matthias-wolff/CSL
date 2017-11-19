package de.tucottbus.kt.csl.lcars;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.micarray3d.AMicArray3DPart;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;
import de.tucottbus.kt.csl.lcars.contributors.EMicrophoneArray;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.MainPanel;
import de.tucottbus.kt.lcars.Screen;
import de.tucottbus.kt.lcars.contributors.EElementArray;
import de.tucottbus.kt.lcars.contributors.EPositioner;
import de.tucottbus.kt.lcars.contributors.ESignalDisplay;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.contributors.IPositionListener;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcars.net.NetUtils;
import de.tucottbus.kt.lcars.swt.ColorMeta;
import de.tucottbus.kt.lcars.swt.ImageMeta;
import de.tucottbus.kt.lcars.util.Range;

/**
 * The CogntiveSystemsLab's microphone array LCARS panel.
 * 
 * @author Matthias Wolff
 */
public class MicrophoneArrayPanel extends MainPanel
{
  private MicArrayCeiling  mac;
  private MicArrayViewer   mav;
  private final int              colorScheme = LCARS.CS_MULTIDISP;
  
  // The microphone array I topology group (prefix "eA1t")
  private EMicrophoneArray eA1tTopo;
  private ERect            eA1tFix;

  // The microphone array II topology group (prefix "eA2t")
  private EMicrophoneArray eA2tTopo;
  private EPositioner      eA2tPoser;
  private ERect            eA2tPosCancel;
  private EValue           eA2tPosValue;
  private ERect            eA2tPosSpacer;
  private ELabel           eA2tPosUnit;
  private ERect            eA2tPosLock;

  // The microphone list group (prefix "eMls")
  private EElementArray    eMlsList;
  private ERect            eMlsLevel;
  private EElbo            eMlsDelay;
  private ELabel           eMlsDelayUnit;
  private EElbo            eMlsGain;
  private ELabel           eMlsGainUnit;
  private ERect            eMlsMute;

  // Other GUI items
  private EElbo            ePanelHost;
  private EElbo            eScreenHost;
  private ESignalDisplay   eSignal;
  private EElbo            eCalibrate;
  private ERect            eAmbiLightMonitor;
  private EElbo            eLockW;
  private ERect            eMute1;
  private ERect            eMute2;
  private ERect            eLockE;
  private ERect            eLamptest;
  private ELabel           eGuiLd;
  private ERect            eA2tPosScreen;
  private ERect            eA2tPosPlus75;
  private ERect            eA2tPosPlus50;
  private ERect            eA2tPosPlus25;
  private ERect            eA2tPosOrigin;
  private ERect            eA2tPosMinus25;
  private ERect            eA2tPosMinus50;
  private ERect            eA2tPosMinus75;
  private ERect            eA2tPosMinus100;
  private ERect            eA2tPosMinus125;
  private ERect            eA2tPosBooth;
  
  // Other fields
  private final ColorMeta      cError   = new ColorMeta(0x00FF0066,false);
  
  public static Thread     ledThread;
  
  /**
   * Creates a new microphone array panel.
   * 
   * @param screen
   *          The LCARS screen to create the panel for.
   */
  public MicrophoneArrayPanel(IScreen screen)
  {
    super(screen);
  }
  
  protected void setPosition(float position)
  {
    Point3d point = mac.getPosition();
    point.setY(position);
    try
    {
      mac.setPosition(point);
    }
    catch (Exception e)
    {
      Log.err("Failed to move "+mac.getName(),e);
    }
  }

  @Override
  public void init()
  {
    super.init();
    MicArray3D.getInstance();
    mac = MicArrayCeiling.getInstance();
    mav = MicArrayViewer.getInstance();
    Rectangle         sBounds;
    Rectangle2D.Float pBounds; 
    ColorMeta cOutline = new ColorMeta(1f,1f,1f,0.25f);

    // The main layout
    EElement e;
    e = new ERect(this,23,3,208,127,LCARS.EC_ELBOUP|LCARS.ES_LABEL_SE|LCARS.ES_SELECTED,"LCARS");
    e.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        panelSelectionDialog();
      }
    });
    add(e);

    ePanelHost = new EElbo(this,23,133,312,181,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE|LCARS.ES_STATIC,null);
    ePanelHost.setArmWidths(208,38); ePanelHost.setArcWidths(170,90);
    add(ePanelHost);
    (new Thread(){
      @Override
      public void run()
      {
        if (getScreen() instanceof Screen)
        {
          ePanelHost.setLabel("PNL: LOCAL");
          return;
        }
        ePanelHost.setLabel("PNL: "+NetUtils.getHostName().toUpperCase());
      }
    }).start();
    
    eScreenHost = new EElbo(this,23,317,312,202,LCARS.EC_ELBOLO|LCARS.ES_SHAPE_NW|LCARS.ES_LABEL_SE|LCARS.ES_STATIC,null);
    eScreenHost.setArmWidths(208,38); eScreenHost.setArcWidths(170,90);
    add(eScreenHost);
    (new Thread(){
      @Override
      public void run()
      {
        if (getScreen() instanceof Screen)
        {
          eScreenHost.setLabel("SCR: LOCAL");
          return;
        }
        try
        {
          eScreenHost.setLabel("SCR: "+getScreen().getHostName().toUpperCase());
        }
        catch (RemoteException e)
        {
          eScreenHost.setLabel("SCR: OFFLINE");
        }
      }
    }).start();

    add(new ERect(this,492,267,195,6,LCARS.EC_ELBOUP|LCARS.ES_SELECTED|LCARS.ES_STATIC,null));
    
    eCalibrate = new EElbo(this,338,276,349,38,LCARS.ES_SHAPE_NW|LCARS.EC_PRIMARY|LCARS.ES_LABEL_W,"CALIBRATE");
    eCalibrate.setArmWidths(129,17); eCalibrate.setArcWidths(1,50);
    eCalibrate.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        try
        {
          MicArray3D.getInstance().calibrate();
        }
        catch (Exception e){
          Log.err(e.getMessage(), e);
        };
      }
    });
    add(eCalibrate);
    
    eLockW = new EElbo(this,338,317,349,38,LCARS.ES_SHAPE_SW|LCARS.EC_ELBOUP|LCARS.ES_SELECTED|LCARS.ES_LABEL_W,"LOCK");
    eLockW.setArmWidths(129,17); eLockW.setArcWidths(1,50);
    add(eLockW);

    eMute1 = new ERect(this,470,296,109,39,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W,"MUTE I");
    eMute1.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        try
        {
          mav.setActive(!mav.isActive());
        } 
        catch (HardwareException e)
        {
          Log.err(e.getMessage(), e);
        }
      }
    });
    add(eMute1);

    eMute2 = new ERect(this,582,296,105,39,LCARS.EC_SECONDARY|LCARS.ES_LABEL_E,"MUTE II");
    eMute2.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        try
        {
          mac.setActive(!mac.isActive());
        } 
        catch (HardwareException e)
        {
          Log.err(e.getMessage(), e);
        }
      }
    });
    add(eMute2);
    
    eLockE = new ERect(this,1798,317,99,38,LCARS.EC_SECONDARY|LCARS.ES_LABEL_E,"LOCK");
    add(eLockE);

    eLamptest = new ERect(this,23,522,208,46,LCARS.EC_SECONDARY|LCARS.ES_LABEL_E,"LAMP TEST");
    eLamptest.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        // TODO: Handle lamp test button
      }
    });
    add(eLamptest);
    
    e = new ERect(this,23,931,208,146,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_SE,"ARRAY\nSELECT");
    e.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        arraySelect(-1);
      }
    });
    add(e);
    
    eAmbiLightMonitor = new ERect(this,690,317,127,38,LCARS.ES_STATIC,null);
    add(eAmbiLightMonitor);
    
    // Place holders (during construction work only)
    e = new ERect(this,690,276,522-1,38-1,LCARS.ES_OUTLINE|LCARS.ES_STATIC,null);
    ((ERect)e).setColor(cOutline);
    add(e);
    e = new ELabel(this,690,276,522-1,38-1,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"TOPOLOGICAL VS. FOCUSING MODE");
    ((ELabel)e).setColor(cOutline);
    add(e);
    
    e = new ERect(this,1215,276,682-1,38-1,LCARS.ES_OUTLINE|LCARS.ES_STATIC,null);
    ((ERect)e).setColor(cOutline);
    add(e);
    e = new ELabel(this,1215,276,682-1,38-1,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"INDIVIDUAL VS. CONFIGURATION MODE");
    ((ELabel)e).setColor(cOutline);
    add(e);
    
    e = new ERect(this,1215,317,156-1,38-1,LCARS.ES_STATIC|LCARS.ES_OUTLINE,null);
    ((ERect)e).setColor(cOutline);
    add(e);

    e = new ERect(this,23,571,208-1,316-1,LCARS.ES_STATIC|LCARS.ES_OUTLINE,null);
    ((ERect)e).setColor(cOutline);
    add(e);

    eGuiLd = new ELabel(this,23,890,208,38,LCARS.ES_STATIC|LCARS.ES_LABEL_W,"000-00/000-00");
    eGuiLd.setColor(cOutline);
    setLoadStatControl(add(eGuiLd));

    e = new ERect(this,1374,120,523-1,131-1,LCARS.ES_STATIC|LCARS.ES_OUTLINE,null);
    ((ERect)e).setColor(cOutline);
    add(e);
    e = new ELabel(this,1374,120,523-1,131-1,LCARS.ES_STATIC|LCARS.ES_LABEL_NE,"AMBIENT LIGHT CONTROL");
    ((ELabel)e).setColor(cOutline);
    add(e);

    // The signal and level displays
    eSignal = new ESignalDisplay(270,127,349,3,100,228,ESignalDisplay.MODE_STATIC|ESignalDisplay.MODE_NOCURSOR);
    eSignal.addToPanel(this);
    
    // The microphone list group (prefix "eMls")
    eMlsLevel = new ERect(this,1374,317,105,38,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E|LCARS.ES_SHAPE_NW,"LEVEL");
    eMlsLevel.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setMlsMode(0);
      }
    });
    add(eMlsLevel);

    eMlsDelay = new EElbo(this,1482,317,105,38,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E|LCARS.ES_SHAPE_NW,"DELAY");
    eMlsDelay.setArcWidths(1,1); eMlsDelay.setArmWidths(91,22); 
    eMlsDelay.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setMlsMode(1);
      }
    });
    add(eMlsDelay);

    eMlsDelayUnit = new ELabel(this,1574,317,16,40,LCARS.EC_PRIMARY|LCARS.ES_STATIC|LCARS.EF_SMALL|LCARS.ES_LABEL_SW,"us");
    add(eMlsDelayUnit);

    eMlsGain = new EElbo(this,1590,317,105,38,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E|LCARS.ES_SHAPE_NW,"GAIN");
    eMlsGain.setArcWidths(1,1); eMlsGain.setArmWidths(90,19); 
    eMlsGain.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setMlsMode(2);
      }
    });
    add(eMlsGain);

    eMlsGainUnit = new ELabel(this,1681,317,16,40,LCARS.EC_PRIMARY|LCARS.ES_STATIC|LCARS.EF_SMALL|LCARS.ES_LABEL_SW,"dB");
    add(eMlsGainUnit);

    eMlsMute = new ERect(this,1698,317,97,38,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E|LCARS.ES_SHAPE_NW,"MUTE");
    eMlsMute.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setMlsMode(3);
      }

      @Override
      public void touchHold(EEvent ee)
      {
        // Toggle mute state of all microphones
        if (ee.ct==20)
          try
          {
            AMicArray3DPart ma = getDisplayedArray().getMicrophoneArray();
            
            // Count muted mics
            int mutedCount = 0;
            for (int i=0; i<eMlsList.getItemCount(); i++)
              if (!ma.isMicActive((Integer)eMlsList.getItemElement(i).getData()))
                mutedCount++;
  
            // Mute or unmute all
            boolean mute = mutedCount<eMlsList.getItemCount()/2;
            for (int i=0; i<eMlsList.getItemCount(); i++)
              ma.setMicActive((Integer)eMlsList.getItemElement(i).getData(),!mute);
          }
          catch (Exception e)
          {
            Log.err(e.getMessage(), e);
          }
      }
    });
    add(eMlsMute);

    eMlsList = new EElementArray(1374,374,EValue.class,new Dimension(260,41),16,2,LCARS.ES_RECT_RND|LCARS.EC_SECONDARY|LCARS.ES_LABEL_E,null);
    eMlsList.setLockControl(eLockE);
    eMlsList.setLock(true);
    eMlsList.addToPanel(this);
    eMlsList.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        int m = (Integer)ee.el.getData();
        boolean sel = !ee.el.isSelected(); 

        // If in mute mode, toggle microphone's mute state
        if (getMlsMode()==3)
          try
          {
            AMicArray3DPart ma = getDisplayedArray().getMicrophoneArray();
            ma.setMicActive(m,!ma.isMicActive(m));
            sel = true;
          }
          catch (Exception e)
          {
            Log.err(e.getMessage(), e);
          }

        // Select/deselect microphone
        getDisplayedArray().setLock(true);
        getDisplayedArray().setSelection(sel?m:null);
      }
    });
    
    // The microphone array I topology group (prefix "eA1t")    
    eA1tFix = new ERect(this,820,317,392,38,LCARS.EC_ELBOUP|LCARS.ES_STATIC|LCARS.ES_LABEL_E,"FIX");
    add(eA1tFix);
    
    sBounds = new Rectangle(260,395,1064,650);
    pBounds = new Rectangle2D.Float(-90,-55,180,110); 
    eA1tTopo = new EMicrophoneArray(mav,sBounds,pBounds);
    ImageMeta.Resource imr = new ImageMeta.Resource("csl/resources/MicrophoneArray1.png");
    eA1tTopo.setMapImage(imr,pBounds,false);
    eA1tTopo.setSelectorControl(eMlsList);
    eA1tTopo.setLockControl(eLockW);
    
    // The microphone array II topology group (prefix "eA2t")
    eA2tPosCancel = new ERect(this,820,317,125,38,LCARS.EC_ELBOUP|LCARS.ES_SELECTED|LCARS.ES_LABEL_E,"CANCEL");
    eA2tPosCancel.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        try 
        {
          mac.cancelPositioning();
        } 
        catch (Exception e) 
        {
          Log.err(e.getMessage(), e);
        }
      }
    });
    add(eA2tPosCancel);

    eA2tPosValue = new EValue(this,948,317,121,38,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_STATIC,"POS");
    eA2tPosValue.setValueMargin(0);
    add(eA2tPosValue);

    eA2tPosSpacer = new ERect(this,1069,317,15,22,LCARS.EC_ELBOUP|LCARS.ES_STATIC,null);
    add(eA2tPosSpacer);

    eA2tPosUnit = new ELabel(this,1068,317,16,40,LCARS.EC_ELBOUP|LCARS.ES_LABEL_SW|LCARS.EF_SMALL|LCARS.ES_STATIC,"cm");
    add(eA2tPosUnit);

    eA2tPosLock = new ERect(this,1087,317,125,38,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"LOCK");
    add(eA2tPosLock);
    
    sBounds = new Rectangle(260,368,710,710);
    pBounds = new Rectangle2D.Float(-105,-105,210,210); 
    eA2tTopo = new EMicrophoneArray(mac,sBounds,pBounds);
    imr = new ImageMeta.Resource("csl/resources/MicrophoneArray2.png");
    eA2tTopo.setMapImage(imr,pBounds,false);
    eA2tTopo.setSelectorControl(eMlsList);
    eA2tTopo.setLockControl(eLockW);
    
    int x = 940;
    int y = 300;
    //sBounds = new Rectangle(1052,368,160,710);
    //pBounds = new Rectangle2D.Float(-45,-265,90,525);
    sBounds = new Rectangle(x+38,y+62,130,710);
    pBounds = new Rectangle2D.Float(-35,-157,70,279);
    eA2tPoser = new EPositioner(sBounds,pBounds,"cm ");
    //pBounds = new Rectangle2D.Float(0,-200,0,400);
    pBounds = new Rectangle2D.Float(0,-140,0,249);
    eA2tPoser.setConstraints(pBounds,true);
    eA2tPoser.setControls(eA2tPosLock,eA2tPosValue);
    eA2tPoser.addPositionListener(new IPositionListener()
    {
      @Override
      public void positionChanging(Float position)
      {
      }
		
      @Override
      public void positionChanged(Float position)
      {
        setPosition((float)position.getY());
      }
    });
    
    x = x+190;
    y = y+100;
    int width = 80;
    
    eA2tPosScreen = new ERect(this,x,y + 20,width,56,LCARS.EC_PRIMARY,"SCREEN");
    eA2tPosScreen.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(107);
      }
    });
    
    eA2tPosPlus75 = new ERect(this,x,y + 78,width,55,LCARS.EC_SECONDARY,"75 cm");
    eA2tPosPlus75.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(75);
      }
    });
    
    eA2tPosPlus50 = new ERect(this,x,y + 135,width,56,LCARS.EC_SECONDARY,"50 cm");
    eA2tPosPlus50.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(50);
      }
    });
    
    eA2tPosPlus25 = new ERect(this,x,y + 193,width,55,LCARS.EC_SECONDARY,"25 cm");
    eA2tPosPlus25.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(25);
      }
    });
    
    eA2tPosOrigin = new ERect(this,x,y + 250,width,56,LCARS.EC_PRIMARY,"ORIGIN");
    eA2tPosOrigin.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(0);
      }
    });
    
    eA2tPosMinus25 = new ERect(this,x,y + 308,width,55,LCARS.EC_SECONDARY,"-25 cm");
    eA2tPosMinus25.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(-25);
      }
    });
    
    eA2tPosMinus50 = new ERect(this,x,y + 365,width,56,LCARS.EC_SECONDARY,"-50 cm");
    eA2tPosMinus50.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(-50);
      }
    });
    
    eA2tPosMinus75 = new ERect(this,x,y + 423,width,55,LCARS.EC_SECONDARY,"-75 cm");
    eA2tPosMinus75.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(-75);
      }
    });
    
    eA2tPosMinus100 = new ERect(this,x,y + 480,width,56,LCARS.EC_SECONDARY,"-100 cm");
    eA2tPosMinus100.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(-100);
      }
    });
    
    eA2tPosMinus125 = new ERect(this,x,y + 538,width,55,LCARS.EC_SECONDARY,"-125 cm");
    eA2tPosMinus125.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(-125);
      }
    });
    
    eA2tPosBooth = new ERect(this,x,y + 595,width,56,LCARS.EC_PRIMARY,"BOOTH");
    eA2tPosBooth.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setPosition(-141);
      }
    });
    
    add(eA2tPosScreen);
    add(eA2tPosPlus75);
    add(eA2tPosPlus50);
    add(eA2tPosPlus25);
    add(eA2tPosOrigin);
    add(eA2tPosMinus25);
    add(eA2tPosMinus50);
    add(eA2tPosMinus75);
    add(eA2tPosMinus100);
    add(eA2tPosMinus125);
    add(eA2tPosBooth);
    
    // Initialize --> TODO: this is to be done from persistent properties
    eA2tPoser.setLock(true);
    arraySelect(1);
    setMlsMode(0);
  }

  @Override
  public void stop() 
  {
    EMicrophoneArray array = getDisplayedArray();
    if(array==null) return;
    array.removeFromPanel();
    super.stop();
  }  
  
  // -- Operations --

  private EMicrophoneArray getDisplayedArray()
  {
    if (eA1tTopo!=null && eA1tTopo.isDisplayed()) return eA1tTopo;
    if (eA2tTopo!=null && eA2tTopo.isDisplayed()) return eA2tTopo;
    return null;
  }
  
  /**
   * Selects microphone array 1 or 2.
   * 
   * @param arrayId
   *          The array to select, -1 for "the other one".
   */
  private void arraySelect(int arrayId)
  {
    if (arrayId<0)
    {
      if (getDisplayedArray()==eA1tTopo) arrayId = 2;
      else                               arrayId = 1;
    }
    
    if (arrayId==1 && getDisplayedArray()==eA1tTopo) return;
    if (arrayId==2 && getDisplayedArray()==eA2tTopo) return;
    
    switch (arrayId)
    {
    case 1:
      setTitle(mav.getName().toUpperCase()+" MICARR");
      showGuiItems(getGuiItemsByFieldName("eA2t"),false);
      showGuiItems(getGuiItemsByFieldName("eA1t"),true );
      eMute1.setColorStyle(LCARS.EC_PRIMARY);
      eMute2.setColorStyle(LCARS.EC_SECONDARY);
      eSignal.reset();
      break;
    case 2:
      setTitle(mac.getName().toUpperCase()+" MICARR");
      showGuiItems(getGuiItemsByFieldName("eA1t"),false);
      showGuiItems(getGuiItemsByFieldName("eA2t"),true );
      eMute1.setColorStyle(LCARS.EC_SECONDARY);
      eMute2.setColorStyle(LCARS.EC_PRIMARY);
      eSignal.reset();
      break;
    }
  }

  /**
   * TODO: write JavaDoc
   * 
   * @param mode
   *          The microphone list mode.
   * @see #getMlsMode()
   */
  private void setMlsMode(int mode)
  {
    eMlsLevel    .setSelected(mode==0);
    eMlsDelay    .setSelected(mode==1);
    eMlsDelayUnit.setSelected(mode==1);
    eMlsGain     .setSelected(mode==2);
    eMlsGainUnit .setSelected(mode==2);
    eMlsMute     .setSelected(mode==3);
  }

  /**
   * TODO: write JavaDoc
   */
  private int getMlsMode()
  {
    if (eMlsLevel.isSelected()) return 0;
    if (eMlsDelay.isSelected()) return 1;
    if (eMlsGain .isSelected()) return 2;
    if (eMlsMute .isSelected()) return 3;
    else return -1;
  }
  
  // -- Auxiliary --

  /**
   * EXPERIMENTAL, Returns an array of {@link EElement} or {@link ElementContributor} fields whose
   * names start with the given prefix.
   * 
   * @param prefix
   *          The filed name prefix.
   * @see #showGuiItems(Vector, boolean) 
   */
  protected Vector<Object> getGuiItemsByFieldName(String prefix)
  {
    Vector<Object> vGuiItems = new Vector<Object>();
    for (Field f : getClass().getDeclaredFields())
    {
      if (!f.getDeclaringClass().equals(getClass())) continue;
      if (!f.getName().startsWith(prefix)) continue;
      try
      {
        Object v = f.get(this); 
        if (v instanceof EElement || v instanceof ElementContributor)
          vGuiItems.add(v);
      }
      catch (Exception e) {
        Log.err(e.getMessage(), e);
      } // Keep iterating
    }
    return vGuiItems;
  }

  /**
   * Shows or hides GUI items. GUI elements are ({@link EElement}s or {@link ElementContributor}s).
   * To <em>show</em> or <em>hide</em> {@link EElement}s means to make them visible or invisible by
   * calling {@link EElement#setVisible(boolean) setVisible(show)}. To <em>show</em> or
   * <em>hide</em> {@link ElementContributor}s means to add them to or to remove them from this
   * panel by calling {@link ElementContributor#addToPanel(de.tucottbus.kt.lcars.Panel)
   * addToPanel(this)} - if <code>show</code> is <code>true</code> - or
   * {@link ElementContributor#removeFromPanel() removeFromPanel()} - if <code>show</code> is
   * <code>false</code>.
   * 
   * @param vGuiItems
   *          An array of GUI items ({@link EElement}s or {@link ElementContributor}s).
   * @param show
   *          If <code>true</code> show the items, otherwise hide them.
   * @see #getGuiItemsByFieldName(String)
   */
  protected void showGuiItems(Vector<Object> vGuiItems, boolean show)
  {
    if (vGuiItems==null) return;
    for (Object guiItem : vGuiItems)
      if (guiItem instanceof EElement)
        ((EElement)guiItem).setVisible(show);
      else if (guiItem instanceof ElementContributor)
      {
        if (show)
          ((ElementContributor)guiItem).addToPanel(this);
        else
          ((ElementContributor)guiItem).removeFromPanel();
      }
  }
  
  // -- Overrides --
  
  @Override
  protected void fps2()
  {
    // Reflect muted state
    // - at mute buttons
    try
    {
      eMute1.setColor(!mav.isActive()?cError:null);
      eMute1.setBlinking(!mav.isActive());
      eMute2.setColor(!mac.isActive()?cError:null);
      eMute2.setBlinking(!mac.isActive());
    }
    catch (Exception e) {}
    
    // Reflect calibration state
    eCalibrate.setColor(MicArray3D.getInstance().isCalibrated()?null:cError);
    eCalibrate.setBlinking(!MicArray3D.getInstance().isCalibrated());
    
    // - on entire panel if currently displayed array is muted
    try
    {
      AMicArray3DPart ma = getDisplayedArray().getMicrophoneArray();
      setColorScheme(!ma.isActive()?LCARS.CS_REDALERT:colorScheme);
    }
    catch (Exception e) {
      Log.err(e.getMessage(), e);
    }
  }

  @Override
  protected void fps10()
  {
    
    // Feed motor position
    Point3d p3d = mac.getPosition();
    Point2D.Float p2d = new Point2D.Float();
    p2d.setLocation(p3d.x, p3d.y);
    //System.out.println("x="+p2d.x+", y="+p2d.y);
    eA2tPoser.setActualPos(Double.isNaN(p2d.y)?null:p2d);
    
    // Feed microphone list
    AMicArray3DPart ma = getDisplayedArray().getMicrophoneArray();
    MicArrayState  mas = ma.getState();
    int           mode = getMlsMode();
    int          micId = getDisplayedArray().getSelection();
    for (int i=0; i<eMlsList.getItemCount(); i++)
    {
      EValue e = (EValue)eMlsList.getItemElement(i);
      int    m = (Integer)e.getData();
       if (m<ma.getMinMicId() || m>ma.getMaxMicId())
       {
         Log.err("Invalid mic ID "+m);
         continue;
       }
      e.setSelected(m==micId);
      e.setAlpha(mas.activeMics[m]?1f:0.25f);
      switch (mode)
      {
      case 1:
        e.setValue(EMicrophoneArray.makeSteerDelay(mas.steerVec[m],false));
        e.setStyle(e.getStyle()&(~LCARS.ES_VALUE_W));
        break;
      case 2:
        e.setValue(EMicrophoneArray.makeGainDB(mas.gains[m],false));
        e.setStyle(e.getStyle()&(~LCARS.ES_VALUE_W));
        break;
      default:
        try
        {
          float  l = ma.getMicLevel(m)+55;
          String s = "";
          int    style = e.getStyle()&(~LCARS.ES_VALUE_W);
          while (l>0)
          {
            s+="I";
            l-=5;
          }
          if (!mas.activeMics[m])
            s="OFF";
          //else if (l<=-20)
          //  s="NOSIG";
          else 
            style|=LCARS.ES_VALUE_W;
          e.setValue(s);
          e.setStyle(style);
        }
        catch (Exception e1)
        {
          e.setValue("ERR");
          Log.err(e1.getMessage(), e1);
        }
        break;
      }
    }

    // Feed signal display
    float l = ma.getLevel();
    // TODO: HACK - Misuse sample display to show levels --> 
    //l = 15384*(float)Math.pow(10,l/20);
    l = 15384*Math.max(0,l+85)/85;
    // <--
    eSignal.addSample(new Range(-l,l),null);

    // Feed microphone array topography
    for (ERect e : getDisplayedArray().getPoints())
    {
      micId = (Integer)e.getData();
      try
      {
        e.setColor(ma.getLedController().getColor(micId-ma.getMinMicId()));
      }
      catch (Exception e1)
      {
        Log.err(e1.getMessage(), e1);
      }
    }
    getDisplayedArray().updateCursor();

    // Feed ambient light monitor
    try
    {
      eAmbiLightMonitor.setColor(ma.getAmbientLight());
    }
    catch (Exception e2)
    {
      eAmbiLightMonitor.setColor((ColorMeta)null);
      Log.err(e2.getMessage(),e2);
    }
    
    // TODO: Feed lamp test button
  }

  // -- Main method (debugging and testing only!) --
  
  /**
   * Runs the microphone array LCARS GUI.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",MicrophoneArrayPanel.class.getName());
	  CSL.main(args);
  }
  
}
