package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.particlefilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle;

public class ParticleFilter<T extends Particle> {
  
  private ParticleEvaluator<T> evaluator;
  
  // TODO: hashmap would be better
  private ArrayList<ParticleWeight<T>> particlesWithWeights = new ArrayList<ParticleWeight<T>>();
  
  private double[] selectionSum = new double[0];
  private boolean recalculateWeightAfterDrift = false;
  
  private final int particlesCount;
  
  /**
   * The threshold for resampling will be T=NUMBERS_OF_PARTICLES/10
   */
  private final double threshold;
  
  private final Random random = new Random();
  
  private final Comparator<ParticleWeight<T>> comparator = new Comparator<ParticleWeight<T>>() {
    @Override
    public int compare(ParticleWeight<T> p0, ParticleWeight<T> p1) {
      double s0 = p0.getWeight();
      double s1 = p1.getWeight();
      if (s0 < s1)
        return -1;
      else if (s0 > s1)
        return +1;
      else
        return 0;
    }
  };

  public ParticleFilter(ParticleEvaluator<T> evaluator,int N) {
    this.evaluator = evaluator;
    particlesCount=N;
    threshold=(N*1.0)/10;
  }

  public void addParticle(T p) {
    ParticleWeight<T> pw = new ParticleWeight<T>(p,1.0/particlesCount,0);
    this.particlesWithWeights.add(pw);
  }

  public int getParticleCount() {
    return particlesWithWeights.size();
  }

  public T get(int i) {
    return particlesWithWeights.get(i).getData();
  }

  /**
   * STEP 1
   * likelihood function
   */
  public void evaluateStrength() {
    for (ParticleWeight<T> pw : this.particlesWithWeights) {
      double weight = evaluator.evaluate(pw.getData());
      pw.setWeight(weight);
    }
  }

  /**
   * Step 2: Resampling of all particles
   * @param r
   */
  public void resample() {
    if(getParticleThreshold()>threshold)
      return;
      
    double sum = getParticleWeightsSum();
    
    int[] selectionDistribution = new int[particlesWithWeights.size()];
    
    ArrayList<ParticleWeight<T>> nextDistribution = new ArrayList<ParticleWeight<T>>();
    
    for (int i = 0; i < particlesWithWeights.size(); i++) {
      double sel = sum * random.nextDouble();
      int index = Arrays.binarySearch(this.selectionSum, sel);
      
      if (index < 0) {
        index = -(index + 1);
      }
      
      ParticleWeight<T> p = particlesWithWeights.get(index);
      @SuppressWarnings("unchecked")
      ParticleWeight<T> particleWeight = new ParticleWeight<T>(
          (T) p.getData().clone(), p.getWeight(), selectionDistribution[index]);
      nextDistribution.add(particleWeight);
      selectionDistribution[index]++;
    }
    
    this.particlesWithWeights = nextDistribution;
  }
  
  /**
   * STEP 3
   * Add removal here! 
   * @param r
   * @param spread
   */
  public void disperseDistribution(double spread) {
    for (ParticleWeight<T> p : this.particlesWithWeights) {
      // do not add error to one copy of the particle
      if (p.getCopyCount() > 0) {
        p.getData().addNoise(random, spread);
        if (recalculateWeightAfterDrift) {
          // The weight ratio depends on small changes in strength after noise
          // is added.
          // The filter can be made more accurate by finding the exact strength
          // of the new particle for the previous timestep.
          p.setLastWeight(evaluator.evaluate(p.getData()));
        }
      }
    }
  }

  
  /**
   * Get the sum of all particles in the collection
   * @return double
   */
  private double getParticleWeightsSum() {
    Collections.sort(this.particlesWithWeights, comparator);
    this.selectionSum = new double[getParticleCount()];
    double sum = 0;
    for (int i = 0; i < particlesWithWeights.size(); i++) {
      ParticleWeight<T> p = particlesWithWeights.get(i);
      sum += p.getWeight();
      this.selectionSum[i] = sum;
    }
    return sum;
  }
  
  /**
   * Calculate the value to compare with the threshold
   * @return double
   */
  private double getParticleThreshold() {
    Collections.sort(this.particlesWithWeights, comparator);
    double sum = 0;
    for (int i = 0; i < particlesWithWeights.size(); i++) {
      ParticleWeight<T> p = particlesWithWeights.get(i);
      sum += p.getWeight()*p.getWeight();
    }
    return 1/sum;
  }

  public void setEvaluator(ParticleEvaluator<T> evaluator) {
    this.evaluator = evaluator;
  }

  public void setReevaluateAfterNoise(boolean b) {
    this.recalculateWeightAfterDrift = b;
  }
}
