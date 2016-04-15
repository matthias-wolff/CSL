package de.tucottbus.kt.csl.hardware.kinect.room;
import javax.vecmath.Vector3f;

/**
 * <p>This class contains data of certain real objects. To create a virtual {@link Object3D} go through these steps:</p>
 * <ui>
 * <li> adapt the count of the objects</li>
 * <li> create a private method for the initializing of the Object3D</li>
 * <li> within this method create an empty Object3D</li>
 * <li> set a recognizable name {@link Object3D#setName(String name)}</li>
 * <li> set the type to identify the object {@link Object3D#setType(int type)}</li>
 * <li> add the type in the class Object3D (e.g. public static final int Display)</li>
 * <li> set the relative position (position in cognitive system lab) {@link Object3D#setRelativePosition(Vector3f position)}</li>
 * <li> set the orientation {@link Object3D#rotate(float xAngle, float yAngle, float zAngle)}</li>
 * <li> set the size {@link Object3D#setSize(Vector3f size)}</li>
 * <li> set the color {@link Object3D#setColor(Vector3f color)}</li>
 * <li> to calculate the absolute values (position and orientation) call the 
 * methods {@link Object3D#calculateAbsolutePosition(Object3D parent)} and
 * {@link Object3D#calculateAbsoluteOrientation(Object3D parent)}. The parents are null.
 * <li> {@link Object3D#update()} the visualization
 * <li> add this object to the {@link #list} </li>
 * </ui>
 * @author Thomas Jung
 *
 */
public class ListOfObject3D 
{
  public static final int COUNT_OBJECT3D = 3;
  /**
   * contains the Object3Ds 
   */
  private static Object3D[] list;
  
  public synchronized static Object3D[] getList()
  {
    if(list != null) return list;
    list = new Object3D[COUNT_OBJECT3D];
    initSensor1_000();
    initSensor2_000();
    initDisplay();
    return list;
  }
  
  public synchronized static void deleteList()
  {
    list = null;
  }
  
  private static void initSensor1_000()
  {
    Vector3f position = new Vector3f(0f, 2.2f, 2.13f);
    float xAngle = 110f;
    float yAngle = 0f;
    float zAngle = 0f;
    float width = 0.3f;
    float height = -0.05f;
    float length = 0.05f;
    Vector3f color = new Vector3f(0f, 0f, 100f);
    
    Object3D sensor1_000 = new Object3D();
    sensor1_000.setName("SENSOR1_000");
    sensor1_000.setType(Object3D.SENSOR1_000);
    sensor1_000.setRelativePosition(position);
    sensor1_000.rotate(xAngle, yAngle, zAngle);
    sensor1_000.setSize(new Vector3f(width, length, height));
    sensor1_000.setColor(color);
    sensor1_000.calculateAbsolutePosition(null);
    sensor1_000.calculateAbsoluteOrientation(null);
    sensor1_000.update();
    list[Object3D.SENSOR1_000] = sensor1_000;
  }
  
  private static void initSensor2_000()
  {
    Vector3f position = new Vector3f(1.04f, -1.77f, 1.9f);
    float xAngle = 97f;
    float yAngle = 0f;
    float zAngle = -143f;
    float width = 0.35f;
    float height = -0.08f;
    float length = 0.08f;
    Vector3f color = new Vector3f(0f, 100f, 100f);
    
    Object3D sensor2_000 = new Object3D();
    sensor2_000.setName("SENSOR2_000");
    sensor2_000.setType(Object3D.SENSOR2_000);
    sensor2_000.setRelativePosition(position);
    sensor2_000.rotate(xAngle, yAngle, zAngle);
    sensor2_000.setSize(new Vector3f(width, length, height));
    sensor2_000.setColor(color);
    sensor2_000.calculateAbsolutePosition(null);
    sensor2_000.calculateAbsoluteOrientation(null);
    sensor2_000.update();
    list[Object3D.SENSOR2_000] = sensor2_000;
  }
  
  private static void initDisplay()
  {
    Vector3f position = new Vector3f(0f, 2.2f, 1.55f);
    float xAngle = 90f;
    float yAngle = 0f;
    float zAngle = 0f;
    float width = 1.58f;
    float height = -0.1f;
    float length = 0.88f;
    Vector3f color = new Vector3f(100f,0f, 0f);
    
    Object3D display = new Object3D();
    display.setName("DISPLAY");
    display.setType(Object3D.DISPLAY);
    display.setRelativePosition(position);
    display.rotate(xAngle, yAngle, zAngle);
    display.setSize(new Vector3f(width, length, height));
    display.setColor(color);
    display.calculateAbsolutePosition(null);
    display.calculateAbsoluteOrientation(null);
    display.update();
    list[Object3D.DISPLAY] = display;
  }
  
  public synchronized static void setObject3DRotation(int object3dType, float xAngle, 
      float yAngle, float zAngle)
  {
    list[object3dType].rotate(xAngle, yAngle, zAngle);
    list[object3dType].calculateAbsoluteOrientation(null);
    list[object3dType].update();
  }

  public synchronized static void setObjec3DPosition(int object3dType, Vector3f position) 
  {
    list[object3dType].setRelativePosition(position);
    list[object3dType].calculateAbsolutePosition(null);
    list[object3dType].update();
  }

  public synchronized static void setObject3DSize(int object3dType, Vector3f size) 
  {
    list[object3dType].setSize(size);
    list[object3dType].update();
  }
  
  public synchronized static void setObject3DColor(int object3dType, Vector3f color)
  {
    list[object3dType].setColor(color);
    list[object3dType].update();
  }
}
