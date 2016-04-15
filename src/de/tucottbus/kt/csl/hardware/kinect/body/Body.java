package de.tucottbus.kt.csl.hardware.kinect.body;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import de.tucottbus.kt.csl.hardware.kinect.room.ListOfObject3D;
import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;
import edu.ufl.digitalworlds.j4k.Skeleton;


/**
 * This class is a wrapper for a skeleton and joints as objects of {@link Bodypart}
 * This class wraps all calculations of the absolute position and orientation of each joint
 * 
 * @author Thomas Jung
 *
 */
public class Body
{

  // -- all bodyparts -- 
  private Bodypart spine_base;
  private Bodypart spine_mid;
  private Bodypart spine_shoulder;
  private Bodypart neck;
  private Bodypart head;
  private Bodypart shoulder_right;
  private Bodypart elbow_right;
  private Bodypart wrist_right;
  private Bodypart hand_right;
  private Bodypart thumb_right;
  private Bodypart hand_tip_right;
  private Bodypart shoulder_left;
  private Bodypart elbow_left;
  private Bodypart wrist_left;
  private Bodypart hand_left;
  private Bodypart thumb_left;
  private Bodypart hand_tip_left;
  private Bodypart hip_right;
  private Bodypart knee_right;
  private Bodypart ankle_right;
  private Bodypart foot_right;
  private Bodypart hip_left;
  private Bodypart knee_left;
  private Bodypart ankle_left;
  private Bodypart foot_left;
  
  private boolean[] lookedObjects = new boolean[ListOfObject3D.COUNT_OBJECT3D]; 
  
  /**
   * Boolean state, whether the skeleton comes from v2 sensor.
   */
  private boolean isSensorv2;
  
  /**
   * The skeleton of the person.
   */
  private Skeleton skeleton;

  /**
   * This constructor creates a {@link Body} object. 
   * All {@link Bodypart}s are initialized.
   * Set the boolean state.
   * 
   * @param isSensorv2
   */
  public Body(boolean isSensorv2)
  {
    skeleton = new Skeleton();
    spine_base = new Bodypart(this, null, Skeleton.SPINE_BASE);
    spine_mid = new Bodypart(this, spine_base, Skeleton.SPINE_MID);
    if(isSensorv2)
    {
      spine_shoulder = new Bodypart(this, spine_mid, Skeleton.SPINE_SHOULDER);
      neck = new Bodypart(this, spine_shoulder, Skeleton.NECK);
      neck.setViewEnable(true);
      shoulder_right = new Bodypart(this, spine_shoulder, Skeleton.SHOULDER_RIGHT);
      shoulder_left = new Bodypart(this, spine_shoulder, Skeleton.SHOULDER_LEFT);
    } else
    {
      neck = new Bodypart(this, spine_mid, Skeleton.NECK);
      shoulder_right = new Bodypart(this, neck, Skeleton.SHOULDER_RIGHT);
      shoulder_left = new Bodypart(this, neck, Skeleton.SHOULDER_LEFT);
    }
    
    head = new Bodypart(this, neck, Skeleton.HEAD);
    elbow_right = new Bodypart(this, shoulder_right, Skeleton.ELBOW_RIGHT);
    wrist_right = new Bodypart(this, elbow_right, Skeleton.WRIST_RIGHT);
    hand_right = new Bodypart(this, wrist_right, Skeleton.HAND_RIGHT);
    elbow_left = new Bodypart(this, shoulder_left, Skeleton.ELBOW_LEFT);
    wrist_left = new Bodypart(this, elbow_left, Skeleton.WRIST_LEFT);
    hand_left = new Bodypart(this, wrist_left, Skeleton.HAND_LEFT);
    hip_right = new Bodypart(this, spine_base, Skeleton.HIP_RIGHT);
    knee_right = new Bodypart(this, hip_right, Skeleton.KNEE_RIGHT);
    ankle_right = new Bodypart(this, knee_right, Skeleton.ANKLE_RIGHT);
    foot_right = new Bodypart(this, ankle_right, Skeleton.FOOT_RIGHT);
    hip_left = new Bodypart(this, spine_base, Skeleton.HIP_LEFT);
    knee_left = new Bodypart(this, hip_left, Skeleton.KNEE_LEFT);
    ankle_left = new Bodypart(this, knee_left, Skeleton.ANKLE_LEFT);
    foot_left = new Bodypart(this, ankle_left, Skeleton.FOOT_LEFT);
    
    if(isSensorv2)
    {
      thumb_left = new Bodypart(this, hand_left, Skeleton.THUMB_LEFT);
      hand_tip_left = new Bodypart(this, hand_left, Skeleton.HAND_TIP_LEFT);
      thumb_right = new Bodypart(this, hand_right, Skeleton.THUMB_RIGHT);
      hand_tip_right = new Bodypart(this, hand_right, Skeleton.HAND_TIP_RIGHT);
    }
    
    this.isSensorv2 = isSensorv2;
  }
  
