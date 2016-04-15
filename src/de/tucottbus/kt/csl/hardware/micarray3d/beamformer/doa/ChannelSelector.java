package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator.MicArray;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.AudioData;

/**
 * All methods containing the selection of the different audio channels.
 * @author Martin Birth
 *
 */
public class ChannelSelector {
  
  /**
   * Selected microphones of the microphone array 1
   */
  private boolean[] channelSelectionArray1;
  
  /**
   * Selected microphones of the microphone array 1
   */
  private boolean[] channelSelectionArray2;
  
  /**
   * Numbers of active channels (array 1)
   */
  private int channelCountArray1;
  
  /**
   * Numbers of active channels (array 2)
   */
  private int channelCountArray2;
  
  /**
   * singleton instance
   */
  private static ChannelSelector instance;
  
  /**
   * Private constructor -> please use {@link #getInstance()}}
   */
  private ChannelSelector(){
    channelSelectionArray1=getDefaultChSelectionArray1();
    channelSelectionArray2=getDefaultChSelectionArray2();
  }
  
  /**
   * Get the instance of the channel selector
   * @return
   */
  public static synchronized ChannelSelector getInstance()
  {
    if (instance==null)
      instance = new ChannelSelector();
    return instance;
  }
  
  /**
   * Getting the default microphone selection for microphone array 1.
   * @return boolean[]
   */
  private boolean[] getDefaultChSelectionArray1(){
    boolean[] chSelection = new boolean[RmeHdspMadi.CHANNEL_COUNT/2];
    Arrays.fill(chSelection, Boolean.FALSE);
    
    chSelection[0]=Boolean.TRUE;
    chSelection[1]=Boolean.TRUE;
    chSelection[2]=Boolean.TRUE;
    chSelection[4]=Boolean.TRUE;
    chSelection[5]=Boolean.TRUE;
    chSelection[7]=Boolean.TRUE;
    chSelection[8]=Boolean.TRUE;
    chSelection[10]=Boolean.TRUE;
    chSelection[11]=Boolean.TRUE;
    chSelection[12]=Boolean.TRUE;
    chSelection[14]=Boolean.TRUE;
    chSelection[15]=Boolean.TRUE;
    chSelection[17]=Boolean.TRUE;
    chSelection[18]=Boolean.TRUE;
    chSelection[19]=Boolean.TRUE;
    chSelection[20]=Boolean.TRUE;
    chSelection[22]=Boolean.TRUE;
    chSelection[23]=Boolean.TRUE;
    chSelection[26]=Boolean.TRUE;
    chSelection[28]=Boolean.TRUE;
    chSelection[29]=Boolean.TRUE;
    chSelection[31]=Boolean.TRUE;
    
    // This microphone has any errors. Do not set to true!
    chSelection[25]=Boolean.FALSE;
    
    setChannelCount(MicArray.ARRAY1, chSelection);
    return chSelection;
  }
  
  /**
   * Getting the default microphone selection for microphone array 2.
   * @return boolean[]
   */
  private boolean[] getDefaultChSelectionArray2(){
    boolean[] chSelection = new boolean[RmeHdspMadi.CHANNEL_COUNT/2];
    Arrays.fill(chSelection, Boolean.TRUE);
    
    // This microphone has any errors. Do not set to true!
    chSelection[3] =Boolean.FALSE;
    chSelection[7] =Boolean.FALSE;
    chSelection[14]=Boolean.FALSE;
    chSelection[15]=Boolean.FALSE;
    chSelection[22]=Boolean.FALSE;
    chSelection[28]=Boolean.FALSE;
    chSelection[29]=Boolean.FALSE;
    
    // Do not set this microphone to false!! its the reference mic
    chSelection[0]=Boolean.TRUE;
        
    setChannelCount(MicArray.ARRAY2, chSelection);
    return chSelection;
  }
   
  /**
   * Fill the hashmaps with the audio and position data
   * @param framesPerChannel - float[n][m] array with n channels and m audio samples
   * @param audioDataHM - ConcurrentHashMap<Integer, AudioData>
   * @param positions - Point3d[] with microphone positions
   * @param array - {@link MicArray}
   */
  public void fillAudioHM(float[][] framesPerChannel, ConcurrentHashMap<Integer, AudioData> audioDataHM, Point3d[] positions, MicArray array){
    audioDataHM.clear();
    
    boolean[] selection = getChannelSelection(array);
    int shift = 0;
    if(array==MicArray.ARRAY2)
      shift=RmeHdspMadi.CHANNEL_COUNT/2;
    
    for (int i = 0; i < selection.length; i++) {
      if(selection[i]==true){
        audioDataHM.put(i, new AudioData(i, framesPerChannel[i+shift], selection[i], positions[i+shift]));
      }
    }
  }
  
  /**
   * Setting the selected channels.
   * @param selection - boolean[]
   * @param array - {@link MicArray}
   */
  public void setChannelSelection(boolean[] selection, MicArray array){
    if(selection.length==channelSelectionArray1.length)
      if(array==MicArray.ARRAY1){
        channelSelectionArray1=selection;
        setChannelCount(array, selection);
      } else {
        channelSelectionArray2=selection;
        setChannelCount(array, selection);
      }
  }
  
  /**
   * Reading out the selected channels.
   * @param array - {@link MicArray}
   * @return boolean[]
   */
  public boolean[] getChannelSelection(MicArray array){
    if(array==MicArray.ARRAY1)
      return channelSelectionArray1;
    else
      return channelSelectionArray2;
  }
  
  /**
   * Reading out the selected channels.
   * @return boolean[]
   */
  public boolean[] getTotalChannelSelection(){
    boolean[] selection = new boolean[RmeHdspMadi.CHANNEL_COUNT];
    
    for (int i = 0; i < selection.length/2; i++) {
      selection[i]=channelSelectionArray1[i];
    }
    for (int i = 0 ; i < selection.length/2; i++) {
      selection[i+RmeHdspMadi.CHANNEL_COUNT/2]=channelSelectionArray2[i];
    }
    
    return selection;
  }
  
  /**
   * Set the number of all active channels per array
   * @param array - {@link MicArray}
   * @return int
   */
  private void setChannelCount(MicArray array, boolean[] selection){
    int j=0;
    for (int i = 0; i < selection.length; i++) {
      j = (selection[i]) ? j+1 : j; 
    }
    
    if(array==MicArray.ARRAY1)
      channelCountArray1=j;
    else
      channelCountArray2=j;
  }
  
  /**
   * Get the number of active channels for a specific microphone array
   * @param array - {@link MicArray}
   * @return int
   */
  public int getChannelCount(MicArray array){
    if(array==MicArray.ARRAY1)
      return channelCountArray1;
    else
      return channelCountArray2;
  }
  
  /**
   * Get the number of all active channels
   * @return int
   */
  public int getTotalChannelCount(){
    return getChannelCount(MicArray.ARRAY1)+getChannelCount(MicArray.ARRAY2);
  }
  
}
