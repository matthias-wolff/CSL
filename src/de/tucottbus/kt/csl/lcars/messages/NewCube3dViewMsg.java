package de.tucottbus.kt.csl.lcars.messages;

import javax.vecmath.Vector3f;

public class NewCube3dViewMsg implements IBeamformerMsg {
  
  private Vector3f position;
  private Boolean tvEnable;
  private Boolean movable;
  
  public NewCube3dViewMsg(Vector3f viewPos, Boolean tvEnable, Boolean movable){
    this.position=viewPos;
    this.tvEnable=tvEnable;
    this.movable=movable;
  }
  
  public Vector3f getViewPosition(){
    return position;
  }
  
  public Boolean isTvEnable(){
    return tvEnable;
  }
  
  public Boolean isMovable() {
    return movable;
  }
  
}
