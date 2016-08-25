package de.tucottbus.kt.csl.speech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.tucottbus.kt.csl.speech.Fvr.Node;

/**
 * @author Werner Meyer Creates a list of possible strings by FVR.
 * @subauthor Peter Gessler extract algorithm and communication structure
 *            in own class.
 * 
 * @param node
 *          The actually node with his characteristics
 * @return Class ArticulateNodeList. content list with all possible strings.
 *         In ArticulateNodeList.getStrSolutionList();
 */
public class SpeechArticulator implements IArticulateSpeech {

  /**
   * Create list with all articulation options. Use objectName.getStrSolutionList().get(k)
   * to get an entry.
   * 
   * @param node
   * @return
   */
  private ArticulateNodeList getArticulate(Node node) {
    ArticulateNodeList newList = new ArticulateNodeList();

    if (node.children.isEmpty()) { // is node a leaf get his notation
      newList.addStrToStrSolutionList("[" + node.label + ":" + node.weight
          + "]");
      return newList;
    }

    // recursive
    for (Node child : node.children) {

      // differentiate between children with same name to parent
      if (node.label.equals(child.label))
        newList.addToSkipList(getArticulate(child)); // fill SkipList
      else
        newList.addToListOfLists(getArticulate(child)); // Children with
                                                        // different name to
                                                        // parent, fill
                                                        // ListOfLists
    }

    // Permutation over all lists in ListofList
    // Preparation for Permutation
    int iper[] = new int[newList.getListOfLists().size()]; // intArray with
                                                           // length of times of
                                                           // lists
    int fak = 1; // faculty
    for (int k = 0; k < newList.getListOfLists().size(); k++) {
      iper[k] = k + 1; // initial. int Array {1,2,3....}
      fak = fak * iper[k]; // and faculty
    }

    String Str;
 // Iterate over max. times of solutions strings
    for (; fak > 0; fak--) {
      
      // all possible order of elements of all lists together
      for (List<String> x : finiteCartesianProduct(newList.getListOfLists())) { 
        Str = "";
        for (String s : x)
          Str += s;
        Str = "[" + node.label + ":" + node.weight + Str + "]";
        newList.compare(); // Compare is new constellation correct
        if (!newList.getAns()) // then take new Str to SolutionList
          newList.addStrToStrSolutionList(Str);
      }

      // calculate next order of elements (lists) in listOfLists
      int a = 0; // point of reverse
      for (int k = 0; k < iper.length; k++) { // iteration over lists
        if (iper[k] > 1) {
          a = k;
          iper[k]--;
          while (k > 0)
            iper[k - 1] = k--;
          break;
        }
      }
   // Swap only if more then one list is existing
      if (newList.getListOfLists().size() > 1) 
        newList.swapListOfLists(a); // reverse part of lists between 0 and a
    }

    return newList;
  }

  // Source:
  // http://www.java-forum.org/mathematik/89313-kombinatorik-permutation-listen-deren-elementen.html?highlight=Kombinatorik%2FPermutation+Listen+Elementen
  // public class _ {

  /*
   * example: buckets=[[A1,A2,..Aa],[B1,B2,..Bb],...,[X1,X2,..Xx]] the method
   * will return an iterable that allows to iterate over all elements from
   * Cartesian product [A1,A2,..Aa]x[B1,B2,..Bb]x[X1,X2,..Xx] that means it
   * returns an iterator with all combinations:
   * 
   * [A1,B1,...X1] [A2,B1,...,X1] [A3,B1,...,X1] ... [A1,B2,...,X1]
   * [A2,B2,...,X1] ... [Aa,Bb,...,Xx]
   * 
   * @param sets: ordered List of collections of <T> structures
   * 
   * @return: Iterable of List<T> with all elements of cartesian product
   */

  private <T> Iterable<List<T>> finiteCartesianProduct(
      final List<Collection<T>> sets) {
    return new Iterable<List<T>>() {
      private long size = 1;
      {
        for (Collection<T> set : sets)
          size *= (long) set.size();
      }

      @Override
      public Iterator<List<T>> iterator() {
        return new Iterator<List<T>>() {
          long counter = 0;
          ArrayList<T> currentValues = new ArrayList<T>(sets.size());
          ArrayList<Iterator<T>> iterators = new ArrayList<Iterator<T>>(
              sets.size());
          {
            for (Iterable<T> set : sets) {
              Iterator<T> it = set.iterator();
              iterators.add(it);
              if (it.hasNext()) {
                // if not, then the size is 0, hasNext is never true, set empty
                currentValues.add(it.next());
              }
            }
          }

          @Override
          public boolean hasNext() {
            return counter < size;
          }

          @Override
          public List<T> next() {
            List<T> result = new LinkedList<T>(currentValues);
            counter++;
            increment(0);
            return result;
          }

          private void increment(int i) {
            if (iterators.get(i).hasNext()) {
              currentValues.set(i, iterators.get(i).next());
            } else {
              iterators.set(i, sets.get(i).iterator());
              currentValues.set(i, iterators.get(i).next());
              if (i < iterators.size() - 1) {
                increment(i + 1);
              }
            }
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException(
                "impossible to change combination set");
          }
        };
      }
    };
  }

  /**
   * {@link IArticulateSpeech#articulateSpeech(Fvr)}
   */
  @Override
  public ArticulateNodeList articulateSpeech(Fvr feedback) {

    Fvr currentFvr = Fvr.fromString(feedback.toString());

    ArticulateNodeList newList = new ArticulateNodeList();

    newList = getArticulate(currentFvr.root);
    
    return newList;
  }
  
  
  public static void main(String[] args) {
    
    String s;
    
    s = "FVR[PROG:0.3[STEP:0.3[FUNC:1[OFF:1]][TIME:0.2[SA-09.00h:0.2]]][STEP:0.2[FUNC:1[ON:1]][TIME:-0.2[SU-20.00h:-0.2]]]]";
    
    Fvr fvr = Fvr.fromString(s);
    
    IArticulateSpeech speechArticulator = new SpeechArticulator();
    ArticulateNodeList newList = speechArticulator.articulateSpeech(fvr);
    
    for (int k = 0; k < newList.getStrSolutionList().size(); k++) {
      System.out.println(k + ": " + newList.getStrSolutionList().get(k));
    }
  }
}
