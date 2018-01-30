package richris;

public class SwitchCase {
  
  public static void main(String[]args){
    
  int iVersion = 1;
    
 /*   if (iVersion == 0){
      
      System.out.println("Freeversion");  
    }
    
    else if(iVersion == 1){
      
      System.out.println("Premiumversion");
    }
    
    else {
      
      System.out.println("unbekannt");
     
    }
    
  **/
    
    
    /*switch(iVersion) {                                     
      
      case 0:         System.out.println("Premiumversion"); //Einsprungspunkt
                      break;                                //Ende
                      
      case 1:         System.out.println("Freeversion");
                      break;
             
      default:       System.out.println("unbekannt");
      
    
    }  **/
    
 

   String sVersion = "free";
    
    switch(sVersion) {                                                  //String Variante
      
      case "premium":      System.out.println("Premiumversion"); 
                           break;
                      
      case "free":         System.out.println("Freeversion");
                           break;
             
      default:             System.out.println("unbekannt");
      
    
    }  
  

   
//  enum Version
//  {
//    
//    FREE, PREMIUM;
//   
//  }
//  
  
//   Version eVersion = Version.FREE;    //Constructor muss noch übergeordnet definiert werden
//   
//   switch(eVersion) {                                                  
//     
//     case PREMIUM:      System.out.println("Premiumversion"); 
//                        break;
//                     
//     case FREE:         System.out.println("Freeversion");
                        

   }
   
   
   
 
 
    
  }
