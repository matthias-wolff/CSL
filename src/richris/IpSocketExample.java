package richris;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;

/**
 * A minimal example class communicating with the NETIO-230B IP power socket.
 * 
 * @author chris
 */
public class IpSocketExample implements Runnable, TelnetNotificationHandler 
{
  /**
   * The Telnet client maintaining the connection to the IP power socket.
   */
  static TelnetClient tc = null;

  /**
   * Main program
   * 
   * @param args
   *          The command line arguments.
   **/
  public static void main(String[] args) throws Exception 
  {
    String remoteip = "141.43.71.13";                                           // IP address of socket
    int remoteport  = 1234;                                                     // Port of socket
    tc = new TelnetClient();                                                    // Telnet client
    tc.setConnectTimeout(1000);
    byte[] buff = new byte[1024];                                               // Buffer for keyboard input
    int ret_read = 0;                                                           // Length of string read from keyboard

    while (true)                                                                // Main loop (forever)
    {                                                                           // >>
      boolean end_loop = false;                                                 //   End flag of keyboard input loop
      try                                                                       //   Try
      {                                                                         //   >>
        
        // Initialize                                                           //     --------------------------------
        System.err.print("Connecting ...");
        tc.connect(remoteip, remoteport);                                       //     Start Telnet connection
        System.err.println(" done");
        OutputStream outstr = tc.getOutputStream();                             //     Get output stream to Telnetport
        Thread reader = new Thread(new IpSocketExample());                      //     Thread reading input from Telnet
        reader.start();                                                         //     Start reader thread
        outstr.write("login ktuser kt\r\n".getBytes());                         //     Send login command via Telnet
        outstr.flush();                                                         //     Force write operation

        // Timer
 /*       Timer timer = new Timer();
        timer.schedule(new TimerTask() 
        {
          
          @Override
          public void run() 
          {
            try 
            {
              outstr.write("port list\r\n".getBytes());
              outstr.flush();
            } 
            catch (IOException e) 
            {
              System.err.println("Exception writing to Telnet: "+e.getMessage());
            }
          }
        }, 1000, 2500);*/
        
        // The main loop                                                        //     --------------------------------
        do                                                                      //     Keyboard input loop
        {                                                                       //     >>
          try                                                                   //       Try
          {                                                                     //       >>
            ret_read = System.in.read(buff);                                    //         Read from keyboard
            if (ret_read > 0)                                                   //         Got string from keyboard
            {                                                                   //         >>
              try                                                               //           Try
              {                                                                 //           >>
                outstr.write(buff, 0, ret_read);                                //             Write string via Telnet
                outstr.flush();                                                 //             Force write operation
              }                                                                 //           <<
              catch (IOException e)                                             //           Telnet I/O exception
              {                                                                 //           >>
                System.err.println("Exception writing to Telnet: "              //             Error message
                  + e.getMessage());                                            //             ...
                end_loop = true;                                                //             Exit keyboard input loop
              }                                                                 //           <<
            }                                                                   //         <<
          }                                                                     //       <<
          catch (IOException e)                                                 //       Keyboard I/O exception
          {                                                                     //       >>
            System.err.println("Exception reading keyboard: "+e.getMessage());  //         Error message
            end_loop = true;                                                    //         Exit keyboard input loop
          }                                                                     //       <<
        }                                                                       //     <<
        while ((ret_read > 0) && (end_loop == false));                          //     Keyboard input closed or end

        // Shut-down                                                            //     --------------------------------
        try                                                                     //     Try 
        {                                                                       //     >>
          tc.disconnect();                                                      //       End Telnet connection
        }                                                                       //     <<
        catch (IOException e)                                                   //     Exception while disconnecting
        {                                                                       //     >>
          System.err.println("Exception while connecting: "+e.getMessage());    //       Error message
        }                                                                       //     <<
      }                                                                         //   <<
      catch (Exception e)                                                       //   Exception while connecting
      {                                                                         //   >>
        System.err.println(" FAILED");
        System.err.println("Exception while connecting: "+e.getMessage());      //     Error message
        System.exit(1);                                                         //     Terminate Java program
      }                                                                         //   <<
    }                                                                           // <<
  }

  /**
   * Run method of Telnet reader thread. Reads lines from the TelnetClient and echoes 
   * them on the screen.
   **/
  @Override
  public void run() 
  {
    // Initialize                                                              // -------------------------------------
    byte[] buff = new byte[1024];                                              // Buffer for input read via Telnet
    int ret_read = 0;                                                          // Length of input string
    InputStream instr = tc.getInputStream();                                   // Get input stream from socket

    // Continuously read input via Telnet                                      // -------------------------------------
    try                                                                        // Try 
    {                                                                          // >>
      do                                                                       //   Telnet reading loop
      {                                                                        //   >>
        ret_read = instr.read(buff);                                           //     Read from Telnet
        if (ret_read > 0)                                                      //     Got string via Telnet
        {                                                                      //     >>
          String s = new String(buff, 0, ret_read);
          System.out.print(s);                                                 //       Echo on screen
          if (s.startsWith("110"))
          {
            break;
          }
        }                                                                      //     <<
      }                                                                        //   <<
      while (ret_read >= 0);                                                   //   Telnet connection closed
    }                                                                          // <<
    catch (IOException e)                                                      // Exception reading from Telnet 
    {                                                                          // >>
      System.err.println("Exception reading from Telnet: "+e.getMessage());    //   Error message
    }                                                                          // <<
    
    // Shut-down                                                               // -------------------------------------
    try                                                                        // Try
    {                                                                          // >>
      System.err.print("Disconnecting ...");
      tc.disconnect();                                                         //   End Telnet connection
      System.err.println(" done");
    }                                                                          // <<
    catch (IOException e)                                                      // Exception while disconnecting
    {                                                                          // >>
      System.err.println(" FAILED");
      System.err.println("Exception while closing Telnet: "+e.getMessage());   //   Error message
    }                                                                          // <<

    System.exit(0);
  }

  @Override
  public void receivedNegotiation(int negotiation_code, int option_code) 
  {
    String command = null;
    if(negotiation_code == TelnetNotificationHandler.RECEIVED_DO)              
    {
      command = "DO";
    }
    else if(negotiation_code == TelnetNotificationHandler.RECEIVED_DONT)
    {
      command = "DONT";
    }
    else if(negotiation_code == TelnetNotificationHandler.RECEIVED_WILL)
    {
      command = "WILL";
    }
    else if(negotiation_code == TelnetNotificationHandler.RECEIVED_WONT)
    {
      command = "WONT";
    }
    System.out.println("Received "+command+" for option code "+option_code);  
  }
}