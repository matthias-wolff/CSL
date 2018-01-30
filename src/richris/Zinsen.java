package richris;

public class Zinsen {
  
  public static void main (String[]args){
    
    double kapital=5000;
    int lauf=35;
    double zinssatz=0.05; //5%
    
    
    System.out.println("Startbetrag :" + kapital + "€");
    
    for (int i=1; i<=lauf; i++){
      
      kapital=(kapital*zinssatz)+kapital;
      
     System.out.println( "Neuer Kontostand nach " + i + " Jahr/en : "  + kapital + "€");
     
    }
    
    
  }

}
