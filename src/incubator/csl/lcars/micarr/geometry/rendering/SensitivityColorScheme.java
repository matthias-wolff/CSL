package incubator.csl.lcars.micarr.geometry.rendering;

import javax.vecmath.Color3f;

/**
 * Color scheme for spatial sensitivity plots of the CSL microphone array.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class SensitivityColorScheme 
{
  private static float COL     = 50.0f/255;
  private static float NORM1   = 255.0f/4;
  private static float NORM2   = 255.0f/8;
  private static float NORM3   = 255.0f/48;
  private static int   A_SHIFT = 0;
  private static int   R_SHIFT = 8;
  private static int   G_SHIFT = 16;
  private static int   B_SHIFT = 24;
  
  /**
   * Returns a color representing a sensitivity.
   * 
   * @param db
   *          The sensitivity (in dB) in the interval [-36,0].
   * @return The color represented as a BGRA integer.
   */
  public static int dbToBGRA(float db) 
  {
    db = (db>0) ? 0 : ((db<-36) ? -36 : db);
    int color = 0;
    if (db>=-4) 
    {
      db = -db*NORM1;
      color = 255                 << A_SHIFT
            | 255                 << R_SHIFT
            | (int)(db)           << G_SHIFT
            | (int)((255-db)*COL) << B_SHIFT;
    } 
    else if (db>=-12) 
    {
      db = -(db+4)*NORM2;
      color = 255                       << A_SHIFT
            | (int)(255-db)             << R_SHIFT
            | (int)(127.5+0.5*(255-db)) << G_SHIFT
            | (int)(db)                 << B_SHIFT;
    } 
    else if (db >= -36) 
    {
      db = 127.5f+(db+12)*NORM3;
      color =  255         << A_SHIFT
            |  0           << R_SHIFT
            | (int)(db)    << G_SHIFT
            | (int)(db+db) << B_SHIFT;
    }
    return color;
  }
  
  /**
   * Returns a color representing a sensitivity.
   * 
   * @param db
   *          The sensitivity (in dB) in the interval [-36,0].
   * @return The color.
   */
  public static Color3f dbToColor3f(float db)
  {
    Color3f color3f;
    
    db = (db>0) ? 0 : ((db<-36) ? -36 : db);
    
    if (db<=0 && db>=-4) 
    {
      db = -db / 4;
      color3f = new Color3f(1f, db, (1-db)*COL);
    } 
    else if (db <= -4 && db >= -12) 
    {
      db = db + 4;
      db = -db / 8;
      color3f = new Color3f(1*(1-db), 0.5f+(0.5f*(1-db)), db*1);
    } 
    else if (db <= -12 && db >= -36) 
    {
      db = db + 12;
      db = -db / 24;
      color3f = new Color3f(0f, 0.5f*(1-db), (1-db)*1);
    }
    else
      color3f = new Color3f(0f, 0f, 0f);
    
    return color3f;
  }

}

// EOF

