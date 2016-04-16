// Brandenburgische Technische Universitaet Cottbus
// Lehrstuhl Kommunikationstechnik
// CogntiveSystemsLab

package de.tucottbus.kt.csl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import de.tucottbus.kt.csl.cognition.BehaviorController;
import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.led.LedControllerCeiling;
import de.tucottbus.kt.csl.hardware.led.LedControllerViewer;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;
import de.tucottbus.kt.csl.hardware.micarray3d.trolley.Motor;
import de.tucottbus.kt.csl.speech.UasrSpeechEngine;
import de.tucottbus.kt.dlabpro.recognizer.Recognizer;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.feedback.UserFeedbackPlayer;
import de.tucottbus.kt.lcars.logging.Log;
import de.tucottbus.kt.lcars.speech.ISpeechEngine;
import de.tucottbus.kt.lcars.swt.ColorMeta;

/**
 * This singleton represents the KT speech lab.
 * 
 * @author Matthias Wolff
 */
public class CognitiveSystemsLab {
  private UasrSpeechEngine se;

  /**
   * The singleton instance.
   */
  private static CognitiveSystemsLab instance;

  /**
   * The private singleton constructor.
   * 
   * @param args
   *          The command line arguments, can be <code>null</code>.
   */
  private CognitiveSystemsLab(String[] args) {
    // Instantiate hardware wrappers
    if (LCARS.getArg(args, "--nohardware") == null) {
      CslHardware.getInstance().printTree("");
      
      // try-catch structure to start CognitiveSystemsLab without
      // physical hardware or flag --nohardware
      try {
        LedControllerViewer.getInstance().setAmbientColor(ColorMeta.BLUE);
        LedControllerCeiling.getInstance().setAmbientColor(ColorMeta.BLUE);
      } catch (IllegalStateException illStateAmbiLight) {

      }
      
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          System.out.println("CSL hardware shut-down ...");
          CslHardware.getInstance().dispose();
          System.out.println("... CSL hardware shut-down");
        }
      });
    }

    // Instantiate the UASR speech engine
    if (LCARS.getArg(args, "--nospeech") == null) {
      String resPckg = getClass().getPackage().getName() + ".resources.de";
      String spePckg = getClass().getPackage().getName() + ".speech";
      Properties config = new Properties();
      try {
        config.setProperty("data.feainfo",
            LCARS.getResourceFile(resPckg, "feainfo.object").getAbsolutePath());
        config.setProperty("data.sesinfo",
            LCARS.getResourceFile(resPckg, "sesinfo.object").getAbsolutePath());
        config.setProperty("data.gmm",
            LCARS.getResourceFile(resPckg, "3_15.gmm").getAbsolutePath());
        config.setProperty("data.dialog",
            LCARS.getResourceFile(resPckg, "dialog.fst").getAbsolutePath());
        config.setProperty("data.vadinfo",
            LCARS.getResourceFile(resPckg, "3_10_mod.vad").getAbsolutePath());
        config.setProperty("fst.sleep", "5");

        String script = LCARS.getResourceFile(spePckg, "postproc.xtp")
            .getAbsolutePath().replace('\\', '/');
        String sesinf = config.getProperty("data.sesinfo").replace('\\', '/');
        String recognizerExe = UasrSpeechEngine.findRecognizerExe()
            .getAbsolutePath().replace('\\', '/');
        String dLabProExe = recognizerExe.replaceAll("/recognizer.",
            "/dlabpro.");
        Log.info("recognizer is: " + recognizerExe);
        Log.info("dLabPro is   : " + dLabProExe);
        config.setProperty("postproc.cmd", dLabProExe + " \"" + script
            + "\" gui \"" + sesinf + "\" ");

        // TODO This takes very long! -->
        Log.info("Seeking audio device \"MADI (29+30)\" ...");
        int audioDeviceId = Recognizer.getAudioDeviceIdForName(new File(
            recognizerExe), "MADI (29+30)");
        if (audioDeviceId >= 0) {
          config.setProperty("audio.dev", String.valueOf(audioDeviceId));
          Log.info("Audio device ID is " + audioDeviceId);
        } else
          Log.info("Audio device not found, using standard recording device");
        // <--

        se = UasrSpeechEngine.getInstance(config);
        se.addSpeechEventListener(BehaviorController.getInstance());

        if (se != null) {
          // HACK: Auditory feedback will disturb the recognizer :/ -->
          se.setUserFeedbackMode(UserFeedbackPlayer.VISUAL);
          // <--

          // Add the main viewer backlight feedback player (uuh what a word...)
          se.addUserFeedbackPlayer(new UserFeedbackPlayer(
              UserFeedbackPlayer.VISUAL) {
            @Override
            public void writeColor(ColorMeta color) {
              if (LCARS.getArg("--nohardware") != null)
                return;
              try {
                MicArrayViewer.getInstance().setAmbientLight(color);
              } catch (Exception e) {
                Log.err("Error displaying visual user feedback", e);
              }
            }
          });
        }
      } catch (Exception e) {
        Log.err(
            "Failed to start UASR speech engine, reason: " + e.getMessage(), e);
        if (e instanceof FileNotFoundException)
          Log.err("Please run Ant build PackRecognizerData.xml!", e);
      }
    }
  }

  /**
   * Returns the singleton instance.
   */
  public static synchronized CognitiveSystemsLab getInstance() {
    if (instance == null)
      instance = new CognitiveSystemsLab(null);
    return instance;
  }

  /**
   * Returns the speech engine.
   */
  public ISpeechEngine getSpeechEngine() {
    return se;
  }

  // == CSL Main Program ==

  /**
   * The CSL main method. Use script <code>csl.bat</code> in the project root
   * folder to start CSL from the command line.
   * 
   * @param args
   *          Command line options, including:
   * 
   *          <pre>
   *  --clientof=hostname - Serve a remote screen [1]
   *  --nogui             - Do not display the LCARS GUI 
   *  --nospeech          - Disable speech I/O
   *  --nohardware        - Disable the CSL hardware drivers
   *  --nomouse           - Hide the mouse cursor
   *  --panel=classname   - LCARS panel to display at start-up [2]
   *  
   *  [1] LCARS is started in server mode by default
   *  [2] The LCARS server panel is show by default
   * </pre>
   * 
   *          All {@linkplain LCARS#main(String[]) LCARS command line options}
   *          may be used, too.
   */
  public static void main(String[] args) {
    // Instantiate the speech lab
    if (LCARS.getArg(args, "--clientof=") == null) {
      Log.info("Instantiating CSL...");
      CognitiveSystemsLab.instance = new CognitiveSystemsLab(args);
    }

    // Start LCARS
    Log.info("Starting LCARS");
    if (LCARS.getArg(args, "--clientof=") == null)
      args = LCARS.setArg(args, "--server", null);
    LCARS.main(args);
  }

}

// EOF
