package de.tucottbus.kt.csl.lcars;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.led.ALedController;
import de.tucottbus.kt.csl.hardware.micarray3d.AMicArray3DPart;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.EMessageBoxListener;
import de.tucottbus.kt.lcars.contributors.EPositioner;
import de.tucottbus.kt.lcars.contributors.IPositionListener;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.elements.modify.EGeometryModifier;
import de.tucottbus.kt.lcars.feedback.UserFeedback;
import de.tucottbus.kt.lcars.feedback.UserFeedbackPlayer;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.GArea;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcars.swt.ColorMeta;

public class MactlWooPanel extends Panel
{
  private MicArray3D         ma3d;
  private MicArrayViewer     mav;
  private MicArrayCeiling    mac;
  private UserFeedbackPlayer ufp;  // The user feedback player

  private ERect       eWizWakeup;
  private ERect       eWizSleep;
  private ERect       eWizAccept;
  private ERect       eWizReject;
  private ERect       eAmbiLightMonitor;
  
  private ELabel      eGuiLd;

  private ERect       eMicActOn;   // 
  private ERect       eMicActOff;  // 
  private ERect       eMicActClr;  // 
  private ERect       eMicActGo;   // 
  private ERect[]     eMicAct;     // Array of mic switches
  private ERect[]     eMicSta;     // Array of mic status displays
  private ERect[]     eMicThr;     // Array "THRU"-buttons 
  private ERect       eA1ActLeft;
  private ERect       eA1ActUpper;
  private ERect       eA1ActLower;
  private ERect       eA1ActRight;
  private ERect       eA1ActAll;
  private ERect       eA2ActInner;
  private ERect       eA2ActMid;
  private ERect       eA2ActOuter;
  private ERect       eA2ActAll;
  private ERect       ePosScreen;
  private ERect       ePosPlus75;
  private ERect       ePosPlus50;
  private ERect       ePosPlus25;
  private ERect       ePosOrigin;
  private ERect       ePosMinus25;
  private ERect       ePosMinus50;
  private ERect       ePosMinus75;
  private ERect       ePosMinus100;
  private ERect       ePosMinus125;
  private ERect       ePosBooth;
  
  private int		  lastMic = -1;
  private boolean	  throughButton = false;
  
  private EPositioner eA2Poser;    // Position slider

  public MactlWooPanel(IScreen iscreen)
  {
    super(iscreen);
  }

  // -- GUI --
  
