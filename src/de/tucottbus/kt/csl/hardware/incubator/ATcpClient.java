package de.tucottbus.kt.csl.hardware.incubator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 * A simple client TCP client which maintains a connection and is capable of
 * sending and receiving lines of text (only). The remote server is expected 
 * to return a reply for each line of text sent by the client, including empty
 * lines.
 * 
 * <p><b style="color:red">Incubating</b></p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * ATcpClient tcpClient = new ATcpClient("141.43.71.7",64321) 
 * {
 *   public void receiveLine(String line) 
 *   {
 *     System.out.println(getName()+" < "+line);
 *   }
 * };
 * tcpClient.addObserver(new Observer() 
 * {
 *   public void update(Observable o, Object arg) 
 *   {
 *     if (arg instanceof String)
 *       System.out.println(tcpClient.getName()+" : "+(String)arg);
 *     else if (arg instanceof Throwable)
 *     {
 *       Throwable t = (Throwable)arg;
 *       System.err.println(tcpClient.getName()+" ! "+t.getMessage());
 *       t.printStackTrace();
 *     }
 *   }
 * });
 * 
 * tcpClient.sendLine("A line of text");
 * ...</pre>
 * 
 * <h3>Remote Server Implementation</h3>
 * <ul>
 *   <li><a href="https://developer.mbed.org/users/NegativeBlack/code/NetworkAPI/wiki/tcp-server-example">mbed</a>
 * </ul>
 * 
 * @see #ATcpClient(String, int)
 * @see #sendLine(String)
 * @see #receiveLine(String)
 *
 * @author Matthias Wolff
 */
public abstract class ATcpClient extends Observable implements Runnable 
{
  /**
   * Hint to {@link #notifyObservers(String)} indicating that client has 
   * started.
   */
  public static final String NOTIFY_STARTED = "started";

  /**
   * Hint to {@link #notifyObservers(String)} indicating that client has 
   * been disposed.
   */
  public static final String NOTIFY_DISPOSED = "disposed";

  /**
   * Hint to {@link #notifyObservers(String)} indicating that the connection
   * has been established.
   */
  public static final String NOTIFY_CONNECTED = "connected";

  /**
   * Hint to {@link #notifyObservers(String)} indicating that the client has
   * been (normally) disconnected.
   */
  public static final String NOTIFY_DISCONNECTED = "disconnected";
  
  /**
   * The heart beat message.
   */
  protected static final String HEARTBEAT = " "; // NOTE: Cannot be empty!
  
  /**
   * Interval between connection checks in milliseconds (kept only 
   * approximately).
   */
  protected static final int HEARTBEAT_INTERVAL = 1000;
  
  /**
   * Interval between reconnection attempts in milliseconds.
   */
  protected static final int RECONNECT_INTERVAL = 2500;
  
  /**
   * The name or IP address of the host this client connects to.
   */
  protected String host;
  
  /**
   * The port this client connects to.
   */
  protected int port;
  
  /**
   * The TCP socket.
   */
  protected Socket socket;

  /**
   * The {@link socket} output writer.
   */
  private BufferedWriter socketWriter;

  /**
   * The {@link #socket} input reader.
   */
  private InputStreamReader socketReader;

  /**
   * The writing queue.
   */
  private ArrayList<String> writeQueue;
  
  /**
   * Run flag for the guard thread.
   */
  protected boolean runGuard;
  
  /**
   * The guard thread maintaining the connection.
   */
  protected Thread guard;
  
  /**
   * Creates a new TCP client.
   * 
   * @param host
   *         The name or IP address of the host to connect to.
   * @param port
   *         The port to connect to.
   */
  public ATcpClient(String host, int port)
  {
    this.host = host;
    this.port = port;
    writeQueue = new ArrayList<String>();
    guard 
      = new Thread(this,getName());
    guard.setDaemon(true);
    guard.start();
  }
  
