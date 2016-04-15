package de.tucottbus.kt.csl.zombie.hardware;

import java.util.Observable;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * @deprecated Use {@link AHardware} instead!
 */
public abstract class Hardware extends Observable
{
  // -- Constants --
  
  /**
   * The COM port of the trolley's laser distance measuring sensor (microphone
   * array II).
   */
  public static final String COM_LASER = "COM7";

  /**
   * The COM port of the LED controller board at microphone array I (main
   * viewer).
   */
  public static final String COM_LED_VIEWER = "COM3";

  /**
   * The COM port of the LED controller board at microphone array II (ceiling).
   */
  public static final String COM_LED_CEILING = "COM4";

  /**
   * The COM port of the trolley motor control (microphone array II).
   */
  public static final String COM_LED_MOTOR = "COM6";

  // -- Constructors --
  
  public Hardware()
  {
    System.err.println("WARNING: Instantiated deprecated hardware wrapper "
      + getClass().getName()+" at");
    Thread.dumpStack();
  }
  
  // -- Abstract API --
  
  /**
   * Disposes of this hardware item. Derived classes must free all system 
   * resources allocated by the wrapper.
   * 
   * @throws IllegalStateException if the wrapper is disposed.
   */
  public abstract void dispose();

  /**
   * Determines whether this hardware wrapper is disposed, normally if all 
   * communication channels with the hardware have been closed and released.
   */
  public abstract boolean isDisposed();
  
  /**
   * Returns the unique, human readable name of this hardware item.
   */
  public abstract String getName();

  /**
   * Returns <code>true</code> if this hardware item is connected,
   * <code>false</code> otherwise.
   * 
   * @throws IllegalStateException if the wrapper is disposed.
   */
  public abstract boolean isConnected();

  /**
   * Returns an element contributor to an {@link Panel LCARS panel} supplying
   * low-level controls of this hardware item. The return value may be
   * <code>null</code> if the item does not support any controls. 
   * 
   * @param x
   *          The x-coordinate of the upper left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the upper left corner (in LCARS panel pixels).
   * @return An LCARS {@link ElementContributor} or <code>null</code>
   */
  public abstract ElementContributor getLcarsSubpanel(int x, int y);
 
  // -- Worker methods --

  /**
   * Checks this hardware wrapper and throws an {@link IllegalStateException} if
   * the wrapper is irrecoverably not functional.
   * 
   * @throws IllegalStateException if the wrapper is disposed. Derived classes
   * may add additional checks. The exception message should contain a 
   * human-readable hint.
   */
  protected void check() throws IllegalStateException
  {
    if (isDisposed())
      throw new IllegalStateException("Wrapper is disposed");
  }
}
