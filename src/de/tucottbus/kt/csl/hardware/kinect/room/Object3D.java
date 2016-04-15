package de.tucottbus.kt.csl.hardware.kinect.room;

import javax.media.opengl.GL2;
import javax.vecmath.Vector3f;

import de.tucottbus.kt.csl.hardware.kinect.body.Body;
import de.tucottbus.kt.csl.hardware.kinect.math.Quaternion;

/**
 * This class representing a virtual object for calculation and
 * visualization in {@link de.tucottbus.kt.csl.hardware.kinect.gui.RoomEditor}
 * @author Thomas Jung
 *
 */
public class Object3D
{
  
  @Override
  public String toString() 
  {
    return name + "";
  }

  private String name;
  
  /**
   * a recognizable name
   * @param name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * the relative position of this object
   */
  private Vector3f relativePosition;
  
  /**
   * the absolute position of this object
   */
  private Vector3f absolutePosition;
  
  /**
   *  a vector with (0, 0, 0)
   */
  private Vector3f origin;

  /**
   * the relative orientation of this object
   */
  private Quaternion relativeOrientation;
  
  /**
   * the relative orientation of this object
   */
  private Quaternion absoluteOrientation;
  
  /**
   * rotation angle about x axis
   */
  private float eulerX;
  
  /**
   * rotation angle about y axis
   */
  private float eulerY;
  
  /**
   * rotation angle about z axis
   */
  private float eulerZ;
  
  public float getEulerX()
  {
    return eulerX;
  }
  
  public float getEulerY()
  {
    return eulerY;
  }
  
  public float getEulerZ()
  {
    return eulerZ;
  }
  
  /**
   * the color of the object
   */
  private Vector3f color;
  
  /**
   * the view of the object
   */
  private Vector3f absoluteView;
  
  /**
   *  the types of the object
   */
  public static final int NOTHING = -1;
  public static final int SENSOR1_000 = 0;
  public static final int SENSOR2_000 = 1;
  public static final int DISPLAY = 2;
 
  /**
   * the object types
   */
  private int type;
  
  
  /**
   * set object type to identify
   * @param type e.g. Object3D.Display
   */
  public void setType(int type)
  {
    this.type = type;
  }
  
  public int getType()
  {
    return type;
  }
  
  private Vector3f size;
  
  /**
   * contents the first rotation in local space
   */
  private Quaternion firstRelativRotation;
  
  /**
   * contents the second rotation in local space
   */
  private Quaternion secondRelativRotation;
  
  /**
   * contents the third rotation in local space
   */
  private Quaternion thirdRelativRotation;
  
  /**
   * contents the first rotation in global space
   */
  private Quaternion firstAbsolutRotation;
  
  /**
   * contents the second rotation in global space
   */
  private Quaternion secondAbsolutRotation;
  
  /**
   * contents the third rotation in global space
   */
  private Quaternion thirdAbsolutRotation;
  
  /**
   * enable for drawing the local coordinate system
   */
  private boolean coordinateSystemEnable = false;
  
  /**
   * enable for drawing the rotation planes
   */
  private boolean rotationPlanesEnable = false;
  
  /**
   * enable for drawing the view
   */
  private boolean viewEnable = false;
  
  // the corners of the object
  private Vector3f relativeLeftBottomAhead;
  private Vector3f relativeRightBottomAhead;
  private Vector3f relativeLeftBottomBehind;
  private Vector3f relativeRightBottomBehind;
  private Vector3f relativeLeftTopAhead;
  private Vector3f relativeRightTopAhead;
  private Vector3f relativeLeftTopBehind;
  private Vector3f relativeRightTopBehind;
  
  private Vector3f absoluteLeftBottomAhead;
  private Vector3f absoluteRightBottomAhead;
  private Vector3f absoluteLeftBottomBehind;
  private Vector3f absoluteRightBottomBehind;
  private Vector3f absoluteLeftTopAhead;
  private Vector3f absoluteRightTopAhead;
  private Vector3f absoluteLeftTopBehind;
  private Vector3f absoluteRightTopBehind;
  
