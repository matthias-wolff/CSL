package de.tucottbus.kt.csl.hardware.led;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.ESlider;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.elements.modify.EGeometryModifier;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.GArea;
import de.tucottbus.kt.lcars.swt.ColorMeta;

/**
 * Hardware wrapper of mbed LED controller boards.
 * 
 * <h3>TCP Communication Protocol <i style="color:red">&mdash; Draft &mdash;</i></h3>
 * TODO >ri: Re-implement wrapper and controller software to match the following specification!
 * <p>Hardware wrapper and controller board communicate through bidirectional
 * transfer of text lines. There is no request-response synchronization. 
 * Commands of the wrapper and messages from the controller are self-contained.
 * The controller does not send any messages without prior request. Multiple 
 * clients can access the controller board simultaneously.</p>
 * <table cellspacing="0" border="0" style="margin-left: 2em; margin-bottom: 9pt;">
 *   <tr>
 *     <th style="text-align:left; border-top:1pt solid black; border-bottom:0.3pt solid black">Command</th>
 *     <th style="text-align:left; border-top:1pt solid black; border-bottom:0.3pt solid black">Description</th>
 *     <th style="text-align:left; border-top:1pt solid black; border-bottom:0.3pt solid black">Method</th>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"SET &lt;LID&gt; &lt;COLOR&gt;\n"</code></td>
 *     <td style="padding-right:1em;">Sets a single LED color.</td>
 *     <td>{@link #sendSet(int)}</td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"SETALL &lt;COLOR_ARRAY&gt;\n"</code></td>
 *     <td style="padding-right:1em;">Sets all LED colors.</td>
 *     <td>{@link #sendSetAll()}</td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"GET &lt;LID&gt;\n"</code></td>
 *     <td style="padding-right:1em;">Retrieves a single LED color.</td>
 *     <td>{@link #sendGet(int)}</td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"GETALL\n"</code></td>
 *     <td style="padding-right:1em;">Retrieves all LED colors.</td>
 *     <td>{@link #sendGetAll()}</td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"RESET\n"</code></td>
 *     <td style="padding-right:1em;">Resets the controller.</td>
 *     <td>{@link #sendReset()}</td>
 *   </tr>
 *   <tr>
 *     <th style="text-align:left; border-top:0.3pt solid black; border-bottom:0.3pt solid black">Message</th>
 *     <th style="text-align:left; border-top:0.3pt solid black; border-bottom:0.3pt solid black">Description</th>
 *     <th style="text-align:left; border-top:0.3pt solid black; border-bottom:0.3pt solid black"> </th>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"LED &lt;LID&gt; &lt;COLOR&gt;\n"</code></td>
 *     <td style="padding-right:1em;">Notifies on the color of one LED.</td>
 *     <td> </td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em;"><code>"LEDALL &lt;COLOR_ARRAY&gt;\n"</code></td>
 *     <td style="padding-right:1em;">Notifies on the colors of all LEDs.</td>
 *     <td> </td>
 *   </tr>
 *   <tr>
 *     <th style="text-align:left; border-top:0.3pt solid black; border-bottom:0.3pt solid black">Value</th>
 *     <th style="text-align:left; border-top:0.3pt solid black; border-bottom:0.3pt solid black">Description</th>
 *     <th style="text-align:left; border-top:0.3pt solid black; border-bottom:0.3pt solid black">Range</th>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em"><code>&lt;LID&gt;</code></td>
 *     <td style="padding-right:1em">LED ID as two-digit hexadecimal,
 *       <code>00</code>...<code>1F</code>: LEDs, <code>20</code>: ambient light.</td>
 *     <td><code>00</code>...<code>20</code></td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em"><code>&lt;COLOR&gt;</code></td>
 *     <td style="padding-right:1em">RGB color value as six-digit hexadecimal.</td>
 *     <td><code>000000</code>...<code>FFFFFF</code></td>
 *   </tr>
 *   <tr>
 *     <td style="padding-right:1em; border-bottom:1pt solid black;"><code>&lt;COLOR_ARRAY&gt;</code></td>
 *     <td style="padding-right:1em; border-bottom:1pt solid black;">Comma-separated sequence of 33 <code>COLOR</code> values.</td>
 *     <td style="border-bottom:1pt solid black;"> </td>
 *   </tr>
 * </table>
 * 
 * @author Martin Birth
 * @author Matthias Wolff
 */
