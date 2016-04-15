package de.tucottbus.kt.csl.speech;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tucottbus.kt.dlabpro.Executable;
import de.tucottbus.kt.dlabpro.recognizer.Recognizer;
import de.tucottbus.kt.lcars.feedback.UserFeedback;
import de.tucottbus.kt.lcars.feedback.UserFeedbackPlayer;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcars.speech.ISpeechEngine;
import de.tucottbus.kt.lcars.speech.ISpeechEventListener;
import de.tucottbus.kt.lcars.speech.events.LevelEvent;
import de.tucottbus.kt.lcars.speech.events.PostprocEvent;
import de.tucottbus.kt.lcars.speech.events.RecognitionEvent;
import de.tucottbus.kt.lcars.speech.events.SpeechEvent;
import de.tucottbus.kt.lcars.speech.events.StateChangedEvent;
import de.tucottbus.kt.lcars.speech.events.VadEvent;
import de.tucottbus.kt.lcars.swt.ColorMeta;

/**
 * A speech engine singleton using the dLabPro speech recognizer of TU Dresden. The singleton
 * dynamically binds to the low-level wrapper class
 * <code>de.tudresden.ias.dlabpro.recognizer.Recognizer</code>. Make sure that this class is
 * available through the class path.
 * 
 * @author Matthias Wolff
 */
public class UasrSpeechEngine implements ISpeechEngine, Observer
{
  /**
   * The speech engine singleton.
   */
  private static UasrSpeechEngine singleton = null;

  /**
   * The dLabPro recognizer wrapper.
   */
  private Recognizer recognizer;
  
  /**
   * The configuration.
   */
  private Properties config;
  
  /**
   * The list of {@link ISpeechEventListener} of this engine.
   */
  private Vector<ISpeechEventListener> speechListeners;

  /**
   * The list of {@link UserFeedbackPlayer}s of this engine.
   */
  private Vector<UserFeedbackPlayer> userFeedbackPlayers;
  
  /**
   * The user feedback mode (see documentation of {@link UserFeedbackPlayer#setMode(int)}
   * for possible values.
   * 
   * @see #setUserFeedbackMode(int)
   * @see #getUserFeedbackMode()
   */
  private int userFeedbackMode;

  // -- Fields: State tracking --
  
  /**
   * GUI Message counter. 
   */
  private int lvlCount;
  
  /**
   * The current frame index.
   */
  private long lvlFrame;
  
  /**
   * The current level value.
   */
  private float lvlValue = -96;

  /**
   * The current dialog state.
   */
  private int dlgState;
  
  /**
   * The current voice activity (-1: offline, 0: silence, 1: speech)
   */
  private int vadActivity;
  
  /**
   * The current busy state.
   */
  private boolean recBusy;

  /**
   * Indicates a fatal recognizer failure.
   */
  private boolean recFailure;
  
  /**
   * A buffer to collect a recognition event in.
   */
  private RecognitionEvent recEvent;
  
  /**
   * A buffer to collect a recognition post-processing event in.
   */
  private PostprocEvent ppfEvent;
  
  // -- Constructors and instance getters --

  /**
   * Returns the speech engine singleton. The method tries to dynamically bind to the low-level
   * wrapper class <code>de.tudresden.ias.dlabpro.recognizer.Recognizer</code>. If this class is not
   * found or does not supply the expected API, an {@link InstantiationException} is thrown.
   * 
   * @param configFile
   *          The configuration, can be <code>null</code> in which case a default configuration
   *          will be used.
   * @throws InstantiationException
   *           If the singleton could not be created.
   */
  public static UasrSpeechEngine getInstance(Properties config)
  throws InstantiationException
  {
    if (singleton==null)
    {
      singleton = new UasrSpeechEngine(config);
    }
    return singleton;
  }

  /**
   * Returns the UASR speech engine singleton
   */
  public static UasrSpeechEngine getInstance()
  {
    return singleton;
  }
  
