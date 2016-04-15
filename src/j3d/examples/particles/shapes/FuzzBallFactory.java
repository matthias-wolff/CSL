/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/
package j3d.examples.particles.shapes;

import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;

public class FuzzBallFactory extends Shape3DFactory {
	private int preferredGeometrySize;
	
	public FuzzBallFactory(){
		this(5, 0, new Color3f(0.5f, 0.5f, 0.5f), new Color3f(0.5f, 0.5f, 0.5f));
	}
	
	public FuzzBallFactory(float aRadius, float aRadiusVariance, Color3f aColor, Color3f aColorVariance){
		this(aRadius, aRadiusVariance, aColor, aColorVariance, 1);
	}
	
	public FuzzBallFactory(float aRadius, float aRadiusVariance, Color3f aColor, Color3f aColorVariance, int aGeometrySize){
		super(aRadius, aRadiusVariance, aColor, aColorVariance);
		preferredGeometrySize = aGeometrySize;
	}
		
	protected Shape3D createShapeBasic(float aRadius) {
		FuzzBall fb = new FuzzBall(aRadius,getVaryingColor3f());
		fb.setCapability(FuzzBall.ALLOW_APPEARANCE_OVERRIDE_READ);
		fb.setCapability(FuzzBall.ALLOW_APPEARANCE_OVERRIDE_WRITE);
		fb.setCapability(FuzzBall.ALLOW_APPEARANCE_READ);
		fb.setCapability(FuzzBall.ALLOW_APPEARANCE_WRITE);
		fb.setPreferredGeometrySize(preferredGeometrySize);
		return fb;
	}

}