  @Override
  public void init()
  {
    try
    {
    super.init();
    int    x, y;
    ERect  eRect;
    EValue eValue;

    // Hardware init
    ma3d = MicArray3D.getInstance();
    mav = MicArrayViewer.getInstance();
    mac = MicArrayCeiling.getInstance();
    ufp = new UserFeedbackPlayer(UserFeedbackPlayer.VISUAL)
    {
      @Override
      public void writeColor(ColorMeta color)
      {
        try
        {
          MicArrayViewer.getInstance().setAmbientLight(color);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    };
    
    // Common
    eValue = new EValue(this,988,23,224,56,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_E|LCARS.ES_SELECTED,"EXIT");
    eValue.setValue("LCARS");
    eValue.addEEventListener(new EEventListenerAdapter(){
      @Override
      public void touchDown(EEvent ee)
      {
        messageBox("QUESTION","EXIT LCARS?","YES","NO",new EMessageBoxListener()
        {
          @Override
          public void answer(String answer)
          {
            if ("YES".equals(answer)) try
            {
              getScreen().exit();
            }
            catch (RemoteException e)
            {
              // Silently ignore network exceptions...
            }
          }
        });
      }
    });
    add(eValue);

    eGuiLd = new ELabel(this,988,82,224,38,LCARS.ES_STATIC|LCARS.ES_LABEL_W,"000-00/000-00");
    eGuiLd.setColor(ColorMeta.GRAY);
    setLoadStatControl(add(eGuiLd));
    
    // WIZARD    
    x = 33;
    y = 23;
    eValue = new EValue(this,x-28,y,758,56,LCARS.ES_STATIC|LCARS.ES_RECT_RND,null);
    eValue.setValue("WIZARD");
    add(eValue);

    eWizWakeup = new ERect(this,x,y+59,173,56,LCARS.EC_SECONDARY,"WAKEUP");
    eWizWakeup.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        wizardStateUserFeedback(UserFeedback.getInstance(UserFeedback.Type.REC_LISTENING));
      }
    });
    add(eWizWakeup);

    eWizSleep = new ERect(this,x+176,y+59,173,56,LCARS.EC_SECONDARY,"SLEEP");
    eWizSleep.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        wizardStateUserFeedback(UserFeedback.getInstance(UserFeedback.Type.REC_SLEEPING));
      }
    });
    add(eWizSleep);

    eWizAccept = new ERect(this,x+352,y+59,173,56,LCARS.EC_SECONDARY,"ACCEPT");
    eWizAccept.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        wizardStateUserFeedback(UserFeedback.getInstance(UserFeedback.Type.REC_ACCEPTED));
      }
    });
    add(eWizAccept);

    eWizReject = new ERect(this,x+528,y+59,173,56,LCARS.EC_SECONDARY,"REJECT");
    eWizReject.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        wizardStateUserFeedback(UserFeedback.getInstance(UserFeedback.Type.REC_REJECTED));
      }
    });
    add(eWizReject);
    
    eAmbiLightMonitor = new ERect(this,x,y+118,701,56,LCARS.ES_STATIC,null);
    add(eAmbiLightMonitor);
    
    // MACTLACT
    HashSet<Integer> h;
    eMicAct = new ERect[64];
    eMicSta = new ERect[64];
    eMicThr = new ERect[2];
    x = 33;
    y = 230;
    final int bw = 75;
    final int bh = 75;
    final int lw = 15;
    final int lh = 15;
    
    add(new ERect(this,x-28,y,56,56,LCARS.ES_STATIC|LCARS.ES_RECT_RND_W,null));
    
    eMicActOn = new ERect(this,x+31,y,122,56,LCARS.EC_PRIMARY,"ON");
    eMicActOn.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    add(eMicActOn);
    
    eMicActOff = new ERect(this,x+156,y,114,56,LCARS.EC_PRIMARY,"OFF");
    eMicActOff.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    add(eMicActOff);
    
    eMicActClr = new ERect(this,x+273,y,114,56,LCARS.EC_PRIMARY,"CLR");
    eMicActClr.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    add(eMicActClr);
    
    eMicActGo = new ERect(this,x+390,y,153,56,LCARS.EC_ELBOUP|LCARS.ES_SELECTED,"GO");
    eMicActGo.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    add(eMicActGo);
    
    eValue = new EValue(this,x+546,y,184,56,LCARS.ES_STATIC|LCARS.ES_RECT_RND_E,null);
    eValue.setValue("MACTLACT"); eValue.setValueWidth(184);
    add(eValue);
    
    for (int a=0; a<2; a++)
    {
      for (int i=0; i<4; i++)
        for (int j=0; j<8; j++)
        {
          int n = a*32+i*8+j;

          eMicAct[n] = new ERect(this,x+j*(bw+3),y+60+a*5*(bh+3)+i*(bh+3),bw,bh,LCARS.EC_SECONDARY,String.valueOf(n+1));
          AMicArray3DPart ma = a==0?mav:mac;
          h = new HashSet<Integer>();
          h.add(new Integer(ma.getMinMicId()+(i*8+j)));
          eMicAct[n].setData(h);
          eMicAct[n].addGeometryModifier(new EGeometryModifier()
          {
            @Override
            public void modify(ArrayList<AGeometry> geos)
            {
              Area are = new Area(((GArea)geos.get(0)).getArea());
              Rectangle r = are.getBounds();
              are.subtract(new Area(new Rectangle(r.x+r.width-lw,r.y+r.height-lh,lw,lh)));
              ((GArea)geos.get(0)).setShape(are);
            }
          });
          eMicAct[n].addEEventListener(new EEventListenerAdapter()
          {
            @Override
            public void touchDown(EEvent ee)
            {
              OnMicAct(ee.el);
            }
          });
          add(eMicAct[n]);
          
          eMicSta[n] = new ERect(this,x+j*(bw+3)+bw-lw+1,y+60+a*5*(bh+3)+i*(bh+3)+bh-lh+1,lw-1,lh-1,LCARS.ES_STATIC,null);
          eMicSta[n].setData(new Integer(ma.getMinMicId()+(i*8+j)));
          add(eMicSta[n]);          
        }
      
      eMicThr[a] = new ERect(this,x+8*(bw+3),y+60+a*5*(bh+3),bw,4*bh+9,LCARS.EC_PRIMARY,"THRU");
      eMicThr[a].addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          OnMicAct(ee.el);
        }
      });
      add(eMicThr[a]);
    }

    eA1ActLeft = new ERect(this,x,y+60+4*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"LEFT");
    eA1ActLeft.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    for (int i=21; i<=26; i++)
      h.add(new Integer(mav.getMinMicId()+(i-1)));
    h.add(new Integer(mav.getMinMicId()+0));
    h.add(new Integer(mav.getMinMicId()+10));
    eA1ActLeft.setData(h);    
    add(eA1ActLeft);

    eA1ActUpper = new ERect(this,x+2*(bw+3),y+60+4*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"UPPER");
    eA1ActUpper.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    for (int i=1; i<=10; i++)
      h.add(new Integer(mav.getMinMicId()+(i-1)));
    eA1ActUpper.setData(h);    
    add(eA1ActUpper);

    eA1ActLower = new ERect(this,x+4*(bw+3),y+60+4*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"LOWER");
    eA1ActLower.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    for (int i=11; i<=20; i++)
      h.add(new Integer(mav.getMinMicId()+(i-1)));
    eA1ActLower.setData(h);    
    add(eA1ActLower);

    eA1ActRight = new ERect(this,x+6*(bw+3),y+60+4*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"RIGHT");
    eA1ActRight.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    for (int i=27; i<=32; i++)
      h.add(new Integer(mav.getMinMicId()+(i-1)));
    h.add(new Integer(mav.getMinMicId()+19));
    h.add(new Integer(mav.getMinMicId()+9));
    eA1ActRight.setData(h);    
    add(eA1ActRight);

    eA1ActAll = new ERect(this,x+8*(bw+3),y+60+4*(bh+3),bw,bh,LCARS.EC_PRIMARY,"ALL");
    eA1ActAll.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    for (int i=1; i<=32; i++)
      h.add(new Integer(mav.getMinMicId()+(i-1)));
    eA1ActAll.setData(h);    
    add(eA1ActAll);

    eA2ActInner = new ERect(this,x,y+60+9*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"INNER");
    eA2ActInner.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    h.add(new Integer(mac.getMinMicId()+ 0));
    h.add(new Integer(mac.getMinMicId()+ 1));
    h.add(new Integer(mac.getMinMicId()+ 8));
    h.add(new Integer(mac.getMinMicId()+ 9));
    h.add(new Integer(mac.getMinMicId()+16));
    h.add(new Integer(mac.getMinMicId()+17));
    h.add(new Integer(mac.getMinMicId()+24));
    h.add(new Integer(mac.getMinMicId()+25));
    eA2ActInner.setData(h);    
    add(eA2ActInner);

    eA2ActMid = new ERect(this,x+2*(bw+3),y+60+9*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"MID");
    eA2ActMid.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    h.add(new Integer(mac.getMinMicId()+ 2));
    h.add(new Integer(mac.getMinMicId()+ 3));
    h.add(new Integer(mac.getMinMicId()+10));
    h.add(new Integer(mac.getMinMicId()+11));
    h.add(new Integer(mac.getMinMicId()+18));
    h.add(new Integer(mac.getMinMicId()+19));
    h.add(new Integer(mac.getMinMicId()+26));
    h.add(new Integer(mac.getMinMicId()+27));
    eA2ActMid.setData(h);    
    add(eA2ActMid);

    eA2ActOuter = new ERect(this,x+4*(bw+3),y+60+9*(bh+3),2*bw+3,bh,LCARS.EC_PRIMARY,"OUTER");
    eA2ActOuter.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    h.add(new Integer(mac.getMinMicId()+ 4));
    h.add(new Integer(mac.getMinMicId()+ 5));
    h.add(new Integer(mac.getMinMicId()+ 6));
    h.add(new Integer(mac.getMinMicId()+ 7));
    h.add(new Integer(mac.getMinMicId()+12));
    h.add(new Integer(mac.getMinMicId()+13));
    h.add(new Integer(mac.getMinMicId()+14));
    h.add(new Integer(mac.getMinMicId()+15));
    h.add(new Integer(mac.getMinMicId()+20));
    h.add(new Integer(mac.getMinMicId()+21));
    h.add(new Integer(mac.getMinMicId()+22));
    h.add(new Integer(mac.getMinMicId()+23));
    h.add(new Integer(mac.getMinMicId()+28));
    h.add(new Integer(mac.getMinMicId()+29));
    h.add(new Integer(mac.getMinMicId()+30));
    h.add(new Integer(mac.getMinMicId()+31));
    eA2ActOuter.setData(h);    
    add(eA2ActOuter);

    eA2ActAll = new ERect(this,x+8*(bw+3),y+60+9*(bh+3),bw,bh,LCARS.EC_PRIMARY,"ALL");
    eA2ActAll.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        OnMicAct(ee.el);
      }
    });
    h = new HashSet<Integer>();
    for (int i=1; i<=32; i++)
      h.add(new Integer(mac.getMinMicId()+(i-1)));
    eA2ActAll.setData(h);    
    add(eA2ActAll);

    // MACTLPOS
    x = 800;
    y = 230;
    add(new ERect(this,x,y,56,56,LCARS.ES_STATIC|LCARS.ES_RECT_RND_W,null));

    eRect = new ERect(this,x+59,y,126,56,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"CANCEL");
    eRect.addEEventListener(new EEventListenerAdapter()
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
          e.printStackTrace();
        }
      }
    });    
    add(eRect);
    
    eValue = new EValue(this,x+188,y,224,56,LCARS.ES_STATIC|LCARS.ES_RECT_RND_E,null);
    eValue.setValue("MACTLPOS");
    add(eValue);
    
    eValue = new EValue(this,x+27,y+778,182,56,LCARS.ES_STATIC|LCARS.ES_RECT_RND|LCARS.ES_LABEL_SE,"POS");
    eValue.setValueWidth(80);
    add(eValue);
        
    Rectangle sBounds = new Rectangle(x+38,y+62,160,710);
    Rectangle2D.Float pBounds = new Rectangle2D.Float(-35,-157,70,279);
    eA2Poser = new EPositioner(sBounds,pBounds,"cm");
    pBounds = new Rectangle2D.Float(0,-142,0,249);
    eA2Poser.setConstraints(pBounds,true);
    eA2Poser.setControls(null,eValue);
    eA2Poser.addPositionListener(new IPositionListener()
    {
      @Override
      public void positionChanging(Float position)
      {
      }
    
      @Override
      public void positionChanged(Float position)
      {
        mactlpos((float)position.getY());
      }
    });
    eA2Poser.addToPanel(this);
    
    // MACTLPOS shortcuts
    
    x = 1030;
    y = 330;
    ePosScreen = new ERect(this,x,y + 20,150,56,LCARS.EC_PRIMARY,"SCREEN");
    ePosScreen.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(107);
      }
    });
    add(ePosScreen);
    
    ePosPlus75 = new ERect(this,x,y + 78,150,55,LCARS.EC_SECONDARY,"75 cm");
    ePosPlus75.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(75);
      }
    });
    add(ePosPlus75);
    
    ePosPlus50 = new ERect(this,x,y + 135,150,56,LCARS.EC_SECONDARY,"50 cm");
    ePosPlus50.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(50);
      }
    });
    add(ePosPlus50);
    
    ePosPlus25 = new ERect(this,x,y + 193,150,55,LCARS.EC_SECONDARY,"25 cm");
    ePosPlus25.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(25);
      }
    });
    add(ePosPlus25);
    
    ePosOrigin = new ERect(this,x,y + 250,150,56,LCARS.EC_PRIMARY,"ORIGIN");
    ePosOrigin.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(0);
      }
    });
    add(ePosOrigin);
    
    ePosMinus25 = new ERect(this,x,y + 308,150,55,LCARS.EC_SECONDARY,"-25 cm");
    ePosMinus25.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(-25);
      }
    });
    add(ePosMinus25);
    
    ePosMinus50 = new ERect(this,x,y + 365,150,56,LCARS.EC_SECONDARY,"-50 cm");
    ePosMinus50.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(-50);
      }
    });
    add(ePosMinus50);
    
    ePosMinus75 = new ERect(this,x,y + 423,150,55,LCARS.EC_SECONDARY,"-75 cm");
    ePosMinus75.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(-75);
      }
    });
    add(ePosMinus75);
    
    ePosMinus100 = new ERect(this,x,y + 480,150,56,LCARS.EC_SECONDARY,"-100 cm");
    ePosMinus100.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(-100);
      }
    });
    add(ePosMinus100);
    
    ePosMinus125 = new ERect(this,x,y + 538,150,55,LCARS.EC_SECONDARY,"-125 cm");
    ePosMinus125.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(-125);
      }
    });
    add(ePosMinus125);
    
    ePosBooth = new ERect(this,x,y + 595,150,56,LCARS.EC_PRIMARY,"BOOTH");
    ePosBooth.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        mactlpos(-141);
      }
    });
    add(ePosBooth);
    

    // Initialize controls
    wizardStateUserFeedback(UserFeedback.getInstance(UserFeedback.Type.REC_SLEEPING));
    MicActClr();
    
  } catch (Exception e)
  { e.printStackTrace(); }
  }
  
  @Override
  protected void fps10()
  {
    // Feed LED colors
    for (int i=0; i<64; i++)
      try
      {
        ALedController led = i<32?mav.getLedController():mac.getLedController();
        eMicSta[i].setColor(led.getColor(i%32));
      }
      catch (Exception e)
      {
      }
    
    // Feed motor position
    Point3d p3d = mac.getPosition();
    Point2D.Float p2d = new Point2D.Float();
    p2d.x = (float)p3d.x;
    p2d.y = (float)p3d.y;
    //System.out.println("x="+p2d.x+", y="+p2d.y);
    eA2Poser.setActualPos(Double.isNaN(p2d.y)?null:p2d);

    // Feed ambient light monitor
    try
    {
      eAmbiLightMonitor.setColor(MicArrayViewer.getInstance().getAmbientLight());
    }
    catch (HardwareException e2)
    {
      eAmbiLightMonitor.setColor((ColorMeta)null);
      Log.err(e2.toString(),e2);
    }  
  }  
  
  // -- Wizard Helpers --
  
  /**
   * Called by GUI elements when activation of deactivation of microphones is
   * requested by the wizard.
   * 
   * @param e
   *          The button pressed by the wizard.
   */
  protected void OnMicAct(EElement e)
  {
    if (e==eMicThr[0] || e==eMicThr[1])
    {
      boolean sel = !e.isSelected();
      eMicThr[0].setSelected(sel);      
      eMicThr[1].setSelected(sel);
      throughButton = sel;
    }
    else if (e==eMicActClr)
    {
      MicActClr();
    }
    else if (e==eMicActGo)
    {
      // Get selected microphones a an array
      HashSet<Integer> h = new HashSet<Integer>();
      for (int i=0; i<64; i++)
        addMics(h,eMicAct[i]);
      addMics(h,eA1ActLeft);
      addMics(h,eA1ActUpper);
      addMics(h,eA1ActLower);
      addMics(h,eA1ActRight);
      addMics(h,eA1ActAll);
      addMics(h,eA2ActInner);
      addMics(h,eA2ActMid);
      addMics(h,eA2ActOuter);
      addMics(h,eA2ActAll);
      
      int[] micIds = new int[h.size()];
      int i=0;
      for (Integer micId : h)
        micIds[i++] = micId;

      // Activate or deactivate
      mactlact(micIds,eMicActOn.isSelected());
      
      // Clear selection
      MicActClr();
    }
    else
    {
      // All other buttons
      e.setSelected(!e.isSelected());
      eMicThr[0].setSelected(false);      
      eMicThr[1].setSelected(false);
      int id = getIdForEElement(e);
      if (id>=0 && throughButton)
      {
    	  if(lastMic < id)
    	  {
    		  for(int i = lastMic; i < id; i++)
    			  eMicAct[i].setSelected(true);
    	  } else {
    		  for(int i = id; i < lastMic; i++)
    			  eMicAct[i].setSelected(true);
    	  }
    	  throughButton = false;
      }
      lastMic = id;
    }
    
    if (e==eMicActOn)
      eMicActOff.setSelected(!eMicActOn.isSelected());
    
    if (e==eMicActOff)
      eMicActOn.setSelected(!eMicActOff.isSelected());
  }
  
  /**
   * Clears all MACTLACT buttons.
   */
  protected void MicActClr()
  {
    eMicActOn.setSelected(false);
    eMicActOff.setSelected(true);
    
    for (int i=0; i<64; i++)
      eMicAct[i].setSelected(false);

    eA1ActLeft.setSelected(false);
    eA1ActUpper.setSelected(false);
    eA1ActLower.setSelected(false);
    eA1ActRight.setSelected(false);
    eA1ActAll.setSelected(false);

    eA2ActInner.setSelected(false);
    eA2ActMid.setSelected(false);
    eA2ActOuter.setSelected(false);
    eA2ActAll.setSelected(false);
    
    eMicThr[0].setSelected(false);
    eMicThr[1].setSelected(false);
  }
  
  @SuppressWarnings("unchecked")
  private void addMics(HashSet<Integer> h, EElement e)
  {
    if (!e.isSelected()) return;
    h.addAll((HashSet<Integer>)e.getData());
  }
  
  private int getIdForEElement(EElement e)
  {
    for (int i=0; i<64; i++)
      if (e==eMicAct[i])
        return i;
    return -1;
  }
  
  // -- Hardware Control --
  
  /**
   * Activates or deactivates microphones.
   * 
   * @param micIds
   *          An array of microphone IDs to activate or deactivate. 
   * @param activate
   *          <code>true</code> to activate, <code>false</code> to deactivate.
   */
  protected void mactlact(int[] micIds, boolean activate)
  {
    if (micIds==null) return;
    for (int micId : micIds)
      try
      {
        ma3d.setMicActive(micId,activate);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
  }
  
  /**
   * Moves microphone array 2 to a new position.
   * 
   * @param position
   *          The new position in centimeters, relative to the standard
   *          position (range: -142..107)
   */
  protected void mactlpos(float position)
  {
    Point3d point = new Point3d();
    point.set(0, position, 220);
    try
    {
      mac.setPosition(point);
    }
    catch (Exception e)
    {
      Log.err("Failed to move microphone array 2",e);
    }
  }
 
  /**
   * Plays visual and audio user feedback.
   * 
   * @param signal
   *          The feedback signal.
   */
  protected void wizardStateUserFeedback(UserFeedback signal)
  {
    ufp.play(signal,UserFeedbackPlayer.VISUAL);
  }
  
  // -- Main method --
  
  /**
   * Runs the microphone array LCARS GUI.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",MactlWooPanel.class.getName());
    args = LCARS.setArg(args,"--nospeech",null);
    CSL.main(args);
  }

}