public abstract class ALedController extends AAtomicHardware implements Runnable
{  

  private static final int PORT = 55000;
  
  private static final String[] PREFIX = {"LED", "RES", "ARR", "DEM"};

  private static final String SUFFIX = ";";

  private ColorMeta[] colors = new ColorMeta[33];
  
  private ColorMeta[] defColors = new ColorMeta[colors.length];

  /**
   * Flag indicating that at least one color has changed. 
   */
  private boolean changed = false;
  
  private Thread guard = null;
  
  private boolean runGuard = false;
  
  private Socket socket = null;
  
  private BufferedWriter socketWriter = null;
  
  /**
   * The last error message.
   */
  private String lastError;
  
  private boolean runLampTest = false;
  
  /**
   * The LCARS GUI for this IP LED controller.
   */
  protected volatile LcarsSubPanel subPanel;

  // -- Wrapper life cycle, Implementation of abstract AHardware methods --

  /**
   * Creates a new LED controller hardware wrapper.
   */
  protected ALedController() 
  {
    // Initialize color arrays
    for (int i=0; i<defColors.length-1; i++)
      defColors[i] = ColorMeta.BLACK;
    defColors[defColors.length-1] = ColorMeta.WHITE;
    resetColors();

    // Start the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }
  
  @Override
  public void run()
  {
    log("Begin of guard thread");
    runGuard = true;
    final int sleepMillis = 10;
    int ctr = 0;
    
    while (runGuard)
      try 
      {
        // - Initialize connection
        socket = new Socket(getIP(), PORT);
        socketWriter 
          = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        log("Connected to "+getIP()+":"+PORT);
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);

        // - Run connection
        while (runGuard) 
        {
          try { Thread.sleep(sleepMillis); } catch (InterruptedException e) {}
          if (!runGuard)
            break;
          if (changed)
            sendAllColors();
          
          ctr+=sleepMillis;
          if (ctr>=1000)
          {
            setChanged();
            notifyObservers(NOTIFY_STATE);
            ctr = 0;
          }
        }

        // - End connection
        resetColors();
        sendAllColors();
        socketWriter.close();
        socket.close();
        socketWriter = null;
        socket = null;
        log("Disconnected");
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
      } 
      catch (Exception e) 
      {
        logErr(e.getMessage(),e);
        try { socketWriter.close(); } catch (Exception e1) {}
        try { socket.close(); } catch (Exception e1) {}
        socketWriter = null;
        socket = null;
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try { Thread.sleep(2500); } catch (InterruptedException e1) {}
      }

    log("End of guard thread");
  }
  
  @Override
  public void dispose() 
  {
    stopLampTest();
    try { reset(); } catch (Exception e) {}
    
    if (guard!=null)
    {
      runGuard = false;
      try 
      {
        guard.interrupt();
        guard.join();
      } 
      catch (Exception e) 
      { 
        logErr("",e); 
      }
    }
    guard = null;
    super.dispose();
  }

  @Override
  public boolean isConnected() 
  {
    if (isDisposed()) return false;
    if (!runGuard) return false;
    if (socket==null) return false;
    return socket.isConnected();
  }

  @Override
  public synchronized ElementContributor getLcarsSubpanel(int x, int y)
  {
    if (subPanel==null)
      subPanel = new LcarsSubPanel(x, y);
    return subPanel;
  }
  
  // -- Method overrides --

  @Override
  protected void log(char type, String message, Throwable throwable)
  {
    if (type==LOG_ERROR)
    {
      lastError = message;
      setChanged();
      notifyObservers(NOTIFY_DETAIL);
    }
    super.log(type, message, throwable);
  }

  // -- Abstract API --
  
  protected abstract String getIP();
  
  // -- Public API --

  /**
   * Sets the color of one LED.
   * 
   * @param ledId
   *          The LED id in the range [0..31].
   * @param color
   *          The color to set, may be <code>null</code> in which case the
   *          default color as obtained through {@link #getDefaultColor}
   *          <code>(ledId)</code> will be selected.
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   * @throws IllegalArgumentException
   *           if the LED id is invalid (i.e. &lt;0 or &gt;31)
   */
  public void setColor(int ledId, ColorMeta color) 
  throws IllegalStateException, IllegalArgumentException
  {
    if(!isConnected()||!runGuard)
      throw new IllegalStateException("Wrapper " + (isDisposed() ? "disposed" : "not connected"));
    if (ledId < 0 || ledId > 31)
      throw new IllegalArgumentException();

    if (color == null)
      color = getDefaultColor(ledId);
    colors[ledId] = color;
    changed = true;
  }

