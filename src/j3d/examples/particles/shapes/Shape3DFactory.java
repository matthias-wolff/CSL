/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/
package j3d.examples.particles.shapes;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

/**
 * Abstract class for all shape particle factories.  The generated
 * shapes will have a bounding box assigned to the collision bounds
 * based on the radius and radius variance used to construct the factory. 
 */
public abstract class Shape3DFactory implements IShape3DFactory {
	private float radius;
	private float radiusVariance;
	private Color3f color;
	private Color3f colorVariance;

	public Shape3DFactory(float aRadius, float aRadiusVariance) {
		this(
			aRadius,
			aRadiusVariance,
			new Color3f(0.6f, 0.6f, 0.6f),
			new Color3f(0.3f, 0.3f, 0.3f));
	}

	public Shape3DFactory() {
		this(
			10,
			4,
			new Color3f(0.6f, 0.6f, 0.6f),
			new Color3f(0.3f, 0.3f, 0.3f));
	}

	public Shape3DFactory(
		float aRadius,
		float aRadiusVariance,
		Color3f aColor,
		Color3f aColorVariance) {
		radius = aRadius;
		radiusVariance = aRadiusVariance;
		color = aColor;
		colorVariance = aColorVariance;
	}

	protected float random() {
		return 2 * (0.5f - (float) Math.random());
	}

	protected float getVaryingRadius() {
		return radius + random() * radiusVariance;
	}

	protected Color3f getEqualVaryingColor3f() {
		float random = random();
		float red = color.x + colorVariance.x * random;
		float green = color.y + colorVariance.y * random;
		float blue = color.z + colorVariance.z * random;
		red = clamp(red, 0, 1);
		green = clamp(green, 0, 1);
		blue = clamp(blue, 0, 1);

		return new Color3f(red, green, blue);
	}

	protected Color3f getVaryingColor3f() {
		float red = color.x + colorVariance.x * random();
		float green = color.y + colorVariance.y * random();
		float blue = color.z + colorVariance.z * random();
		red = clamp(red, 0, 1);
		green = clamp(green, 0, 1);
		blue = clamp(blue, 0, 1);

		return new Color3f(red, green, blue);
	}

	protected float clamp(float aFloat, float aLowValue, float aHighValue) {
		float answer = aFloat;
		if (aFloat < aLowValue) {
			answer = aLowValue;
		} else if (aFloat > aHighValue) {
			answer = aHighValue;
		}
		return answer;
	}
	
	public Shape3D createShape(){
		float r = getVaryingRadius();
		Shape3D aShape = createShapeBasic(r);
		aShape.setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_READ);
		aShape.setCapability(Shape3D.ALLOW_APPEARANCE_OVERRIDE_WRITE);
		aShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		aShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		aShape.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		aShape.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
		BoundingBox bb = new BoundingBox(new Point3d(-r,-r,-r), new Point3d(r,r,r));
		aShape.setCollisionBounds(bb);
		return aShape;
	}
	
	protected abstract Shape3D createShapeBasic(float aRadius);

}
