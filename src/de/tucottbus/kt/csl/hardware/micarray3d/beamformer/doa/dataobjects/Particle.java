package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects;

import java.util.Random;

public interface Particle extends Cloneable {
  public Particle clone();
  public void addNoise(Random r, double spread);
}

