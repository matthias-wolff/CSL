package de.tucottbus.kt.csl.lcars.messages;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;

public class NewBeamformerMsg implements IBeamformerMsg {
  
 private final MicArrayState state;
 private final Boolean targetEnable;
  
 public NewBeamformerMsg(MicArrayState state, Boolean targetEnable){
   this.state=state;
   this.targetEnable=targetEnable;
 }
 
 public MicArrayState getMicArrayState(){
   return state;
 }
 
 public Boolean isTargetEnable(){
   return targetEnable;
 }
 
}