  /**
   * Creates a new speech engine.
   * 
   * @param config
   *          The configuration, can be <code>null</code> in which case a default configuration will
   *          be used.
   * @throws InstantiationException
   *           If the instance could not be created.
   */
  protected UasrSpeechEngine(Properties config) throws InstantiationException
  {
    singleton           = this;
    speechListeners     = new Vector<ISpeechEventListener>();
    userFeedbackPlayers = new Vector<UserFeedbackPlayer>();
    this.config         = config;
    
    // Add default user feedback player
    addUserFeedbackPlayer(new UserFeedbackPlayer(UserFeedbackPlayer.AUDITORY)
    {
      @Override
      public void writeColor(ColorMeta color)
      {
      }
    });
    
    // Start
    (new Thread(){
      public void run()
      {
        singleton.start();
      }
    }).start();
  }
  
  // -- Implementation of the ISpeechEngine interface --
  
  @Override
  public synchronized void start()
  {
    if (isStarted()) return;

    // Instantiate the recognizer wrapper
    try
    {
      recognizer = new Recognizer(findRecognizerExe(),config);
      recognizer.addObserver(this);
      recBusy = true;
      recFailure = false;
      fire(new StateChangedEvent(this,-1));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  @Override
  public synchronized void stop()
  {
    if (recognizer==null) return;
    
    // Dispose the recognizer wrapper
    try
    {
      recognizer.dispose();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    recognizer = null;
    fire(new StateChangedEvent(this,-1));
  }

  @Override
  public boolean isStarted()
  {
    return recognizer!=null;
  }

  @Override
  public boolean isBusy()
  {
    return recBusy;
  }
  
  @Override
  public boolean hasFailed()
  {
    return recFailure;
  }
  
  @Override
  public boolean getVoiceActivity()
  {
    return vadActivity>0;
  }

  @Override
  public int getListenMode()
  {
    if (!isStarted()) return Integer.MIN_VALUE;
    if (vadActivity<0) return -1;
    if (!config.containsKey("data.dialog")) return 1;
    else return dlgState;
  }

  @Override
  public void setListenMode(int mode)
  {
    try
    {
      recognizer.setListenMode(mode);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  @Override
  public void setConfiguration(Properties config)
  {
    stop();
    this.config = config;
    start();
  }
  
  // -- Events -- 

  @Override
  public void addSpeechEventListener(ISpeechEventListener listener)
  {
    if (listener==null) return;
    if (speechListeners.contains(listener)) return;
    speechListeners.add(listener);
  }

  @Override
  public void removeSpeechEventListener(ISpeechEventListener listener)
  {
    if (listener==null) return;
    speechListeners.remove(listener);
  }

  /**
   * Fires a speech event.
   * 
   * @param event The event.
   */
  protected final void fire(SpeechEvent event)
  {
    for (ISpeechEventListener listener : speechListeners)
      listener.speechEvent(event);
    
    playUserFeedback(event);
  }

  /**
   * Post-processes and fires the current recognition event buffer. If the
   * buffer ({@link #recEvent}) is <code>null</code>, then method does nothing. 
   * 
   * @see #recEvent
   */
  protected void fireRecognitionEvent()
  {
    if (recEvent==null) return;
    try
    {
      String s = recEvent.result;
      if (!recEvent.accepted && s.startsWith("(") && s.endsWith(""))
        s = s.substring(1,s.length()-1);
      if (!s.startsWith("FVR[)") && !s.endsWith("]"))
        s = "FVR["+s+"]";
      Fvr sem = Fvr.fromString(s);
      recEvent.result = sem.toString();
      recEvent.result = recEvent.result.substring(4,recEvent.result.length()-1);
      recEvent.text = sem.getComment();
    }
    catch (IllegalArgumentException e)
    {
      Log.err("Fvr parse error: "+e.getMessage(),e);
    }
    fire(recEvent);
    Log.info("res="+recEvent.result+", "+(recEvent.accepted?"accepted":"REJECTED"));
    Log.info("txt="+recEvent.text);
  }
  
  // -- User feedback --
  
  @Override
  public void addUserFeedbackPlayer(UserFeedbackPlayer player)
  {
    if (player==null) return;
    if (userFeedbackPlayers.contains(player)) return;
    userFeedbackPlayers.add(player);
  }

  @Override
  public void removeUserFeedbackPlayer(UserFeedbackPlayer player)
  {
    if (player==null) return;
    userFeedbackPlayers.remove(player);
  }

  @Override
  public void setUserFeedbackMode(int mode)
  {
    userFeedbackMode = mode;
  }
  
  @Override
  public int getUserFeedbackMode()
  {
    return userFeedbackMode;
  }

  /**
   * Gives audio-visual user feedback on a speech event.
   * 
   * @param event
   *          The event.
   */
  protected final void playUserFeedback(SpeechEvent event)
  {
    // Create the signal
    UserFeedback signal = null;
    if (event instanceof StateChangedEvent)
    {
      StateChangedEvent se = (StateChangedEvent)event;
      if (!event.spe.isStarted())
        signal = UserFeedback.getInstance(UserFeedback.Type.NONE);
      else if (se.listening)
        signal = UserFeedback.getInstance(UserFeedback.Type.REC_LISTENING);
      else if (se.sleeping)
        signal = UserFeedback.getInstance(UserFeedback.Type.REC_SLEEPING);
    }
    else if (event instanceof RecognitionEvent)
    {
      RecognitionEvent re = (RecognitionEvent)event;
      if (getListenMode()>0 && !re.incremenral)
      {
        if (re.accepted)
        {
          if (!"__WAKEUP__".equals(re.result))
            signal = UserFeedback.getInstance(UserFeedback.Type.REC_ACCEPTED);
        }
        else
          signal = UserFeedback.getInstance(UserFeedback.Type.REC_REJECTED);
      }
    }
    
    // Dispatch the signal
    if (signal!=null)
      for (UserFeedbackPlayer player : userFeedbackPlayers)
        player.play(signal,userFeedbackMode);
  }
  
  // -- Implementation of the Observer interface --

  @Override
  public void update(Observable o, Object arg)
  {
    assert o==recognizer : "Observable is not the wrapped recognizer instance";
    char   type = ((String)arg).charAt(0);
    String msg  = ((String)arg).substring(1);
    
    // Process messages
    if (processGuiMessage         (type,msg)) return;
    if (processStateMessage       (type,msg)) return;
    if (processRecognitionMessage (type,msg)) return;
    if (processPostprocMessage    (type,msg)) return;
    if (processOptionUpdateMessage(type,msg)) return;

    // Echo unprocessed messages
    echoMessage(type,msg+" *UNPROCESSED*");
  }

  // -- Messages --

  private Pattern PAT_CMD_RES    = Pattern.compile("cmd: res: (.*?) acc: (.*?)");
  private Pattern PAT_DBG_REFREC = Pattern.compile("dbg: refrec (.*?)");
  private Pattern PAT_DBG_RECGW  = Pattern.compile("dbg: rec gw: (.*?) (.*?)");
  private Pattern PAT_DBG_RECNAD = Pattern.compile("dbg: rec nad: (.*?) ned: (.*?) tnad: (.*?) tned: (.*?)");
  private Pattern PAT_DBG_VAD    = Pattern.compile("dbg: vad (.*?) at frame (.*?)");
  private Pattern PAT_GUI_LVL    = Pattern.compile("gui: frm: (.*?) lvl: (.*?) tim: (.*?)");
  private Pattern PAT_STA_BUFSKP = Pattern.compile("sta: buf skipped: (.*?)");
  private Pattern PAT_STA_INCRES = Pattern.compile("sta: liveres: (.*?)");
  private Pattern PAT_STA_REC    = Pattern.compile("sta: rec (.*?) \\((.*?)\\)");
  private Pattern PAT_STA_RECSKP = Pattern.compile("sta: rec skip for (.*?)");
  private Pattern PAT_STA_VADCOL = Pattern.compile("sta: vad collected (.*?) frames from (.*?) \\(sigmax: (.*?)\\)");
  private Pattern PAT_STA_VOCCHG = Pattern.compile("sta: voc change: (.*?)(\\s(.*?))*?");
  private Pattern PAT_PPF_FRM    = Pattern.compile("ppf: frm: (.*?)\t(.*?)\t(.*?)\t(.*?)\t(.*?)");
  private Pattern PAT_PPF_RES    = Pattern.compile("ppf: res: (.*?)");
  private Pattern PAT_OPT_UPD    = Pattern.compile("Option updated (.*?)=(.*?)");
  
  /**
   * Echoes a message from the dLabPro recognizer at the standard or the error output.
   * 
   * @param type
   *          The message type.
   * @param msg
   *          The message.
   */
  protected void echoMessage(char type, String msg)
  {
    switch (type)
    {
      case '!': Log.err (msg); break;
      default : Log.info(type,msg);
    }
  }
  
  /**
   * Processes a GUI message from the dLabPro recognizer.
   * 
   * @param type
   *          The message type. The method only processes output messages where
   *          <code>type='&lt;'</code>.
   * @param msg
   *          The message. The method processes only message starting with <code>"gui: "</code>.
   * @return <code>true</code> if the message was processed, <code>false</code> otherwise.
   */
  protected boolean processGuiMessage(char type, String msg)
  {
    if (type!='<') return false;
    Matcher matcher = PAT_GUI_LVL.matcher(msg);
    if (!matcher.matches()) return false;

    // Convert to speech message
    lvlFrame = capturedLong(matcher,1);
    lvlValue = (float)Math.max(capturedDouble(matcher,2),lvlValue);
    lvlCount++;
    if (lvlCount==10)
    {
      fire(new LevelEvent(this,lvlFrame,lvlValue));
      lvlCount = 0;
      lvlValue = -96;
    }
    
    return true;
  }
  
  /**
   * Processes state messages from the dLabPro recognizer.
   * 
   * @param type
   *          The message type. The method only processes output messages where
   *          <code>type='&lt;'</code>.
   * @param msg
   *          The message.
   * @return <code>true</code> if the message was processed, <code>false</code> otherwise.
   */
  protected boolean processStateMessage(char type, String msg)
  {
    // Special: recognizer wrapper message
    if (type==':' && "Recognizer failed to start".equals(msg))
    {
      recognizer = null;
      recBusy = false;
      recFailure = true;
      echoMessage(type,msg);
      fire(new StateChangedEvent(this,-1));
      return true;
    }
    
    if (type!='<') return false;
    
    // State message
    if ("sta: online recognizer initialized".equals(msg))
    {
      recBusy = false;
      recFailure = false;
      echoMessage(type,msg);
      fire(new StateChangedEvent(this,-1));
      return true;
    }

    Matcher matcher = PAT_STA_BUFSKP.matcher(msg);
    if (matcher.matches())
    {
      // TODO: Fire some event?
      Log.err("WARNING: recognizer skipped speech frames");
      return true;
    }
    
    matcher = PAT_STA_VADCOL.matcher(msg);
    if (matcher.matches())
    {
      // TODO: Fire VAD event?
      return true;
    }
    
    matcher = PAT_STA_RECSKP.matcher(msg);
    if (matcher.matches())
    {
      // TODO: Fire state changed event?
      return true;
    }

    // Dialog state message
    matcher = PAT_STA_VOCCHG.matcher(msg);
    if (matcher.matches())
    {
      int oldDlgState = dlgState;
      dlgState = (int)capturedLong(matcher,1);
      StateChangedEvent event = new StateChangedEvent(this,-1);
      event.listening = oldDlgState==0 && dlgState!=0;
      event.sleeping  = oldDlgState!=0 && dlgState==0;
      fire(event);
      echoMessage(type,msg);
      return true;
    }
    
    // VAD message
    matcher = PAT_DBG_VAD.matcher(msg);
    if (matcher.matches())
    {
      vadActivity = -1;
      if      ("on" .equals(matcher.group(1))) vadActivity = 1; 
      else if ("off".equals(matcher.group(1))) vadActivity = 0;
      long vadFrame = capturedLong(matcher,2);
      fire(new VadEvent(this,vadFrame,vadActivity));
      return true;
    }
    
    return false;
  }
  
  /**
   * Processes recognition messages from the dLabPro recognizer.
   * 
   * @param type
   *          The message type. The method only processes output messages where
   *          <code>type='&lt;'</code>.
   * @param msg
   *          The message.
   * @return <code>true</code> if the message was processed, <code>false</code> otherwise.
   */
  protected boolean processRecognitionMessage(char type, String msg)
  {
    if (type!='<') return false;

    // Recognition state message (start, end, or post)
    Matcher matcher = PAT_STA_REC.matcher(msg);
    if (matcher.matches())
    {
      String cs1 = capturedString(matcher,1);
      if ("start".equals(cs1))
      {
        recBusy = true;
        recEvent = new RecognitionEvent(this);
        recEvent.incremenral = false;
        ppfEvent = null;
        fire(new StateChangedEvent(this,-1));
      }
      else if ("post".equals(cs1) || "end".equals(cs1))
      {
        recBusy = (!"end".equals(cs1));
        fire(new StateChangedEvent(this,-1));
        assert recEvent!=null : "No recognition start message received";
        fireRecognitionEvent();
        recEvent = null;
      }
      return true;
    }

    // Global weight message
    matcher = PAT_DBG_RECGW.matcher(msg);
    if (matcher.matches())
    {
      recEvent.details.setProperty("gw.res",capturedString(matcher,1));
      recEvent.details.setProperty("gw.ref",capturedString(matcher,2));
      return true;
    }

    // Confidence message
    matcher = PAT_DBG_RECNAD.matcher(msg);
    if (matcher.matches())
    {
      float nad  = (float)capturedDouble(matcher,1);
      float ned  = (float)capturedDouble(matcher,2);
      float tnad = (float)capturedDouble(matcher,3);
      float tned = (float)capturedDouble(matcher,4);
      // TODO: Compute confidence! -->
      float l = 0.5f;
      recEvent.confidence = l*Math.max(1-ned/tned,-1)+(1-l)*Math.max(1-nad/tnad,-1);
      // <--
      recEvent.details.setProperty("nad" ,capturedString(matcher,1));
      recEvent.details.setProperty("ned" ,capturedString(matcher,2));
      recEvent.details.setProperty("tnad",capturedString(matcher,3));
      recEvent.details.setProperty("tned",capturedString(matcher,4));
      return true;
    }

    // Recognition result message
    matcher = PAT_CMD_RES.matcher(msg);
    if (matcher.matches())
    {
      recEvent.result   = capturedString(matcher,1);
      recEvent.accepted = "1".equals(capturedString(matcher,2));
      return true;
    }

    // Reference recognition result message
    matcher = PAT_DBG_REFREC.matcher(msg);
    if (matcher.matches())
    {
      recEvent.details.setProperty("reference",capturedString(matcher,1));
      return true;
    }

    // Incremental recognition result message
    matcher = PAT_STA_INCRES.matcher(msg);
    if (matcher.matches())
    {
      RecognitionEvent re = new RecognitionEvent(this);
      re.incremenral = true;
      re.result = capturedString(matcher,1);
      re.text = Fvr.extractComment(re.result);
      fire(re);
      //LCARS.log("USE","inc="+re.result+", txt="+re.text);
      return true;
    }

    return false;
  }
  
  /**
   * Processes a recognition post-processing message from the dLabPro recognizer.
   * 
   * @param type
   *          The message type. The method only processes output messages where
   *          <code>type='&lt;'</code>.
   * @param msg
   *          The message. The method processes only messages starting with <code>"ppf:"</code>.
   * @return <code>true</code> if the message was processed, <code>false</code> otherwise.
   */
  protected boolean processPostprocMessage(char type, String msg)
  {
    if (type!='<') return false;
    
    if ("ppf: start".equals(msg))
    {
      ppfEvent = new PostprocEvent(this);
      return true;
    }
    
    if ("ppf: end".equals(msg))
    {
      assert ppfEvent!=null : "No post-processing start message received";
      fire(ppfEvent);
      Log.info("ppf res="+ppfEvent.result);
      ppfEvent = null;
      return true;
    }
    
    Matcher matcher = PAT_PPF_FRM.matcher(msg);
    if (matcher.matches())
    {
      assert ppfEvent!=null : "No post-processing start message received";
      PostprocEvent.Frame frame = ppfEvent.new Frame();
      frame.recPhn = capturedString(matcher,1);
      frame.refPhn = capturedString(matcher,2);
      frame.lsr    = (float)capturedDouble(matcher,3);
      frame.nll    = (float)capturedDouble(matcher,4);
      frame.recOut = capturedString(matcher,5);
      ppfEvent.addFrame(frame);
      return true;
    }
    
    matcher = PAT_PPF_RES.matcher(msg);
    if (matcher.matches())
    {
      assert ppfEvent!=null : "No post-processing start message received";
      ppfEvent.result = capturedString(matcher,1);
      return true;      
    }

    return false;
  }
  
  /**
   * Processes an option update message from the dLabPro recognizer.
   * 
   * @param type
   *          The message type. The method only processes output messages where
   *          <code>type=':'</code>.
   * @param msg
   *          The message. The method processes only message starting with <code>"Option updated "</code>.
   * @return <code>true</code> if the message was processed, <code>false</code> otherwise.
   */
  protected boolean processOptionUpdateMessage(char type, String msg)
  {
    if (type!=':') return false;

    // Option update wrapper message
    Matcher matcher = PAT_OPT_UPD.matcher(msg);
    return matcher.matches();
  }

  // -- Auxiliary methods --
  
  /**
   * Returns a capturing group.
   * 
   * @param matcher
   *          The matcher.
   * @param group
   *          The index of a capturing group in the matcher's pattern.
   */
  protected String capturedString(Matcher matcher, int group)
  {
    try
    {
      return matcher.group(group);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
  }
  
  /**
   * Returns the long representation of a capturing group.
   * 
   * @param matcher
   *          The matcher.
   * @param group
   *          The index of a capturing group in the matcher's pattern.
   */
  protected long capturedLong(Matcher matcher, int group)
  {
    try
    {
      return Long.valueOf(matcher.group(group));
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return 0;
    }
  }
  
  /**
   * Returns the double precision float representation of a capturing group.
   * 
   * @param matcher
   *          The matcher.
   * @param group
   *          The index of a capturing group in the matcher's pattern.
   */
  protected double capturedDouble(Matcher matcher, int group)
  {
    String s = matcher.group(group);
    if ("nan".equals(s))
      return Double.NaN;
    try
    {
      return Double.valueOf(s);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return 0;
    }
  }
  
  /**
   * Finds the dLabPro recognizer executable. The method first tries to locate
   * the executable in the system path. If unsuccessful it tries to locate the 
   * resource file <code>de/tucottbus/kt/dlabpro/bin/Win32/dlabpro.exe</code>.
   * 
   * @throws FileNotFoundException
   *           If no recognizer executable was found.
   */
  public static File findRecognizerExe()
  throws FileNotFoundException
  {
    try 
    {
      return Executable.findExecutable("recognizer");
    } 
    catch (FileNotFoundException e) 
    {
      String name = "de/tucottbus/kt/dlabpro/bin/Win32/recognizer.exe";
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      URL url = cl.getResource(name);
      if (url==null) throw new FileNotFoundException(name);
      try
      {
        File file = new File(url.getFile());
        if (!file.exists()) throw new FileNotFoundException(name);
        return file;
      }
      catch (Exception e1)
      {
        FileNotFoundException e2 = new FileNotFoundException(name+" ("+e1.toString()+")");
        e2.initCause(e);
        throw e2;
      }
    }
  }
  
}
