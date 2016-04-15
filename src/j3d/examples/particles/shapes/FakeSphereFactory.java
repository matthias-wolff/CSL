/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/
package j3d.examples.particles.shapes;

import javax.media.j3d.*;
import javax.vecmath.*;

public class FakeSphereFactory extends Shape3DFactory {
	
	public FakeSphereFactory(){
		this(2.5f, 0, new Color3f(0.5f, 0.5f, 0.5f), new Color3f(0.5f, 0.5f, 0.5f));
	}
	
	public FakeSphereFactory(float aRadius, float aRadiusVariance, Color3f aColor, Color3f aColorVariance){
		super(aRadius, aRadiusVariance, aColor, aColorVariance);
	}
		
	protected Shape3D createShapeBasic(float aRadius) {
		FakeSphere fs = new FakeSphere(aRadius,getVaryingColor3f());
		return fs;
	}

}
