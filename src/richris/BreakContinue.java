package richris;

public class BreakContinue {
  
  public static void main(String[]args){
    
    for (int i = 0; i < 20; i++){
      if (i > 10){
        break;                               //Schleife wird unterbrochen
      }
   //System.out.println(i);
    }
    
    for (int i = 0; i < 20; i++){
      if (i % 2 == 0){                       //Modulo Rest der Division gleich Null dann nächster Durchlauf
        continue;                            //aktueller Durchlauf wird unterbrochen Schleife aber weiter geführt
      }
    System.out.println(i);
    }

}
}