  /**
   * Update the skeleton.
   * 
   * @param skeleton
   */
  public void setSkeleton(Skeleton skeleton)
  {
    this.skeleton = skeleton;
  }

  /**
   * 
   * @return The current skeleton.
   */
  public Skeleton getSkeleton()
  {
    return this.skeleton;
  }
  
  /**
   * 
   * @return The player id
   */
  public int getID()
  {
    return skeleton.getPlayerID();
  }

  /**
   * 
   * @return The tracking state of the person. 
   */
  public boolean isTracked()
  {
    return skeleton.isTracked();
  }

  /**
   * This method sets the current relative orientation for each joint.
   */
  public void setAllRelativOrientation()
  {
     if(isSensorv2)
     {
       head.rotate(0, 180, 0);
       spine_shoulder.setRelativeOrientation();
       spine_base.setRelativeOrientation(); 
       spine_mid.setRelativeOrientation();
       neck.setRelativeOrientation();
       shoulder_right.setRelativeOrientation();
       elbow_right.setRelativeOrientation();
       wrist_right.setRelativeOrientation();
       hand_right.setRelativeOrientation();
       thumb_right.rotate(0, 180, 0);
       hand_tip_right.rotate(0, 180, 0);
       shoulder_left.setRelativeOrientation();
       elbow_left.setRelativeOrientation();
       wrist_left.setRelativeOrientation();
       hand_left.setRelativeOrientation();
       thumb_left.rotate(0, 180, 0);
       hand_tip_left.rotate(0, 180, 0);
       hip_right.setRelativeOrientation();
       knee_right.setRelativeOrientation();
       ankle_right.setRelativeOrientation();
       foot_right.rotate(0, 180, 0);
       hip_left.setRelativeOrientation();
       knee_left.setRelativeOrientation();
       ankle_left.setRelativeOrientation();
       foot_left.rotate(0, 180, 0);
     } 
     else
     {
       head.rotate(0, 180, 0);
       spine_base.rotate(0,180,0); 
       spine_mid.rotate(0,180,0); 
       neck.rotate(0,180,0); 
       shoulder_right.rotate(0,180,0);
       elbow_right.rotate(0,180,0); 
       wrist_right.rotate(0,180,0); 
       hand_right.rotate(0,180,0); 
       shoulder_left.rotate(0,180,0); 
       elbow_left.rotate(0,180,0); 
       wrist_left.rotate(0,180,0); 
       hand_left.rotate(0,180,0);
       hip_right.rotate(0,180,0);
       knee_right.rotate(0,180,0);
       ankle_right.rotate(0,180,0);
       foot_right.rotate(0,180,0);
       hip_left.rotate(0,180,0);
       knee_left.rotate(0,180,0);
       ankle_left.rotate(0,180,0);
       foot_left.rotate(0,180,0);
     }    
  }

  /**
   * This method sets the current relative position for each joint.
   */
  public void setAllRelativePosition()
  {
    if(isSensorv2)
    {
      spine_shoulder.setRelativePosition();
      thumb_left.setRelativePosition();
      thumb_right.setRelativePosition();
      hand_tip_left.setRelativePosition();
      hand_tip_right.setRelativePosition();
    }
    
    head.setRelativePosition();
    neck.setRelativePosition();
    spine_mid.setRelativePosition();
    spine_base.setRelativePosition();
    spine_base.setRelativePosition();
    shoulder_right.setRelativePosition();
    elbow_right.setRelativePosition();
    wrist_right.setRelativePosition();
    hand_right.setRelativePosition();
    shoulder_left.setRelativePosition();
    elbow_left.setRelativePosition();
    wrist_left.setRelativePosition();
    hand_left.setRelativePosition();
    hip_right.setRelativePosition();
    knee_right.setRelativePosition();
    ankle_right.setRelativePosition();
    foot_right.setRelativePosition();
    hip_left.setRelativePosition();
    knee_left.setRelativePosition();
    ankle_left.setRelativePosition();
    foot_left.setRelativePosition();
  }

