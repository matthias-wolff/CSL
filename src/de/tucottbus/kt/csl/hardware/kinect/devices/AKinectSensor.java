package de.tucottbus.kt.csl.hardware.kinect.devices;

import javax.vecmath.Vector3f;

import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.kinect.body.Body;
import de.tucottbus.kt.csl.hardware.kinect.gui.RoomEditor;
import de.tucottbus.kt.csl.hardware.kinect.gui.VideoPlayer;
import de.tucottbus.kt.csl.hardware.kinect.room.ListOfObject3D;
import edu.ufl.digitalworlds.j4k.DepthMap;
import edu.ufl.digitalworlds.j4k.J4KSDK;
import edu.ufl.digitalworlds.j4k.VideoFrame;

/**
 * This class is the wrapper for every kinect. Do not use directly this wrapper to use a kinect.
 * To use a certain kinect extend this class. 
 * 
 *<p> The class {@link de.tucottbus.kt.csl.hardware.kinect.gui.KinectStartGUI} 
 *contains an example, how to use the wrapper.
 * 
 * @author Thomas Jung
 *
 */
public abstract class AKinectSensor extends AAtomicHardware implements Runnable
{
  private final String CLASSKEY = "KinectSensor";

  protected Kinect kinect = null;
  
  /**
   * contains the state of which streams are open
   */
  private boolean isColorStreamOpen = false;
  private boolean isDepthStreamOpen = false;
  private boolean isSkeletonStreamOpen = false;
  private boolean isInfraredStreamOpen = false;
  
  /**
   * windows displays color, depth, infrared etc.
   */
  private VideoPlayer colorPlayer;
  private VideoPlayer depthPlayer;
  private VideoPlayer infraredPlayer;
  private VideoPlayer mapPlayer;
  
  protected Thread guard;
  
  private boolean runGuard = false;
  
  /**
   * kinect version
   */
  private byte deviceType;
  
  /**
   * id for each kinect
   */
  private int deviceID;
  
  /**
   * represents the objects as Object3D
   */
  private int object3dType;
  
  /**
   * which streams are activated
   */
  private int flags;
  
  /**
   * AKinectSensor has a Version (1 or 2)
   * , a continuous numbering
   * and is representing as Object3D for intern calculation
   * reasons.
   * 
   * @param deviceType
   * @param deviceID
   * @param object3dType
   */
  public AKinectSensor(byte deviceType, int deviceID, int object3dType)
  {    
    this.deviceType = deviceType;
    this.deviceID = deviceID;
    this.object3dType = object3dType;
   
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }
  
  @Override
  public void run() {
    runGuard = true;
    final int sleepMillis = 100;
    int ctr = 0;
    
    if(kinect == null)
    {
      kinect = new Kinect(deviceType, deviceID, object3dType);
      RoomEditor.getInstance().addKinect(kinect);
    }
    if(colorPlayer == null) colorPlayer = new VideoPlayer(VideoPlayer.COLOR, this);
    if(depthPlayer == null) depthPlayer = new VideoPlayer(VideoPlayer.DEPTH, this);
    if(infraredPlayer == null) infraredPlayer = new VideoPlayer(VideoPlayer.INFRARED, this);
    if(mapPlayer == null) mapPlayer = new VideoPlayer(VideoPlayer.MAP, this);
    
    while(runGuard){
      try {
        
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        // - Run connection
        while(runGuard){
          try { Thread.sleep(sleepMillis); } catch (InterruptedException e) {}
          if (!runGuard)
            break;
          
          if (!isConnected()) 
            break;
          
          if(isSkeletonRunning()); // delete old skeleton data        
             
          ctr+=sleepMillis;
          if (ctr>=1000)
          {
            setChanged();
            notifyObservers(NOTIFY_STATE);
            ctr = 0;
          }
        }
      } catch (Exception e) {
        logErr(e.getMessage(), e);
        dispose();
        
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try { Thread.sleep(1000); } catch (InterruptedException e1) {}
      }
    }
  }
  
  /**
   * set Position for the kinect for intern calculation
   * and displaying in the RoomEditor
   * @param position
   */
  public void setSensorPosition(Vector3f position)
  {
    ListOfObject3D.setObjec3DPosition(object3dType, position);
  }
  
