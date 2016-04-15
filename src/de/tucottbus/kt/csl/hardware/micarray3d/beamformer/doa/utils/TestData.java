package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils;

import org.apache.commons.lang3.StringUtils;

import de.tucottbus.kt.lcars.logging.Log;

/**
 * This static class contains the filenames and the path of the testdata as strings.
 * @author Martin Birth
 *
 */
public class TestData {
  
  /**
   * path to the test files
   */
  private static final String PATH = "main/resources/testaudiodata/";
  
  /**
   * test cases
   */
  public static final String[] TEST = {
      "00_Silence.wav",
      "01_Sprache.wav",
      "02_Sprache_15%WhiteNoise.wav",
      "03_Sprache_30%WhiteNoise.wav",
      "04_MusicalTonesSweep.wav",
      "05_1kHz_10sec.wav",
      "06_WhiteNoise_10Sec.wav",
      "07_LogSweep_1sec.wav",
      "08_LinSweep_1sec.wav",
      "09_Noise_White_30sec.wav"};
  
  /**
   * Get the absolute path for a certain file/test case
   * @param fileName - String
   * @return String
   */
  public String getPath(String fileName){
    String path = null;
    String classPath = this.getClass().toString();
    int occurance = StringUtils.countMatches(classPath, ".");
    String filepath = ""; 
    for (int i = 0; i < occurance; i++) {
      filepath +="../";
    }
    try {
      path = this.getClass().getResource(filepath+PATH+fileName).getPath();
    } catch (Exception e) {
      Log.err(e.getMessage(),e);
    }
    return path;
  }
  

}
