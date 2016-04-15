package org.selman.java3d;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.QuadArray;
import javax.media.j3d.TriangleArray;
import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;

//Based on Sun's GeomBuffer.java 1.12 98/07/08 11:30:12
//This version actually returns Quadstrips for a Quadstrip array
//unlike the newer version that returns TriangleStrips....

/**
 * OldGeomBuffer allows OpenGL-like input of geometry data. It outputs Java 3D
 * geometry array objects. This utility is to simplify porting of OpenGL
 * programs to Java 3D.
 * <p>
 * Here is a sample code that use this utility to create some quads.
 * <P>
 * <blockquote>
 * 
 * <pre>
 * 
 * 
 * OldGeomBuffer gbuf = new OldGeomBuffer(100);
 * gbuf.begin(OldGeomBuffer.QUADS);
 * 
 * for (int i = 0; i &lt; 5; i++) {
 *   gbuf.normal3d(0.0, 1.0, 0.0);
 *   gbuf.vertex3d(1.0, 1.0, 0.0);
 * 
 *   gbuf.normal3d(0.0, 1.0, 0.0);
 *   gbuf.vertex3d(0.0, 1.0, 0.0);
 * 
 *   gbuf.normal3d(0.0, 1.0, 0.0);
 *   gbuf.vertex3d(0.0, 0.0, 0.0);
 * 
 *   gbuf.normal3d(0.0, 1.0, 0.0);
 *   gbuf.vertex3d(1.0, 0.0, 0.0);
 * }
 * gbuf.end();
 * Shape3D shape = new Shape3D(gbuf.getGeom(OldGeomBuffer.GENERATE_NORMALS));
 * </pre>
 * 
 * </blockquote> Notice, that you only need to specify some upperbound on the
 * number of points you'll use at the beginning (100 in this case).
 * <p>
 * Currently, you are limited to one primitive type per geom buffer. Future
 * versions will add support for mixed primitive types.
 * 
 */

public class OldGeomBuffer extends Object {

  // Supported Primitives
  static final int QUAD_STRIP = 0x01;

  static final int TRIANGLES = 0x02;

  static final int QUADS = 0x04;

  private int flags;

  static final int GENERATE_NORMALS = 0x01;

  static final int GENERATE_TEXTURE_COORDS = 0x02;

  Point3f[] pts = null;

  Vector3f[] normals = null;

  Point2f[] tcoords = null;

  int currVertCnt;

  int currPrimCnt;

  int[] currPrimType = null, currPrimStartVertex = null,
      currPrimEndVertex = null;

  GeometryArray geometry;

  int numVerts = 0;

  int numTris = 0;

  static final int DEBUG = 0;

  /**
   * Creates a geometry buffer of given number of vertices
   * 
   * @param numVerts
   *          total number of vertices to allocate by this buffer. This is an
   *          upper bound estimate.
   */
  OldGeomBuffer(int numVerts) {
    pts = new Point3f[numVerts];
    normals = new Vector3f[numVerts];
    tcoords = new Point2f[numVerts];
    // max primitives is numV/3
    currPrimType = new int[numVerts / 3];
    currPrimStartVertex = new int[numVerts / 3];
    currPrimEndVertex = new int[numVerts / 3];
    currVertCnt = 0;
    currPrimCnt = 0;
  }

  /*
   * Returns a Java 3D geometry array from the geometry buffer. You need to call
   * begin, vertex3d, end, etc. before calling this, of course.
   * 
   * @param format vertex format.
   */

  GeometryArray getGeom(int format) {
    GeometryArray obj;
    flags = format;

    numTris = 0;

    // Switch based on first primitive.
    switch (currPrimType[0]) {
      case TRIANGLES:
        obj = processTriangles();
        obj.setCapability(Geometry.ALLOW_INTERSECT);
        return obj;
      case QUADS:
        obj = processQuads();
        obj.setCapability(Geometry.ALLOW_INTERSECT);
        return obj;
      case QUAD_STRIP:
        obj = processQuadStrips();
        obj.setCapability(Geometry.ALLOW_INTERSECT);
        return obj;
    }
    return null;
  }

  /**
   * Begins a new primitive given the primitive type.
   * 
   * @param prim
   *          the primitive type (listed above).
   * 
   */

  @SuppressWarnings("unused")
  void begin(int prim) {
    if (DEBUG >= 1)
      System.out.println("quad");
    currPrimType[currPrimCnt] = prim;
    currPrimStartVertex[currPrimCnt] = currVertCnt;
  }

  /**
   * End of primitive.
   * 
   * 
   */
  @SuppressWarnings("unused")
  void end() {
    if (DEBUG >= 1)
      System.out.println("end");
    currPrimEndVertex[currPrimCnt] = currVertCnt;
    currPrimCnt++;
  }

