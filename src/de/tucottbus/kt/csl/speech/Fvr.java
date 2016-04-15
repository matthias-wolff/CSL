package de.tucottbus.kt.csl.speech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.*;

/**
 * Experimental: a feature-value relation.
 * 
 * @author Matthias Wolff
 */
public class Fvr
{
  private Node   root;
  private String comment;
  
  /**
   * Private constructor.
   * 
   * @see #fromString(String)
   */
  private Fvr()
  {
    root = new Node(null);
  }
  
  /**
   * Creates a feature-value relation from a string representation. 
   * 
   * @param string
   *          The string representation. The string is to start with "FVR",
   *          sub-trees are to be enclosed in square brackets, e.&nbsp;g.
   *          <code>"FVR[root[child1[grandchild]][child2]]"</code>. Sub-strings
   *          enclosed in braces <code>"(...)"</code> are stripped
   *          end stored in the {@link #comment} field. If the string does not
   *          start  with <code>"FVR["</code> or not end with <code>"]"</code>
   *          the method will create an empty tree and store the entire string
   *          in the {@link #comment} field.
   * @return The feature-value relation.
   * @throws IllegalArgumentException
   *           On parse errors. The message will contain details.
   */
  public static Fvr fromString(String string)
  throws IllegalArgumentException
  {
    Fvr fvr = new Fvr();
    if (string==null) string = "";

    if (string.startsWith("FVR[") && string.endsWith("]"))
      fvr.parse(null,string.substring(3));
    else
      fvr.comment = string;
    return fvr;
  }
  
  // ArticulateNoteList class
  // Subauthor: Werner Meye
  static class ArticulateNodeList{
    private List<Collection<String>> listOfLists  = new LinkedList<Collection<String>>();   // list of Lists from children with strings ([part]Solutions)
    private List<Collection<String>> SkipList     = new LinkedList<Collection<String>>();   // List of children to pass notes with same name
    private List<String> StrSolutionList          = new ArrayList<String>();                // List of all possible Strings with children of the note
    private List<Integer> iSym                    = new ArrayList<Integer>();               // Reference integer list, to numbering the times of different lists           e.g.: lists: ABBCB => 12232
    private List<Integer> iSeq                    = new ArrayList<Integer>();               // Reference integer list, to numbering the times of different sequence inside a list e.g.: ABBCB => 11213
    private int timesList = 0;                                                              // count times of lists
    private boolean ans;                                                                    // Is String after permutation correct?
    
    // Constructor
    public ArticulateNodeList (){
    }
    
    // Sets/Adds
    public void addToListOfLists(ArticulateNodeList prevList){
      this.listOfLists.add(prevList.getStrSolutionList());            // Add list of children to listOfLists
      timesList += 1; iSym.add(timesList); iSeq.add(1);               // Numbering the reference integer lists 
      if (!prevList.getSkipList().isEmpty()){                         // If children SkipList is not empty, put all in listOfLists too
        this.listOfLists.addAll(prevList.getSkipList());
        for(int i = 1; i<=prevList.getSkipList().size(); i++){
          iSym.add(timesList);                                        // Numbering the reference integer lists
          iSeq.add(i+1); 
        }
      }
    }
    public void addToSkipList(ArticulateNodeList prevList){           // By same name of parent and children add all lists of children to SkipList
      this.SkipList.add(prevList.getStrSolutionList());
      this.SkipList.addAll(prevList.getSkipList());
    } 
    public void swapListOfLists(int a){                               // Reverse all Element from Element 1 to Element a
      List<Collection<String>> AccList = new LinkedList<Collection<String>>();
      List<Integer> iAccSym = new ArrayList<Integer>();
      List<Integer> iAccSeq = new ArrayList<Integer>();
     
      // Take Sublist of main-list and reference integer lists
      AccList = listOfLists.subList(0, a+1);
      iAccSym = iSym.subList(0, a+1);
      iAccSeq = iSeq.subList(0, a+1);
      
      // Reverse the sublists
      Collections.reverse(AccList);
      Collections.reverse(iAccSym);
      Collections.reverse(iAccSeq);
      
      // Put sublist back to original list
      for(int i=0; i<AccList.size(); i++){
        listOfLists.set(i,AccList.get(i));
        iSym.set(i, iAccSym.get(i));
        iSeq.set(i, iAccSeq.get(i));
      }
    }
    public void addStrToStrSolutionList(String Str){
      StrSolutionList.add(Str);
    }