  public Object3D()
  {
    super();
    relativePosition = new Vector3f();
    absolutePosition = new Vector3f();
    origin = new Vector3f();
    absoluteView = new Vector3f();
    relativeOrientation = new Quaternion();
    absoluteOrientation = new Quaternion();
    
    firstRelativRotation = new Quaternion();
    secondRelativRotation = new Quaternion();
    thirdRelativRotation = new Quaternion();
    
    firstAbsolutRotation = new Quaternion();
    secondAbsolutRotation = new Quaternion();
    thirdAbsolutRotation = new Quaternion();

    setSize(new Vector3f());
    color = new Vector3f(1, 1, 1);
    setType(NOTHING);
    rotate(0, 0, 0);
    calculateAbsoluteOrientation(null);
    calculateAbsolutePosition(null);
  }
  
  /**
   * set the size of object
   * 
   * @param width
   * @param height
   * @param length
   */
  public void setSize(Vector3f size)
  {
    relativeLeftBottomAhead = new Vector3f(-size.getX()/2 ,size.getY()/2 , 0);
    relativeRightBottomAhead = new Vector3f(size.getX()/2 ,size.getY()/2 , 0);
    relativeLeftBottomBehind = new Vector3f(-size.getX()/2 ,-size.getY()/2 , 0);
    relativeRightBottomBehind = new Vector3f(size.getX()/2 ,-size.getY()/2 , 0);
    relativeLeftTopAhead = new Vector3f(-size.getX()/2 ,size.getY()/2 , size.getZ());
    relativeRightTopAhead = new Vector3f(size.getX()/2 ,size.getY()/2 , size.getZ());
    relativeLeftTopBehind = new Vector3f(-size.getX()/2 ,-size.getY()/2 , size.getZ());
    relativeRightTopBehind = new Vector3f(size.getX()/2 ,-size.getY()/2 , size.getZ());
    this.size = size;
  }
  
  public Vector3f getSize()
  {
    return size;
  }
  
  /**
   * set color of the object
   * @param color
   */
  public void setColor(Vector3f color)
  {
    this.color = color;
  }
  
  /**
   * get the color of the object
   * @return
   */
  public Vector3f getColor()
  {
    return color;
  }
  
  /**
   * get the view of the object
   * @return
   */
  public Vector3f getView()
  {
    return absoluteView;
  }

  /**
   * get the relative position
   * @return
   */
  public Vector3f getRelativePosition()
  {
    return relativePosition;
  }

  /**
   * set the relative Position of the object
   * @param relativePosition
   */
  public void setRelativePosition(Vector3f relativePosition)
  {
    this.relativePosition = relativePosition;
  }

  /**
   * get the absolute position
   * @return
   */
  public Vector3f getAbsolutePosition()
  {
    return absolutePosition;
  }

  /**
   * set the absolute position of the object
   * @param absolutePosition
   */
  public void setAbsolutePosition(Vector3f absolutePosition)
  {
    this.absolutePosition = absolutePosition;
  }

  /**
   * get the relative orientation
   * @return
   */
  public Quaternion getRelativeOrientation()
  {
    return relativeOrientation;
  }

  /**
   * set the relative orientation of the object
   * @param relativeOrientation
   */
  public void setRelativeOrientation(Quaternion relativeOrientation)
  {
    this.relativeOrientation = relativeOrientation;
  }

  /**
   * get the absolute orientation
   * @return
   */
  public Quaternion getAbsoluteOrientation()
  {
    return absoluteOrientation;
  }

  /**
   * set the absolute orientation of the object
   * @param absoluteOrientation
   */
  public void setAbsolutOrientation(Quaternion absolutOrientation)
  {
    this.absoluteOrientation = absolutOrientation;
  }

  /**
   * rotate this object with euler angles in zxy order
   * 
   * @param xAngle
   * @param yAngle
   * @param zAngle
   */
  public void rotate(float xAngle, float yAngle, float zAngle)
  {
    eulerX = xAngle;
    eulerY = yAngle;
    eulerZ = zAngle;
    
    Vector3f xAxis = new Vector3f(1, 0, 0);
    Vector3f yAxis = new Vector3f(0, 1, 0);
    Vector3f zAxis = new Vector3f(0, 0, 1);

    Quaternion qx = new Quaternion(xAxis, xAngle);
    Quaternion qy = new Quaternion(yAxis, yAngle);
    Quaternion qz = new Quaternion(zAxis, zAngle);
    
    firstRelativRotation = qz;
    secondRelativRotation = firstRelativRotation.mul(qx);
    thirdRelativRotation = secondRelativRotation.mul(qy);
   
    relativeOrientation = thirdRelativRotation;
  }
  