  /**
   * rotates the kinect with 3 euler angles for intern calculation 
   * and displaying in the RoomEditor
   * @param xAngle
   * @param yAngle
   * @param zAngle
   */
  public void setSensorRotation(float xAngle, float yAngle, float zAngle)
  {
    ListOfObject3D.setObject3DRotation(object3dType, xAngle, yAngle, zAngle);
  }
  
  /**
   * resize the kinect for displaying in the RoomEditor
   * @param width
   * @param height
   * @param length
   */
  public void setSensorSize(Vector3f size)
  {
    ListOfObject3D.setObject3DSize(object3dType, size);
  }
  
  /**
   * Is the returning status false, 
   * then the old skeleton data are deleted
   * @return true, if we are receiving skeleton data,
   * false otherwise
   */
  private synchronized boolean isSkeletonRunning() 
  {
    if(!isConnected()) return false;
    if(!isSkeletonStreamOpen) return false;
    kinect.setSkeletonRunning(false);
    try { Thread.sleep(1000); } catch (InterruptedException e) { }
    if(!kinect.isSkeletonRunning()) kinect.resetSkeletonRelatedData();
    return kinect.isSkeletonRunning();
  }

  /**
   * starts the color stream
   */
  public synchronized void startColorStream()
  {
    if(kinect == null) return;
    if(isColorStreamOpen) return;
    kinect.stop();
    flags |= J4KSDK.COLOR;
    kinect.start(flags);
    isColorStreamOpen = true;
  }
  
  /**
   * stops the color stream
   */
  public synchronized void stopColorStream()
  {
    if(kinect == null) return;
    if(!isColorStreamOpen) return;
    kinect.stop();
    flags &= ~J4KSDK.COLOR;
    if(flags != 0) kinect.start(flags);
    isColorStreamOpen = false;
  }
  
  /**
   * starts the depth stream
   */
  public synchronized void startDepthStream()
  {
    if(kinect == null) return;
    if(isDepthStreamOpen) return;
    kinect.stop();
    flags |= (J4KSDK.DEPTH | J4KSDK.XYZ | J4KSDK.UV | J4KSDK.PLAYER_INDEX);
    kinect.start(flags);
    isDepthStreamOpen = true;
  }
  
  /**
   * stops the depth stream
   */
  public synchronized void stopDepthStream()
  {
    if(kinect == null) return;
    if(!isDepthStreamOpen) return;
    kinect.stop();
    flags &= ~(J4KSDK.DEPTH | J4KSDK.XYZ | J4KSDK.UV | J4KSDK.PLAYER_INDEX);
    if(flags != 0) kinect.start(flags);
    isDepthStreamOpen = false;
  }
  
  
  /**
   * starts the skeleton stream
   */
  public synchronized void startSkeletonStream()
  {
    if(kinect == null) return;
    if(isSkeletonStreamOpen) return;
    kinect.stop();
    flags |= J4KSDK.SKELETON;
    kinect.start(flags);
    isSkeletonStreamOpen =  true;
  }
  
  /**
   * stops the skeleton stream
   */
  public synchronized void stopSkeletonStream()
  {
    if(kinect == null) return;
    if(!isSkeletonStreamOpen) return;
    kinect.stop();
    flags &= ~J4KSDK.SKELETON;
    if(flags != 0) kinect.start(flags);
    kinect.resetSkeletonRelatedData();
    isSkeletonStreamOpen = false;
  }
  
  /**
   * starts the infrared stream
   */
  public synchronized void startInfraredStream()
  {
    if(kinect == null) return;
    if(isInfraredStreamOpen) return;
    kinect.stop();
    flags |= J4KSDK.INFRARED;
    kinect.start(flags);
    isInfraredStreamOpen = true;
  }
  
  /**
   * stops the infrared stream
   */
  public synchronized void stopInfraredStream()
  {
    if(kinect == null) return;
    if(!isInfraredStreamOpen) return;
    if(!(isColorStreamOpen || isDepthStreamOpen)) return;
    kinect.stop();
    flags &= ~J4KSDK.INFRARED;
    if(flags != 0) kinect.start(flags);
    isInfraredStreamOpen = false;
  }
  
  /**
   * stops all activated streams
   */
  public synchronized void stopAllStreams()
  {
    if(kinect == null) return;
    kinect.stop();
    flags = 0;
    isColorStreamOpen = false;
    isDepthStreamOpen = false;
    isInfraredStreamOpen = false;
    isSkeletonStreamOpen = false;
  }
  
