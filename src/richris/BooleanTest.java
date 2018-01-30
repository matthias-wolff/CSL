package richris;

public class BooleanTest {
  /** Es wird festgestellt ob eine Strichrechnung durchgeführt wurde.
   * bei Multiplikation  "int Wert=60*20" oder Division "int Wert=60/20"
   * ist gerechnet "false" 
   * @chris
   */
  
  public static void main(String[]args) {
    
    int Wert=60/20; //Rechenoperation +-*/ einsetzen
    boolean gerechnet=false;//Rechenoperation * und Rechenoperation/ ergeben "false" 
    
    System.out.println(Wert);
    
    
    if (Wert==80){
      
      System.out.println("Addition");
      gerechnet=true; //+ ergibt "true" Ergebnis für Addition
        
    }
    
    if (Wert==40){
      
      System.out.println("Subtraktion");
      gerechnet=true; //- ergibt "true" Ergebnis für Subtraktion
      
    }
    
    if (gerechnet==false){
      
      System.out.println("Es wurde keine Summe oder Differenz gebildet"); //Rechenoperation nicht definiert
    }
    
    
        
    
    
  }
  
  

}

