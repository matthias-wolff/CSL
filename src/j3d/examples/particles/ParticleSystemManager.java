/**********************************************************
Copyright (C) 2005, Michael N. Jacobs, All Rights Reserved

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

**************************************************************/

/*
 * A simple behavior that notifies all particle systems. 
 */
package j3d.examples.particles;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedFrames;

public class ParticleSystemManager extends Behavior {
	private static ParticleSystemManager current;
	@SuppressWarnings("rawtypes")
  private Collection listeners;
	private WakeupCondition wakeupCondition = null;
	private long lastTime = 0;
	float frameCycleTime = 0;

	public static ParticleSystemManager getCurrent(float cycleTime){
		if(current == null){
			current = new ParticleSystemManager(cycleTime);
		}
		return current;
	}
	
	public static ParticleSystemManager getCurrent(){
		return getCurrent(0.033f);
	}

	public ParticleSystemManager(float frameCycleTime) {
		this.frameCycleTime = frameCycleTime;	
	}

	@SuppressWarnings("rawtypes")
  private Collection getListeners(){
		if(listeners == null){
			listeners = new Vector();
		}
		return listeners;
	}
	
	private WakeupCondition getMyWakeupCondition(){
		if(wakeupCondition == null){
			wakeupCondition = new WakeupOnElapsedFrames(0);
		}
		return wakeupCondition;
	}
	
	public void initialize() {
		wakeupOn(getMyWakeupCondition());
		lastTime = System.currentTimeMillis();
	}
	
	private void notifyListeners(@SuppressWarnings("rawtypes") Collection c, float dt){
		@SuppressWarnings("rawtypes")
    Iterator iterator = c.iterator();
		while(iterator.hasNext()){
			IParticleSystem ps = (IParticleSystem)iterator.next();
			ps.nextFrame(dt);
		}
	}

	public void processStimulus(@SuppressWarnings("rawtypes") Enumeration criteria) {
		wakeupOn(getMyWakeupCondition());
		while (criteria.hasMoreElements()) {
			criteria.nextElement();
			long currentTime = System.currentTimeMillis();
			float dt = Math.min(((float)(currentTime - lastTime))/1000, frameCycleTime);
			lastTime = currentTime;
			//System.out.println(dt);
			notifyListeners(getListeners(), dt);
		}
		
	}

	@SuppressWarnings("unchecked")
  public void register(IParticleSystem aParticleSystem){
			getListeners().add(aParticleSystem);
	}
}
