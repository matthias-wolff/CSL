package de.tucottbus.kt.csl.retired.lcars.messages;

@Deprecated
public interface IObservable {

  public void addObserver(IObserver o);

  public void deleteObserver(IObserver o);

  public void notifyObservers();
  public void notifyObservers(Object arg);

  public void deleteObservers();

  public void setChanged();
  public boolean hasChanged();
  public int countObservers();

  
}
