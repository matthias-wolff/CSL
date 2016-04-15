package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;

public class DoATest {
  // ################### for testings only ###################
  
  public static void writetoTextFile(int[] array, int arrayType){
    BufferedWriter writer = null;
    try {
        //create a temporary file
        File logFile = new File("c://temp//taus_"+arrayType+".txt");

        writer = new BufferedWriter(new FileWriter(logFile, true));
        for (int i = 0; i < array.length; i++) {
          writer.write(array[i]+";");
        }
        writer.newLine();
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            // Close the writer regardless of what happens...
            writer.close();
        } catch (Exception e) {
        }
    }
  }
  
  @SuppressWarnings("unused")
  private void writetoTextFile(double[] array){
    BufferedWriter writer = null;
    try {
        //create a temporary file
        File logFile = new File("c://temp//r_Si.txt");

        writer = new BufferedWriter(new FileWriter(logFile, true));
        for (int i = 0; i < array.length; i++) {
          writer.write(array[i]+";");
        }
        writer.newLine();
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            // Close the writer regardless of what happens...
            writer.close();
        } catch (Exception e) {
        }
    }
  }
  
  public static void main(String[] args) {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        RmeHdspMadi.getInstance();
        // WavePlayer wavePlayer = new WavePlayer();
        // wavePlayer.playSound(new TestData().getPath(TestData.TEST[9]));
        DoAEstimator.getInstance().setAutoMode(true);
        DoAEstimator.getInstance().setVerbose(1);
      }
    });
    thread.setName("DoA-Test-Thread");
    thread.start();
  }
}
