package richris;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;

import de.tucottbus.kt.csl.hardware.incubator.ATcpClient;

/**
 * Christian's TCP client test program.
 */
public class TcpClientExample 
{
  public static void main(String[] args)
  {
    // Instantiate and instrument TCP client
    ATcpClient tcpClient = new ATcpClient("141.43.71.5",55000) 
    {
      @Override
      public void receiveLine(String line) 
      {
        System.out.println(getName()+" < "+line);
      }
    };
    tcpClient.addObserver(new Observer() 
    {
      @Override
      public void update(Observable o, Object arg) 
      {
        if (arg instanceof String)
          System.out.println(tcpClient.getName()+" : "+(String)arg);
        else if (arg instanceof Throwable)
        {
          Throwable t = (Throwable)arg;
          System.err.println(tcpClient.getName()+" ! "+t.getMessage());
          t.printStackTrace();
        }
      }
    });
    
    // Read and dispatch stdin commands
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String line;
    try
    {
      System.out.println("Enter q to quit program");
      while (!("q".equals(line = in.readLine())))
      {
        tcpClient.sendLine(line);
      }
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    tcpClient.dispose();
  }
}
