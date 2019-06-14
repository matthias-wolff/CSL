package de.tucottbus.kt.csl.speech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
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

/**
 * Experimental: a feature-value relation.
 * 
 * @author Matthias Wolff
 */
public class Fvr {
  protected Node root;
  private String comment;

  /**
   * Private constructor.
   * 
   * @see #fromString(String)
   */
  private Fvr() {
    root = new Node(null);
  }

  /**
   * Creates a feature-value relation from a string representation.
   * 
   * @param string
   *          The string representation. The string is to start with "FVR",
   *          sub-trees are to be enclosed in square brackets, e.&nbsp;g.
   *          <code>"FVR[root[child1[grandchild]][child2]]"</code>. Sub-strings
   *          enclosed in braces <code>"(...)"</code> are stripped end stored in
   *          the {@link #comment} field. If the string does not start with
   *          <code>"FVR["</code> or not end with <code>"]"</code> the method
   *          will create an empty tree and store the entire string in the
   *          {@link #comment} field.
   * @return The feature-value relation.
   * @throws IllegalArgumentException
   *           On parse errors. The message will contain details.
   */
  public static Fvr fromString(String string) throws IllegalArgumentException {
    Fvr fvr = new Fvr();
    if (string == null)
      string = "null";

    if (string.startsWith("FVR[") && string.endsWith("]"))
      fvr.parse(null, string.substring(3));
    else
      fvr.comment = string;
    return fvr;
  }

  /**
   * Extracts the comment from a (possibly incomplete) string representation.
   * 
   * @param string
   *          The string representation, see {@link #fromString(String)} for
   *          details
   * @return The comment if any, otherwise an empty string.
   */
  public static String extractComment(String string) {
    String comment = "";
    Pattern pattern = Pattern.compile("\\((.*?)\\)");
    Matcher matcher = pattern.matcher(string);
    while (matcher.find()) {
      if (comment.length() > 0)
        comment += " ";
      String grp = matcher.group();
      comment += grp.substring(1, grp.length() - 1);
    }
    return comment;
  }

  @Override
  public String toString() {
    return "FVR" + root.toString();
  }

  /**
   * Return the comment. See {@link #fromString(String)} for details how the
   * comment is generated.
   */
  public String getComment() {
    return comment;
  }

  /**
   * <p>
   * <b style="color:red">Experimental.</b>
   * </p>
   * 
   * <p>
   * Finds a node by its (qualified) label. Equivalent to
   * {@link #findNode(Node, String) findNode}<code>(</code>{@link #root}
   * <code>,label)</code>.
   * </p>
   * 
   * <p>
   * <b>Example:</b></br> The qualified node label "B.D" will be found in
   * "FVR[A[B[c][D]]]".
   * </p>
   * 
   * @param label
   *          The node label. If a qualified label is committed, fields are to
   *          be separated by '.'
   * @return A vector of matching nodes.
   */
  public Collection<Node> findNode(String label) {
    return findNode(root, label);
  }

  /**
   * <p>
   * <b style="color:red">Experimental.</b>
   * </p>
   * 
   * </p>Finds a node by its (qualified) label.</p>
   * 
   * <p>
   * <b>Example:</b></br> The qualified node label "B.D" will be found in
   * "FVR[A[B[c][D]]]".
   * </p>
   *
   *
   * @param node
   *          The root of the sub-tree to search in.
   * @param label
   *          The node label. If a qualified label is committed, fields are to
   *          be separated by '.'
   * @return A vector of matching nodes.
   */
  public Collection<Node> findNode(Node node, String label) {
    ArrayList<Node> result = new ArrayList<Node>();
    if (node == null)
      return result;
    if (label == null)
      return result;

    if (node.getQualifiedName().endsWith(label))
      result.add(node);
    for (Node child : node.children)
      result.addAll(findNode(child, label));

    return result;
  }

  public Node getFirstNode() {

    Collection<Node> nodeCollection = findNode("");
    Node firstNode = null;
    
    for (Node node : nodeCollection) {
      firstNode = node;
      break;
    }
    
    return firstNode;
  }

  public Node getLastNode() {

    Collection<Node> nodeCollection = findNode("");
    Node lastNode = null;

    for (Node node : nodeCollection) {
      lastNode = node;
    }

    return lastNode;
  }

  public Node getNodeAt(long index) {

    Collection<Node> nodeCollection = findNode("");
    Node nodeAtIndex = null;
    long cnt = nodeCollection.stream().findFirst().get().id;  // Start on first ID of actually FVR, ctr of node are static 
    
    for (Node node : nodeCollection) {
      nodeAtIndex = node;

      if (cnt == index)
        break;

      cnt++;
    }

    return nodeAtIndex;
  }

