
/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/
package j3d.examples.particles.shapes;

import java.awt.image.BufferedImage;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import javax.imageio.*;

public class FakeSphere extends ImplicitSurface {
	private static final int IMAGE_SIZE = 256;
	//TODO: FakeSphere geometry should be shared by factory, not class?
	private static Geometry sharedGeometry;
	private static ImageComponent2D sharedImage;

	private static Geometry getSharedGeometry() {
		return sharedGeometry;
	}

	private static ImageComponent2D getSharedImage() {
		return sharedImage;
	}

	private static void setSharedGeometry(Geometry geometry) {
		sharedGeometry = geometry;
	}

	private static void setSharedImage(ImageComponent2D component2D) {
		sharedImage = component2D;
	}

	public FakeSphere() {
		this(1);
	}
	public FakeSphere(float aRadius) {
		this(aRadius, new Color3f(0, 1, 0));
	}

	public FakeSphere(float aRadius, Color3f aColor) {
		super(aRadius, aColor, IMAGE_SIZE);
		postConstructionInitialization();
	}

	protected Geometry createGeometry() {
		if (getSharedGeometry() == null) {
			Geometry g = super.createGeometry();
			setSharedGeometry(g);
		}
		return getSharedGeometry();
	}

	protected void customizeAppearance(Appearance anAppearance) {
		Material material = new Material();
		material.setAmbientColor(getColor());
		anAppearance.setMaterial(material);
		/*
		 * Set up the texture mode to simplify the changing of the 
		 * transparency of this object.  Java3D will modulate (multiply) 
		 * the object alpha (1 - transparency) with the alpha channel of 
		 * the texture when rendering.  The transparency of the object 
		 * then can control the overall transparency of the texture.
		 * As the object ages, simply changing the transparency will
		 * affect the entire texture.
		 */
		TextureAttributes textureAttributes = new TextureAttributes();
		textureAttributes.setTextureMode(TextureAttributes.MODULATE);
		anAppearance.setTextureAttributes(textureAttributes);
	}

	protected void generateImage(BufferedImage bi) { 
		float center = (getImageSize()) / 2;

		for (int column = 0; column < getImageSize(); column++) {
			float columnDistance = (column - center) * (column - center);
			for (int row = 0; row < getImageSize(); row++) {
				float rowDistance = (row - center) * (row - center);
				float distance2 = rowDistance + columnDistance;
				double opacity = 0;
				if (Math.sqrt(distance2) >= center) {
					opacity = 0;
				} else {
					opacity = 255;
				}
				int alpha = (int) opacity;
				bi.setRGB(
					column,
					row,
					(alpha << 24) + (alpha << 16) + (alpha << 8) + alpha);
			}
		}
		
		// For debugging the image creation 
		if(DEBUG){
			try {
				File outfile = new File("fakesphere.png");
				System.out.println("out");
				ImageIO.write(bi, "png", outfile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected ImageComponent2D getImage() {
		if (getSharedImage() == null) {
			ImageComponent2D i = super.getImage();
			setSharedImage(i);
		}
		return getSharedImage();
	}
	protected int getPreferredGeometrySize(){
		return 32;
	}


}
