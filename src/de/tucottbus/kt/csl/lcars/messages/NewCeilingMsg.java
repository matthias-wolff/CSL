package de.tucottbus.kt.csl.lcars.messages;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;

public class NewCeilingMsg implements IBeamformerMsg {
  
 private final MicArrayState state;
 private final Boolean trolleyEnable;
  
 public NewCeilingMsg(MicArrayState state, Boolean trolleyEnable){
   this.state=state;
   this.trolleyEnable=trolleyEnable;
 }
 
 public MicArrayState getMicArrayState(){
   return state;
 }
 
 public Boolean isTrolleyEnable(){
   return trolleyEnable;
 }

}
