package de.tucottbus.kt.csl.lcars.messages;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.lcars.components.threeDim.Cube3d;

public class NewSlicePositionMsg implements IBeamformerMsg {

  private Point3d slicePos;
  private Double x,y,z;
  
  private boolean enable;
  private boolean reset;
  
  /**
   * Constructor for a new slice message to communicate with the {@link Cube3d} class
   * @param xPos, Double value - slice position on the x-axis
   * @param yPos, Double value - slice position on the y-axis
   * @param zPos, Double value - slice position on the z-axis
   * @param enable, boolean - set this to TRUE to display the slices; if it's FALSE all slices will be disabled and the cube will be movable
   * @param sliceReset, boolean - set this to TRUE to reset the slices
   */
  public NewSlicePositionMsg(Double xPos, Double yPos, Double zPos, boolean enable, boolean sliceReset){
    x=xPos;
    y=yPos;
    z=zPos;
    this.enable=enable;
    this.reset=sliceReset;
  }
  
  public Double getXSlicePos(){
    return x;
  }
  
  public Double getYSlicePos(){
    return y;
  }
  
  public Double getZSlicePos(){
    return z;
  }
  
  public boolean isEnable(){
    return enable;
  }
  
  public boolean isReset(){
    return reset;
  }
  
  @Override
  public String toString(){
    return "["+getClass().getSimpleName() + " SlicePos="+slicePos.getX()+","+slicePos.getY()+","+slicePos.getZ()+"; ]";
  }

}
