package de.tucottbus.kt.csl.hardware.kinect.body;
import javax.vecmath.Vector3f;

import de.tucottbus.kt.csl.hardware.kinect.math.Quaternion;
import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;

/**
 * This class represents a joint as a virtual room object.
 * This class inherits from {@link Object3D}
 * 
 * @author Thomas Jung
 *
 */
public class Bodypart extends Object3D
{
  /**
   * Body wrapper of this joint.
   */
  private Body body;
  
  /**
   * The parent joint of the bone.
   */
  private Bodypart parent;
  
  /**
   * joint type
   */
  private int type;

  /**
   * This constructor creates an {@link Bodypart} object.
   * 
   * @param body
   * @param parent
   * @param type
   */
  public Bodypart(Body body, Bodypart parent, int type)
  {
    super();
    this.body = body;
    this.parent = parent;
    this.type = type;
  }

  /**
   * This method set the relative position for the specified joint
   */
  public void setRelativePosition()
  {
    Vector3f relPos = new Vector3f();
    relPos.setX(body.getSkeleton().get3DJointX(type));
    relPos.setY(body.getSkeleton().get3DJointY(type));
    relPos.setZ(body.getSkeleton().get3DJointZ(type));
    super.setRelativePosition(relPos);
    if(parent == null)
    {
      setSize(new Vector3f(0.05f, -0.05f, 0.05f));
      setColor(new Vector3f(0, 0, 1));
      return;
    }
    Vector3f bone = new Vector3f();
    bone.add(getRelativePosition());
    bone.sub(parent.getRelativePosition());
    setSize(new Vector3f(0.05f, bone.length() / 2f, -0.05f));
    setColor(new Vector3f(0, 0.5f, (float) body.getID() / 10f + 0.4f));
  }

  /**
   * This method set the relative orientation for the specified joint
   */
  public void setRelativeOrientation()
  {
    float x = body.getSkeleton().getJointOrientations()[type * 4 + 0];
    float y = body.getSkeleton().getJointOrientations()[type * 4 + 1];
    float z = body.getSkeleton().getJointOrientations()[type * 4 + 2];
    float w = body.getSkeleton().getJointOrientations()[type * 4 + 3];
    super.setRelativeOrientation(new Quaternion(x, y, z, w));
  }
}
