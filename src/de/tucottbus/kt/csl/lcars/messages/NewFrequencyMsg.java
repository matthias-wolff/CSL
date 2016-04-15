package de.tucottbus.kt.csl.lcars.messages;

public class NewFrequencyMsg implements IBeamformerMsg {
  
  float frequency;
  
  public NewFrequencyMsg(float freq){
    frequency=freq;
  }
  
  public float getFrequency() {
    return frequency;
  }
  
}