  /**
   * Returns the current color of one LED.
   * 
   * @param ledId
   *          The LED id in the range [0..31].
   * @throws IllegalStateException
   *           if the wrapper is disposed.
   * @throws IllegalArgumentException
   *           if the LED id is invalid (i.e. &lt;0 or &gt;31)
   */
  public ColorMeta getColor(int ledId) 
  throws IllegalStateException, IllegalArgumentException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");
    if (ledId < 0 || ledId > 31)
      throw new IllegalArgumentException();

    return colors[ledId];
  }  

  /**
   * Sets the default color of one LED.
   * 
   * @param ledId
   *          The LED id in the range [0..31].
   * @param color
   *          The color to set.
   * @throws IllegalStateException
   *           if the wrapper is disposed.
   * @throws IllegalArgumentException
   *           if the LED id is invalid (i.e. &lt;0 or &gt;31) or if
   *           <code>color</code> is <code>null</code>.
   */
  public void setDefaultColor(int ledId, ColorMeta color)
  throws IllegalStateException, IllegalArgumentException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");
    if (color == null || ledId < 0 || ledId > 31)
      throw new IllegalArgumentException();

    defColors[ledId] = color;
  }

  /**
   * Returns the default color of one LED.
   * 
   * @param ledId
   *          The LED id in the range [0..31].
   * @throws IllegalStateException
   *           if the wrapper is disposed.
   * @throws IllegalArgumentException
   *           if the LED id is invalid (i.e. &lt;0 or &gt;31)
   */
  public ColorMeta getDefaultColor(int ledId) 
  throws IllegalStateException, IllegalArgumentException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");
    if (ledId < 0 || ledId > 31)
      throw new IllegalArgumentException();

    return defColors[ledId];
  }

  /**
   * Sets the color of the ambient light.
   * 
   * @param color
   *          The color to set, may be <code>null</code> in which case the
   *          default color as obtained through {@link #getDefaultAmbientColor()}
   *          will be selected.
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   */
  public void setAmbientColor(ColorMeta color)
  throws IllegalStateException
  {
    if(!isConnected()||!runGuard)
        throw new IllegalStateException("Wrapper " + (isDisposed() ? "disposed" : "not connected"));
    if (color == null)
      color = getDefaultAmbientColor();

    colors[colors.length-1] = color;
    changed = true;
  }
  
  /**
   * Returns the color of the ambient light.
   * 
   * @throws IllegalStateException
   *           if the wrapper is disposed.
   */
  public ColorMeta getAmbientColor()
  throws IllegalStateException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");

    return colors[colors.length-1];
  }

  /**
   * Sets the default ambient light color.
   * 
   * @param color
   *          The color to set.
   * @throws IllegalStateException
   *           if the wrapper is disposed.
   * @throws IllegalArgumentException
   *           if <code>color</code> is <code>null</code>.
   */
  public void setDefaultAmbientColor(ColorMeta color)
  throws IllegalStateException, IllegalArgumentException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");
    if (color == null)
      throw new IllegalArgumentException();

    defColors[colors.length-1] = color;
  }

  /**
   * Returns the default color of the ambient light.
   * 
   * @throws IllegalStateException
   *           if the wrapper is disposed.
   */
  public ColorMeta getDefaultAmbientColor() 
  throws IllegalStateException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");

    return defColors[colors.length-1];
  }

  /**
   * Resets this wrapper and sends a hardware reset command to the controller
   * board.
   * 
   * <p><b>Note:</b> The method blocks until reset request has been completed by
   * the hardware.</p>
   * 
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   * @throws HardwareException
   *           on hardware or communication failures.
   */
  public void reset()
  throws IllegalStateException, HardwareException
  {
    sendReset();
    resetColors();
  }

  // -- Mid-level communication - LED controller board commands --

  /**
   * Sends a single color.
   * 
   * @param ledId
   *          The LED id, 0..31 for a LED, 32 for the ambient light.
   * @throws IllegalArgumentException
   *           if <code>ledID</code> is out of range.
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   * @throws HardwareException
   *           on hardware or communication failures.
   */
  protected void sendColor(int ledId) 
  throws IllegalArgumentException, IllegalStateException, HardwareException
  {
    if (ledId < 0 || ledId > 32)
      throw new IllegalArgumentException();

    String output = PREFIX[0] + intToHexDigitString(ledId)
    + intToHexDigitString(colors[ledId].getRed()) 
    + intToHexDigitString(colors[ledId].getGreen())
    + intToHexDigitString(colors[ledId].getBlue())
    + SUFFIX;
    
    // Send LED id and blue, green, and red value
    sendLedCommand(output);
    setChanged();
    notifyObservers(NOTIFY_STATE);
  }

  /**
   * Send the reset command.
   * 
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   * @throws HardwareException
   *           on hardware or communication failures.
   */
  protected void sendReset()
  throws IllegalStateException, HardwareException
  {
    sendLedCommand(PREFIX[1]);
    try { Thread.sleep(300); } catch (InterruptedException e) {}
    setChanged();
    notifyObservers(NOTIFY_STATE);
  }
  
  /**
   * Sends all colors.
   * 
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   * @throws HardwareException
   *           on hardware or communication failures.
   */
  protected void sendAllColors()
  throws IllegalStateException, HardwareException
  {
    sendLedCommand(PREFIX[2]+colorArrayToString(colors)+SUFFIX);
    setChanged();
    notifyObservers(NOTIFY_STATE);
  }

  // -- Low level communication (TCP/IP I/O) --

  /**
   * Sends a command to the controller.
   * 
   * @param command
   *          The command
   * @throws IllegalStateException
   *           if the wrapper is not connected to the hardware or disposed.
   * @throws HardwareException
   *           on hardware or communication failures.
   */
  protected synchronized void sendLedCommand(String command)
  throws IllegalStateException, HardwareException
  {
    check();
    try
    {
      socketWriter.write(command);
      socketWriter.flush();
    }
    catch (IOException e)
    {
      throw new HardwareException("I/O error",e);
    }
    catch (NullPointerException e)
    {
      throw new HardwareException("No output stream",e);
    }
  }

  // -- Helpers --
  
  protected void resetColors() 
  {
    for (int i=0; i<colors.length; i++)
      colors[i] = defColors[i];
    changed = true;
  }

  protected String intToHexDigitString(int value) 
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(Integer.toHexString(value));
    if (sb.length() < 2) 
    {
      sb.insert(0, '0'); // pad with leading zero if needed
    }
    return sb.toString();
  }
  
  protected String colorArrayToString(ColorMeta[] colors) 
  {
    StringBuilder linArray = new StringBuilder();

    for (int i=0; i<colors.length; i++) 
    {
      linArray.append(intToHexDigitString(colors[i].getRed()));
      linArray.append(intToHexDigitString(colors[i].getGreen()));
      linArray.append(intToHexDigitString(colors[i].getBlue()));
    }
    
    return linArray.toString();
  }
  
  // -- Demo and test API --
  
  protected static final ColorMeta[] testColors =
    { 
      new ColorMeta(0x000000),
      new ColorMeta(0xFF0000), new ColorMeta(0x7F0000), new ColorMeta(0x7F7F00),
      new ColorMeta(0x007F00), new ColorMeta(0x007F7F), new ColorMeta(0x00007F),
      new ColorMeta(0x7F007F), new ColorMeta(0x7F7F7F), new ColorMeta(0x3F3F3F) 
    };

  private Thread lampTestThread = null;
  
  // TODO: on board thread not working yet
  public void startOnboardDemoMode()
  throws IllegalStateException, HardwareException
  {
    String output = PREFIX[3]+SUFFIX;
    sendLedCommand(output);
    log("Start LED onBoard Demo ("+PREFIX[3]+" of "+getName()+")");
  }
  
  public void startLampTest(int inverval)
  {
    if (lampTestThread!=null) 
      return;
    resetColors();
    runLampTest = true;
    final int millis = Math.max(inverval,50);
    lampTestThread = new Thread()
    {
      @Override
      public void run()
      {
        while (runLampTest)
          for (ColorMeta color : testColors)
          {
            for (int i=0; i<32; i++)
              setColor(i,color);
            setAmbientColor(color);
            try { sleep(millis); } catch (InterruptedException e) { }
            if (!runLampTest)
              break;
          }
      }
    };
    lampTestThread.setName(getClass().getSimpleName()+" lamp test");
    lampTestThread.start();
  }
  
  public void stopLampTest()
  {
    runLampTest = false;
    if (lampTestThread!=null)
    {
      lampTestThread.interrupt();
      try { lampTestThread.join(); } catch (InterruptedException e) {}
    }
    lampTestThread = null;
    resetColors();
  }

  public boolean isLampTestRunning()
  {
    return runLampTest && lampTestThread!=null && lampTestThread.isAlive();
  }
  
  // -- LCARS Element Contributor --
  
  protected static class ERgbSlider extends ESlider
  {
    public ERgbSlider(int x, int y, int w, int h)
    {
      super(x,y,w,h,LCARS.ES_NONE,0);
      setMinMaxValue(0,255);
      setValue(0f);
      addScaleTick((float)0x00,"00",LCARS.EF_TINY);
      addScaleTick((float)0x33,"33",LCARS.EF_TINY);
      addScaleTick((float)0x66,"66",LCARS.EF_TINY);
      addScaleTick((float)0x99,"99",LCARS.EF_TINY);
      addScaleTick((float)0xCC,"CC",LCARS.EF_TINY);
      addScaleTick((float)0xFF,null,LCARS.EF_TINY);
    }
  }

  protected static class LinkedColor extends Observable
  {
    private ColorMeta color = null;
    
    protected void link(ColorMeta color)
    {
      if (this.color==color)
        return;
      this.color = color;
      setChanged();
      notifyObservers(this.color);
    }
   
    protected void unlink()
    {
      if (this.color==null)
        return;
      this.color=null;
      setChanged();
      notifyObservers(this.color);
    }
    
    protected boolean isLinked()
    {
      return color!=null;
    }
  }

  protected static LinkedColor linkedColor = new LinkedColor();
  
  public class LcarsSubPanel extends ElementContributor implements Observer
  {
    private EElbo        eLampTest;
    private ERect        eLampTestFast;
    private ERect        eSelAll;
    private ERect        eSelNone;
    private ERect        eReset;
    private ERect        eAllOff;
    private EElbo        eApply;
    private EValue       eTitle;
    private ERect[]      eLedAct = new ERect[32];
    private ERect[]      eLedSta = new ERect[32];
    private ERect        eAmbAct;
    private ERect        eAmbSta;
    private ERect        eLink;
    private EValue[]     eRgbUp = new EValue[3];
    private ERgbSlider[] eRgbSliders = new ERgbSlider[3];
    private EValue[]     eRgbDown = new EValue[3];
    private ERect        eSelThru;
    private ELabel       eError;
    private int          lastMicAct  = -1;

    public LcarsSubPanel(int x, int y) 
    {
      super(x,y);

      final int bw = 70;
      final int bh = 70; // Must be even!
      final int lw = 20;
      final int lh = 20;
      final int ax = 1220-11*(bw+3)+3;  
      final int ay = 47;
      
      // The upper frame
      eLampTest = new EElbo(null,0,0,ax-3,bh+3+bh/2+45,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_NW|LCARS.ES_LABEL_SE|LCARS.ES_DISABLED,"LAMP TEST");
      eLampTest.setArmWidths(ax-bw-6,44); eLampTest.setArcWidths(150,bw+8);
      eLampTest.addGeometryModifier(new EGeometryModifier() 
      {
        @Override
        public void modify(ArrayList<AGeometry> geos) 
        {
          Area are = new Area(((GArea)geos.get(0)).getArea());
          Rectangle r = are.getBounds();
          are.subtract(new Area(new Rectangle(r.x,r.y+r.height-bh-3,bw+3,bh+3)));
          ((GArea)geos.get(0)).setShape(are);
        }
      });
      eLampTest.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          if (isLampTestRunning())
            stopLampTest();
          else
            startLampTest(eLampTestFast.isSelected()?50:1000);
        }
      });
      add(eLampTest);
      
      eLampTestFast = new ERect(null,0,bh/2+48,bw,bh,LCARS.EC_ELBOUP|LCARS.ES_LABEL_SE|LCARS.ES_DISABLED,"FAST");
      eLampTestFast.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          ee.el.setSelected(!ee.el.isSelected());
          if (isLampTestRunning())
            stopLampTest();
          startLampTest(ee.el.isSelected()?50:1000);
        }
      });
      add(eLampTestFast);

      eTitle = new EValue(null,ax,0,8*(bw+3)-3,44,LCARS.EC_ELBOUP|LCARS.ES_LABEL_W|LCARS.ES_STATIC,getIP()+":"+PORT);
      eTitle.setValue(getName());
      eTitle.setValueMargin(24);
      add(eTitle);

      eReset = new ERect(null,0,bh+3+bh/2+48,(ax-bw-6)/2-1,bh,LCARS.EC_ELBOUP|LCARS.ES_LABEL_NE|LCARS.ES_DISABLED,"RESET");
      eReset.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          stopLampTest();
          try 
          {
            reset();
          } 
          catch (IllegalStateException | HardwareException e) 
          {
            logErr(e.getMessage(),e);
          }
        }
      });
      add(eReset);

      eAllOff = new ERect(null,0,2*(bh+3)+bh/2+48,(ax-bw-6)/2-1,bh,LCARS.EC_ELBOUP|LCARS.ES_LABEL_NE|LCARS.ES_DISABLED,"ALL OFF");
      eAllOff.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          for (int i=0; i<32; i++)
            setColor(i,ColorMeta.BLACK);
          setAmbientColor(ColorMeta.BLACK);
        }
      });
      add(eAllOff);
      
      eSelAll = new ERect(null,(ax-bw-6)/2+2,bh+3+bh/2+48,(ax-bw-6)/2-2,bh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_NE|LCARS.ES_DISABLED,"SEL ALL");
      eSelAll.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchUp(EEvent ee) 
        {
          for (int i=0; i<32; i++)
            eLedAct[i].setSelected(true);
        }
      });
      add(eSelAll);

      eSelNone = new ERect(null,(ax-bw-6)/2+2,2*(bh+3)+bh/2+48,(ax-bw-6)/2-2,bh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_NE|LCARS.ES_DISABLED,"SEL NONE");
      eSelNone.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchUp(EEvent ee) 
        {
          for (int i=0; i<32; i++)
            eLedAct[i].setSelected(false);
        }
      });
      add(eSelNone);
      
      // The button array
      eSelThru = new ERect(null,ax-bw-3,ay,bw,4*(bh+3)-3,LCARS.EC_PRIMARY|LCARS.ES_RECT_RND_W|LCARS.ES_DISABLED,"THRU");
      eSelThru.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          ee.el.setSelected(!ee.el.isSelected());
        }
      });
      add(eSelThru);
      
      for (int i=0; i<4; i++)
        for (int j=0; j<8; j++)
        {
          int n = i*8+j;

          eLedAct[n] = new ERect(null,ax+j*(bw+3),ay+i*(bh+3),bw,bh,LCARS.EC_SECONDARY|LCARS.ES_DISABLED,String.valueOf(n+1));
          eLedAct[n].addGeometryModifier(new EGeometryModifier()
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
          eLedAct[n].addEEventListener(new EEventListenerAdapter()
          {
            @Override
            public void touchDown(EEvent ee)
            {
              int n = (Integer)ee.el.getData();
              if (eSelThru.isSelected() && lastMicAct>=0)
              {
                if (n>lastMicAct)
                  for (int i=lastMicAct+1; i<=n; i++)
                    eLedAct[i].setSelected(eLedAct[lastMicAct].isSelected());
                else
                  for (int i=lastMicAct-1; i>=n; i--)
                    eLedAct[i].setSelected(eLedAct[lastMicAct].isSelected());
                eSelThru.setSelected(false);
              }
              else
                ee.el.setSelected(!ee.el.isSelected());
              lastMicAct = n;
            }
          });
          eLedAct[n].setData(new Integer(n));
          add(eLedAct[n]);
          
          eLedSta[n] = new ERect(null,ax+j*(bw+3)+bw-lw+1,ay+i*(bh+3)+bh-lh+1,lw-1,lh-1,LCARS.ES_STATIC|LCARS.ES_DISABLED,null);
          add(eLedSta[n]);          
        }
      
      // The RGB sliders
      for (int i=0; i<3; i++)
      {
        eRgbUp[i] = new EValue(null,ax+(8+i)*(bw+3),0,bw,44,LCARS.EC_ELBOUP,null);
        eRgbUp[i].addGeometryModifier(new EGeometryModifier() 
        {
          @Override
          public void modify(ArrayList<AGeometry> geos) 
          {
            Area are = new Area(((GArea)geos.get(0)).getArea());
            Rectangle r = are.getBounds();
            int cx = r.x+15;
            int cy = r.y+r.height-11;
            Shape triangle = new Polygon(new int[]{cx-9,cx+9,cx},new int[]{cy+4,cy+4,cy-4},3);
            geos.add(new GArea(new Area(triangle),true));
          }
        });
        eRgbUp[i].setValueMargin(0);
        eRgbUp[i].setValue(i==0?"R":(i==1?"G":"B"));
        eRgbUp[i].addEEventListener(new EEventListenerAdapter()
        {
          @Override
          public void touchDown(EEvent ee) 
          {
            incRgb(ee.el);
          }

          @Override
          public void touchHold(EEvent ee) 
          {
            if (ee.ct<5) return;
            incRgb(ee.el);
          }
        });
        add(eRgbUp[i]);
        
        eRgbSliders[i] = new ERgbSlider(ax+(8+i)*(bw+3),ay+17,bw,4*(bh+3)-38);
        eRgbSliders[i].setDisabled(true);
        eRgbSliders[i].addSelectionListener((value)->
        {
          onRgbChanged();
        });
        add(eRgbSliders[i]);

        eRgbDown[i] = new EValue(null,ax+(8+i)*(bw+3),ay+4*(bh+3),bw,44,LCARS.EC_ELBOUP,null);
        eRgbDown[i].addGeometryModifier(new EGeometryModifier() 
        {
          @Override
          public void modify(ArrayList<AGeometry> geos) 
          {
            Area are = new Area(((GArea)geos.get(0)).getArea());
            Rectangle r = are.getBounds();
            int cx = r.x+15;
            int cy = r.y+r.height-11;
            Shape triangle = new Polygon(new int[]{cx-9,cx+9,cx},new int[]{cy-4,cy-4,cy+4},3);
            geos.add(new GArea(new Area(triangle),true));
          }
        });
        eRgbDown[i].setValueMargin(0);
        eRgbDown[i].setValue("00"); eRgbDown[i].setValueWidth(36);
        eRgbDown[i].addEEventListener(new EEventListenerAdapter()
        {
          @Override
          public void touchDown(EEvent ee) 
          {
            incRgb(ee.el);
          }

          @Override
          public void touchHold(EEvent ee) 
          {
            if (ee.ct<5) return;
            incRgb(ee.el);
          }
        });
        add(eRgbDown[i]);
      }
      
      // The lower frame
      eApply = new EElbo(null,0,ay+3*(bh+3)+bh/2+1,ax-3,bh/2+46,LCARS.EC_ELBOUP|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE|LCARS.ES_SELDISED,"APPLY");
      eApply.setArmWidths(ax-bw-6,44); eApply.setArcWidths(150,bw+8);
      eApply.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          applySelectedColor();
        }
      });
      add(eApply);

      eAmbAct = new ERect(null,ax,ay+4*(bh+3),6*(bw+3)-3,44,LCARS.EC_SECONDARY|LCARS.ES_LABEL_W|LCARS.ES_DISABLED,"AMBIENT");
      eAmbAct.addGeometryModifier(new EGeometryModifier()
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
      eAmbAct.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchUp(EEvent ee) 
        {
          ee.el.setSelected(!ee.el.isSelected());
        }
      });
      add(eAmbAct);

      eAmbSta = new ERect(null,ax+5*(bw+3)+bw-lw+1,ay+4*(bh+3)+44-lh+1,lw-1,lh-1,LCARS.ES_STATIC|LCARS.ES_DISABLED,null);
      add(eAmbSta);
      
      eLink = new ERect(null,ax+6*(bw+3),ay+4*(bh+3),2*(bw+3)-3,44,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E,"LINK");
      eLink.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchUp(EEvent ee) 
        {
          if (!ee.el.isBlinking())
            linkedColor.link(getSelectedColor());
          else
            linkedColor.unlink();
        }
      });
      add(eLink);

      // The error display
      eError = new ELabel(null,464,ay+4*(bh+3)+47,756,20,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_STATIC|LCARS.EF_SMALL,null);
      eError.setColor(COLOR_ERROR);
      add(eError); 
    }

    @Override
    public void addToPanel(Panel panel) 
    {
      addObserver(this);
      super.addToPanel(panel);
      linkedColor.addObserver(this);
    }

    @Override
    public void removeFromPanel() 
    {
      deleteObserver(this);
      super.removeFromPanel();
      linkedColor.deleteObserver(this);
    }

    @Override
    public void update(Observable o, Object arg) 
    {
      if (o instanceof LinkedColor)
      {
        ColorMeta color = (ColorMeta)arg;
        eLink.setBlinking(color!=null);
        if (color!=null)
        {
          eRgbSliders[0].setValue(color.getRed());
          eRgbSliders[1].setValue(color.getGreen());
          eRgbSliders[2].setValue(color.getBlue());
          eRgbDown[0].setValue(String.format("%02X",color.getRed()));
          eRgbDown[1].setValue(String.format("%02X",color.getGreen()));
          eRgbDown[2].setValue(String.format("%02X",color.getBlue()));
          applySelectedColor();
        }      
      }
      else if (o instanceof ALedController)
      {
        //String hint = (String)arg;
        boolean connected = isConnected();
        
        // Set enabled/disabled states
        forAllElements(new Consumer<EElement>() 
        {
          @Override
          public void accept(EElement el) 
          {
            if (el==eRgbDown[0]) return;
            if (el==eRgbDown[1]) return;
            if (el==eRgbDown[2]) return;
            if (el==eLink      ) return;
            if (el==eError     ) return;
            if 
            (
              el==eTitle    ||
              el==eRgbUp[0] ||
              el==eRgbUp[1] ||
              el==eRgbUp[2]
            )
            {
              el.setColor(connected?null:COLOR_ERROR);
              el.setBlinking(!connected);
            }
            else
              el.setDisabled(!connected);
          }
        });
        if (connected)
        {
          eLampTest.setBlinking(isLampTestRunning());
          eError.setLabel(null);
        }
        else
        {
          eLampTest.setBlinking(false);
          eError.setLabel(lastError!=null&&lastError.length()>0?lastError.toUpperCase():"(ERROR)");
        }

        // Set lamp indicator colors
        for (int i=0; i<32; i++)
          eLedSta[i].setColor(connected?getColor(i):null);
        eAmbSta.setColor(connected?getAmbientColor():null);
        
      }
    }

    private void applySelectedColor()
    {
      if (!isConnected())
        return;
      ColorMeta color = getSelectedColor();
      for (int i=0; i<32; i++)
        if (eLedAct[i].isSelected())
          setColor(i,color);
      if (eAmbAct.isSelected())
        setAmbientColor(color);
    }
    
    private ColorMeta getSelectedColor()
    {
      int r = Math.round(eRgbSliders[0].getValue());
      int g = Math.round(eRgbSliders[1].getValue());
      int b = Math.round(eRgbSliders[2].getValue());
      return new ColorMeta(r,g,b);
    }
    
    private void onRgbChanged()
    {
      ColorMeta color = getSelectedColor();
      eRgbDown[0].setValue(String.format("%02X",color.getRed()));
      eRgbDown[1].setValue(String.format("%02X",color.getGreen()));
      eRgbDown[2].setValue(String.format("%02X",color.getBlue()));
      if (linkedColor.isLinked())
        linkedColor.link(color);
      applySelectedColor();
    }
  
    private void incRgb(EElement el)
    {
      int i = -1;
      int j = 0;
      if      (el==eRgbUp  [0]) { i=0; j= 1; }
      else if (el==eRgbUp  [1]) { i=1; j= 1; }
      else if (el==eRgbUp  [2]) { i=2; j= 1; }
      else if (el==eRgbDown[0]) { i=0; j=-1; }
      else if (el==eRgbDown[1]) { i=1; j=-1; }
      else if (el==eRgbDown[2]) { i=2; j=-1; }
      else return;
      int pos = Math.round(eRgbSliders[i].getValue());
      pos = Math.max(0,Math.min(255,pos+j));
      eRgbSliders[i].setValue(pos);
      onRgbChanged();
    }
  }

  // -- LCARS Low-level Hardware Access Panel --

  public static class LedControllersPanel extends HardwareAccessPanel
  {
    public LedControllersPanel(IScreen iscreen) 
    {
      super(iscreen, ALedController.class);
    }

    @Override
    protected void createSubPanels() 
    {
      LedControllerViewer.getInstance().getLcarsSubpanel(677,140).addToPanel(this);
      LedControllerCeiling.getInstance().getLcarsSubpanel(677,555).addToPanel(this);
    }
  }  
  
  // == Main method ==

  /**
   * Starts LCARS with the {@link LedControllersPanel}.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",LedControllersPanel.class.getName());
    LCARS.main(args);
  }
  
}
