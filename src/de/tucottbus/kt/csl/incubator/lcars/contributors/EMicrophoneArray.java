package de.tucottbus.kt.csl.incubator.lcars.contributors;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import de.tucottbus.kt.csl.hardware.micarray3d.AMicArray3DPart;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ETopography;

/**
 * This class contributes the topology of a {@linkplain AMicArray3DPart
 * microphone array part} to an LCARS panel.
 * 
 * <p><b>Incubating:</b> <i>Is going to replace 
 * {@link de.tucottbus.kt.csl.lcars.contributors.EMicrophoneArray}.</i>
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>Redesign is necessary because LCARS GUI elements should not directly
 *     access any hardware wrapper.</li>
 * </ul>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class EMicrophoneArray extends ETopography
{
  // -- Public final fields --
  
  /**
   * The microphone array type being displayed,
   * {@link MicArrayCeiling}<code>.class</code> or
   * {@link MicArrayViewer}<code>.class</code>.
   */
  public final Class<? extends AMicArray3DPart> tMicArray;

  //  -- Fields --
  
  /**
   * Microphone array state.
   */
  private MicArrayState mas;
  
  // -- Life cycle --
  
  /**
   * Creates a microphone array topography.
   * 
   * @param tMicArray
   *          The microphone array type,
   *          {@link MicArrayCeiling}<code>.class</code> or
   *          {@link MicArrayViewer}<code>.class</code>.
   * @param sBounds
   *          The bounds of the element contributor (in LCARS panel pixels).
   * @param pBounds
   *          The physical bounds of the microphone array (in centimeters).
   */
  public EMicrophoneArray
  (
    Class<? extends AMicArray3DPart> tMicArray,
    Rectangle                        sBounds,
    Rectangle2D.Float                pBounds
  )
  {
    super(sBounds.x,sBounds.y,sBounds.width,sBounds.height,LCARS.EC_HEADLINE);
    this.tMicArray = tMicArray;
    this.mas = MicArrayState.getDummy();
    
    setPhysicalBounds(pBounds,"cm",false);    
    setGrid(new Point2D.Float(50,50),null,false);
    setGridStyle(LCARS.EC_SECONDARY|LCARS.EF_TINY,0.2f,0,false);

    // TODO: Add map image
    // TODO: Add microphone points
  }

}

// EOF
