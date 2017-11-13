package incubator.csl.lcars.micarr.contributors;

import javax.vecmath.Point3d;

/**
 * Interface for selection change listeners of {@link ESensitivityPlots}.
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public interface ESensitivityPlotsListener
{

  /**
   * Called when the selection, i.e. the plot slice positions, has changed.
   * 
   * @param point
   *          The new plot slice position in cm (CSL room coordinates).
   */
  public void selectionChanged(Point3d point);

}

// EOF
