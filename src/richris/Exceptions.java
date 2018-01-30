package richris;


import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class Exceptions {
  
  public static void main (String[]args) throws FileNotFoundException {  //Ausnahme wird an höhere Instanz übergeben catch kann entfallen
    
    int array[] = new int [5];
    int b = 6;
    
    
    try{
    for (int i = 0; i < b; i++)
      {  
      array[i] = i; throw new Exception("Es wurde zu weit gezählt!");
      }
    
    }catch(Exception e){
      
    // b = 5;
      
     e.printStackTrace();  
    }
    
    try{
    RandomAccessFile f = new  RandomAccessFile("test.txt","rw");
    
    }catch(FileNotFoundException f){
      
      System.out.println(f.getMessage());
    
      f.printStackTrace();
      
    }finally{                               // auch ohne catch möglich wird immer ausgeführt
  }
  }
}


