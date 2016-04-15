/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/
package j3d.examples.particles.shapes;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.media.j3d.Appearance;
import javax.media.j3d.Geometry;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Material;
import javax.media.j3d.TextureAttributes;
import javax.vecmath.Color3f;

public class FuzzBall extends ImplicitSurface {
	private static final int IMAGE_SIZE = 64;
	//TODO: FuzzBall geometry should be shared by factory, not class
	private static Geometry sharedGeometry;
	private static ImageComponent2D sharedImage;

	public FuzzBall() {
		this(1);
	}

	public FuzzBall(float aRadius) {
		this(aRadius, new Color3f(1, 0, 0));
	}

	public FuzzBall(float aRadius, Color3f aColor) {
		super(aRadius, aColor, IMAGE_SIZE);
		postConstructionInitialization();
	}

	protected void generateImage(BufferedImage bi) {
		// Generates a fuzzy ball based on an atmospheric
		// density using Wyvill's standard cubic function 
		// (Wyvill, McPheeters, and Wyvill 1986).
		// Set all colors to be white compatible with the use
		// of MODULATE texture mode.
		int red = 255;
		int green = 255; 
		int blue = 255; 
		float center = (getImageSize() + 1) / 2;
		float center2 = center * center;
		float center4 = center2 * center2;
		float center6 = center4 * center2;
		for (int column = 0; column < getImageSize(); column++) {
			float columnDistance = (column - center) * (column - center);
			for (int row = 0; row < getImageSize(); row++) {
				float rowDistance = (row - center) * (row - center);
				float distance2 = rowDistance + columnDistance;
				double opacity = 0;
				if (distance2 > center2) {
					opacity = 0;
				} else {
					float distance4 = distance2 * distance2;
					float distance6 = distance4 * distance2;
					opacity =
						255
							* (1
								- (4 / 9) * (distance6 / center6)
								+ (17 / 9) * (distance4 / center4)
								- (22 / 9) * (distance2 / center2));
				}
				int alpha = (int) opacity;
				if(DEBUG){
					red = alpha;
					green = alpha;
					blue = alpha;
				}
				bi.setRGB(
					column,
					row,
					(alpha << 24) + (red << 16) + (green << 8) + blue);
			}
		}
		if(DEBUG){
			try {
				File outfile = new File("fuzzball.png");
				ImageIO.write(bi, "png", outfile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void customizeAppearance(Appearance anAppearance) {
	  anAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
	  anAppearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE);
		anAppearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		anAppearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		anAppearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
		anAppearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_READ);
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(getColor());
		material.setDiffuseColor(getColor());
		material.setSpecularColor(0.3f,0.3f,0.3f);
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

	protected Geometry createGeometry() {
		if (getSharedGeometry() == null) {
			Geometry g = super.createGeometry();
			setSharedGeometry(g);
		}
		return getSharedGeometry();
	}

	protected ImageComponent2D getImage() {
		if (getSharedImage() == null) {
			ImageComponent2D i = super.getImage();
			setSharedImage(i);
		}
		return getSharedImage();
	}

	private static Geometry getSharedGeometry() {
		return sharedGeometry;
	}

	private static void setSharedGeometry(Geometry geometry) {
		sharedGeometry = geometry;
	}

	private static ImageComponent2D getSharedImage() {
		return sharedImage;
	}

	private static void setSharedImage(ImageComponent2D component2D) {
		sharedImage = component2D;
	}

}
