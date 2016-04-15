package de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity;

import javax.vecmath.Color3f;

/**
 * This class will map a dB values to a representing RGB color
 */
public class SensitivityColorScheme {
  
  private static float COL = (float) (50.0/255);
  
  private static float NORM1 = 255.0f/ 4;
  private static float NORM2 = 255.0f/ 8;
  private static float NORM3 = 255.0f/ 48;

  private static int A_SHIFT = 0;
  private static int R_SHIFT = 8;
  private static int G_SHIFT = 16;
  private static int B_SHIFT = 24;
  
  /**
   * Getting the color representing a special logarithmic dB-value.
   * <br><br>
   * The BGRA color scheme is for SWT-Implementation only.
   * 
   * @param db
   *          Double dB-value.
   * @return int value of the dB value (BGRA color scheme).
   */
  public static int getIntColor(double dbIn) {
    double db = (dbIn>0) ? 0 : ((dbIn< -36) ? -36 : dbIn);
    int color = 0;
    if (db >= -4) {
        db = -db*NORM1;
        color = 255            << A_SHIFT
       | 255                  << R_SHIFT
       | (int) (db)           << G_SHIFT
       | (int) ((255 - db) * COL) << B_SHIFT;
    } else if (db >= -12) {
        db = -(db + 4)*NORM2;
        color = 255                << A_SHIFT
       | (int) (255 - db)         << R_SHIFT
       | (int) (127.5 + 0.5 * (255 - db)) << G_SHIFT
       | (int) (db)             << B_SHIFT;
    } else if (db >= -36) {
        db = 127.5+(db + 12) * NORM3;
        color =  255          << A_SHIFT
       |  0            << R_SHIFT
       | (int) (db)    << G_SHIFT
       | (int) (db+db) << B_SHIFT;
    }
    return color;
  }
  
  /**
   * Getting the color representing a special logarithmic dB-value.
   * <br><br>
   * The ARGB color scheme is for AWT-Implementation only.
   * 
   * @param db
   *          Double dB-value.
   * @return Color3f of the dB valuu (ARGB color scheme).
   */
  public static Color3f getColor3f(double db){
    Color3f color3f = null;
    
    db = (db>0) ? 0 : ((db< -36) ? -36 : db);
    
    if (db <= 0 && db >= -4) {
      db = -db / 4;
      color3f = new Color3f(1f, (float)db, (float)((1 - db) * COL));
    } else if (db <= -4 && db >= -12) {
      db = db + 4;
      db = -db / 8;
      color3f = new Color3f((float) (1 * (1 - db)), (float)(0.5 + (0.5 * (1 - db))), (float) (db * 1));
    } else if (db <= -12 && db >= -36) {
      db = db + 12;
      db = -db / 24;
      color3f = new Color3f(0f, (float) (0.5 * (1 - db)), (float) ((1 - db) * 1));
    }
    
    return color3f;
  }

}