  @SuppressWarnings("unused")
  void vertex3d(double x, double y, double z) {
    if (DEBUG >= 2)
      System.out.println("v " + x + " " + y + " " + z);
    pts[currVertCnt] = new Point3f();
    pts[currVertCnt].x = (float) x;
    pts[currVertCnt].y = (float) y;
    pts[currVertCnt].z = (float) z;
    currVertCnt++;
  }

  @SuppressWarnings("unused")
  void normal3d(double x, double y, double z) {
    if (DEBUG >= 2)
      System.out.println("n " + x + " " + y + " " + z);
    double sum = x * x + y * y + z * z;
    if (Math.abs(sum - 1.0) > 0.001) {
      if (DEBUG >= 2)
        System.out.println("normalizing");
      double root = Math.sqrt(sum) + 0.0000001;
      x /= root;
      y /= root;
      z /= root;
    }
    normals[currVertCnt] = new Vector3f();
    normals[currVertCnt].x = (float) x;
    normals[currVertCnt].y = (float) y;
    normals[currVertCnt].z = (float) z;
  }

  @SuppressWarnings("unused")
  void texCoord2d(double s, double t) {
    if (DEBUG >= 2)
      System.out.println("t " + s + " " + t);
    tcoords[currVertCnt] = new Point2f();
    tcoords[currVertCnt].x = (float) s;
    tcoords[currVertCnt].y = (float) t;
  }

  /**
   * Returns the Java 3D geometry gotten from calling getGeom.
   * 
   */

  GeometryArray getComputedGeometry() {
    return geometry;
  }

  int getNumTris() {
    return numTris;
  }

  int getNumVerts() {
    return numVerts;
  }

