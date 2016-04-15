package de.tucottbus.kt.csl.hardware.kinect.gui;
import java.awt.Dimension;

import javax.media.opengl.GL2;
import javax.swing.JFrame;

import de.tucottbus.kt.csl.hardware.kinect.body.Body;
import de.tucottbus.kt.csl.hardware.kinect.devices.AKinectSensor;
import de.tucottbus.kt.csl.hardware.kinect.devices.Kinect;
import edu.ufl.digitalworlds.j4k.DepthMap;
import edu.ufl.digitalworlds.j4k.J4KSDK;
import edu.ufl.digitalworlds.j4k.VideoFrame;
import edu.ufl.digitalworlds.opengl.OpenGLPanel;

/**
 * This class displays the different streams (see {@link #type}). 
 * @author Thomas Jung
 *
 */
public class VideoPlayer extends OpenGLPanel
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   *  this window
   */
  private JFrame frame;
  
  /**
   * the types of data to display
   */
  public static final int COLOR = 0;
  public static final int DEPTH = 1;
  public static final int INFRARED = 2;
  public static final int MAP = 3;
  
  /**
   * the data type
   */
  private int type;
  
 
  private AKinectSensor kinect;
  
  /**
   * 
   * @param type
   * @param kinect
   */
  public VideoPlayer(int type, AKinectSensor kinect)
  {
    this.type = type;
    this.kinect = kinect;
    initFrame();
  }
  
  public void open()
  {
    frame.setVisible(true);
  }
  
  
  private void initFrame()
  {
    frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frame.add(this);
    frame.setSize(100, 100);
  }
  
  @Override
  /**
   * draws color, depth, infrared and skeleton data
   */
  public void draw() 
  {
    GL2 gl = getGL2();
    pushMatrix();
    VideoFrame videoFrame = null;
    double w  = 0;
    double h = 0;
    double z = 0;
    DepthMap map = null;
    Body[] bodies = null;
    Dimension dim = new Dimension(800, 600);
    switch(type)
    {
      case COLOR:
        videoFrame = kinect.getColorFrame();
        if(kinect.getDeviceType() == J4KSDK.MICROSOFT_KINECT_1)
        {
          dim = new Dimension(640, 480);
          w = 8.0;
          h = 3.0;
          z = -2.2;
        } else 
        {
          dim = new Dimension(960, 540);
          w = 32.0;
          h = 9.0;
          z = - 2.2;
        }
        break;
      case DEPTH:
        videoFrame = kinect.getDepthFrame();
        if(kinect.getDeviceType() == J4KSDK.MICROSOFT_KINECT_1)
        {
          dim = new Dimension(640, 480);
          w = 8.0;
          h = 3.0;
          z = -2.2;
        } else 
        {
          dim = new Dimension(512, 424);
          w = 10.24;
          h = 4.24;
          z = -2.2;
        }
        break;
      case INFRARED:
        videoFrame = kinect.getInfraredFrame();
        if(kinect.getDeviceType() == J4KSDK.MICROSOFT_KINECT_1)
        {
          dim = new Dimension(640, 480);
          w = 8.0;
          h = 3.0;
          z = -2.2;
        } else 
        {
          dim = new Dimension(512, 424);
          w = 10.24;
          h = 4.24;
          z = -2.2;
        }
        break;
      case MAP:
        map = kinect.getDepthMap();
        bodies = kinect.getBodies();
        if(kinect.getDeviceType() == J4KSDK.MICROSOFT_KINECT_1)
        {
          dim = new Dimension(640, 480);
          w = 8.0;
          h = 3.0;
          z = -2.2;
        } else 
        {
          dim = new Dimension(512, 424);
          w = 10.24;
          h = 4.24;
          z = -2.2;
        }
        break;
    }
    setPreferredSize(dim);
    frame.pack();
    if(videoFrame != null)
    {
      gl.glDisable(GL2.GL_LIGHTING);
      gl.glEnable(GL2.GL_TEXTURE_2D);
      gl.glColor3f(1f, 1f, 1f);
      videoFrame.use(gl);
      translate(0, 0, z);
      rotateZ(180);
      image(w / h, 2);
      gl.glDisable(GL2.GL_TEXTURE_2D);
      popMatrix();
      return;
    }
    
    if(map != null)
    {
      gl.glEnable(GL2.GL_LIGHTING);
      gl.glDisable(GL2.GL_TEXTURE_2D);
      gl.glColor3f(1f,1f,1f);
      translate(0, 0, z);
      map.drawNormals(gl);
    }
    
    if((bodies != null) && (map != null))
    {
      gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
      gl.glDisable(GL2.GL_LIGHTING);
    }
    
    if(bodies != null)
    {
      gl.glLineWidth(2);
      for(Body body : bodies)
      { 
        if(body == null) break;
        body.drawSkeleton(gl);
      }
    }
    popMatrix();
  }
}
