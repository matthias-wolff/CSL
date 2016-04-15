/**********************************************************
  Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  
**************************************************************/
package j3d.examples.particles;
import javax.media.j3d.*;

/**
 * The interface implemented by all particle systems.
 */
public interface IParticleSystem {
	/**
	 * Retrieves the local coordinates to virtual world coordinates 
	 * transform for this particle system.
	 * @param aTransform3D - The <code>Transform3D</code> to hold the result.
	 */
	public void getLocalToVworld(Transform3D aTransform3D);
	
	/**
	 * @return <code>true</code> if this particle system is still alive.
	 */
	public boolean isAlive();
	/**
	 * @return <code>true</code> if this particle system is dead.
	 */
	public boolean isDead();
	/**
	 * Called during the animation of the particle system.
	 */
	public void nextFrame(float dt);
}