    public void compare(){                                // Check actually constellation
      int ref;
      ans = false;                                        // Everytime on start set ans false, is it true the new constellation is not access to write a string to solution list
      for (int iout = 1; iout <= timesList; iout++){      // loop over times of different lists 
        ref=0;
        for (int iin = 0; iin < iSym.size(); iin++){      // loop over all lists of same name
          if (!iSym.get(iin).equals(iout))                // is not same name continue
            continue;
          ans = ((ref>iSeq.get(iin)));                    // is previous value bigger than actually value -> wrong constellation
          if (ans) 
            break;
          if (iSym.get(iin)==iout){                       // take new value to ref, to compare in next loop with next element of list with same name
            ref = iSeq.get(iin);       
          }
        }
        if (ans)
          break;
      }
    }
    
    // Gets
    public List<Collection<String>> getListOfLists(){
      return listOfLists;
    }
    public List<String> getStrSolutionList(){
      return StrSolutionList;
    }
    public List<Collection<String>> getSkipList(){
      return SkipList;
    } 
    public boolean getAns(){
      return ans;
    }
  }
  
  /**
   * Subauthor: Werner Meyer
   * Creates a list of possible strings by FVR. 
   * 
   * @param node 
   *          The actually node with his characteristics
   * @return Class ArticulateNodeList.
   *          content list with all possible strings. In ArticulateNodeList.getStrSolutionList();
   */
  public static ArticulateNodeList getArticulate(Node node)
  { 
    ArticulateNodeList newList = new ArticulateNodeList();

    if (node.children.isEmpty()){                                                     // is node a leaf get his notation
      newList.addStrToStrSolutionList("[" + node.label + ":" + node.weight + "]");
      return newList;
    }
    
    for (Node child : node.children){                                                 // recursive 
      if (node.label.equals(child.label))                                             // differentiate between children wit same name to parent
        newList.addToSkipList(getArticulate(child));                                  // fill SkipList
      else
        newList.addToListOfLists(getArticulate(child));                               // Children with different name to parent, fill ListOfLists
    }
    
    // Permutation over all lists in ListofList
    // Preparation for Permutation
    int iper[] = new int[newList.getListOfLists().size()];                            // intArray with length of times of lists
    int fak=1;                                                                        // faculty
    for(int k = 0; k < newList.getListOfLists().size(); k++){ 
      iper[k]=k+1;                                                                    // initial. int Array {1,2,3....}
      fak = fak * iper[k];                                                            // and faculty
    }
    
    String Str;
    for ( ; fak>0; fak--){                                                      // Iterate over max. times of solutions strings
      for(List<String> x:finiteCartesianProduct(newList.getListOfLists())){     // all possible Order of elements of all lists together
        Str = "";
        for (String s : x)
          Str += s;
        Str = "[" + node.label + ":" + node.weight + Str + "]";
        newList.compare();                                                      // Compare is new constellation correct 
        if (!newList.getAns())                                                  // then take new Str to SolutionList
          newList.addStrToStrSolutionList(Str);
      }
    
      // calculate next order of elements (lists) in listOfLists 
      int a=0;                                                                  // point of reverse
      for(int k=0; k<iper.length; k++){                                         // iteration over lists
        if (iper[k]>1){
          a = k;
          iper[k]--;
          while (k>0)
            iper[k-1]=k--;
          break;
        }
      }
      if (newList.getListOfLists().size()>1)                                    // Swap only if more then one list is existing
        newList.swapListOfLists(a);                                             // reverse part of lists between 0 and a 
    }
    
    return newList;
  }
  
  
  // Source: http://www.java-forum.org/mathematik/89313-kombinatorik-permutation-listen-deren-elementen.html?highlight=Kombinatorik%2FPermutation+Listen+Elementen
    //public class _ {
     