  /**
   * Calculates the absolute orientation.
   * Without a parent the object has already 
   * an absolute Orientation.
   * @param parent
   */
  public void calculateAbsoluteOrientation(Object3D parent)
  {
    if (parent == null)
    {
      absoluteOrientation = relativeOrientation;
      firstAbsolutRotation = firstRelativRotation;
      secondAbsolutRotation = secondRelativRotation;
      thirdAbsolutRotation = thirdRelativRotation;
      return;
    }
    
    absoluteOrientation = parent.getAbsoluteOrientation().mul(relativeOrientation);
    firstAbsolutRotation = parent.getAbsoluteOrientation().mul(firstRelativRotation);
    secondAbsolutRotation = parent.getAbsoluteOrientation().mul(secondRelativRotation);
    thirdAbsolutRotation = parent.getAbsoluteOrientation().mul(thirdRelativRotation);
    
    
  }

  /**
   * calculate the visualization
   */
  public void update()
  {
    absoluteLeftBottomAhead = absoluteOrientation.rotateVector(relativeLeftBottomAhead);
    absoluteRightBottomAhead = absoluteOrientation.rotateVector(relativeRightBottomAhead);
    absoluteLeftBottomBehind = absoluteOrientation.rotateVector(relativeLeftBottomBehind);
    absoluteRightBottomBehind = absoluteOrientation.rotateVector(relativeRightBottomBehind);
    absoluteLeftTopAhead = absoluteOrientation.rotateVector(relativeLeftTopAhead);
    absoluteRightTopAhead= absoluteOrientation.rotateVector(relativeRightTopAhead);
    absoluteLeftTopBehind = absoluteOrientation.rotateVector(relativeLeftTopBehind);
    absoluteRightTopBehind = absoluteOrientation.rotateVector(relativeRightTopBehind);
    absoluteView = absoluteOrientation.rotateVector(new Vector3f(0, 0, 5));
  }
  
  /**
   * calculates the absolute position. Without a parent the object is already in 
   * absolute Position
   * @param parent
   */
  public void calculateAbsolutePosition(Object3D parent)
  {
    if (parent == null)
    {
      absolutePosition = relativePosition;
      return;
    }
    Vector3f tmp = parent.getAbsoluteOrientation().rotateVector(relativePosition);
    absolutePosition.add(tmp, parent.getAbsolutePosition());
  }
  
  // drawing methods

  /**
   * draw the object in 3D editor
   * @param gl
   */
  public void draw(GL2 gl)
  { 
    
    
    gl.glColor3f(color.getX(), color.getY(), color.getZ());
    drawLine(gl, absolutePosition, absoluteLeftBottomAhead, absoluteRightBottomAhead);
    drawLine(gl, absolutePosition, absoluteLeftBottomAhead, absoluteLeftBottomBehind);
    drawLine(gl, absolutePosition, absoluteRightBottomAhead, absoluteRightBottomBehind);
    drawLine(gl, absolutePosition, absoluteLeftBottomBehind, absoluteRightBottomBehind);
    drawLine(gl, absolutePosition, absoluteLeftTopAhead, absoluteRightTopAhead);
    drawLine(gl, absolutePosition, absoluteLeftTopAhead, absoluteLeftTopBehind);
    drawLine(gl, absolutePosition, absoluteRightTopAhead, absoluteRightTopBehind);
    drawLine(gl, absolutePosition, absoluteLeftTopBehind, absoluteRightTopBehind);
    drawLine(gl, absolutePosition, absoluteLeftBottomAhead, absoluteLeftTopAhead);
    drawLine(gl, absolutePosition, absoluteRightBottomAhead, absoluteRightTopAhead);
    drawLine(gl, absolutePosition, absoluteLeftBottomBehind, absoluteLeftTopBehind);
    drawLine(gl, absolutePosition, absoluteRightBottomBehind, absoluteRightTopBehind);
    
    drawCoordinateSystem(gl);
    drawRotationPlanes(gl);
    drawView(gl);
  }
  
  /**
   * draws a line between two position vectors in relation to pos
   * 
   * @param gl
   * @param pos
   * @param start
   * @param end
   */
  protected void drawLine(GL2 gl, Vector3f pos, Vector3f start, Vector3f end)
  {
    gl.glBegin(GL2.GL_LINES);
    gl.glVertex3f(start.getX() + pos.getX(), start.getY() + pos.getY(),
        start.getZ() + pos.getZ());
    gl.glVertex3f(end.getX() + pos.getX(), end.getY() + pos.getY(), end.getZ()
        + pos.getZ());
    gl.glEnd();
  }
  
