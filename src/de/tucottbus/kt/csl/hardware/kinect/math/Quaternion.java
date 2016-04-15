package de.tucottbus.kt.csl.hardware.kinect.math;

import javax.vecmath.Vector3f;


public class Quaternion
{
  private float x;
  private float y;
  private float z;
  private float w;

  public Quaternion()
  {
    this.x = 0;
    this.y = 0;
    this.z = 0;
    this.w = 0;
  }

  public Quaternion(float x, float y, float z, float w)
  {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
  }

  public Quaternion(Vector3f v, float angle)
  {
    float a = (float) Math.toRadians(angle);
    float sin = (float) Math.sin(a / 2);
    float cos = (float) Math.cos(a / 2);
    this.x = v.getX() * sin;
    this.y = v.getY() * sin;
    this.z = v.getZ() * sin;
    this.w = cos;
  }

  public Quaternion(Vector3f v)
  {
    this.x = v.getX();
    this.y = v.getY();
    this.z = v.getZ();
  }
  
  /**
   * 
   * @return euler angles of this quaternion
   */
  public Vector3f getEulerAngles()
  {
    Vector3f euler = new Vector3f();
    float w_2 = w * w;
    float x_2 = x * x;
    float y_2 = y * y;
    float z_2 = z * z;
    euler.setX((float) Math.toDegrees(Math.atan(2 * (x * y + w * z) / (w_2 + x_2 - y_2 - z_2))));
    euler.setY((float) Math.toDegrees(Math.atan(2 * (w * y - x * z))));
    euler.setZ(- (float) Math.toDegrees(Math.atan(2 * (y * z + w * x) / - (w_2 - x_2 - y_2 + z_2))));
    return euler;
  }

  /**
   * rotates a vector with this quaternion
   * @param v
   * @return
   */
  public Vector3f rotateVector(Vector3f v)
  {
    Quaternion p = new Quaternion(v);
    Quaternion res = this.mul(p).mul(this.conjugate());
    return res.getRotAxis();
  }

  public Vector3f getRotAxis()
  {
    return new Vector3f(x, y, z);
  }
  
  /**
   * calculate a quaternion between two vectors 
   * @param a
   * @param b
   * @return
   */
  public static Quaternion createQuaternionBetweenVectors(Vector3f a, Vector3f b)
  {
    a.normalize();
    b.normalize();
    
    Vector3f rotAxis = new Vector3f();
    rotAxis.cross(a, b);
    rotAxis.normalize();
    
    float cos = a.dot(b);
    float angle = (float) Math.toDegrees(Math.acos(cos));
    
    return new Quaternion(rotAxis, angle);
  }

  /**
   * 
   * @return the length of this quaternion
   */
  public float length()
  {
    return (float) Math.sqrt(x * x + y * y + z * z + w * w);
  }

  /**
   * normalized this quaternion
   * @return
   */
  public Quaternion normalized()
  {
    float length = length();

    return new Quaternion(x / length, y / length, z / length, w / length);
  }

  /**
   * conjugate this quaternion
   * @return
   */
  public Quaternion conjugate()
  {
    return new Quaternion(-x, -y, -z, w);
  }

  /**
   * multiply this quaternion with quaternion r
   * @param r
   * @return
   */
  public Quaternion mul(Quaternion r)
  {
    float w_ = w * r.getW() - x * r.getX() - y * r.getY() - z * r.getZ();
    float x_ = x * r.getW() + w * r.getX() - z * r.getY() + y * r.getZ();
    float y_ = y * r.getW() + z * r.getX() + w * r.getY() - x * r.getZ();
    float z_ = z * r.getW() - y * r.getX() + x * r.getY() + w * r.getZ();

    return new Quaternion(x_, y_, z_, w_);
  }

  public float getX()
  {
    return x;
  }

  /**
   * set x value of this quaternion
   * 
   * @param x
   */
  public void setX(float x)
  {
    this.x = x;
  }

  public float getY()
  {
    return y;
  }

  /**
   * set y value of this quaternion
   * @param y
   */
  public void setY(float y)
  {
    this.y = y;
  }

  public float getZ()
  {
    return z;
  }

  /**
   * set z value of this quaternion
   * @param z
   */
  public void setZ(float z)
  {
    this.z = z;
  }

  public float getW()
  {
    return w;
  }

  /**
   * set w value of this quaternion
   * @param w
   */
  public void setW(float w)
  {
    this.w = w;
  }

  @Deprecated
  public void print()
  {
    System.out.println("w: " + w + " x: " + x + " y: " + y + " z:" + z);
  }
}
