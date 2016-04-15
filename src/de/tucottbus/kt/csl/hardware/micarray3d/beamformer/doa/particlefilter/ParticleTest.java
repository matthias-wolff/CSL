package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.particlefilter;

import java.util.Random;

import javax.vecmath.Point2d;

import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle2D;

public class ParticleTest {

  public ParticleTest() {
    MultiEvaluator<Particle2D> evaluator = new MultiEvaluator<Particle2D>();
    final int N = 500;
    ParticleFilter<Particle2D> filter = new ParticleFilter<Particle2D>(evaluator,N);
    
    final Random r = new Random();
    for (int i = 0; i < N; i++) {
      double x = 4.40 * (r.nextDouble() - 0.5);
      double z = 2.50 * r.nextDouble();
      filter.addParticle(new Particle2D(new Point2d(x,z)));
    }
  }
  
  public static void main(String[] args) {
    new ParticleTest();
  }

}