  /**
   * draws the local coordinate system of this object
   * 
   * (x axis is red,
   * y axis is green,
   * z axis is blue)
   * 
   * @param gl
   */
  private void drawCoordinateSystem(GL2 gl)
  {
    if(!coordinateSystemEnable) return;
    
    Vector3f xA1 = new Vector3f(1f, 0, 0);
    Vector3f yA1 = new Vector3f(0, 1f, 0);
    Vector3f zA1 = new Vector3f(0, 0, 1f);

    Vector3f xA = absoluteOrientation.rotateVector(xA1);
    Vector3f yA = absoluteOrientation.rotateVector(yA1);
    Vector3f zA = absoluteOrientation.rotateVector(zA1);

    gl.glColor3f(1, 0, 0);
    drawLine(gl, absolutePosition, origin, xA);

    gl.glColor3f(0, 1, 0);
    drawLine(gl, absolutePosition, origin, yA);

    gl.glColor3f(0, 0, 1);
    drawLine(gl, absolutePosition, origin, zA);
  }
  
  /**
   * draws the rotation planes of this object
   * 
   * (x plane is red,
   * y plane is green,
   * z plane is blue)
   * 
   * @param gl
   */
  private void drawRotationPlanes(GL2 gl)
  {
    if(!rotationPlanesEnable) return;
    
    Vector3f xA1 = new Vector3f(0, 0.3f, 0.3f);
    Vector3f xB1 = new Vector3f(0, -0.3f, 0.3f);
    Vector3f xC1 = new Vector3f(0, 0.3f, -0.3f);
    Vector3f xD1 = new Vector3f(0, -0.3f, -0.3f);
    
    Vector3f xA = secondAbsolutRotation.rotateVector(xA1);
    Vector3f xB = secondAbsolutRotation.rotateVector(xB1);
    Vector3f xC = secondAbsolutRotation.rotateVector(xC1);
    Vector3f xD = secondAbsolutRotation.rotateVector(xD1);
    
    gl.glColor3f(1, 0, 0);
    drawLine(gl, absolutePosition, xA, xB);
    drawLine(gl, absolutePosition, xB, xD);
    drawLine(gl, absolutePosition, xC, xD);
    drawLine(gl, absolutePosition, xA, xC);
    
    Vector3f yA1 = new Vector3f(0.3f, 0, 0.3f);
    Vector3f yB1 = new Vector3f(-0.3f, 0, 0.3f);
    Vector3f yC1 = new Vector3f(0.3f, 0, -0.3f);
    Vector3f yD1 = new Vector3f(-0.3f, 0, -0.3f);
   
    Vector3f yA = thirdAbsolutRotation.rotateVector(yA1);
    Vector3f yB = thirdAbsolutRotation.rotateVector(yB1);
    Vector3f yC = thirdAbsolutRotation.rotateVector(yC1);
    Vector3f yD = thirdAbsolutRotation.rotateVector(yD1);
    
    gl.glColor3f(0, 1, 0);
    drawLine(gl, absolutePosition, yA, yB);
    drawLine(gl, absolutePosition, yB, yD);
    drawLine(gl, absolutePosition, yC, yD);
    drawLine(gl, absolutePosition, yA, yC);
    
    Vector3f zA1 = new Vector3f(0.3f, 0.3f, 0);
    Vector3f zB1 = new Vector3f(-0.3f, 0.3f, 0);
    Vector3f zC1 = new Vector3f(0.3f, -0.3f, 0);
    Vector3f zD1 = new Vector3f(-0.3f, -0.3f, 0);
   
    Vector3f zA = firstAbsolutRotation.rotateVector(zA1);
    Vector3f zB = firstAbsolutRotation.rotateVector(zB1);
    Vector3f zC = firstAbsolutRotation.rotateVector(zC1);
    Vector3f zD = firstAbsolutRotation.rotateVector(zD1);
    
    gl.glColor3f(0, 0, 1);
    drawLine(gl, absolutePosition, zA, zB);
    drawLine(gl, absolutePosition, zB, zD);
    drawLine(gl, absolutePosition, zC, zD);
    drawLine(gl, absolutePosition, zA, zC);
   
  }
  
  /**
   * draws the view of this object 
   * 
   * (color is yellow)
   * 
   * @param gl
   */
  private void drawView(GL2 gl)
  {
    if(!viewEnable) return;
    
    gl.glColor3f(1, 1, 0);
    drawLine(gl, absolutePosition, origin, absoluteView);
  }
  