    	/*
    	 * example: buckets=[[A1,A2,..Aa],[B1,B2,..Bb],...,[X1,X2,..Xx]]
    	 * the method will return an iterable that allows to iterate over all elements from Cartesian product
    	 * [A1,A2,..Aa]x[B1,B2,..Bb]x[X1,X2,..Xx]
    	 * that means it returns an iterator with all combinations:
    	 * 
    	 * [A1,B1,...X1]
    	 * [A2,B1,...,X1]
    	 * [A3,B1,...,X1]
    	 * ...
    	 * [A1,B2,...,X1]
    	 * [A2,B2,...,X1]
    	 * ...
    	 * [Aa,Bb,...,Xx]
    	 * 
    	 * @param sets:			ordered List of collections of <T> structures
    	 * @return:				Iterable of List<T> with all elements of cartesian product
    	 */

    	public static <T> Iterable<List<T>> finiteCartesianProduct(final List<Collection<T>> sets){
    		return new Iterable<List<T>>(){
    			private long size=1;
    			{
    				for(Collection<T> set:sets)size*=(long)set.size();
    			}
    			@Override
    			public Iterator<List<T>> iterator() {
    				return new Iterator<List<T>>(){
    					long counter=0;
    					ArrayList<T> currentValues=new ArrayList<T>(sets.size());
    					ArrayList<Iterator<T>> iterators=new ArrayList<Iterator<T>>(sets.size());
    					{
    						for(Iterable<T> set:sets){
    							Iterator<T> it=set.iterator();
    							iterators.add(it);
    							if(it.hasNext()){
    								//if not, then the size is 0, hasNext is never true, set empty
    								currentValues.add(it.next());
    							}
    						}
    					}
     
    					@Override
    					public boolean hasNext() {
    						return counter<size;
    					}
     
    					@Override
    					public List<T> next() {
    						List<T> result=new LinkedList<T>(currentValues);
    						counter++;
    						increment(0);
    						return result;
    					}
     
    					private void increment(int i){
    						if(iterators.get(i).hasNext()){
    							currentValues.set(i,iterators.get(i).next());
    						}else{
    							iterators.set(i,sets.get(i).iterator());
    							currentValues.set(i,iterators.get(i).next());
    							if(i<iterators.size()-1){
    								increment(i+1);
    							}
    						}
    					}
     
    					@Override
    					public void remove() {
    						throw new UnsupportedOperationException("impossible to change combination set");
    					}
    				};
    			}
    		};
    	}  
  
  /**
   * Extracts the comment from a (possibly incomplete) string representation.
   * 
   * @param string
   *          The string representation, see {@link #fromString(String)} for
   *          details
   * @return The comment if any, otherwise an empty string.
   */
  public static String extractComment(String string)
  {
    String comment = "";
    Pattern pattern = Pattern.compile("\\((.*?)\\)");
    Matcher matcher = pattern.matcher(string);
    while (matcher.find())
    {
      if (comment.length()>0) comment+=" ";
      String grp = matcher.group();
      comment += grp.substring(1,grp.length()-1);
    }
    return comment;
  }
  
  @Override
  public String toString()
  {
    return "FVR"+root.toString();
  }

  /**
   * Return the comment. See {@link #fromString(String)} for details how the
   * comment is generated.
   */
  public String getComment()
  {
    return comment;
  }
  
