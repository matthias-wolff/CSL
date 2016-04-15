package org.selman.java3d;

/**********************************************************
 Copyright (C) 2001   Daniel Selman

 First distributed with the book "Java 3D Programming"
 by Daniel Selman and published by Manning Publications.
 http://manning.com/selman

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation, version 2.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 The license can be found on the WWW at:
 http://www.fsf.org/copyleft/gpl.html

 Or by writing to:
 Free Software Foundation, Inc.,
 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 Authors can be contacted at:
 Daniel Selman: daniel@selman.org

 If you make changes you think others would like, please 
 contact one of the authors or someone at the 
 www.j3d.org web site.
 **************************************************************/

import javax.media.j3d.Appearance;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.NodeComponent;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Primitive;



/**
 * Cuboid is a geometry primitive created with a given length, width, and
 * height. It is centered at the origin. By default, it lies within the bounding
 * Cuboid, [-1,-1,-1] and [1,1,1].
 * 
 * When a texture is applied to a Cuboid, it is map CCW like on a Cylinder. A
 * texture is mapped CCW from the back of the body. The top and bottom faces are
 * mapped such that the texture appears front facing when the faces are rotated
 * 90 toward the viewer.
 */

public class Cuboid extends Primitive {

  protected int numTris = 0;

  protected int numVerts = 0;

  /**
   * Primitive flags.
   */
  protected int flags;

  /**
   * Used to designate the front side of the Cuboid when using getShape().
   * 
   * @see Cuboid#getShape
   */
  public static final int FRONT = 0;

  /**
   * Used to designate the back side of the Cuboid when using getShape().
   * 
   * @see Cuboid#getShape
   */
  public static final int BACK = 1;

  /**
   * Used to designate the right side of the Cuboid when using getShape().
   * 
   * @see Cuboid#getShape
   */
  public static final int RIGHT = 2;

  /**
   * Used to designate the left side of the Cuboid when using getShape().
   * 
   * @see Cuboid#getShape
   */
  public static final int LEFT = 3;

  /**
   * Used to designate the top side of the Cuboid when using getShape().
   * 
   * @see Cuboid#getShape
   */
  public static final int TOP = 4;

  /**
   * Used to designate the bottom side of the Cuboid when using getShape().
   * 
   * @see Cuboid#getShape
   */
  public static final int BOTTOM = 5;

  float xDim, yDim, zDim;

  /**
   * Constructs a default Cuboid of 1.0 in all dimensions.
   */

  public Cuboid() {
    this(1.0f, 1.0f, 1.0f, GENERATE_NORMALS, null);
  }

  @Override
  public Appearance getAppearance(int index) {
    return null;
  }

  /**
   * Constructs a Cuboid of a given dimension and appearance.
   * 
   * @param xdim
   *          X-dimension size.
   * @param ydim
   *          Y-dimension size.
   * @param zdim
   *          Z-dimension size.
   * @param ap
   *          Appearance
   */

  public Cuboid(float xdim, float ydim, float zdim, Appearance ap) {
    this(xdim, ydim, zdim, GENERATE_NORMALS, ap);
  }

  /**
   * Constructs a Cuboid of a given dimension, flags, and appearance.
   * 
   * @param xdim
   *          X-dimension size.
   * @param ydim
   *          Y-dimension size.
   * @param zdim
   *          Z-dimension size.
   * @param primflags
   *          primitive flags.
   * @param ap
   *          Appearance
   */

  public Cuboid(float xdim, float ydim, float zdim, int primflags, Appearance ap) {
    int i;
    double sign;

    xDim = xdim;
    yDim = ydim;
    zDim = zdim;
    flags = primflags;

    // Depending on whether normal inward bit is set.
    if ((flags & GENERATE_NORMALS_INWARD) != 0)
      sign = -1.0;
    else
      sign = 1.0;

    TransformGroup objTrans = new TransformGroup();
    objTrans.setCapability(ALLOW_CHILDREN_READ);
    this.addChild(objTrans);

    Shape3D shape[] = new Shape3D[6];

    for (i = FRONT; i <= BOTTOM; i++) {
      OldGeomBuffer gbuf = new OldGeomBuffer(4);

      gbuf.begin(OldGeomBuffer.QUAD_STRIP);
      for (int j = 0; j < 2; j++) {
        gbuf.normal3d(NORMALS[i].x * sign, NORMALS[i].y
            * sign, NORMALS[i].z * sign);
        gbuf.texCoord2d(TCOORDS[i * 8 + j * 2], TCOORDS[i * 8 + j * 2 + 1]);
        gbuf.vertex3d((double) VERTS[i * 12 + j * 3] * xdim, (double) VERTS[i
            * 12 + j * 3 + 1]
            * ydim, (double) VERTS[i * 12 + j * 3 + 2] * zdim);
      }
      for (int j = 3; j > 1; j--) {
        gbuf.normal3d(NORMALS[i].x * sign, NORMALS[i].y
            * sign, NORMALS[i].z * sign);
        gbuf.texCoord2d(TCOORDS[i * 8 + j * 2], TCOORDS[i * 8 + j * 2 + 1]);
        gbuf.vertex3d((double) VERTS[i * 12 + j * 3] * xdim, (double) VERTS[i
            * 12 + j * 3 + 1]
            * ydim, (double) VERTS[i * 12 + j * 3 + 2] * zdim);
      }
      gbuf.end();
      shape[i] = new Shape3D(gbuf.getGeom(flags));
      numVerts = gbuf.getNumVerts();
      numTris = gbuf.getNumTris();

      if ((flags & ENABLE_APPEARANCE_MODIFY) != 0) {
        (shape[i]).setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        (shape[i]).setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
      }

      objTrans.addChild(shape[i]);
    }

    if (ap == null) {
      setAppearance();
    } else
      setAppearance(ap);
  }