  /**
   * 
   * @return the status of the color stream
   */
  public synchronized boolean isColorStreamOpen() 
  {
    return isColorStreamOpen;
  }

  /**
   * 
   * @return the status of the depth stream
   */
  public synchronized boolean isDepthStreamOpen() 
  {
    return isDepthStreamOpen;
  }

  /**
   * 
   * @return the status of the skeleton stream
   */
  public synchronized boolean isSkeletonStreamOpen() 
  {
    return isSkeletonStreamOpen;
  }

  /**
   * 
   * @return the status of the infrared stream
   */
  public synchronized boolean isInfraredStreamOpen() 
  {
    return isInfraredStreamOpen;
  }
  
  /**
   * 
   * @return the number of persons
   */
  public synchronized int getPersonCount()
  {
    if(kinect == null) return 0;
    return kinect.getPersonCount();
  }
  
  /**
   * 
   * @param bodyID
   * @param type
   * @return true, if a tracked person is looking to the object (e.g. Object3D.DISPLAY),
   * false otherwise
   */
  public synchronized
  boolean personLooksToObject(int bodyID, int type)
  {
    if(kinect == null) return false;
    if(kinect.getBodies()[bodyID].isTracked()) 
      return kinect.getBodies()[bodyID].getLooksToObject(type);
    else return false;
    
  }
  
  /**
   * 
   * @param personID
   * @return the position vector of the head, if the person is tracked,
   *  null vector otherwise
   */
  public synchronized Vector3f getTrackedHeadPositionOfPerson(int personID)
  {
    if(kinect == null) return new Vector3f();
    return  kinect.getBodies()[personID].getHeadPosition();
  }
  
  /**
   * 
   * @param bodyID
   * @return the tracked state of the person
   */
  public synchronized boolean isPersonTracked(int bodyID)
  {
    if(kinect == null) return false;
    return kinect.getBodies()[bodyID].isTracked();
  }
  
  /**
   * shows the player with color video
   */
  public synchronized void openColorVideo()
  {
    if(colorPlayer == null) return;
    colorPlayer.open();
  }
  
  /**
   * shows the player with depth video
   */
  public synchronized void openDepthVideo()
  {
    if(depthPlayer == null) return;
    depthPlayer.open();
  }
  
  /**
   * shows the player with infrared video
   */
  public synchronized void openInfraredVideo()
  {
    if(infraredPlayer == null) return;
    infraredPlayer.open();
  }
  
  /**
   * shows the player with depth map
   */
  public synchronized void openMapVideo()
  {
    if(mapPlayer == null) return;
    mapPlayer.open();
  }
  
  /**
   * open the RoomEditor
   */
  public synchronized void openSetting()
  {
    RoomEditor.getInstance().open();
  }
  
  public VideoFrame getColorFrame()
  {
    if(kinect == null) return null;
    return kinect.getColorVideoFrame();
  }
  
  public VideoFrame getDepthFrame()
  {
    if(kinect == null) return null;
    return kinect.getDepthVideoFrame();
  }
 
  public VideoFrame getInfraredFrame()
  {
    if(kinect == null) return null;
    return kinect.getInfraredVideoFrame();
  }
  
  public DepthMap getDepthMap()
  {
    if(kinect == null) return null;
    return kinect.getMap();
  }
  
  /**
   * Do only use this method, if you know how to handle the body data
   * @return the bodies (representing the skeleton data)
   */
  public Body[] getBodies()
  {
    if(kinect == null) return null;
    return kinect.getBodies();
  }
  
  public byte getDeviceType()
  {
    if(kinect == null) return -1;
    return deviceType;
  }

  @Override
  public void dispose() {
   stopAllStreams();
   RoomEditor.getInstance().removeKinect(kinect);
   kinect = null;
   colorPlayer = null;
   depthPlayer = null;
   infraredPlayer = null;
   mapPlayer = null;
   
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
  public String getName() 
  {
    return CLASSKEY;
  }

  @Override
  public boolean isConnected() 
  {
    if(kinect == null) return false;
    return kinect.isInitialized();
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AHardware getParent() 
  {
    return CslHardware.getInstance();
  }
}