  /**
   * This method calculates the absolute position and orientation for each joint. 
   * 
   * @param camera
   */
  public void calculateAbsolute(Object3D camera)
  {
    if(isSensorv2)
    {
      spine_shoulder.calculateAbsolutePosition(camera);
      spine_shoulder.calculateAbsoluteOrientation(camera);
      thumb_left.calculateAbsolutePosition(camera);
      thumb_left.calculateAbsoluteOrientation(camera);
      thumb_right.calculateAbsolutePosition(camera);
      thumb_right.calculateAbsoluteOrientation(camera);
      hand_tip_left.calculateAbsolutePosition(camera);
      hand_tip_left.calculateAbsoluteOrientation(camera);
      hand_tip_right.calculateAbsolutePosition(camera);
      hand_tip_right.calculateAbsoluteOrientation(camera);
      spine_shoulder.update();
      thumb_left.update();
      thumb_right.update();
      hand_tip_left.update();
      hand_tip_right.update();
    }
    
    spine_base.calculateAbsolutePosition(camera);
    spine_base.calculateAbsoluteOrientation(camera);
    spine_mid.calculateAbsolutePosition(camera);
    spine_mid.calculateAbsoluteOrientation(camera);
    neck.calculateAbsolutePosition(camera);
    neck.calculateAbsoluteOrientation(camera);
    head.calculateAbsolutePosition(camera);
    head.calculateAbsoluteOrientation(camera);
    shoulder_right.calculateAbsolutePosition(camera);
    shoulder_right.calculateAbsoluteOrientation(camera);
    elbow_right.calculateAbsolutePosition(camera);
    elbow_right.calculateAbsoluteOrientation(camera);
    wrist_right.calculateAbsolutePosition(camera);
    wrist_right.calculateAbsoluteOrientation(camera);
    hand_right.calculateAbsolutePosition(camera);
    hand_right.calculateAbsoluteOrientation(camera);
    shoulder_left.calculateAbsolutePosition(camera);
    shoulder_left.calculateAbsoluteOrientation(camera);
    elbow_left.calculateAbsolutePosition(camera);
    elbow_left.calculateAbsoluteOrientation(camera);
    wrist_left.calculateAbsolutePosition(camera);
    wrist_left.calculateAbsoluteOrientation(camera);
    hand_left.calculateAbsolutePosition(camera);
    hand_left.calculateAbsoluteOrientation(camera);
    hip_right.calculateAbsoluteOrientation(camera);
    hip_right.calculateAbsolutePosition(camera);
    knee_right.calculateAbsoluteOrientation(camera);
    knee_right.calculateAbsolutePosition(camera);
    ankle_right.calculateAbsoluteOrientation(camera);
    ankle_right.calculateAbsolutePosition(camera);
    foot_right.calculateAbsoluteOrientation(camera);
    foot_right.calculateAbsolutePosition(camera);
    hip_left.calculateAbsoluteOrientation(camera);
    hip_left.calculateAbsolutePosition(camera);
    knee_left.calculateAbsoluteOrientation(camera);
    knee_left.calculateAbsolutePosition(camera);
    ankle_left.calculateAbsoluteOrientation(camera);
    ankle_left.calculateAbsolutePosition(camera);
    foot_left.calculateAbsoluteOrientation(camera);
    foot_left.calculateAbsolutePosition(camera);
    
    spine_base.update();
    spine_mid.update();   
    neck.update();   
    head.update();   
    shoulder_right.update();    
    elbow_right.update();   
    wrist_right.update();   
    hand_right.update();  
    shoulder_left.update();    
    elbow_left.update();   
    wrist_left.update();    
    hand_left.update();
    hip_right.update();  
    knee_right.update();   
    ankle_right.update();  
    foot_right.update();   
    hip_left.update();  
    knee_left.update();  
    ankle_left.update();
    foot_left.update();
   
  }