  /**
   * <p><b style="color:red">Experimental.</b></p>
   * 
   * <p>Finds a node by its (qualified) label. Equivalent to {@link #findNode(Node, 
   * String) findNode}<code>(</code>{@link #root}<code>,label)</code>.</p>
   * 
   * <p><b>Example:</b></br>
   * The qualified node label "B.D" will be found in "FVR[A[B[c][D]]]".</p>
   * 
   * @param label
   *          The node label. If a qualified label is committed, fields are to
   *          be separated by '.'
   * @return A vector of matching nodes.
   */
  public Collection<Node> findNode(String label)
  {
    return findNode(root,label);
  }
  
  /**
   * <p><b style="color:red">Experimental.</b></p>
   * 
   * </p>Finds a node by its (qualified) label.</p>
   * 
   * <p><b>Example:</b></br>
   * The qualified node label "B.D" will be found in "FVR[A[B[c][D]]]".</p>
   *
   *
   * @param node
   *          The root of the sub-tree to search in.
   * @param label
   *          The node label. If a qualified label is committed, fields are to
   *          be separated by '.'
   * @return A vector of matching nodes.
   */
  public Collection<Node> findNode(Node node, String label)
  {
    ArrayList<Node> result = new ArrayList<Node>();
    if (node==null) return result;
    if (label==null) return result;

    if (node.getQualifiedName().endsWith(label))
      result.add(node);
    for (Node child : node.children)
      result.addAll(findNode(child,label));

    return result;
  }
  
