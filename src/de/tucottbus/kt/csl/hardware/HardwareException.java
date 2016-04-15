package de.tucottbus.kt.csl.hardware;

public class HardwareException extends Exception
{
  private static final long serialVersionUID = 1L;

  public HardwareException()
  {
  }

  public HardwareException(String message)
  {
    super(message);
  }

  public HardwareException(Throwable cause)
  {
    super(cause);
  }

  public HardwareException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public HardwareException(String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
