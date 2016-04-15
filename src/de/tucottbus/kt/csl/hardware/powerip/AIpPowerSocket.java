package de.tucottbus.kt.csl.hardware.powerip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Observable;
import java.util.Observer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.telnet.TelnetClient;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.EValue;

/**
 * Low-level hardware wrapper of NETIO-230B and NETIO4 IP power sockets.
 * 
 * @author Matthias Wolff
 * @author Christian Richter
 */
public abstract class AIpPowerSocket extends AAtomicHardware
implements Runnable
{
  
  /**
   * The IP address of the power socket wrapped by this instance.
   */
  protected String ip;
  
  /**
   * The port used for the Telnet connection.
   */
  protected final int port = 1234;

  /**
   * Number of milliseconds to wait for Telnet reply. The value also controls 
   * the polling interval of the port states and the interval of re-connect 
   * attempts. 500 ms is a reasonable choice.
   */
  protected final int replyTimeout = 500;
  
  /**
   * The Telnet client of this IP power socket.
   */
  protected TelnetClient tc;
  
  /**
   * Manages the Telnet connection to the IP power socket.
   */
  protected Thread guard;
  
  /**
   * Flag indicating that the wrapper is connected to the IP power socket.
   */
  private boolean connected;
  
  /**
   * The most recently known state of the ports.
   */
  private final boolean[] portStates = new boolean[4];
  
  /**
   * Human-readable names of the ports.
   */
  private String[] portNames;
  
  /**
   * Flag indicating that {@link #waitingForMessage} is waiting for the next
   * line of text received via Telnet. 
   */
  private boolean waitingForMessage;
  
  /**
   * Message buffer used by {@link #readMessage()}.
   */
  private String message;
  
  /**
   * The last message received from the IP power socket.
   */
  private String lastMessage;
  
  /**
   * The last error message.
   */
  private String lastError;
  
  /**
   * Telnet return time.
   */
  private long replyTime = 0;
  
  /**
   * The LCARS GUI for this IP power socket.
   */
  protected volatile LcarsSubPanel subPanel;
  
  // -- Telnet message constants --
  
  protected final Pattern PAT_HELLO           = Pattern.compile("100 HELLO (.*?)\\s*?");
  protected final Pattern PAT_BYE             = Pattern.compile("110 BYE\\s*?");
  protected final Pattern PAT_OK              = Pattern.compile("250 OK\\s*?");
  protected final Pattern PAT_PORT            = Pattern.compile("250 ([01])\\s*?");
  protected final Pattern PAT_PORTLIST        = Pattern.compile("250 ([01])([01])([01])([01])\\s*?");
  protected final Pattern PAT_ALREADYLOGGEDIN = Pattern.compile("504 ALREADY LOGGED IN\\s*?");
  
  // -- Wrapper life cycle, Implementation of abstract AHardware methods --
  
  /**
   * Creates a new power socket wrapper. Applications must invoke {@link 
   * #dispose()} to free system resources allocated by this wrapper.
   * 
   * @param ip
   *          The IP address of the socket, e. g. "141.43.71.12".
   */
  public AIpPowerSocket(String ip, String[] portNames)
  {
    this.ip = ip;
    if (portNames!=null)
    {
      if (portNames.length!=4)
        throw new IllegalArgumentException("portNames must contain exactly 4 elements");
      this.portNames = portNames;
    }
    else
      this.portNames = new String[]
        { "PORT 1", "PORT 2", "PORT 3", "PORT 4" };
    
    // Create the Telnet client
    tc = new TelnetClient("ANSI");
    
    // Start the guard (lazily connects to IP power socket) 
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }

  @Override
  public void run()
  {
    log("Begin of guard thread");

    Thread reader = null;
    while (tc!=null)
      try
      {
        // Create and start Telnet reader thread
        if (reader==null || !reader.isAlive())
        {
          reader = new Thread(new Runnable()
          {
            @Override
            public void run()
            {
              log("Begin of reader thread");
              while(true)
                try
                {
                  // Wait while not (yet) connected
                  while (!tc.isConnected())
                    try
                    {
                      log("Reader thread asleep (max. "+replyTimeout*5+" ms)");
                      Thread.sleep(replyTimeout*5);
                      log("Reader thread woke up");
                    } 
                    catch (InterruptedException e1)
                    {
                      log("Reader thread awoken");
                    }
  
                  // Read and dispatch input received from IP power socket
                  String message = "";
                  int ret_read;      
                  try
                  {
                    do
                    {
                      ret_read = tc.getInputStream().read();
                      if (ret_read>=0)
                      {
                        char c = (char)ret_read;
                        if ((c=='\r' || c=='\n') && message.length()>0)
                        {
                          dispatchMessage(message);
                          message = "";
                        }
                        else
                          message += c;
                      }
                    } while (ret_read>=0);
                    break;
                  }
                  catch (NullPointerException e)
                  {
                    throw new HardwareException("No input stream",e);
                  }
                }
                catch (Exception e)
                {
                  logErr(e.getMessage(),e);
                  if (tc==null) break;
                }
              log("End of reader thread");
            }
          },getClass().getSimpleName()+".reader");
          reader.start();
        }

        // Connect to IP power socket
        tc.connect(ip,port);
        reader.interrupt();
        log("Connected to "+ip+":"+port);
        waitForHello();
        login();
        log("Logged in to "+ip+":"+port);
        connected = true;
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        
        // Maintain connection
        while (reader.isAlive())
        {
          try
          {
            portList(null,true);
            if (!connected)
            {
              connected = true;
              setChanged();
              notifyObservers(NOTIFY_CONNECTION);
            }
          }
          catch (HardwareException e)
          {
            //connected = e.getMessage().startsWith("Unexpected reply \"250 OK\"");
            connected = false;
            setChanged();
            notifyObservers(NOTIFY_CONNECTION);
            logErr(e.getMessage(),e);
          }
          try 
          { 
            Thread.sleep(replyTimeout*2); 
          } 
          catch (InterruptedException e) 
          {
            // Interrupt invoked by dispose()
            log("Guard thread awoken");
            reader.join(replyTimeout*2);
          }
        }
      }
      catch (Exception e)
      {
        connected = false;
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        logErr(e.getMessage(),e);
        if (tc==null) break;
        try { tc.disconnect(); } catch (Exception e1) {}
        try { Thread.sleep(replyTimeout*5); } catch (InterruptedException e1) {}
      }
    
    log("End of guard thread");
  }

  /**
   * Disposed of this wrapper. The method frees all system resources allocated
   * by the wrapper.
   */
  @Override
  public void dispose()
  {
    log("Dispose");
    try
    {
      quit();
    }
    catch (Exception e)
    {
      logErr(e.getMessage(),e);
    }
    try
    {
      guard.interrupt();
      tc.disconnect();
    }
    catch (Exception e)
    {
      logErr(e.getMessage(),e);
    }
    tc = null;
    super.dispose();
  }
  
  @Override
  public boolean isConnected()
  {
    return connected;
  }
  
  @Override
  public String getName()
  {
    try
    {
      String[] as = ip.split("\\.");
      String s = as[as.length-1];
      while (s.length()<3)
        s = "0"+s;
      return "IPS "+s;
    }
    catch (Exception e)
    {
      return "IPS ???";
    }
  }

  @Override
  public AHardware getParent()
  {
    return CslHardware.getInstance();
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
  
  // -- High-level communication - Public API --
 
  /**
   * Switches ports (power outlets) of the IP power socket on or off.
   * 
   * @param port
   *          The port ID (1..4).
   * @param on
   *          The new port state.
   * @throws HardwareException
   *           on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   * @throws IllegalArgumentException
   *           if <code>port</code> is <1 or >4.
   */
  public void setPortOn(int port, boolean on)
  throws IllegalStateException, IllegalArgumentException, HardwareException
  {
    if (port<1 || port>4)
      throw new IllegalArgumentException("Invalid port ID "+port);
    
    char[] portStates = "uuuu".toCharArray();
    portStates[port-1] = on?'1':'0';
    portList(new String(portStates),false);
  }
  
  /**
   * Switches all ports (power outlets) of the IP power socket on or off.
   * 
   * @param on
   *          The new port state.
   * @throws HardwareException
   *           on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   * @throws IllegalArgumentException
   *           if <code>port</code> is <1 or >4.
   */
  public void setAllPortsOn(boolean on)
  throws IllegalStateException, IllegalArgumentException, HardwareException
  {
    String portStates = on?"1111":"0000";
    portList(portStates,false);
  }
  
  /**
   * Determines the state of ports (power outlets) of the IP power socket. The
   * method immediately returns an internally tracked "most recently known"
   * state without actually communicating with the hardware. Hence state changes
   * or failures may be reflected with some delay.
   * 
   * @param port
   *          The port ID (1..4).
   * @throws IllegalArgumentException
   *           if <code>port</code> is <1 or >4.
   */
  public boolean isPortOn(int port)
  throws IllegalArgumentException
  {
    if (port<1 || port>4)
      throw new IllegalArgumentException("Invalid port ID "+port);
    
    return portStates[port-1];
  }  
  
 /**
   * Returns the name of ports (power outlets) of the IP power socket.
   * 
   * @param port
   *          The port ID (1..4).
   * @return The human-readable name.
   * @throws IllegalArgumentException
   *           if <code>port</code> is <1 or >4.
   */
  public String getPortName(int port)
  throws IllegalArgumentException
  {
    if (port<1 || port>4)
      throw new IllegalArgumentException("Invalid port ID "+port);
    
    return portNames[port-1];
  }
  
  // -- Mid-level communication - Telnet commands and messages --
  
  /**
   * Logs in to the IP power socket. The method blocks until the login procedure
   * has completed. Invoking the method if the wrapper is already logged has no
   * effect.
   * 
   * @throws HardwareException
   *           if the login failed and on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   * @see #quit()
   */
  protected void login()
  throws HardwareException, IllegalStateException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper disposed");
    String reply = sendCommand("login ktuser kt");
    Matcher matcher = PAT_OK.matcher(reply);
    if (matcher.matches()) return;
    matcher = PAT_ALREADYLOGGEDIN.matcher(reply);
    if (matcher.matches()) return;
    throw new HardwareException("Login failed ("+reply+")");
  }
  
  /**
   * Logs out from the the IP power socket. The method blocks until the logout
   * procedure has completed.
   * 
   * @throws HardwareException
   *           if the logout failed and on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   * @see #login()
   */
  protected void quit()
  throws HardwareException, IllegalStateException
  {
    check();
    String reply = sendCommand("quit");
    Matcher matcher = PAT_BYE.matcher(reply);
    if (matcher.matches()) return;
    throw new HardwareException("Logout failed ("+reply+")");
  }
  
  /**
   * Waits for the "HELLO" message after the connection has been initiated.
   * 
   * @throws HardwareException
   *           on unexpected replies and on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   */
  protected void waitForHello()
  throws HardwareException, IllegalStateException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper disposed");
    String reply = sendCommand(null);
    Matcher matcher = PAT_HELLO.matcher(reply);
    if (matcher.matches()) return;
    throw new HardwareException("Unexpected reply waiting on HELLO ("+reply+")");
  }

  /**
   * Posts a keep-alive command (no operation). The method returns immediately
   * without waiting for a reply from the IP power socket.
   * 
   * @throws HardwareException
   *           on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   */
  protected void noop()
  throws HardwareException, IllegalStateException
  {
    check();
    postCommand("noop");
  }
  
  /**
   * Reads, and optionally sets, the state of all ports.
   * 
   * @param state
   *          A string consisting of exactly four of the characters '0', '1',
   *          'i', and 'u' (see NETIO manual for details). For example "10uu"
   *          means: switch port 1 on, port 2 off, and leave ports 3 and 4 in
   *          their current states. If <code>null</code>, the method will not
   *          change any port states (equivalent to "uuuu").
   * @param sync
   *          If <code>true</code>, the method blocks until a reply is received
   *          from the IP power socket and returns the reply. If 
   *          <code>false</code>, the method will return <code>null</code>
   *          immediately. The reply of the IP power socket is processed through
   *          {@link #processMessage(String)} in this case.
   * @return A string consisting of exactly four of the characters '0' and '1'
   *         indicating the (new) port states if <code>sync</code> was 
   *         <code>true</code> and <code>null</code> otherwise.
   * @throws HardwareException
   *           on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   */
  protected String portList(String state, boolean sync)
  throws HardwareException, IllegalStateException
  {
    check();
    String command = "port list";
    if (state!=null)
      command += " "+state;
    if (!sync)
    {
      postCommand(command);
      return null;
    }
    String reply = sendCommand(command);
    Matcher matcher = PAT_PORTLIST.matcher(reply);
    if (matcher.matches())
    {
      reply = "";
      for (int i=1; i<=4; i++)
      {
        boolean saveState = portStates[i-1]; 
        portStates[i-1]="1".equals(matcher.group(i));
        if (saveState!=portStates[i-1])
          setChanged();
        reply += matcher.group(i);
      }
      notifyObservers(NOTIFY_STATE);
      return reply;
    }
    else
      throw new HardwareException("Unexpected reply \""+reply+"\"");
  }
    
  /**
   * Reads, and optionally sets, the state of one port. The method blocks until
   * a reply is received from the IP power socket or an exception is triggered.
   * 
   * @param port
   *          The port ID (1..4).
   * @param state
   *          The new state, "0" to switch the port off and "1" to switch the
   *          port on.
   * @return The port state, "0" or "1".
   * @throws HardwareException
   *           on hardware or communication failures.
   * @throws IllegalStateException
   *           if the wrapper is not yet connected to the hardware or if the
   *           wrapper is {@linkplain #dispose() disposed}.
   * @throws IllegalArgumentException
   *           if <code>port</code> is <1 or >4 or state is not "0" or "1".
   */
  protected String port(int port, String state)
  throws HardwareException, IllegalStateException, IllegalArgumentException
  {
    check();
    if (port<1 || port>4)
      throw new IllegalArgumentException("Invalid port ID "+port);
    if (state!=null && "0"!=state && "1"!=state)
      throw new IllegalArgumentException("Invalid port state \""+state+"\"");

    String command = "port";
    if (state!=null)
      command += " "+state;
    String reply = sendCommand(command);
    Matcher matcher = PAT_PORT.matcher(reply);
    if (matcher.matches())
    {
      boolean saveState = portStates[port-1]; 
      portStates[port-1]="1".equals(matcher.group(1));
      if (saveState!=portStates[port-1])
      {
        setChanged();
        notifyObservers(NOTIFY_STATE);
      }
      return reply;
    }
    else
      throw new HardwareException("Unexpected reply \""+reply+"\"");
  }
  
  // -- Low level communication - Telnet I/O --

  /**
   * Posts a command via Telnet. The method immediately returns without waiting
   * for a reply.
   * 
   * @param command
   *          The command without tailing line break.
   * @throws HardwareException
   *           on hardware or communication failures.
   * @see #sendCommand(String)
   */
  protected synchronized void postCommand(String command)
  throws HardwareException
  {
    log(LOG_IN,command,null);
    OutputStream os = tc.getOutputStream();
    command += "\n";
    try
    {
      os.write(command.getBytes());
      os.flush();
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
  
  /**
   * Sends a command via Telnet and returns the reply. The method blocks until a
   * reply is received via Telnet.
   * 
   * @param command
   *          The command without tailing line break.
   * @return The reply.
   * @throws HardwareException
   *           on hardware or communication failures.
   * @see #postCommand(String)
   */
  protected synchronized String sendCommand(String command)
  throws HardwareException
  {
    waitingForMessage = true;
    message = null;
    if (command!=null)
      postCommand(command);
    try
    {
      long then = System.currentTimeMillis();
      wait(replyTimeout);
      replyTime = System.currentTimeMillis()-then;
      setChanged();
      notifyObservers(NOTIFY_DETAIL);
    } 
    catch (InterruptedException e)
    {
      logErr(e.getMessage(),e);
    }
    waitingForMessage = false;
    if (message==null)
      throw new HardwareException("No reply");
    return message;
  }
  
  /**
   * Reads a message from the Telnet input stream. A message is a complete line
   * of text. The method block until a message is received.
   * 
   * @return The message
   * @throws HardwareException
   *           on hardware or communication failures.
   */
  protected synchronized String readMessage()
  throws HardwareException
  {
    return sendCommand(null);
  }

  /**
   * Invoked by Telnet reader thread in order to dispatch a message read from
   * the Telnet input stream. A message is a complete line of text. The method
   * first checks if {@link #sendCommand(String)} is waiting for a reply. In
   * this case it will store the message in the {@linkplain #message internal
   * message buffer} and notify {@link #sendCommand(String)}. Otherwise the
   * method will invoke {@link #processMessage(String)} to have the message
   * processed.
   * 
   * @param message
   *          The message to process.
   */
  protected synchronized void dispatchMessage(String message)
  {
    message = message.trim();
    if (this.lastMessage!=message)
    {
      this.lastMessage = message;
      setChanged();
      notifyObservers(NOTIFY_DETAIL);
    }
    this.lastError = "";
    log(LOG_OUT,message,null);
    
    // sendCommand is waiting for a reply
    if (waitingForMessage)
    {
      this.message = message;
      notifyAll();
      return;
    }

    // Process other messages
    if (PAT_OK.matcher(message).matches())
      // Ignore "250 OK"
      return;

    Matcher matcher = PAT_PORTLIST.matcher(message);
    if (matcher.matches())
    {
      for (int i=1; i<=4; i++)
      {
        boolean saveState = portStates[i-1];
        portStates[i-1] = "1".equals(matcher.group(i));
        if (saveState!=portStates[i-1])
          setChanged();
      }
      notifyObservers(NOTIFY_STATE);
      return;
    }

    // TODO: Process any other messages here
    log(LOG_ERROR,"Unprocessed message \""+message+"\"",null);
  }
  
  // -- LCARS Element Contributor --
  
  class LcarsSubPanel extends ElementContributor implements Observer
  {
    protected EElbo    eAllOn;
    protected EElbo    eAllOff;
    protected EValue   eTitle;
    protected EValue   eStatus;
    protected ELabel   eError;
    protected EValue[] ePorts = new EValue[4];

    public LcarsSubPanel(int x, int y)
    {
      super(x, y);
      
      // The frame
      eAllOn = new EElbo(null,0,0,461,70,LCARS.EC_ELBOUP|LCARS.ES_DISABLED|LCARS.ES_SHAPE_NW|LCARS.ES_LABEL_SE,"ALL ON");
      eAllOn.setArmWidths(208,44); eAllOn.setArcWidths(150,60);
      eAllOn.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try { setAllPortsOn(true); } catch (Exception e) {}
        }
      });
      add(eAllOn);

      eTitle = new EValue(null,464,0,756,44,LCARS.EC_ELBOUP|LCARS.ES_LABEL_W|LCARS.ES_STATIC,ip+":"+port);
      eTitle.setValue(getName());
      eTitle.setValueMargin(24);
      add(eTitle);

      eAllOff = new EElbo(null,0,73,461,58,LCARS.EC_ELBOUP|LCARS.ES_DISABLED|LCARS.ES_SELECTED|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_NE,"ALL OFF");
      eAllOff.setArmWidths(208,32); eAllOff.setArcWidths(150,60);
      eAllOff.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try { setAllPortsOn(false); } catch (Exception e) {}
        }
      });
      add(eAllOff);  

      eStatus = new EValue(null,464,99,756,32,LCARS.EC_ELBOUP|LCARS.ES_DISABLED|LCARS.ES_LABEL_W|LCARS.EF_SMALL|LCARS.ES_STATIC,null);
      eStatus.setValue("000");
      eStatus.setValueMargin(24);
      add(eStatus); 

      // The error display
      eError = new ELabel(null,464,134,756,20,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_STATIC|LCARS.EF_SMALL,null);
      eError.setColor(COLOR_ERROR);
      add(eError); 
      
      // Port switches
      int portStyle = LCARS.EC_PRIMARY|LCARS.ES_DISABLED|LCARS.ES_LABEL_SE;
      for (int i=1; i<=4; i++)
      {
        ePorts[i-1] = new EValue(null,211+253*(i-1),47,250,49,portStyle|(i==1?LCARS.ES_RECT_RND_W:0),getPortName(i));
        ePorts[i-1].setValue("OFF");
        ePorts[i-1].setData(new Integer(i));
        ePorts[i-1].addEEventListener(new EEventListenerAdapter()
        {
          @Override
          public void touchDown(EEvent ee)
          {
            try
            {
              int portID = (Integer)ee.el.getData();
              setPortOn(portID,!isPortOn(portID));
            }
            catch (Exception e) {}
          }
        });
        add(ePorts[i-1]);
      }
    }

    @Override
    public void update(Observable o, Object arg) 
    {
      eTitle.setColor(isConnected()?null:COLOR_ERROR);
      eTitle.setBlinking(!isConnected());
      eAllOn.setDisabled(!isConnected());
      eAllOff.setDisabled(!isConnected());
      eStatus.setDisabled(!isConnected());
      eStatus.setLabel(replyTime>=replyTimeout?"TIMEOUT":"RT "+replyTime+" ms");
      eStatus.setValue(lastMessage!=null?lastMessage:"000");
      eError.setLabel(lastError!=null?lastError.toUpperCase():"");

      boolean allOn = true;
      boolean allOff = true;
      if (isConnected())
        for (int i=1; i<=4; i++)
        {
          ePorts[i-1].setColorStyle(isPortOn(i)?LCARS.EC_ELBOUP:LCARS.EC_PRIMARY);
          ePorts[i-1].setSelected(isPortOn(i));
          ePorts[i-1].setDisabled(false);
          ePorts[i-1].setValue(isPortOn(i)?"ON":"OFF");
          allOn &= isPortOn(i);
          allOff &= !isPortOn(i);
        }
      else
        for (int i=1; i<=4; i++)
        {
          ePorts[i-1].setDisabled(true);
          ePorts[i-1].setSelected(isPortOn(i));
          ePorts[i-1].setColorStyle(LCARS.EC_ELBOUP);
          ePorts[i-1].setValue(isPortOn(i)?"ON?":"OFF?");
          allOn &= isPortOn(i);
          allOff &= !isPortOn(i);
        }
      eAllOn.setSelected(allOn);
      eAllOff.setSelected(allOff);
    }

    @Override
    public void addToPanel(Panel panel)
    {
      addObserver(this);
      super.addToPanel(panel);
    }

    @Override
    public void removeFromPanel()
    {
      deleteObserver(this);
      super.removeFromPanel();
    }

  }

  // -- LCARS Low-level Hardware Access Panel --

  public static class IpPowerSocketsPanel extends HardwareAccessPanel
  {
    public IpPowerSocketsPanel(IScreen iscreen) 
    {
      super(iscreen, AIpPowerSocket.class);
    }

    @Override
    protected void createSubPanels() 
    {
      IpPowerSocket_010.getInstance().getLcarsSubpanel(677,180).addToPanel(this);
      IpPowerSocket_011.getInstance().getLcarsSubpanel(677,360).addToPanel(this);
      IpPowerSocket_012.getInstance().getLcarsSubpanel(677,540).addToPanel(this);
      IpPowerSocket_013.getInstance().getLcarsSubpanel(677,720).addToPanel(this);
    }
  }  
  
  // == Main method ==

  /**
   * Starts LCARS with the {@link IpPowerSocketsPanel}.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",IpPowerSocketsPanel.class.getName());
    LCARS.main(args);
  }
  
}