  /**
   * Renders this feature-value relation as SVG file. The rendering does not include
   * any {@linkplain #comment comments}.
   * <p><b style="color:red">NOTE:</b> The method invokes the <code>dot</code>
   * tool by AT&amp;T which has to be found in the <code>PATH</code>.</p>
   *  
   * @param file
   *          The SVG output file.
   */
  public void renderSvgFile(File file)
  {
    try
    {
      File tmp = File.createTempFile("fvr",null);
      tmp.deleteOnExit();
      FileOutputStream fos = new FileOutputStream(tmp);
      render(fos);
      fos.close();
      String command = String.format("dot -Tsvg %s -o %s",
        tmp.getAbsolutePath(), file.getAbsolutePath());
      int exitVal = Runtime.getRuntime().exec(command).waitFor();
      if (exitVal!=0)
        throw new Exception(command+" failed (exit value "+exitVal+")");
      tmp.delete();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  /**
   * Renders this feature-value relation as SVG. The rendering does not include
   * any {@linkplain #comment comments}.
   * <p><b style="color:red">NOTE:</b> The method invokes the <code>dot</code>
   * tool by AT&amp;T which has to be found in the <code>PATH</code>.</p>
   *
   * @return The svg code.
   */
  public String renderSvg() throws Exception
  {
    String command = "dot -Tsvg";
    Process process = Runtime.getRuntime().exec(command);
    render(process.getOutputStream());
    process.getOutputStream().close();
    String svg = extractSvg(process.getInputStream());
    return svg;
  }
  
  // -- Workers --
  
  private void parse(Node parent, String input)
  throws IllegalArgumentException
  {
    //System.out.println("NODE: \""+input+"\"");
    
    Node node = new Node(parent);
    if (parent==null)
    {
      root = node;
      input = stripComments(input);
    }

    // Check
    if (!input.startsWith("["))
      throw new IllegalArgumentException("Node \""+input+"\" must start with '['");
    if (!input.endsWith("]"))
      throw new IllegalArgumentException("Node \""+input+"\" must end with ']'");
    
    // Scan
    String name    = "";
    int brace      = 0;
    int beginIndex = -1;
    for (int i=1; i<input.length()-1; i++)
    {
      char c = input.charAt(i);
      if (c=='[')
      {
        if (brace==0)
          beginIndex = i;
        brace++;
      }
      else if (c==']')
      {
        brace--;
        if (brace<0)
          throw new IllegalArgumentException("Too many ']' in node \""+input+"\"");
        if (brace==0)
          parse(node,input.substring(beginIndex,i+1));
      }
      else if (brace==0)
      {
        name += c;
      }
    }
    if (brace>0)
      throw new IllegalArgumentException("Too many '[' in node \""+input+"\"");

    // Parse "name:weight"
    Pattern pattern = Pattern.compile("(.*?):(.*?)");
    Matcher matcher = pattern.matcher(name);
    if (matcher.matches())
    {
      node.label = matcher.group(1);
      try
      {
        node.weight = Float.parseFloat(matcher.group(2));
      }
      catch (NumberFormatException e)
      {
        System.err.print(String.format("\n[FVR: %s]",e.toString()));
        node.label = name;
        node.weight = Float.NaN;
      }
    }
    else
    {
      node.label = name;
      node.weight = Float.NaN;
    }
    
    // Weird wire: Cascade equally labeled siblings
    ArrayList<Node> children = new ArrayList<Fvr.Node>(node.children);
    HashMap<String,Node> preds = new HashMap<String,Node>();
    for (Node child : children)
    {
      Node pred = preds.get(child.label);
      if (pred!=null)
        child.setParent(pred);
      preds.put(child.label,child);
    }
  }

  private String stripComments(String string)
  {
    int brace = 0;
    String result  = "";
    comment = "";
    for (char c : string.toCharArray())
      if (c=='(')
        brace++;
      else if (c==')')
      {
        brace--;
        comment += " ";
      }
      else if (brace>0)
        comment += c;
      else
        result += c; 

    comment = comment.replaceAll(" -","");
    return result;
  }
  
  private void render(OutputStream os)
  throws IOException
  {
    //String fontname = "Compacta LT Light";
    String fontname = "Swiss911 UCm BT";
    int    fontsize = 24;
    os.write("digraph G {\n".getBytes());
    //os.write("  size=\"4,4\";\n".getBytes());
    os.write("  bgcolor=\"black\";\n".getBytes());
    os.write(("  node [color=\"#FF9900\", style=filled, fontname=\""+fontname+"\", fontsize="+fontsize+"];\n").getBytes());
    os.write(("  edge [fontcolor=\"#FF9900\", color=\"#FF9900\", fontname=\""+fontname+"\", style=bold, fontsize="+fontsize+"];\n").getBytes());
    renderNode(root,os);
    renderEdge(root,os);
    os.write("}\n".getBytes());
  }

  private void renderNode(Node node, OutputStream os)
  throws IOException
  {
    String s;
    
    if (node.parent==null)
      s = String.format(Locale.ENGLISH,"  %d [label=\"%s:%3.2f\"%s]\n",
        node.id, node.label, node.weight, node.isLeaf() ? ", shape=box" : "");
    else
      s = String.format(Locale.ENGLISH,"  %d [label=\"%s\"%s]\n",
          node.id, node.label, node.isLeaf() ? ", shape=box" : "");      
    os.write(s.getBytes());
    for (Node child : node.children)
      renderNode(child,os);
  }

  private void renderEdge(Node node, OutputStream os)
  throws IOException
  {
    if (node.parent!=null)
    {
      String s = String.format(Locale.ENGLISH,
       "  %d -> %d [label=\"%3.2f\", dir=none]\n",node.parent.id,node.id,
       node.weight);
      os.write(s.getBytes());
    }
    for (Node child : node.children)
      renderEdge(child,os);
  }
  
  // -- Node class --
  
  public static class Node
  {
    private static long       ctr = 0;
    private        long       id;
    public         String     label;
    public         float      weight;
    private        Node       parent;
    private        List<Node> children;
    
    Node(Node parent)
    {
      if (parent!=null)
        parent.children.add(this);

      this.id       = Node.ctr++;
      this.parent   = parent;
      this.children = new ArrayList<Node>();
    }
    
    Node getParent()
    {
      return parent;
    }
    
    void setParent(Node parent)
    {
      if (this.parent!=null)
        this.parent.children.remove(this);
      this.parent = parent;
      if (!parent.children.contains(this))
        parent.children.add(this);
    }
    
    List<Node> getChildren()
    {
      return children;
    }
    
    boolean isLeaf()
    {
      return children.size()==0;
    }
    
    long getId()
    {
      return id;
    }

    @Override
    public String toString()
    {
      String s = "[" + label;
      if (!Float.isNaN(weight)) s += ":" + weight;
      for (Node node : children)
        s += node.toString();
      return s + "]";
    }

    public String getQualifiedName()
    {
      String s = label;
      if (parent!=null)
        s = parent.getQualifiedName()+"."+s;
      return s;
    }
  }
  
  // -- SVG extraction --
  // TODO: Move elsewhere!?
 
  /**
   * Extracts the SVG node containing an FVR's rendering from a SVG file. The
   * result may be embedded into HTML.
   * 
   * @param is
   *          An SVG stream.
   * @return The extracted SVG code.
   * @throws Exception
   *           on problems.
   */
  public static String extractSvg(InputStream is) throws Exception
  {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);

    // Parse input file
    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new EntityResolver() 
    {
      // NOTE: This prevents from trying to download DTDs
      @Override
      public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException 
      {
        return new InputSource(new StringReader(""));
      }
    });
    Document doc = db.parse(is);
      
    // Select first svg node
    org.w3c.dom.NodeList list = doc.getElementsByTagName("svg");
    org.w3c.dom.Node node = list.item(0);

    // Get svg soure code
    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer transformer = tFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT,"yes");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
    DOMSource source = new DOMSource(node);
    StringWriter swSvg = new StringWriter();
    StreamResult result = new StreamResult(swSvg);
    transformer.transform(source, result);
      
