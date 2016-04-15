/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/
package j3d.examples.particles.shapes;

import java.awt.image.BufferedImage;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.OrientedShape3D;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Texture2D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;
// TODO: Document ImplicitSurface class
public abstract class ImplicitSurface extends OrientedShape3D {
	protected static final boolean DEBUG = false;
	private final static int GEOMETRY_SIZE = 32;
	private final Color3f color;
	private int imageSize;
	private int preferredGeometrySize;
	private final float radius;

	public ImplicitSurface(float aRadius, Color3f aColor, int aSize) {
		imageSize = aSize;
		radius = aRadius;
		color = aColor;
	}

	protected Appearance createAppearance() {
		Appearance appearance = new Appearance();
		appearance.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		appearance.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		appearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		appearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		appearance.setPolygonAttributes(polyAttrib);
		
		ColoringAttributes ca = new ColoringAttributes();
		ca.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
		ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		ca.setShadeModel(ColoringAttributes.NICEST);
		appearance.setColoringAttributes(ca);

		Texture2D texture =
			new Texture2D(
				Texture2D.BASE_LEVEL,
				Texture2D.RGBA,
				getImageSize(),
				getImageSize());

		texture.setImage(0, getImage());
		texture.setEnable(true);
		texture.setMagFilter(Texture2D.NICEST);
		texture.setMinFilter(Texture2D.NICEST);
		appearance.setTexture(texture);

		TransparencyAttributes ta = new TransparencyAttributes();
		ta.setTransparencyMode(TransparencyAttributes.NICEST);
		appearance.setTransparencyAttributes(ta);
		return appearance;
	}
	
	protected Geometry createGeometry() {
		return createGeometry(getPreferredGeometrySize());
	}
	
	private Geometry createGeometry(int aSize) {
		int vertexCount = 4 * aSize * aSize;
		QuadArray geometry = new QuadArray(vertexCount, getVertexFormat());
		float[] coordinates = new float[vertexCount * 3];
		float[] normals = new float[vertexCount * 3];
		float[] textureCoordinates = new float[vertexCount * 2];

		int ci = 0; // coordinate index
		int ti = 0; // texture index
		float increment = 2 * radius / aSize;
		float textureIncrement = 1.0f/aSize;
		float rowValue = -radius;
		for (int row = 0; row < aSize; row++) {
			float colValue = -radius;
			for (int col = 0; col < aSize; col++) {
				// SW
				coordinates[ci + 0] = colValue;
				coordinates[ci + 1] = rowValue;
				coordinates[ci + 2] = 0;
				
				Vector3f aNormal = generateNormal(coordinates[ci + 0], coordinates[ci + 1]);
				normals[ci + 0] = aNormal.x;
				normals[ci + 1] = aNormal.y;
				normals[ci + 2] = aNormal.z;
				
				textureCoordinates[ti + 0] = col * textureIncrement;
				textureCoordinates[ti + 1] = row * textureIncrement;
				ti = ti + 2;
				
				// SE
				coordinates[ci + 3] = colValue + increment;
				coordinates[ci + 4] = rowValue;
				coordinates[ci + 5] = 0;
				
				aNormal = generateNormal(coordinates[ci + 3], coordinates[ci + 4]);
				normals[ci + 3] = aNormal.x;
				normals[ci + 4] = aNormal.y;
				normals[ci + 5] = aNormal.z;
				
				textureCoordinates[ti + 0] = (col + 1) * textureIncrement;
				textureCoordinates[ti + 1] = row * textureIncrement;
				ti = ti + 2;
				
				// NE
				coordinates[ci + 6] = colValue + increment;;
				coordinates[ci + 7] = rowValue + increment;
				coordinates[ci + 8] = 0;
				
				aNormal = generateNormal(coordinates[ci + 6], coordinates[ci + 7]);
				normals[ci + 6] = aNormal.x;
				normals[ci + 7] = aNormal.y;
				normals[ci + 8] = aNormal.z;
				
				textureCoordinates[ti + 0] = (col + 1) * textureIncrement;
				textureCoordinates[ti + 1] = (row + 1) * textureIncrement;
				ti = ti + 2;
				
				// NW
				coordinates[ci + 9] = colValue;
				coordinates[ci + 10] = rowValue + increment;
				coordinates[ci + 11] = 0;
				
				aNormal = generateNormal(coordinates[ci + 9], coordinates[ci + 10]);
				normals[ci + 9] = aNormal.x;
				normals[ci + 10] = aNormal.y;
				normals[ci + 11] = aNormal.z;
				
				textureCoordinates[ti + 0] = (col + 0) * textureIncrement;
				textureCoordinates[ti + 1] = (row + 1) * textureIncrement;
				ti = ti + 2;
				
				ci = ci + 12;
				colValue = colValue + increment;
			}
			rowValue = rowValue + increment;
		}

		geometry.setCoordinates(0, coordinates);
		geometry.setTextureCoordinates(0, 0, textureCoordinates);
		geometry.setNormals(0, normals);

		return geometry;
	}

	protected abstract void generateImage(BufferedImage bi);
	
	protected Vector3f generateNormal(float x, float y){
		float distance2 = x*x + y*y;
		float radius2 = radius * radius;
		float z = 0;
		if(radius2 >= distance2){
			z = (float)Math.sqrt(radius2 - distance2);
		}
		Vector3f vector = new Vector3f(x, y, z);
		vector.normalize();
		return vector;
	}

	protected Color3f getColor() {
		return color;
	}

	protected ImageComponent2D getImage() {
		BufferedImage bi =
			new BufferedImage(
				getImageSize(),
				getImageSize(),
				BufferedImage.TYPE_INT_ARGB);

		generateImage(bi);

		ImageComponent2D image =
			new ImageComponent2D(
				ImageComponent2D.FORMAT_RGBA,
				bi,
				true,
				false);

		return image;
	}

	protected int getImageSize() {
		return imageSize;
	}
	
	protected int getPreferredGeometrySize(){
		if(preferredGeometrySize == 0){
			preferredGeometrySize = GEOMETRY_SIZE;
		}
		
		return preferredGeometrySize;
	}
	protected int getVertexFormat() {
		return GeometryArray.COORDINATES
			| GeometryArray.NORMALS
			| GeometryArray.TEXTURE_COORDINATE_2;
	}

	protected void postConstructionInitialization() {
		setGeometry(createGeometry());
		setAppearance(createAppearance());
		setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);
		setRotationPoint(0, 0, 0);
	}
	protected void setImageSize(int aSize) {
		imageSize = aSize;
	}
	
	protected void setPreferredGeometrySize(int aSize){
		preferredGeometrySize = aSize;
	}

}