  /**
   * Renders this feature-value relation as SVG file. The rendering does not
   * include any {@linkplain #comment comments}.
   * <p>
   * <b style="color:red">NOTE:</b> The method invokes the <code>dot</code> tool
   * by AT&amp;T which has to be found in the <code>PATH</code>.
   * </p>
   * 
   * @param file
   *          The SVG output file.
   */
  public void renderSvgFile(File file) {
    try {
      File tmp = File.createTempFile("fvr", null);
      tmp.deleteOnExit();
      FileOutputStream fos = new FileOutputStream(tmp);
      render(fos);
      fos.close();
      String command = String.format("dot -Tsvg %s -o %s",
          tmp.getAbsolutePath(), file.getAbsolutePath());
      int exitVal = Runtime.getRuntime().exec(command).waitFor();
      if (exitVal != 0)
        throw new Exception(command + " failed (exit value " + exitVal + ")");
      tmp.delete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Renders this feature-value relation as SVG. The rendering does not include
   * any {@linkplain #comment comments}.
   * <p>
   * <b style="color:red">NOTE:</b> The method invokes the <code>dot</code> tool
   * by AT&amp;T which has to be found in the <code>PATH</code>.
   * </p>
   *
   * @return The svg code.
   */
  public String renderSvg() throws Exception {
    String command = "dot -Tsvg";
    Process process = Runtime.getRuntime().exec(command);
    render(process.getOutputStream());
    process.getOutputStream().close();
    String svg = extractSvg(process.getInputStream());
    return svg;
  }

  // -- Workers --

  private void parse(Node parent, String input) throws IllegalArgumentException {
    // System.out.println("NODE: \""+input+"\"");

    Node node = new Node(parent);
    if (parent == null) {
      root = node;
      input = stripComments(input);
    }

    // Check
    if (!input.startsWith("["))
      throw new IllegalArgumentException("Node \"" + input
          + "\" must start with '['");
    if (!input.endsWith("]"))
      throw new IllegalArgumentException("Node \"" + input
          + "\" must end with ']'");

    // Scan
    String name = "";
    int brace = 0;
    int beginIndex = -1;
    for (int i = 1; i < input.length() - 1; i++) {
      char c = input.charAt(i);
      if (c == '[') {
        if (brace == 0)
          beginIndex = i;
        brace++;
      } else if (c == ']') {
        brace--;
        if (brace < 0)
          throw new IllegalArgumentException("Too many ']' in node \"" + input
              + "\"");
        if (brace == 0)
          parse(node, input.substring(beginIndex, i + 1));
      } else if (brace == 0) {
        name += c;
      }
    }
    if (brace > 0)
      throw new IllegalArgumentException("Too many '[' in node \"" + input
          + "\"");

    // Parse "name:weight"
    Pattern pattern = Pattern.compile("(.*?):(.*?)");
    Matcher matcher = pattern.matcher(name);
    if (matcher.matches()) {
      node.label = matcher.group(1);
      try {
        node.weight = Float.parseFloat(matcher.group(2));
      } 
      catch (NumberFormatException e) 
      {
        System.err.print(String.format("\n[FVR: %s]", e.toString()));
        node.label = name;
        node.weight = Float.NaN;
      }
    } 
    else 
    {
      node.label = name;
      node.weight = Float.NaN;
    }
    
    // Check: is label a value or feature
    if (node.label.startsWith("$"))
    {
      node.setValue(true);
      node.label = node.label.substring(1);
    }
    else
      node.setValue(false);

    // Weird wire: Cascade equally labeled siblings
    ArrayList<Node> children = new ArrayList<Fvr.Node>(node.children);
    HashMap<String, Node> preds = new HashMap<String, Node>();
    for (Node child : children) {
      Node pred = preds.get(child.label);
      if (pred != null)
        child.setParent(pred);
      preds.put(child.label, child);
    }
  }

  private String stripComments(String string) {
    int brace = 0;
    String result = "";
    comment = "";
    for (char c : string.toCharArray())
      if (c == '(')
        brace++;
      else if (c == ')') {
        brace--;
        comment += " ";
      } else if (brace > 0)
        comment += c;
      else
        result += c;

    comment = comment.replaceAll(" -", "");
    return result;
  }

  private void render(OutputStream os) throws IOException {
    // String fontname = "Compacta LT Light";
    String fontname = "Swiss911 UCm BT";
    int fontsize = 24;
    os.write("digraph G {\n".getBytes());
    // os.write("  size=\"4,4\";\n".getBytes());
    os.write("  bgcolor=\"black\";\n".getBytes());
    os.write(("  node [color=\"#FF9900\", style=filled, fontname=\"" + fontname
        + "\", fontsize=" + fontsize + "];\n").getBytes());
    os.write(("  edge [fontcolor=\"#FF9900\", color=\"#FF9900\", fontname=\""
        + fontname + "\", style=bold, fontsize=" + fontsize + "];\n")
        .getBytes());
    renderNode(root, os);
    renderEdge(root, os);
    os.write("}\n".getBytes());
  }

  private void renderNode(Node node, OutputStream os) throws IOException {
    String s;

    if (node.parent == null)
      s = String.format(Locale.ENGLISH, "  %d [label=\"%s:%3.2f\"%s]\n",
          node.id, node.label, node.weight, node.isLeaf() ? ", shape=box" : "");
    else
      s = String.format(Locale.ENGLISH, "  %d [label=\"%s\"%s, height=\"0.1\"]\n", node.id,
          node.label, node.isValue() ? ", shape=box" : "");
    os.write(s.getBytes());
    for (Node child : node.children)
      renderNode(child, os);
  }

  private void renderEdge(Node node, OutputStream os) throws IOException {
    if (node.parent != null) {
      String s = String.format(Locale.ENGLISH,
          "  %d -> %d [label=\"%3.2f\", dir=none, len=1.1]\n", node.parent.id, node.id,
          node.weight);
      os.write(s.getBytes());
    }
    for (Node child : node.children)
      renderEdge(child, os);
  }

  /**
   * Union of input FVR with expectation FVR. Features of input FVR must be inside 
   * of expectation FVR. Function should start from expectation FVR. 
   * @param fvrInp 
   *          Input FVR 
   * @param opt 
   *          Option of handle by double value ("all" takes everything, "not" takes nothing,
   *          "old" takes previous value and "new" takes new value). Standard is "new"
   * @return Bool true if union successful
   */
  public boolean expUnion(Fvr fvrInp, String opt)
  {
    if (opt==null) opt = "new";
    return featureCompare(null, fvrInp, null, opt);
  }
  
  private boolean featureCompare(Node nodeExp, Fvr fvrInp, Node nodeInp, String opt)
  {
    boolean foundValFeat;
    if (nodeExp == null) 
      nodeExp = this.root;
    if (nodeInp == null) 
      nodeInp = fvrInp.root;

    for (Node elementInp : nodeInp.getChildren())   // Iterate over input FVR
    {
      foundValFeat = false;
      for (Node elementExp : nodeExp.getChildren()) // Control features and values
      {
        if (elementExp.label.equals(elementInp.label)) 
          {
            foundValFeat = featureCompare(elementExp, fvrInp, elementInp, opt);  // Start search on next node 
            if (foundValFeat==false)
              return false;
            break;  // Features are only once time under same parent node
          }
        else if (elementInp.isValue() && elementExp.isValue() && foundValFeat == false)
        {
          switch(opt)
          {
            case "all":  //Add missing node (Value) 
              parse(nodeExp, "[$" + elementInp.label + "]");  
              break;
            case "not":  // Delete previous value TODO: values!
              nodeExp.children.remove(elementExp);  
              break;
            case "new":  // Delete previous value and add new value TODO: values!
              nodeExp.children.remove(elementExp);  
              parse(nodeExp, "[$" + elementInp.label + "]");  
              break;
            case "old":  // Do nothing, because Expectation is the result of ExpUnion
              break;  
          }
          foundValFeat = true;
          break;
        }
      }
      if (foundValFeat == false && elementInp.isValue())
        parse(nodeExp, "[$" + elementInp.label + "]");  //Add missing node (Value) 
      else if (foundValFeat == false)  // Result that feature is not available in expectation
        return false;
    }
    return true;
  }
  
  public void printFvr(String dataName) {
    String htmlText = "";
    
    try {
      htmlText = this.renderSvg();
    } catch (Exception e) {
      // TODO * Auto-generated catch block
      e.printStackTrace();
    }
    
    try {
      Files.write(Paths.get(dataName + ".html"), htmlText.getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public int getRank() {
    // TO DO Rank wit Node.Rank()!
    return 4;
  }
  
  // -- Node class --

  public static class Node {
    private static  long ctr = 0;
    private         long id;
    public          String label;
    public          float weight;
    private         Node parent;
    public          List<Node> children;
    private         boolean    value; 

    Node(Node parent) {
      if (parent != null)
        parent.children.add(this);

      this.id = Node.ctr++;
      this.parent = parent;
      this.children = new ArrayList<Node>();
    }

    Node getParent() {
      return parent;
    }

    public void setParent(Node parent) {
      if (this.parent != null)
        this.parent.children.remove(this);
      this.parent = parent;
      if (!parent.children.contains(this))
        parent.children.add(this);
    }

    public List<Node> getChildren() {
      return children;
    }

    public boolean isLeaf() {
      return children.size() == 0;
    }

    long getId() {
      return id;
    }

    boolean isValue()
    {
      return value;
    }
    
    void setValue(final boolean value) 
    {
      this.value = value;
    }
    
    @Override
    public String toString() {
      String s = "[" + label;
      if (!Float.isNaN(weight))
        s += ":" + weight;
      for (Node node : children)
        s += node.toString();
      return s + "]";
    }

    public String getQualifiedName() {
      String s = label;
      if (parent != null)
        s = parent.getQualifiedName() + "." + s;
      return s;
    }

    public String getLabel() {

      return label;
    }

    public float getWeight() {

      return weight;
    }
  
    public int getRank() {
      if (this.getParent() == null)
        return 0;
      else
        return this.getParent().getRank() + 1;
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
  public static String extractSvg(InputStream is) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
    dbf.setValidating(false);

    // Parse input file
    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new EntityResolver() {
      // NOTE: This prevents from trying to download DTDs
      @Override
      public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException {
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
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    DOMSource source = new DOMSource(node);
    StringWriter swSvg = new StringWriter();
    StreamResult result = new StreamResult(swSvg);
    transformer.transform(source, result);

    return swSvg.toString();
  }

  // -- Main method: Debugging only! --

  public static void main(String[] args) {
// TODO: Test function union for fvrPool compare
    String s = "";
    if (args.length > 0) {
      s = args[0];
      // HACK: Always assume a FVR!
      if (!s.startsWith("FVR["))
        s = "FVR[" + s + "]";
    } else {
      // s =
      // "FVR[(bitte)(verschiebe)[MAID[(das)(obere)ma2(Mikrofonfeld)]]MOVETOREL(noch)[relpos[(ein)(kleines)-(Stueck)bit]][dir[towards]](zum)[abspos[(Bildschirm)max]]]";
      // s =
      // "FVR[SWITCH[switch[(aktiviere)on]](Mikrofon/e)[MCID[CN2[o[(eins)1]]]][MCID[(bis)-]][MCID[CN2[o[0]][t[(zehn)1]]]](sowie)[MCID[CN2[t[(zwan)2(-zig)]]]][MCID[CN2[o[(acht)8]](und)[t[(zwan)2(-zig)]]]](und)[MCID[CN2[o[(vier)4]](und)[t[(vier)4(-zig)]]]]]";
      // s =
      // "FVR[PROG:0.3[STEP:0.3[FUNC:1[OFF:1]][TIME:0.2[SA-09.00h:0.2]]][STEP:0.2[FUNC:1[ON:1]][TIME:-0.2[SU-20.00h:-0.2]]]]";

      // s =
      // "FVR[PROG:1.0[STEP:1.0[HEATREL:1.0[TEMPDIM:1.0[int:1.0[DIG:1.0[o:1.0[2:1.0]]][unit:1.0[Grad:1.0]]]][SCALE:1.0[UP:1.0]]][DATETIME:1.0[absTime:1.0[DATE:1.0[26.05.16:1.0]][TIME:1.0[18.00h:1.0]]]]]]";

      // s =
      // "FVR[NEWTEMP:1.0[TEMPDIM:1.0[int[sig:1.0[+:1.00]][CN2:1.00[t:1.00[]][o:1.00[]]]]]]";

      s = "FVR[PROG:0.3[STEP:0.3[FUNC:1[OFF:1]][TIME:0.2[SA-09.00h:0.2]]][STEP:0.2[FUNC:1[ON:1]][TIME:-0.2[SU-20.00h:-0.2]]]]";

      // s = "FVR[LDIM[val[DIG[o[1]]]][val[komma]][unit[m]][val[CN2[t[2]]]]]";
    }

    Fvr fvr = Fvr.fromString(s);
    System.out.println(fvr.getRank());
    Node nNode;
    nNode = fvr.findNode("PROG.STEP.FUNC.OFF").stream().findFirst().get();
    System.out.println(nNode.label);
    System.out.println(nNode.getRank());
    
    fvr.printFvr("TestFvr");
    
    
/*    System.out.println("input  : " + s);
    System.out.println("fvr    : " + fvr.toString()); //
    System.out.println("comment: " + fvr.getComment());

    try {
      fvr.render(System.out);
    } catch (IOException e) {} */
    /*try {
      System.out.println(fvr.renderSvg());
    } catch (Exception e) {
      // TODO * Auto-generated catch block
      e.printStackTrace();
    }*/

  }
}
