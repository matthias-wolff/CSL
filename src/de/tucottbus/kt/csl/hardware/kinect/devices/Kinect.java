package de.tucottbus.kt.csl.hardware.kinect.devices;

import de.tucottbus.kt.csl.hardware.kinect.body.Body;
import de.tucottbus.kt.csl.hardware.kinect.gui.RoomEditor;
import de.tucottbus.kt.csl.hardware.kinect.room.ListOfObject3D;
import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;
import edu.ufl.digitalworlds.j4k.DepthMap;
import edu.ufl.digitalworlds.j4k.J4KSDK;
import edu.ufl.digitalworlds.j4k.Skeleton;
import edu.ufl.digitalworlds.j4k.VideoFrame;

/**
 * This class handles the different data events.
 * This class managed the persons, which are represented by the class {@link Body},  and virtual room objects, 
 * which are created with the {@link RoomEditor}
 * 
 * @author Thomas Jung
 *
 */
public class Kinect extends J4KSDK
{
  //private final String CLASSKEY = "Kinect";
 
  
  private boolean isRunning;
  
  public void setRunning(boolean isRunning)
  {
    this.isRunning = isRunning;
  }
  
  public boolean isRunning()
  {
    return isRunning;
  }
  
  private boolean isSkeletonRunning;
  
  public void setSkeletonRunning(boolean isSkeletonRunning)
  {
    this.isSkeletonRunning = isSkeletonRunning;
  }
  
  public boolean isSkeletonRunning()
  {
    return isSkeletonRunning;
  }

  /**
   * Contains the DepthMap.
   */
  private DepthMap map;

  /**
   * Contains six body wrappers of six persons.
   */
  private Body[] bodies = new Body[6];
  
  /**
   * Contains color data and resolution.
   */
  private VideoFrame colorVideoFrame = new VideoFrame();
  
  /**
   * Contains depth data and resolution.
   */
  private VideoFrame depthVideoFrame = new VideoFrame();
  
  /**
   * Contains infrared data and resolution.
   */
  private VideoFrame infraredVideoFrame = new VideoFrame();
  
  /**
   * 
   * @return The current color videoframe.
   */
  public VideoFrame getColorVideoFrame()
  {
    return colorVideoFrame;
  }
  
  /**
   * 
   * @return The current depth videoframe.
   */
  public VideoFrame getDepthVideoFrame()
  {
    return depthVideoFrame;
  }
  
  /**
   * 
   * @return The current infrared videoframe.
   */
  public VideoFrame getInfraredVideoFrame()
  {
    return infraredVideoFrame;
  }
  

  public Body[] getBodies()
  {
    return bodies;
  }
  
  private int personCount;
  
  /**
   * 
   * @return The number of persons.
   */
  public int getPersonCount()
  {
    return personCount;
  }

  /**
   * 
   * @return The current DepthMap.
   */
  public DepthMap getMap()
  {
    return map;
  }

  /**
   * This constructor creates a {@link Kinect} object.
   *
   * @param type
   * @param deviceID
   * @param object3dType
   */
  public Kinect(byte type, int deviceID, int object3dType)
  {
    super(type, deviceID);
    this.object3dType = object3dType;
    for(int i = 0; i < 6; i++)
    {
      bodies[i] = new Body(type == J4KSDK.MICROSOFT_KINECT_2);
    }
  }
  
  /**
   * This method is fired, if new color data are arriving.
   */
  @Override
  public void onColorFrameEvent(byte[] color)
  { 
    isRunning = true;
    colorVideoFrame.update(getColorWidth(), getColorHeight(), color);
  }