  @Override
  public void run() 
  {
    runGuard = true;
    setChanged(); notifyObservers(NOTIFY_STARTED);

    while (runGuard)
      try
      {
        // Initialize connection
        socket = new Socket(host,port);
        socketWriter 
          = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        socketReader = new InputStreamReader(socket.getInputStream());
        setChanged(); notifyObservers(NOTIFY_CONNECTED);
  
        // Run connection
        String message = "";
        int ctr = 0;
        while (runGuard)
        {
          synchronized (writeQueue)
          {
            while (writeQueue.size()>0)
            {
              String output = writeQueue.remove(0);
              if (!HEARTBEAT.equals(output))
              {
                setChanged(); notifyObservers("> "+output);
              }
              socketWriter.write(output);
              socketWriter.write("\n");
              socketWriter.flush();
            }
          }
          
          while (socketReader.ready())
          {
            int read = socketReader.read();
            if (read<0)
              throw new IOException("end of input stream");
            char c = (char)read;
            if (c=='\n' || c=='\r')
            {
              if (message.length()>0 && !HEARTBEAT.equals(message))
                receiveLine(message);
              message = "";
            }
            else
              message += c;
            ctr=0;
          }

          ctr++;
          try { Thread.sleep(10); } catch (InterruptedException e) {}
          if (ctr==HEARTBEAT_INTERVAL/10)
            sendLine(" "); // Heart beat
          else if (ctr==(RECONNECT_INTERVAL+HEARTBEAT_INTERVAL)/10)
            throw new IllegalStateException("no reply");
        }
        
        // Terminate connection
        socketReader.close(); socketReader = null;
        socketWriter.close(); socketWriter  = null;
        socket.close();       socket        = null;
        setChanged(); notifyObservers(NOTIFY_DISCONNECTED);
      }
      catch (Exception e)
      {
        try { socketReader.close(); } catch (Exception e1) {}
        try { socketWriter.close(); } catch (Exception e1) {}
        try { socket.close();       } catch (Exception e1) {}
        socketReader = null;
        socketWriter = null;
        socket       = null;
        if (runGuard)
        {
          setChanged(); notifyObservers(e);
          try { Thread.sleep(RECONNECT_INTERVAL); } catch (InterruptedException e1) {}
        }
        else
        {
          setChanged(); notifyObservers(NOTIFY_DISCONNECTED);
        }
      }
    
    setChanged(); notifyObservers(NOTIFY_DISPOSED);
  }

  /**
   * Terminates the connection and disposes of this client. Once disposed, the
   * client cannot be re-run.
   * 
   * @see #isDisposed()
   */
  public void dispose()
  {
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
        setChanged();
        notifyObservers(e);
      }
    }
    guard = null;
  }

  /**
   * Checks this client.
   * 
   * @throws IllegalStateException if the client is not fully operational 
   *         (details contained in the exception message).
   */
  protected void check()
  throws IllegalStateException
  {
    if (isDisposed())
      throw new IllegalStateException("client disposed");
    if (socket==null || !socket.isConnected())
      throw new IllegalStateException("socked not connected");
    if (socket.isClosed())
      throw new IllegalStateException("socked closed");
    if (socketReader==null || socketWriter==null)
      throw new IllegalStateException("no socked I/O");
  }
  
  // -- Getters and setters --

  /**
   * Determines if this client is disposed.
   * 
   * @see #dispose()
   */
  public boolean isDisposed()
  {
    return guard==null && guard.isAlive();
  }

  /**
   * Determines if this client is connected.
   */
  public boolean isConnected()
  {
    try
    {
      check();
    }
    catch (Exception e)
    {
      return false;
    }
    return true;
  }
  
  /**
   * Returns the human-readable name of this client.
   */
  public String getName()
  {
    return getClass().getSimpleName()+".guard["+host+":"+port+"]";
  }
  
  // -- I/O --
  
  /**
   * Sends a line of text. The method returns immediately. Applications should
   * register as {@link Observer} in order to be notified on any connection 
   * problems.
   * 
   * @param line
   *          The line to send.
   * @throws IllegalStateException if the client is not fully operational 
   *         (details contained in the exception message).
   */
  public void sendLine(String line)
  throws IllegalStateException
  {
    check();
    if (line==null)
      line = "";
    while (line.length()>0 && (line.charAt(line.length()-1)=='\n' || line.charAt(line.length()-1)=='\r'))
      line = line.substring(0,line.length()-1);
    synchronized (writeQueue) 
    {
      writeQueue.add(line);
    }
    if (runGuard)
      guard.interrupt();
  }
  
  // -- Abstract API --
  
  /**
   * Invoked when a line of text is received.
   * 
   * @param line
   *          The text without the terminating line separator.
   */
  public abstract void receiveLine(String line);

}