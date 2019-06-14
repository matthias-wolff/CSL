package de.tucottbus.kt.csl.cognition;

import de.tucottbus.kt.lcars.speech.ISpeechEventListener;
import de.tucottbus.kt.lcars.speech.events.PostprocEvent;
import de.tucottbus.kt.lcars.speech.events.SpeechEvent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;

import de.tucottbus.kt.csl.hardware.micarray3d.trolley.Motor;
import de.tucottbus.kt.csl.hardware.micarray3d.trolley.Trolley;
import de.tucottbus.kt.csl.speech.Fvr;
import de.tucottbus.kt.csl.speech.Fvr.Node;

/**
 * CSL's behavior controller.
 * 
 * @author Peter Gessler
 * @author Werner Meyer
 */
public final class BehaviorController implements ISpeechEventListener 
{
  // -- Singleton implementation --
  private static volatile BehaviorController singleton = null;
  public synchronized static BehaviorController getInstance()
  {
    if (singleton==null)
      singleton = new BehaviorController();
    return singleton;
  }
  
  private BehaviorController()
  {
    // TODO Constructor stub
    //if(se==NULL)
    System.out.println("Test: BehaviorController is started");
  }
  
  // -- Implementation of ISpeechEventListener --
  
  @Override
  public void speechEvent(SpeechEvent event) 
  {
    Trolley trolley = Trolley.getInstance();
    // TODO Auto-generated method stub
    if (event instanceof PostprocEvent)
    {      
      PostprocEvent pe = (PostprocEvent)event;
      String result = pe.result;

      if(result.startsWith("FVR")) 
      {
        Fvr fvrInp = Fvr.fromString(result);
        Node nNode;
        Node nodeInt = null;
        Node nodeDec = null;
        double value = 0;
        int valPreCom = 0;
        int valAftCom = 0;
        int decPlace = 1;
        boolean bIntExist = false;
        boolean bDecExist = false;
        
        // Function for absolute max position
        if (!fvrInp.findNode("MOVETOABS.abspos.max").isEmpty())
        {
          trolley.setCeilingPosition(90);
        }
        // Function for absolute middle position
        else if (!fvrInp.findNode("MOVETOABS.abspos.0").isEmpty())
        {
          trolley.setCeilingPosition(0);
        }
        // compute only MOVETOABS 
        else if (fvrInp.getFirstNode().getLabel().equals("MOVETOABS"))
        {               
          //MOVETOABS[MAID[ma2]][abspos[0]]
          
          //=======================================================================
          // FUNCTION: check number and compress to int value
          Collection<Node> nodeCollection;
          Collection<Node> nodeDigitCollection;      
          nNode = fvrInp.findNode("MOVETOABS.abspos.LDIM").stream().findFirst().get();
          if (!nNode.getLabel().isEmpty())
          {
            nodeCollection = nNode.getChildren();
            for (Node nNodeIterator : nodeCollection)
            {
              if (nNodeIterator.getLabel().equals("int"))
              {
                nodeInt = nNodeIterator;
                nNodeIterator = nNodeIterator.getChildren().stream().findFirst().get();
                if (nNodeIterator.getLabel().equals("DIG")||nNodeIterator.getLabel().equals("CN3"))
                {
                  nodeDigitCollection = nNodeIterator.getChildren();
                  for (Node nNodeAux : nodeDigitCollection)
                  {
                    valPreCom = DigitToNumber(nNodeAux) + valPreCom;                
                  }
                }
                bIntExist = true;
              }
              if (nNodeIterator.getLabel().equals("dec"))
              {
                nodeDec = nNodeIterator;
                nNodeIterator = nNodeIterator.getChildren().stream().findFirst().get();
                if (nNodeIterator.getLabel().equals("DIG")||nNodeIterator.getLabel().equals("CN3")||nNodeIterator.getLabel().equals("CN2"))
                {
                  switch (nNodeIterator.getLabel()) {
                  case "DIG":
                    decPlace = 10; break;
                  case "CN2":
                    decPlace = 100; break;
                  case "CN3":
                    decPlace = 1000; break;
                  }
                  nodeDigitCollection = nNodeIterator.getChildren();
                  for (Node nNodeAux : nodeDigitCollection)
                  {
                    valAftCom = DigitToNumber(nNodeAux) + valAftCom;                
                  }
                }
                bDecExist = true;
              }
            }
            
            // Delete old nodes
            if (bIntExist)
              nNode.children.remove(nodeInt);
            if (bDecExist)
              nNode.children.remove(nodeDec);
            
            // Add result to previous feature (LDIM, MCID, ...)
            value = (valPreCom*decPlace + valAftCom);
            value = value/decPlace;
            Fvr.fromString("FVR[number[$" + Double.toString(value) + "]]").getFirstNode().setParent(nNode);
          } 
          //=====================================================================
          // END OF FUNCTION: check number and compress to int value
          //fvrInp.printFvr("test");
          
          //=====================================================================
          // Check unit must be 'cm'
          String valStr;
          valStr = fvrInp.findNode("MOVETOABS.abspos.LDIM.unit").stream().findFirst().get().getChildren().stream().findFirst().get().label;
          value = Double.parseDouble(fvrInp.findNode("MOVETOABS.abspos.LDIM.number").stream().findFirst().get().getChildren().stream().findFirst().get().label); //.substring(1));
          if (!fvrInp.findNode("MOVETOABS.abspos.LDIM.sign.-").isEmpty())
            value = -1*value;
          if (valStr.equals("cm"))
          {
            System.out.println("Detect unit 'cm' with a value of " + value);
          } 
          else if(valStr.equals("m"))
          {
            System.out.println("Detect unit 'm' with a value of " + value);
            value = value*100;
          }
          System.out.println("Value is now " + value);
          if (-101<value && value<91) {  //Range of trolley
            System.out.println("Set attitude of " + value + "cm!");
            try{
              trolley.setCeilingPosition(value);
            } 
            catch(Exception e){
              System.out.println("Fehler beim starten");
              System.out.println(e);
            }           
          }
        }
      }
    }
  }
  
