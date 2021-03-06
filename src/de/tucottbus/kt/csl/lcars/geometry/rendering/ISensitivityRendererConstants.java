package de.tucottbus.kt.csl.lcars.geometry.rendering;

/**
 * Constants for 2D spatial sensitivity plots of the CSL microphone array.
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public interface ISensitivityRendererConstants
{
  /**
   * XY-slice of 2D sensitivity plot, view from the ceiling to the floor.
   */
  public static final int SLICE_XY = 1;

  /**
   * XZ-slice of 2D sensitivity plot, view from the rear (workstations) to the
   * front (main viewer).
   */
  public static final int SLICE_XZ = 2;

  /**
   * YZ-slice of 2D sensitivity plot, view from the windows to the door.
   */
  public static final int SLICE_YZ = 3;

}
