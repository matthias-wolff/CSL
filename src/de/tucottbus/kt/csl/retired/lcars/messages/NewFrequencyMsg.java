package de.tucottbus.kt.csl.retired.lcars.messages;

@Deprecated
public class NewFrequencyMsg implements IBeamformerMsg {
  
  float frequency;
  
  public NewFrequencyMsg(float freq){
    frequency=freq;
  }
  
  public float getFrequency() {
    return frequency;
  }
  
}
