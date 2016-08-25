package de.tucottbus.kt.csl.speech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Werner Meyer
 * 
 */
// Subauthor: Werner Meyer
public class ArticulateNodeList {

  // list of lists from children with strings ([part]Solutions)
  private List<Collection<String>> listOfLists = new LinkedList<Collection<String>>();

  // List of children to pass notes with same name
  private List<Collection<String>> SkipList = new LinkedList<Collection<String>>();

  // List of all possible Strings with children of the note
  private List<String> StrSolutionList = new ArrayList<String>();

  // Reference integer list, to numbering the times of different lists
  // e.g.: lists: ABBCB => 12232
  private List<Integer> iSym = new ArrayList<Integer>();

  // Reference integer list, to numbering the times of different sequence
  // inside a list
  // e.g.: lists: ABBCB => 11213
  private List<Integer> iSeq = new ArrayList<Integer>();

  private int timesList = 0; // count times of lists
  private boolean ans; // Is String after permutation correct?

  // Constructor
  public ArticulateNodeList() {}

  // Sets/Adds
  public void addToListOfLists(ArticulateNodeList prevList) {

    // Add list of children to listOfLists
    this.listOfLists.add(prevList.getStrSolutionList());
    timesList += 1;
    iSym.add(timesList);
    iSeq.add(1); // Numbering the reference integer lists

    // If children SkipList is not empty, put all in listOfLists too
    if (!prevList.getSkipList().isEmpty()) {
      this.listOfLists.addAll(prevList.getSkipList());
      for (int i = 1; i <= prevList.getSkipList().size(); i++) {
        iSym.add(timesList); // Numbering the reference integer lists
        iSeq.add(i + 1);
      }
    }
  }

  // By same name of parent and children add all lists of children to SkipList
  public void addToSkipList(ArticulateNodeList prevList) {
    this.SkipList.add(prevList.getStrSolutionList());
    this.SkipList.addAll(prevList.getSkipList());
  }

  // Reverse all elements from element 1 to element a
  public void swapListOfLists(int a) {
    List<Collection<String>> AccList = new LinkedList<Collection<String>>();
    List<Integer> iAccSym = new ArrayList<Integer>();
    List<Integer> iAccSeq = new ArrayList<Integer>();

    // Take Sublist of main-list and reference integer lists
    AccList = listOfLists.subList(0, a + 1);
    iAccSym = iSym.subList(0, a + 1);
    iAccSeq = iSeq.subList(0, a + 1);

    // Reverse the sublists
    Collections.reverse(AccList);
    Collections.reverse(iAccSym);
    Collections.reverse(iAccSeq);

    // Put sublist back to original list
    for (int i = 0; i < AccList.size(); i++) {
      listOfLists.set(i, AccList.get(i));
      iSym.set(i, iAccSym.get(i));
      iSeq.set(i, iAccSeq.get(i));
    }
  }

  public void addStrToStrSolutionList(String Str) {
    StrSolutionList.add(Str);
  }

  public void compare() { // Check actually constellation
    int ref;

    // Everytime on start set ans false, is it true the new
    // constellation is not access to write a string to solution
    // list
    ans = false;

    // loop over times of different lists
    for (int iout = 1; iout <= timesList; iout++) {
      ref = 0;

      // loop over all lists of same name
      for (int iin = 0; iin < iSym.size(); iin++) {
        if (!iSym.get(iin).equals(iout)) // is not same name continue
          continue;

        // is previous value bigger than actually value -> wrong constellation
        ans = ((ref > iSeq.get(iin)));
        if (ans)
          break;

        // take new value to ref, to compare in next loop with next element of
        // list with same name
        if (iSym.get(iin) == iout) {
          ref = iSeq.get(iin);
        }
      }
      if (ans)
        break;
    }
  }

  // Gets
  public List<Collection<String>> getListOfLists() {
    return listOfLists;
  }

  public List<String> getStrSolutionList() {
    return StrSolutionList;
  }

  public List<Collection<String>> getSkipList() {
    return SkipList;
  }

  public boolean getAns() {
    return ans;
  }
}
