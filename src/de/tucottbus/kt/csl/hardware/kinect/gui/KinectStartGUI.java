package de.tucottbus.kt.csl.hardware.kinect.gui;

import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.tucottbus.kt.csl.hardware.kinect.devices.KinectSensor1_000;
import de.tucottbus.kt.csl.hardware.kinect.devices.KinectSensor2_000;
import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;


/**
 * Example for using the sensor. This class is not necessary.
 * 
 * @author Thomas Jung
 *
 */
public class KinectStartGUI extends JPanel
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JFrame frame;
  
  public KinectStartGUI()
  {
    initFrame();
  }
  
  /**
   * initializes the gui
   */
  private void initFrame() 
  {
    frame = new JFrame("Kinect");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(100, 100);
    frame.add(this);
    frame.setVisible(true);
    KinectSensor2_000 v1 = KinectSensor2_000.getInstance();
    while(!v1.isConnected())
    {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("Warten");
    };
    v1.startSkeletonStream();
    v1.openMapVideo();
    v1.openSetting();
    v1.startColorStream();
    v1.openColorVideo();
    v1.startDepthStream();
    v1.openDepthVideo();
    int i = 0;
    /*while(true)
    {
      i++;
      i %= 6;
      if(i == 0)
        {
        System.out.println(v1.getPersonCount());
        System.out.println();
        }
      if(v1.isPersonTracked(i))
      {
        System.out.println(v1.getTrackedHeadPositionOfPerson(i));
        System.out.println(v1.personLooksToObject(i, Object3D.DISPLAY));
      }
      
    }*/
    
    //v1.startColorStream();
    //v1.startDepthStream();
    //v1.startInfraredStream();
    
  }

  public static void main(String[] args) 
  {
    new KinectStartGUI();
  }
}