  /**
   * checked if the view line is intersected with this object
   * @param position
   * @param view
   * @return
   */
  public void calculateLookedBy(Body body)
  { 
    boolean result = false;
    Vector3f absoluteLeftEdge = new Vector3f();
    Vector3f absoluteBehindEdge = new Vector3f();
    absoluteLeftEdge.sub(absoluteLeftBottomAhead, absoluteLeftBottomBehind);
    absoluteBehindEdge.sub(absoluteRightBottomBehind, absoluteLeftBottomBehind);
    
    Vector3f pos = new Vector3f();
    pos.add(absolutePosition, absoluteLeftBottomBehind);
    
    Vector3f r = new Vector3f();
    if(solve3f(setSystemOfLinearEquationOf3(body.getNeck().getAbsolutePosition(), pos, 
        absoluteLeftEdge, absoluteBehindEdge, body.getNeck().getView()), r))
    {
      result = (r.getX() >= 0 && r.getY() >= 0 && r.getX() <= 1 && r.getY() <= 1 && r.getZ() >= 0);
    }
    body.setLooksToObject(type, result);
  }
  
  //System of Linear Equation position - pos = le + be - view
  private float[][] setSystemOfLinearEquationOf3(Vector3f position, Vector3f pos,Vector3f le, Vector3f be, Vector3f view)
  {
    float[][] equation = new float[3][4];
    
    equation[0][0] = le.getX();
    equation[1][0] = le.getY();
    equation[2][0] = le.getZ();

    equation[0][1] = be.getX();
    equation[1][1] = be.getY();
    equation[2][1] = be.getZ();

    equation[0][2] = -view.getX();
    equation[1][2] = -view.getY();
    equation[2][2] = -view.getZ();

    equation[0][3] = position.getX() - pos.getX();
    equation[1][3] = position.getY() - pos.getY();
    equation[2][3] = position.getZ() - pos.getZ(); 

    return equation;
  }
  
  private boolean solve3f(float[][] eq, Vector3f r)
  {
    for (int p = 0; p < 3; p++)
    {
      // find pivot row and swap
      int max = p;
      for (int i = p + 1; i < 3; i++)
      {
        if (Math.abs(eq[i][p]) > Math.abs(eq[max][p]))
        {
          max = i;
        }
      }
      float[] temp = eq[p];
      eq[p] = eq[max];
      eq[max] = temp;

      // singular or nearly singular
      if (Math.abs(eq[p][p]) <= 1e-6f)
      {
        return false;
      }

      // pivot within A and b
      for (int i = p + 1; i < 3; i++)
      {
        float alpha = eq[i][p] / eq[p][p];
        eq[i][3] -= alpha * eq[p][3];
        for (int j = p; j < 3; j++)
        {
          eq[i][j] -= alpha * eq[p][j];
        }
      }
    }

    // back substitution
    float[] x = new float[3];
    for (int i = 3 - 1; i >= 0; i--)
    {
      float sum = 0.0f;
      for (int j = i + 1; j < 3; j++)
      {
        sum += eq[i][j] * x[j];
      }
      x[i] = (eq[i][3] - sum) / eq[i][i];
    }
    r.set(x);
    return true;
  }
  
  // enable switcher
  
  /**
   * switch on/off the coordinate system
   */
  public void setCoordinateSystemEnable(boolean coordinateSystemEnable)
  {
    this.coordinateSystemEnable = coordinateSystemEnable;
  }
  
  /**
   * return on/ off status of the coordinate system
   * 
   * @return
   */
  public boolean getCoordinateSystemEnable()
  {
    return coordinateSystemEnable;
  }
  
  /**
   * switch on/off the rotation planes
   */
  public void setRotationPlanesEnable(boolean rotationPlanesEnable)
  {
    this.rotationPlanesEnable = rotationPlanesEnable;
  }
  
  /**
   * return on/ off status of the rotation planes
   * 
   * @return
   */
  public boolean getRotationPlanesEnable()
  {
    return rotationPlanesEnable;
  }
  
  /**
   * switch on/off the view line
   */
  public void setViewEnable(boolean viewEnable)
  {
    this.viewEnable = viewEnable;
  }
  
  /**
   * return on/ off status of the view
   * 
   * @return
   */
  public boolean getViewEnable()
  {
    return viewEnable;
  }
}