  public static Fvr FvrUnion(Fvr fvrExp, Fvr fvrInp)
  {
    System.out.println("Start function");
    System.out.println(fvrExp.getFirstNode().label);
    System.out.println(fvrInp.getFirstNode().label);
    if (fvrExp.getFirstNode().label.equals(fvrInp.getFirstNode().label))
      System.out.println("Test");
    
    return fvrExp;
  }
  
  /**
   * Sprachausgabe TTS
   */
  public void FvrSpeech()
  {
    
  }
  
  // FUNCTION DigitToNumber
  private static int DigitToNumber(Node nNode)
  {    
    String nLabel;
    nLabel = nNode.getChildren().stream().findFirst().get().label;
    switch(nNode.getLabel())
    {
    case "o":
      return Integer.parseInt(nLabel);
    case "t":
      return Integer.parseInt(nLabel)*10;                
    case "h":
      return Integer.parseInt(nLabel)*100;
    default: 
      return 0;
    }
  }
  
  public static void main(String[] args) {
    Trolley trolley = Trolley.getInstance();
    System.out.println("Start Main Function");
//    Fvr fvrInp = Fvr.fromString("FVR[MOVETOABS[MAID[$ma]][abspos[LDIM[int[DIG[o[$1]]]][unit[$m]][dec[CN2[o[$6]][t[$2]]]]]]]");

    Fvr fvrInp = Fvr.fromString("FVR[MOVETOABS[MAID[ma]][abspos[LDIM[sign[-]][int[CN3[h[0]][t[2]]]][unit[cm]]]]]");
    
    fvrInp.printFvr("fvrInp");
    Node nNode;
    Node nodeInt = null;
    Node nodeDec = null;
    double value = 0;
    int valPreCom = 0;
    int valAftCom = 0;
    int decPlace = 1;
    boolean bIntExist = false;
    boolean bDecExist = false;
    
    // compute only MOVETOABS 
    if (fvrInp.getFirstNode().getLabel().equals("MOVETOABS"))
    {
      //=======================================================================
      // FUNCTION: check number and compress to int value
      Collection<Node> nodeCollection;
      Collection<Node> nodeDigitCollection;      
      nNode = fvrInp.findNode("MOVETOABS.abspos.LDIM").stream().findFirst().get();
      if (!nNode.getLabel().isEmpty())
      {
        nodeCollection = nNode.getChildren();
        for (Node nNodeIterator : nodeCollection)
        {
          if (nNodeIterator.getLabel().equals("int"))
          {
            nodeInt = nNodeIterator;
            nNodeIterator = nNodeIterator.getChildren().stream().findFirst().get();
            if (nNodeIterator.getLabel().equals("DIG")||nNodeIterator.getLabel().equals("CN3"))
            {
              nodeDigitCollection = nNodeIterator.getChildren();
              for (Node nNodeAux : nodeDigitCollection)
              {
                valPreCom = DigitToNumber(nNodeAux) + valPreCom;                
              }
            }
            bIntExist = true;
          }
          if (nNodeIterator.getLabel().equals("dec"))
          {
            nodeDec = nNodeIterator;
            nNodeIterator = nNodeIterator.getChildren().stream().findFirst().get();
            if (nNodeIterator.getLabel().equals("DIG")||nNodeIterator.getLabel().equals("CN3")||nNodeIterator.getLabel().equals("CN2"))
            {
              switch (nNodeIterator.getLabel()) {
              case "DIG":
                decPlace = 10; break;
              case "CN2":
                decPlace = 100; break;
              case "CN3":
                decPlace = 1000; break;
              }
              nodeDigitCollection = nNodeIterator.getChildren();
              for (Node nNodeAux : nodeDigitCollection)
              {
                valAftCom = DigitToNumber(nNodeAux) + valAftCom;                
              }
            }
            bDecExist = true;
          }
        }
        
        // Delete old nodes
        if (bIntExist)
          nNode.children.remove(nodeInt);
        if (bDecExist)
          nNode.children.remove(nodeDec);
        
        // Add result to previous feature (LDIM, MCID, ...)
        System.out.println("value of valPreCom " + valPreCom + " and of valAftCom " + valAftCom + " and value of decPlace " + decPlace);
        value = (valPreCom*decPlace + valAftCom);
        System.out.println("value is " + value);
        value = value/decPlace;
        System.out.println("value is " + value);
        Fvr.fromString("FVR[number[$" + Double.toString(value) + "]]").getFirstNode().setParent(nNode);
      } 
      //=====================================================================
      // END OF FUNCTION: check number and compress to int value
      fvrInp.printFvr("test");
      
      //=====================================================================
      // Check unit must be 'cm'
      String valStr;
      valStr = fvrInp.findNode("MOVETOABS.abspos.LDIM.unit").stream().findFirst().get().getChildren().stream().findFirst().get().label;
      //valStr = valStr.substring(1);
      //System.out.println("valStr is " + valStr);
      value = Double.parseDouble(fvrInp.findNode("MOVETOABS.abspos.LDIM.number").stream().findFirst().get().getChildren().stream().findFirst().get().label); //.substring(1));
      if (!fvrInp.findNode("MOVETOABS.abspos.LDIM.sign.-").isEmpty())
        value = -1*value;
      if (valStr.equals("cm"))
      {
        System.out.println("Detect unit 'cm' with a value of " + value);
      } 
      else if(valStr.equals("m"))
      {
        System.out.println("Detect unit 'm' with a value of " + value);
        value = value*100;
      }
      
      if (-100<value && value <110) {
        System.out.println("Set attitude of " + value + "cm!");
        try{
          trolley.setCeilingPosition(value);
          System.out.println("Teststring");
        }   
        catch(Exception e){
          System.out.println("Fehler beim starten");
          System.out.println(e);
        }           
      }
    }      
    
    System.out.println("Ende");
  }
}