  @SuppressWarnings("unused")
  private GeometryArray processQuadStrips() {
    GeometryArray obj = null;
    int i;
    int totalVerts = 0;

    for (i = 0; i < currPrimCnt; i++) {
      int numQuadStripVerts;

      numQuadStripVerts = currPrimEndVertex[i] - currPrimStartVertex[i];
      totalVerts += (numQuadStripVerts / 2 - 1) * 4;
    }

    if (DEBUG >= 1)
      System.out.println("totalVerts " + totalVerts);

    if (((flags & GENERATE_NORMALS) != 0)
        && ((flags & GENERATE_TEXTURE_COORDS) != 0)) {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES | QuadArray.NORMALS
          | QuadArray.TEXTURE_COORDINATE_2);
    } else if (((flags & GENERATE_NORMALS) == 0)
        && ((flags & GENERATE_TEXTURE_COORDS) != 0)) {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES
          | QuadArray.TEXTURE_COORDINATE_2);
    } else if (((flags & GENERATE_NORMALS) != 0)
        && ((flags & GENERATE_TEXTURE_COORDS) == 0)) {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES | QuadArray.NORMALS);
    } else {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES);
    }

    Point3f[] newpts = new Point3f[totalVerts];
    Vector3f[] newnormals = new Vector3f[totalVerts];
    TexCoord2f[] newtcoords = new TexCoord2f[totalVerts];
    int currVert = 0;

    for (i = 0; i < currPrimCnt; i++) {
      for (int j = currPrimStartVertex[i] + 2; j < currPrimEndVertex[i]; j += 2) {
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j - 2);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j - 1);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j + 1);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j);
        numTris += 2;
      }
    }
    numVerts = currVert;

    obj.setCoordinates(0, newpts);
    if ((flags & GENERATE_NORMALS) != 0)
      obj.setNormals(0, newnormals);
    if ((flags & GENERATE_TEXTURE_COORDS) != 0)
      obj.setTextureCoordinates(0, 0, newtcoords);

    geometry = obj;
    return obj;
  }

  @SuppressWarnings("unused")
  private GeometryArray processQuads() {
    GeometryArray obj = null;
    int i;
    int totalVerts = 0;

    for (i = 0; i < currPrimCnt; i++) {
      totalVerts += currPrimEndVertex[i] - currPrimStartVertex[i];
    }

    if (DEBUG >= 1)
      System.out.println("totalVerts " + totalVerts);

    if (((flags & GENERATE_NORMALS) != 0)
        && ((flags & GENERATE_TEXTURE_COORDS) != 0)) {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES | QuadArray.NORMALS
          | QuadArray.TEXTURE_COORDINATE_2);
    } else if (((flags & GENERATE_NORMALS) == 0)
        && ((flags & GENERATE_TEXTURE_COORDS) != 0)) {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES
          | QuadArray.TEXTURE_COORDINATE_2);
    } else if (((flags & GENERATE_NORMALS) != 0)
        && ((flags & GENERATE_TEXTURE_COORDS) == 0)) {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES | QuadArray.NORMALS);
    } else {
      obj = new QuadArray(totalVerts, QuadArray.COORDINATES);
    }

    Point3f[] newpts = new Point3f[totalVerts];
    Vector3f[] newnormals = new Vector3f[totalVerts];
    TexCoord2f[] newtcoords = new TexCoord2f[totalVerts];
    int currVert = 0;

    if (DEBUG > 1)
      System.out.println("total prims " + currPrimCnt);

    for (i = 0; i < currPrimCnt; i++) {
      if (DEBUG > 1)
        System.out.println("start " + currPrimStartVertex[i] + " end "
            + currPrimEndVertex[i]);
      for (int j = currPrimStartVertex[i]; j < currPrimEndVertex[i] - 3; j += 4) {
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j + 1);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j + 2);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j + 3);
        numTris += 2;
      }
    }
    numVerts = currVert;

    obj.setCoordinates(0, newpts);
    if ((flags & GENERATE_NORMALS) != 0)
      obj.setNormals(0, newnormals);
    if ((flags & GENERATE_TEXTURE_COORDS) != 0)
      obj.setTextureCoordinates(0, 0, newtcoords);

    geometry = obj;
    return obj;
  }

  @SuppressWarnings("unused")
  private GeometryArray processTriangles() {
    GeometryArray obj = null;
    int i;
    int totalVerts = 0;

    for (i = 0; i < currPrimCnt; i++) {
      totalVerts += currPrimEndVertex[i] - currPrimStartVertex[i];
    }

    if (DEBUG >= 1)
      System.out.println("totalVerts " + totalVerts);

    if (((flags & GENERATE_NORMALS) != 0)
        && ((flags & GENERATE_TEXTURE_COORDS) != 0)) {
      obj = new TriangleArray(totalVerts, TriangleArray.COORDINATES
          | TriangleArray.NORMALS | TriangleArray.TEXTURE_COORDINATE_2);
    } else if (((flags & GENERATE_NORMALS) == 0)
        && ((flags & GENERATE_TEXTURE_COORDS) != 0)) {
      obj = new TriangleArray(totalVerts, TriangleArray.COORDINATES
          | TriangleArray.TEXTURE_COORDINATE_2);
    } else if (((flags & GENERATE_NORMALS) != 0)
        && ((flags & GENERATE_TEXTURE_COORDS) == 0)) {
      obj = new TriangleArray(totalVerts, TriangleArray.COORDINATES
          | TriangleArray.NORMALS);
    } else {
      obj = new TriangleArray(totalVerts, TriangleArray.COORDINATES);
    }

    Point3f[] newpts = new Point3f[totalVerts];
    Vector3f[] newnormals = new Vector3f[totalVerts];
    TexCoord2f[] newtcoords = new TexCoord2f[totalVerts];
    int currVert = 0;

    for (i = 0; i < currPrimCnt; i++) {
      for (int j = currPrimStartVertex[i]; j < currPrimEndVertex[i] - 2; j += 3) {
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j + 1);
        outVertex(newpts, newnormals, newtcoords, currVert++, pts, normals,
            tcoords, j + 2);
        numTris += 1;
      }
    }
    numVerts = currVert;

    obj.setCoordinates(0, newpts);
    if ((flags & GENERATE_NORMALS) != 0)
      obj.setNormals(0, newnormals);
    if ((flags & GENERATE_TEXTURE_COORDS) != 0)
      obj.setTextureCoordinates(0, 0, newtcoords);

    geometry = obj;
    return obj;
  }

  @SuppressWarnings("unused")
  void outVertex(Point3f[] dpts, Vector3f[] dnormals, TexCoord2f[] dtcoords,
      int dloc, Point3f[] spts, Vector3f[] snormals, Point2f[] stcoords,
      int sloc) {
    if (DEBUG >= 1)
      System.out.println("v " + spts[sloc].x + " " + spts[sloc].y + " "
          + spts[sloc].z);
    dpts[dloc] = new Point3f();
    dpts[dloc].x = spts[sloc].x;
    dpts[dloc].y = spts[sloc].y;
    dpts[dloc].z = spts[sloc].z;

    if ((flags & GENERATE_NORMALS) != 0) {
      dnormals[dloc] = new Vector3f();
      dnormals[dloc].x = snormals[sloc].x;
      dnormals[dloc].y = snormals[sloc].y;
      dnormals[dloc].z = snormals[sloc].z;
    }
    if ((flags & GENERATE_TEXTURE_COORDS) != 0) {
      if (DEBUG >= 2)
        System.out.println("final out tcoord");
      dtcoords[dloc] = new TexCoord2f();
      dtcoords[dloc].x = stcoords[sloc].x;
      dtcoords[dloc].y = stcoords[sloc].y;
    }
  }
}

// Based on Sun's Box.java 1.13 98/11/23 10:23:02
// Work around for the Box bug when rendered in Wireframe mode.