  /**
   * Gets one of the faces (Shape3D) from the Cuboid that contains the geometry
   * and appearance. This allows users to modify the appearance or geometry of
   * individual parts.
   * 
   * @param partId
   *          The part to return.
   * @return The Shape3D object associated with the partID. If an invalid partId
   *         is passed in, null is returned.
   */

  @Override
  public Shape3D getShape(int partId) {
    if ((partId >= FRONT) && (partId <= BOTTOM))
      return (Shape3D) (((Group) getChild(0)).getChild(partId));
    return null;
  }

  /**
   * Sets appearance of the Cuboid. This will set each face of the Cuboid to the
   * same appearance. To set each face's appearance separately, use
   * getShape(partId) to get the individual shape and call
   * shape.setAppearance(ap).
   */

  @Override
  public void setAppearance(Appearance ap) {
    ((Shape3D) ((Group) getChild(0)).getChild(TOP)).setAppearance(ap);
    ((Shape3D) ((Group) getChild(0)).getChild(LEFT)).setAppearance(ap);
    ((Shape3D) ((Group) getChild(0)).getChild(RIGHT)).setAppearance(ap);
    ((Shape3D) ((Group) getChild(0)).getChild(FRONT)).setAppearance(ap);
    ((Shape3D) ((Group) getChild(0)).getChild(BACK)).setAppearance(ap);
    ((Shape3D) ((Group) getChild(0)).getChild(BOTTOM)).setAppearance(ap);
  }

  private static final float[] VERTS = {
      // front face
      1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
      1.0f,
      1.0f,
      -1.0f,
      -1.0f,
      1.0f,
      // back face
      -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f,
      -1.0f,
      1.0f,
      -1.0f,
      -1.0f,
      // right face
      1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
      1.0f,
      -1.0f,
      1.0f,
      // left face
      -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
      -1.0f,
      -1.0f,
      // top face
      1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
      1.0f,
      // bottom face
      -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
      1.0f, };

  private static final double[] TCOORDS = {
      // front
      1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0,
      // back
      1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0,
      // right
      1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0,
      // left
      1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0,
      // top
      1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0,
      // bottom
      0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0 };

  private static final Vector3f[] NORMALS = { new Vector3f(0.0f, 0.0f, 1.0f), // front
      // face
      new Vector3f(0.0f, 0.0f, -1.0f), // back face
      new Vector3f(1.0f, 0.0f, 0.0f), // right face
      new Vector3f(-1.0f, 0.0f, 0.0f), // left face
      new Vector3f(0.0f, 1.0f, 0.0f), // top face
      new Vector3f(0.0f, -1.0f, 0.0f), // bottom face
  };

  /**
   * Used to create a new instance of the node. This routine is called by
   * <code>cloneTree</code> to duplicate the current node.
   * <code>cloneNode</code> should be overridden by any user subclassed objects.
   * All subclasses must have their <code>cloneNode</code> method consist of the
   * following lines:
   * <P>
   * <blockquote>
   * 
   * <pre>
   * public Node cloneNode(boolean forceDuplicate) {
   *   UserSubClass usc = new UserSubClass();
   *   usc.duplicateNode(this, forceDuplicate);
   *   return usc;
   * }
   * </pre>
   * 
   * </blockquote>
   * 
   * @param forceDuplicate
   *          when set to <code>true</code>, causes the
   *          <code>duplicateOnCloneTree</code> flag to be ignored. When
   *          <code>false</code>, the value of each node's
   *          <code>duplicateOnCloneTree</code> variable determines whether
   *          NodeComponent data is duplicated or copied.
   * 
   * @see Node#cloneTree
   * @see Node#duplicateNode
   * @see NodeComponent#setDuplicateOnCloneTree
   */
  @Override
  public Node cloneNode(boolean forceDuplicate) {
    Cuboid b = new Cuboid(xDim, yDim, zDim, flags, getAppearance());
    b.duplicateNode(this, forceDuplicate);
    return b;
  }

  /**
   * Copies all node information from <code>originalNode</code> into the current
   * node. This method is called from the <code>cloneNode</code> method which
   * is, in turn, called by the <code>cloneTree</code> method.
   * <P>
   * For any <i>NodeComponent </i> objects contained by the object being
   * duplicated, each <i>NodeComponent </i> object's
   * <code>duplicateOnCloneTree</code> value is used to determine whether the
   * <i>NodeComponent </i> should be duplicated in the new node or if just a
   * reference to the current node should be placed in the new node. This flag
   * can be overridden by setting the <code>forceDuplicate</code> parameter in
   * the <code>cloneTree</code> method to <code>true</code>.
   * 
   * @param originalNode
   *          the original node to duplicate.
   * @param forceDuplicate
   *          when set to <code>true</code>, causes the
   *          <code>duplicateOnCloneTree</code> flag to be ignored. When
   *          <code>false</code>, the value of each node's
   *          <code>duplicateOnCloneTree</code> variable determines whether
   *          NodeComponent data is duplicated or copied.
   * 
   * @see Node#cloneTree
   * @see Node#cloneNode
   * @see NodeComponent#setDuplicateOnCloneTree
   */
  @Override
  public void duplicateNode(Node originalNode, boolean forceDuplicate) {
    super.duplicateNode(originalNode, forceDuplicate);
  }

}