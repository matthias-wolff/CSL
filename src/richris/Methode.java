package richris;

import java.util.*;


public class Methode {
  
  public static void main(String[]args){
    
   Scanner sc = new Scanner(System.in);
 
    
    System.out.println("Gebe die Anzahl der Spieler ein");
         
    int spieler = sc.nextInt(); 
         
    System.out.println("Die Spieleranzahl ist: " + spieler); 
    
         
    System.out.println("Wie viele Tore wurden erziehlt?");
    
    int tore = sc.nextInt();
    
    System.out.println("Ihr habt zusammen: " + tore + " Tore erzielt.");
    
    double torquote = tore/spieler;
    
    System.out.println("Torquote: " + torquote);
    
    
             
  }
  

      
      
      
  
  
  
  
  
  
}
  


