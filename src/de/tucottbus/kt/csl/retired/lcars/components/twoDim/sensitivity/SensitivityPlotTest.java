package de.tucottbus.kt.csl.retired.lcars.components.twoDim.sensitivity;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot.CLState;

/**
 * -- for testings only --
 * 
 * @author Martin Birth
 */
@Deprecated
public class SensitivityPlotTest {

  @SuppressWarnings("unused")
  private static void saveImage(BufferedImage img) {
    try {
      // retrieve image
      File outputfile = new File("c:/temp/SensitivityPlot.png");
      ImageIO.write(img, "png", outputfile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    Runnable r = () -> {
      SensitivityPlot sens = SensitivityPlot.getInstance(CLState.CL_BUFFER);
      MicArrayState state = MicArrayState.getCurrent();
      sens.getHorizontalSensitivityIntArray(state, new Point3d(0, 0, 160),1000f, 440, 440);
      long time = System.currentTimeMillis();
      for (int i = 0; i <= 250; i++) {
        sens.getHorizontalSensitivityIntArray(state, new Point3d(0, 0, i),1000f, 440, 440);
      }
      time = System.currentTimeMillis() - time;
      System.out.println(time);
      MicArray3D.getInstance().dispose();
      System.exit(0);
    };
    SwingUtilities.invokeLater(r);
  }
}
