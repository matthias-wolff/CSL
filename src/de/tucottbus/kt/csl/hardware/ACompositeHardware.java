package de.tucottbus.kt.csl.hardware;

import de.tucottbus.kt.lcars.logging.Log;

/**
 * Abstract wrapper class of composite hardware devices. A composite hardware
 * wrapper has a non-empty list of {@linkplain AHardware#getChildren() children}.
 * This class provides common implementations of the {@link AHardware#dispose()}, 
 * {@link AHardware#isDisposed()}, and {@link AHardware#isConnected()} methods
 * for composite devices:
 * <ul>
 *   <li>Invoking {@link #dispose()} on a composite hardware wrapper disposes of
 *   all children.</li>
 *   <li>A composite hardware wrapper {@linkplain #isDisposed() is disposed} iff
 *   all children are disposed.</li>
 *   <li>A composite hardware wrapper {@linkplain #isConnected() is connected} 
 *   iff all children are connected.</li>
 * </ul>
 * @see AAtomicHardware
 * @see AHardware#getChildren()
 * 
 * @author Matthas Wolff
 */
public abstract class ACompositeHardware extends AHardware 
{
  @Override
  protected void checkSubclass()
  {
  }

  @Override
  public void dispose() 
  {
    for (AHardware child : getChildren())
      try
      {
        if (!child.isDisposed())
          child.dispose();
      }
      catch (Exception e)
      {
        Log.err("Error disposing composite hardware",e);
      }
    super.dispose();
  }

  @Override
  public boolean isConnected() 
  {
    for (AHardware child : getChildren())
      if (!child.isConnected())
        return false;
    return true;
  }

}
