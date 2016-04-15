package de.tucottbus.kt.csl.hardware;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract wrapper class of atomic hardware devices.
 *
 * @see AHardware
 * @see ACompositeHardware
 * 
 * @author Matthias Wolff
 */
public abstract class AAtomicHardware extends AHardware 
{
  @Override
  protected void checkSubclass()
  {
  }

  @Override
  public final Collection<AHardware> getChildren() 
  {
   return new ArrayList<AHardware>();
  }

}