  /**
   * This method is fired, if new depth data are arriving.
   */
  @Override
  public void onDepthFrameEvent(short[] depth, byte[] player_index,
      float[] xyz, float[] uv)
  {
    isRunning = true;
    int sz = getDepthWidth() * getDepthHeight();
    byte bgra[] = new byte[sz * 4];
    int idx = 0;
    int iv = 0;
    byte blue = 0;
    byte green = 0;
    byte red = 0;
    for (int i = 0; i < sz; i++)
    {
      if(getDeviceType() == J4KSDK.MICROSOFT_KINECT_2) iv = depth[i];
      else iv = depth[i] >> 3;
    
      // rainbow coloring
      if(iv >= 0 && iv < 500){blue = (byte) 0; green = (byte) 0; red = (byte) 0;}
      else if(iv >= 500 && iv < 600){blue = (byte) 0; green = (byte) 0; red = (byte) 255;}
      
      else if(iv >= 600 && iv < 700){blue = (byte) 0; green = (byte) 23; red = (byte) 255;}
      else if(iv >= 700 && iv < 800){blue = (byte) 0; green = (byte) 46; red = (byte) 255;}
      else if(iv >= 800 && iv < 900){blue = (byte) 0; green = (byte) 69; red = (byte) 255;}
      else if(iv >= 900 && iv < 1000){blue = (byte) 0; green = (byte) 92; red = (byte) 255;}
      else if(iv >= 1000 && iv < 1100){blue = (byte) 0; green = (byte) 115; red = (byte) 255;}
      else if(iv >= 1100 && iv < 1200){blue = (byte) 0; green = (byte) 138; red = (byte) 255;}
      else if(iv >= 1200 && iv < 1300){blue = (byte) 0; green = (byte) 161; red = (byte) 255;}
      else if(iv >= 1300 && iv < 1400){blue = (byte) 0; green = (byte) 184; red = (byte) 255;}
      else if(iv >= 1400 && iv < 1500){blue = (byte) 0; green = (byte) 208; red = (byte) 255;}
      else if(iv >= 1500 && iv < 1600){blue = (byte) 0; green = (byte) 232; red = (byte) 255;}
      
      else if(iv >= 1600 && iv < 1700){blue = (byte) 0; green = (byte) 255; red = (byte) 255;}
      
      else if(iv >= 1700 && iv < 1800){blue = (byte) 0; green = (byte) 255; red = (byte) 232;}
      else if(iv >= 1800 && iv < 1900){blue = (byte) 0; green = (byte) 255; red = (byte) 208;}
      else if(iv >= 1900 && iv < 2000){blue = (byte) 0; green = (byte) 255; red = (byte) 184;}
      else if(iv >= 2000 && iv < 2100){blue = (byte) 0; green = (byte) 255; red = (byte) 161;}
      else if(iv >= 2100 && iv < 2200){blue = (byte) 0; green = (byte) 255; red = (byte) 138;}
      else if(iv >= 2200 && iv < 2300){blue = (byte) 0; green = (byte) 255; red = (byte) 115;}
      else if(iv >= 2300 && iv < 2400){blue = (byte) 0; green = (byte) 255; red = (byte) 92;}
      else if(iv >= 2400 && iv < 2500){blue = (byte) 0; green = (byte) 255; red = (byte) 69;}
      else if(iv >= 2500 && iv < 2600){blue = (byte) 0; green = (byte) 255; red = (byte) 46;}
      else if(iv >= 2600 && iv < 2700){blue = (byte) 0; green = (byte) 255; red = (byte) 23;}
      
      else if(iv >= 2700 && iv < 2800){blue = (byte) 0; green = (byte) 255; red = (byte) 0;}
      
      else if(iv >= 2800 && iv < 2900){blue = (byte) 23; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 2900 && iv < 3000){blue = (byte) 46; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3000 && iv < 3100){blue = (byte) 69; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3100 && iv < 3200){blue = (byte) 92; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3200 && iv < 3300){blue = (byte) 115; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3300 && iv < 3400){blue = (byte) 138; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3400 && iv < 3500){blue = (byte) 161; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3500 && iv < 3600){blue = (byte) 184; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3600 && iv < 3700){blue = (byte) 208; green = (byte) 255; red = (byte) 0;}
      else if(iv >= 3700 && iv < 3800){blue = (byte) 232; green = (byte) 255; red = (byte) 0;}
      
      else if(iv >= 3800 && iv < 3900){blue = (byte) 255; green = (byte) 255; red = (byte) 0;}
      
      else if(iv >= 3900 && iv < 4000){blue = (byte) 255; green = (byte) 212; red = (byte) 0;}
      else if(iv >= 4000 && iv < 4100){blue = (byte) 255; green = (byte) 170; red = (byte) 0;}
      else if(iv >= 4100 && iv < 4200){blue = (byte) 255; green = (byte) 127; red = (byte) 0;}
      else if(iv >= 4200 && iv < 4300){blue = (byte) 255; green = (byte) 85; red = (byte) 0;}
      else if(iv >= 4300 && iv < 4400){blue = (byte) 255; green = (byte) 42; red = (byte) 0;}
      else if(iv >= 4400 && iv < 4500){blue = (byte) 255; green = (byte) 0; red = (byte) 0;}
      
      else {blue = (byte) 0; green = (byte) 0; red = (byte) 0;}

      bgra[idx] = blue;
      idx++;
      bgra[idx] = green;
      idx++;
      bgra[idx] = red;
      idx++;
      bgra[idx] = 0;
      idx++;
    }
    
    depthVideoFrame.update(getDepthWidth(), getDepthHeight(), bgra);
    map = new DepthMap(getDepthWidth(), getDepthHeight(), xyz);
  }
  
  private int object3dType;
  
  /**
   * This method is fired, if new skeleton data are arriving.
   */
  @Override
  public void onSkeletonFrameEvent(boolean[] flags, float[] positions,
      float[] orientations, byte[] states)
  {
    isSkeletonRunning = true;
    reseted = false;
    personCount = 0;
    for (int i = 0; i < 6; i++)
    {
      bodies[i].setSkeleton(Skeleton.getSkeleton(i, flags, positions,
          orientations, states, this));
      bodies[i].setAllRelativePosition();
      bodies[i].setAllRelativOrientation();
        
      if(bodies[i].isTracked())
      {
        for(Object3D object : ListOfObject3D.getList())
        {
          if(object.getType() == object3dType) bodies[i].calculateAbsolute(object);
          object.calculateLookedBy(bodies[i]);
        }
        personCount++;
      }
    }
  }

  /**
   * This method is fired, if new infrared data are arriving.
   */
  @Override
  public void onInfraredFrameEvent(short[] infrared)
  {
    isRunning = true;
    int sz = getInfraredWidth() * getInfraredHeight();
    byte bgra[] = new byte[sz * 4];
    int idx = 0;
    int iv = 0;
    short sv = 0;
    byte bv = 0;
    for (int i = 0; i < sz; i++)
    {
      sv = infrared[i];
      iv = sv >= 0 ? sv : 0x10000 + sv;
      bv = (byte) ((iv & 0xfff8) >> 6);
      bgra[idx] = bv;
      idx++;
      bgra[idx] = bv;
      idx++;
      bgra[idx] = bv;
      idx++;
      bgra[idx] = 0;
      idx++;
    }
    infraredVideoFrame.update(getInfraredWidth(), getInfraredHeight(), bgra);
  }
  
  private boolean reseted = false;
  
  public void resetSkeletonRelatedData()
  {
    if(reseted) return;
    personCount = 0;
    for(int i = 0; i < 6; i++)
    {
      bodies[i] = new Body(getDeviceType() == J4KSDK.MICROSOFT_KINECT_2);
    }
    reseted = true;
  }
}
