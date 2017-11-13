package de.tucottbus.kt.csl;

/**
 * Only for backward-compatibility. -- <i>Use {@link CSL} instead!</i> --
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 * @deprecated
 */
@Deprecated
public class CognitiveSystemsLab extends CSL
{

  /**
   * The protected singleton constructor.
   * 
   * @param args
   *          The command line arguments, can be <code>null</code>.
   */
  protected CognitiveSystemsLab(String[] args)
  {
    super(args);
    System.err.println("Class de.tucottbus.kt.csl.CognitiveSystemsLab "
        + "is deprecated. Use de.tucottbus.kt.csl.CSL instead!");
  }

  /**
   * Returns the singleton instance.
   */
  public static synchronized CognitiveSystemsLab getInstance()
  {
    return (CognitiveSystemsLab)CSL.getInstance();
  }
  
  public static void main(String[] args)
  {
    CSL.main(args);
  }

}

// EOF
