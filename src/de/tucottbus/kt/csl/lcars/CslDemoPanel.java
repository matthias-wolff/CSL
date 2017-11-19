package de.tucottbus.kt.csl.lcars;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.net.URL;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.speech.Fvr;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Screen;
import de.tucottbus.kt.lcars.contributors.EBrowser;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.speech.SpeechEnginePanel;
import de.tucottbus.kt.lcars.speech.events.PostprocEvent;
import de.tucottbus.kt.lcars.speech.events.SpeechEvent;

public class CslDemoPanel extends SpeechEnginePanel
{
  private EBrowser cSpeechFvr;
  private String lastSpeechFvrHtml = null;
  
  public CslDemoPanel(IScreen screen)
  {
    super(screen);
  }
  
  @Override
  public void init()
  {
    super.init();
    
    // Re-configure Speech Engine Panel's widgets
    getELcars().removeAllEEventListeners();
    getELcars().addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        if (cSpeechFvr.isDisplayed())
        {
          cSpeechFvr.removeFromPanel();
          getCSpeechPostproc().addToPanel(CslDemoPanel.this);
        }
        panelSelectionDialog();
      }
    });    
    
    getEElboL().setLabel("PRESENTATION");
    getEElboL().setStatic(false);
    getEElboL().removeAllEEventListeners();
    getEElboL().addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        doc();
      }
    });

    getEModeL().removeAllEEventListeners();
    getEModeL().addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        modeSelectL(-1);
      }
    });

    // New widgets: The FVR recognition result display (lower sub panel)
    // NOTE: Content is filled in by speechEvent()
    cSpeechFvr = new EBrowser(490,370,1407,687,LCARS.ES_NONE);
    cSpeechFvr.setText("");

    // Add listener for presenter keys
    addKeyListener(new KeyListener()
    {
      @Override
      public void keyTyped(KeyEvent e)
      {
      }

      @Override
      public void keyPressed(KeyEvent e)
      {
        if (e.getModifiers()!=0) return;
        switch (e.getKeyCode())
        {
        case KeyEvent.VK_F5:        // aka. "Play"
        case KeyEvent.VK_ESCAPE:    // aka. "Play"
          getEElboL().setHighlighted(true);
          break;
        case KeyEvent.VK_PERIOD:    // aka. "Hide"
          getEVad().setHighlighted(true);
          break;
        case KeyEvent.VK_PAGE_DOWN: // aka. "Forward"
          getEModeL().setHighlighted(true);
          break;
        case KeyEvent.VK_PAGE_UP:   // aka. "Backward"
          getEModeL().setHighlighted(true);
          break;
        }
      }
      
      @Override
      public void keyReleased(KeyEvent e)
      {
        if (e.getModifiers()!=0) return;
        switch (e.getKeyCode())
        {
        case KeyEvent.VK_F5:        // aka. "Play"
        case KeyEvent.VK_ESCAPE:    // aka. "Play"
          getEElboL().setHighlighted(false);
          doc();
          break;
        case KeyEvent.VK_PERIOD:    // aka. "Hide"
          getEVad().setHighlighted(false);
          onVad();
          break;
        case KeyEvent.VK_PAGE_DOWN: // aka. "Forward"
          getEModeL().setHighlighted(false);
          modeSelectL(-1);
          break;
        case KeyEvent.VK_PAGE_UP:   // aka. "Backward"
          getEModeL().setHighlighted(false);
          modeSelectL(-1);
          break;
        }
      }
    });

    // Initialize demo
    //modeSelectL(0);
  }
  
  @Override
  public void stop()
  {
    cSpeechFvr.removeFromPanel();
    super.stop();
  }

  @Override
  public String getDocIndex()
  {
    try
    {
      File docIndex = LCARS.getResourceFile("docs/cslPresentation.html");
      URL  docUrl = new URL("file","/",docIndex.getAbsolutePath());
      return docUrl.toString();
    }
    catch (Exception e)
    {
      return null;
    }
  }  

  @Override
  public void speechEvent(SpeechEvent event) 
  {
    super.speechEvent(event);
    
    // Speech recognition post-processing events
    if (event instanceof PostprocEvent)
    {
      PostprocEvent pe = (PostprocEvent)event;
      String result = pe.result;
      if (!result.startsWith("FVR[")) result = "FVR["+result+"]";
      Fvr fvr = Fvr.fromString(result);
      String sHtml = "<!doctype html>\n";
      sHtml += "<html>\n";
      sHtml += "<body style=\"background-color:#000000; overflow:hidden;\">\n";
      try
      {
        sHtml += fvr.renderSvg();
      }
      catch (Exception e)
      {
        sHtml += "FVR renderer malfunction<br>Reason:"+e.toString();
      }
      sHtml +="\n</body></html>\n";
      lastSpeechFvrHtml = sHtml;
      //cSpeechFvr.setText(sHtml);
      cSpeechFvr.setTextViaTmpFile(sHtml);
    }
  }

  // -- Operations --

  protected void modeSelectL(int mode)
  {
    if (mode<0)
    {
      if (getCSpeechPostproc().isDisplayed()) mode = 0;
      else if (cSpeechFvr.isDisplayed())      mode = 1;
      else                                    mode = -1;
      mode++;
    }
    
    switch (mode)
    {
    case 1:
      getCSpeechPostproc().removeFromPanel();
      if (cSpeechFvr.isDisplayed())
        cSpeechFvr.setVisible(true);
      else
        cSpeechFvr.addToPanel(CslDemoPanel.this);
      // HACK: Re-display last FVR HTML
      Screen.getLocal(getScreen()).getSwtShell().getDisplay().asyncExec(new Runnable()
      {
        public void run()
        {
          if (lastSpeechFvrHtml!=null)
            cSpeechFvr.setTextViaTmpFile(lastSpeechFvrHtml);
        }
      });
      break;
    default: // mode 0
      cSpeechFvr.setVisible(false);
      //cSpeechFvr.removeFromPanel();
      getCSpeechPostproc().addToPanel(CslDemoPanel.this);
      break;
    }
  }

  // -- Main method --

  /**
   * Runs the CSL Presentation panel.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",CslDemoPanel.class.getName());
    CSL.main(args);
  }
  
}