    return swSvg.toString();
  }
  
  // -- Main method: Debugging only! --
  
  public static void main(String[] args)
  {
    ArticulateNodeList newList = new ArticulateNodeList();
    
    String s = "";
    if (args.length>0)
    {
      s = args[0];
      // HACK: Always assume a FVR!
      if (!s.startsWith("FVR["))
        s = "FVR["+s+"]";
    }
    else
    {
      //s = "FVR[(bitte)(verschiebe)[MAID[(das)(obere)ma2(Mikrofonfeld)]]MOVETOREL(noch)[relpos[(ein)(kleines)-(Stueck)bit]][dir[towards]](zum)[abspos[(Bildschirm)max]]]";
      //s = "FVR[SWITCH[switch[(aktiviere)on]](Mikrofon/e)[MCID[CN2[o[(eins)1]]]][MCID[(bis)-]][MCID[CN2[o[0]][t[(zehn)1]]]](sowie)[MCID[CN2[t[(zwan)2(-zig)]]]][MCID[CN2[o[(acht)8]](und)[t[(zwan)2(-zig)]]]](und)[MCID[CN2[o[(vier)4]](und)[t[(vier)4(-zig)]]]]]";
      s = "FVR[PROG:0.3[STEP:0.3[FUNC:1[OFF:1]][TIME:0.2[SA-09.00h:0.2]]][STEP:0.2[FUNC:1[ON:1]][TIME:-0.2[SU-20.00h:-0.2]]]]";
    }
    
    Fvr fvr = Fvr.fromString(s);
    
    //NEW
    newList = getArticulate(fvr.root);
    for (int k = 0; k < newList.getStrSolutionList().size(); k++){
      System.out.println(k + ": " + newList.getStrSolutionList().get(k));
    }
    
    System.out.println("input  : "+s);
    System.out.println("fvr    : "+fvr.toString());
//    System.out.println("comment: "+fvr.getComment());
//    try { fvr.render(System.out); } catch (IOException e) {}
    try {
      System.out.println(fvr.renderSvg());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