  /**
   * This method draws the joints in the coordinate space of
   * the cognitive system lab.
   * 
   * @param gl
   */
  public void draw(GL2 gl)
  {
    if(isSensorv2)
    {
      spine_shoulder.draw(gl);
      thumb_left.draw(gl);
      thumb_right.draw(gl);
      hand_tip_left.draw(gl);
      hand_tip_right.draw(gl);
    }
    head.draw(gl);
    neck.draw(gl);
    spine_base.draw(gl);
    spine_mid.draw(gl);
    shoulder_right.draw(gl);
    elbow_right.draw(gl);
    wrist_right.draw(gl);
    hand_right.draw(gl);
    shoulder_left.draw(gl);
    elbow_left.draw(gl);
    wrist_left.draw(gl);
    hand_left.draw(gl);
    hip_right.draw(gl);
    knee_right.draw(gl);
    ankle_right.draw(gl);
    foot_right.draw(gl);
    hip_left.draw(gl);
    knee_left.draw(gl);
    ankle_left.draw(gl);
    foot_left.draw(gl);
  }

  /**
   * This methods draws the skeleton in the coordinate space
   * of OpenGL
   * 
   * @param gl
   */
  public void drawSkeleton(GL2 gl)
  {
    if(gl == null) return;
    gl.glColor3f(0, 1, 0);
    skeleton.draw(gl);
  }
  
  /**
   * 
   * @return the rounded position (cm precision) of the head
   */
  public Vector3f getHeadPosition()
  {
    Vector3f rounded = new Vector3f();

    rounded.setX((float) ((int) (head.getAbsolutePosition().getX() * 100)) / 100f);
    rounded.setY((float) ((int) (head.getAbsolutePosition().getY() * 100)) / 100f);
    rounded.setZ((float) ((int) (head.getAbsolutePosition().getZ() * 100)) / 100f);
    return rounded;
  }

  // -- getter of all Bodyparts --
  
  public Bodypart getHead()
  {
    return head;
  }
  
  public Bodypart getWrist_right() 
  {
    return wrist_right;
  }

  public void setWrist_right(Bodypart wrist_right) 
  {
    this.wrist_right = wrist_right;
  }

  public Bodypart getWrist_left()
  {
    return wrist_left;
  }

  public void setWrist_left(Bodypart wrist_left) 
  {
    this.wrist_left = wrist_left;
  }

  public Bodypart getSpine_base() 
  {
    return spine_base;
  }

  public Bodypart getSpine_mid() 
  {
    return spine_mid;
  }

  public Bodypart getSpine_shoulder() 
  {
    return spine_shoulder;
  }

  public Bodypart getNeck() 
  {
    return neck;
  }

  public Bodypart getShoulder_right()
  {
    return shoulder_right;
  }

  public Bodypart getElbow_right()
  {
    return elbow_right;
  }

  public Bodypart getHand_right() 
  {
    return hand_right;
  }

  public Bodypart getThumb_right()
  {
    return thumb_right;
  }

  public Bodypart getHand_tip_right() 
  {
    return hand_tip_right;
  }

  public Bodypart getShoulder_left() 
  {
    return shoulder_left;
  }

  public Bodypart getElbow_left() 
  {
    return elbow_left;
  }

  public Bodypart getHand_left() 
  {
    return hand_left;
  }

  public Bodypart getThumb_left() 
  {
    return thumb_left;
  }

  public Bodypart getHand_tip_left()
  {
    return hand_tip_left;
  }

  public Bodypart getHip_right() 
  {
    return hip_right;
  }

  public Bodypart getKnee_right() 
  {
    return knee_right;
  }

  public Bodypart getAnkle_right() 
  {
    return ankle_right;
  }

  public Bodypart getFoot_right() 
  {
    return foot_right;
  }

  public Bodypart getHip_left() 
  {
    return hip_left;
  }

  public Bodypart getKnee_left() 
  {
    return knee_left;
  }

  public Bodypart getAnkle_left() 
  {
    return ankle_left;
  }

  public Bodypart getFoot_left() 
  {
    return foot_left;
  }

  public boolean isSensorv2() 
  {
    return isSensorv2;
  }

  public boolean getLooksToObject(int type) 
  {
    return lookedObjects[type];
  }
  
  public void setLooksToObject(int type, boolean isLooked) 
  {
    lookedObjects[type] = isLooked;
  }
}
