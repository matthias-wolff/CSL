package de.tucottbus.kt.csl.cognition;

import de.tucottbus.kt.lcars.speech.ISpeechEventListener;
import de.tucottbus.kt.lcars.speech.events.SpeechEvent;

/**
 * CSL's behavior controller.
 * 
 * @author Peter Gessler
 */
public final class BehaviorController implements ISpeechEventListener 
{
  // -- Singleton implementation --

  private static volatile BehaviorController singleton = null;

  public synchronized static BehaviorController getInstance()
  {
    if (singleton==null)
      singleton = new BehaviorController();
    return singleton;
  }

  private BehaviorController()
  {
    // TODO Constructor stub
  }
  
  // -- Implementation of ISpeechEventListener --
  
  @Override
  public void speechEvent(SpeechEvent event) 
  {
    // TODO Auto-generated method stub

  }

}
