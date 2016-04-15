package de.tucottbus.kt.csl.hardware.kinect.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.media.opengl.GL2;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Vector3f;

import de.tucottbus.kt.csl.hardware.kinect.body.Body;
import de.tucottbus.kt.csl.hardware.kinect.devices.Kinect;
import de.tucottbus.kt.csl.hardware.kinect.room.ListOfObject3D;
import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;
import edu.ufl.digitalworlds.opengl.OpenGLPanel;

/**
 * 
 * This class visualized a virtual room with virtual objects. 
 * This virtual objects can be edit in the position, size, orientation and coloring. 
 * @author Thomas Jung
 *
 */
public class RoomEditor extends OpenGLPanel
{
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private static RoomEditor instance;
  private static boolean created;
  
  public static RoomEditor getInstance()
  {
    if(created) return instance;
    instance = new RoomEditor();
    created = true;
    return instance;
  }
  
  private JFrame frame;

  private Object3D room = new Object3D();
  
  private int object3dType = -1;
  
  private boolean drawBodies = true;
  
  private JPanel settings = new JPanel();
  private JLabel labelTypeSelection = new JLabel("Typ des Objektes");
  private JComboBox<String> typeSelection = new JComboBox<String>();
  
  private boolean loadingValues;
  
  private JSlider angX = new JSlider();
  private JSlider angY = new JSlider();
  private JSlider angZ = new JSlider();

  private JSlider posX = new JSlider();
  private JSlider posY = new JSlider();
  private JSlider posZ = new JSlider();
  
  private JSlider width = new JSlider();
  private JSlider height = new JSlider();
  private JSlider length = new JSlider();
  
  private JSlider colR = new JSlider();
  private JSlider colG = new JSlider();
  private JSlider colB = new JSlider();
  
  private JLabel labelSize = new JLabel("Größe des Objektes");
  private JLabel labelWidth = new JLabel("Breite: 0 cm");
  private JLabel labelHeight = new JLabel("Höhe: 0 cm");
  private JLabel labelLength = new JLabel("Länge: 0 cm");

  private JLabel labelAngle = new JLabel("Orientierung des Objektes");
  private JLabel labelAngX = new JLabel("x-Drehung um 0°");
  private JLabel labelAngY = new JLabel("y-Drehung um 0°");
  private JLabel labelAngZ = new JLabel("z-Drehung um 0°");

  private JLabel labelPos = new JLabel("Position des Objektes");
  private JLabel labelPosX = new JLabel("x-Position: 0 cm");
  private JLabel labelPosY = new JLabel("y-Position: 0 cm");
  private JLabel labelPosZ = new JLabel("z-Position: 0 cm");
  
  private JLabel labelCol = new JLabel("Farbe des Objektes");
  private JLabel labelColR = new JLabel("Rotanteil: 0 %");
  private JLabel labelColG = new JLabel("Grünanteil: 0 %");
  private JLabel labelColB = new JLabel("Blauanteil: 0 %");
  
  private JLabel labelSwitch = new JLabel("Ein und ausblenden");
  private JButton switchCoordinateSystem = new JButton("Koordinatensystem");
  private JButton switchRotationPlanes = new JButton("Drehebenen");
  private JButton switchView = new JButton("Blickrichtung");
  private JButton switchBodies = new JButton("Skelett");
  
  private int prevMouseX;
  private int prevMouseY;
  private float view_rotx = -90;
  private float view_rotz;
  private float zoom = -10;
  private float up;
  private float side;
  
  private ArrayList<Kinect> kinects = new ArrayList<Kinect>();

  private RoomEditor()
  {
    room.setSize(new Vector3f(4.4f, 4.4f, 2.5f));
    room.setColor(new Vector3f(1, 1, 1));
    room.setViewEnable(false);
    room.update();
    initFrame();
  }
  
  private void initFrame() {
    frame = new JFrame();
    frame.setSize(1200, 700);
    frame.setLayout(new GridLayout(1, 2));
    frame.add(this);
    frame.add(settings);
    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    settings.setLayout(new GridLayout(20, 2));
    
    settings.add(labelTypeSelection);
    settings.add(typeSelection);
  
    settings.add(labelAngle);
    settings.add(new JLabel());
    settings.add(angX);
    settings.add(labelAngX);
    settings.add(angY);
    settings.add(labelAngY);
    settings.add(angZ);
    settings.add(labelAngZ);
    
    settings.add(labelPos);
    settings.add(new JLabel());
    settings.add(posX);
    settings.add(labelPosX);
    settings.add(posY);
    settings.add(labelPosY);
    settings.add(posZ);
    settings.add(labelPosZ);
    
    settings.add(labelSize);
    settings.add(new JLabel());
    settings.add(width);
    settings.add(labelWidth);
    settings.add(height);
    settings.add(labelHeight);
    settings.add(length);
    settings.add(labelLength);
    
    settings.add(labelCol);
    settings.add(new JLabel());
    settings.add(colR);
    settings.add(labelColR);
    settings.add(colG);
    settings.add(labelColG);
    settings.add(colB);
    settings.add(labelColB);
    
    settings.add(labelSwitch);
    settings.add(new JLabel());
    settings.add(switchCoordinateSystem);
    settings.add(switchRotationPlanes);
    settings.add(switchView);
    settings.add(switchBodies);
       
    angX.setMaximum(180);
    angX.setMinimum(-180);
    angX.setValue(0);
    
    angY.setMaximum(180);
    angY.setMinimum(-180);
    angY.setValue(0);
    
    angZ.setMaximum(180);
    angZ.setMinimum(-180);
    angZ.setValue(0);
    
    posX.setMaximum(250);
    posX.setMinimum(-250);
    posX.setValue(0);
    
    posY.setMaximum(250);
    posY.setMinimum(-250);
    posY.setValue(0);
    
    posZ.setMaximum(250);
    posZ.setMinimum(-250);
    posZ.setValue(0);
    
    width.setMaximum(500);
    width.setMinimum(-500);
    width.setValue(0);
    
    height.setMaximum(500);
    height.setMinimum(-500);
    height.setValue(0);
    
    length.setMaximum(220);
    length.setMinimum(-220);
    length.setValue(0);
    
    colR.setMaximum(100);
    colR.setMinimum(0);
    colR.setValue(0);
    
    colG.setMaximum(100);
    colG.setMinimum(0);
    colG.setValue(0);
    
    colB.setMaximum(100);
    colB.setMinimum(0);
    colB.setValue(0);
    
    angX.addChangeListener(new AngAct());
    angY.addChangeListener(new AngAct());
    angZ.addChangeListener(new AngAct());
    
    posX.addChangeListener(new PosAct());
    posY.addChangeListener(new PosAct());
    posZ.addChangeListener(new PosAct());
    
    width.addChangeListener(new SizeAct());
    height.addChangeListener(new SizeAct());
    length.addChangeListener(new SizeAct());
    
    colR.addChangeListener(new ColAct());
    colG.addChangeListener(new ColAct());
    colB.addChangeListener(new ColAct());


    for(Object3D objects : ListOfObject3D.getList())
    {
      typeSelection.addItem(objects + "");
    }
    
    typeSelection.addActionListener(new TypeSelection());
    
    typeSelection.setSelectedItem(ListOfObject3D.getList()[Object3D.SENSOR1_000]);
    object3dType = Object3D.SENSOR1_000;
    loadObject();
    
    switchCoordinateSystem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(object3dType == -1) return;
        ListOfObject3D.getList()[object3dType].setCoordinateSystemEnable(
            !ListOfObject3D.getList()[object3dType].getCoordinateSystemEnable());
      }
    });
    
    switchView.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(object3dType == -1) return;
        ListOfObject3D.getList()[object3dType].setViewEnable(
            !ListOfObject3D.getList()[object3dType].getViewEnable());
      }
    });
    
    switchRotationPlanes.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(object3dType == -1) return;
        ListOfObject3D.getList()[object3dType].setRotationPlanesEnable(
            !ListOfObject3D.getList()[object3dType].getRotationPlanesEnable());
      }
    });
    
    switchBodies.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        drawBodies = !drawBodies;
      }
    });
  }
  

  
  /**
   * displays the room editor
   */
  public void open()
  {
    frame.setVisible(true);
  }
  
  @Override
  /**
   * draws the room with all objects
   */
  public void draw() {
    GL2 gl = getGL2();
    pushMatrix();
    translate(side, up, zoom);
    rotateX(view_rotx);
    rotateZ(view_rotz);
    room.draw(gl);
    for(Object3D current : ListOfObject3D.getList())
    {
      current.draw(gl);
    } 
    if(drawBodies)
    {
      for(Kinect kinect : kinects)
      {
        for(Body body : kinect.getBodies())
        {
          if(body.isTracked()) body.draw(gl);
        }
      }
    }
    popMatrix();
  }
  
  public synchronized void addKinect(Kinect kinect)
  {
    kinects.add(kinect);
  }
  
  public synchronized void removeKinect(Kinect kinect)
  {
    kinects.remove(kinect);
  }
  
  /**
   * rotates the room representing
   */
  public void mouseDragged(int x, int y, MouseEvent e) 
  {
    Dimension size = e.getComponent().getSize();
    if(isMouseButtonPressed(1))
    {
      float thetaZ = 360.0f * ( (float)(x-prevMouseX)/(float)size.width);
      float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)size.height);
      view_rotx -= thetaX;
      view_rotz += thetaZ;    
    }
    
    if(isMouseButtonPressed(3))
    {
      float side = 10.0f * ( (float)(x-prevMouseX)/(float)size.width);
      float up = 10.0f * ( (float)(prevMouseY-y)/(float)size.height);
      this.side += side;
      this.up += up;
    }
    if(isMouseButtonPressed(2))
    {
      float zoom = 100f * ( (float)(prevMouseY-y)/(float)size.height);
      this.zoom -= zoom;
    }
    
    prevMouseX = x;
    prevMouseY = y;

   }

  public void mousePressed(int x, int y, MouseEvent e) 
  {
    prevMouseX = x;
    prevMouseY = y;
  }
  
  private class TypeSelection implements ActionListener
  {
    @Override
    /**
     * changes the type of the object
     */
    public void actionPerformed(ActionEvent e) {
      object3dType = typeSelection.getSelectedIndex();
      loadObject();
    }
  }

  private class AngAct implements ChangeListener
  {
    @Override
    /**
     * rotates the object
     */
    public void stateChanged(ChangeEvent e)
    {
      if(object3dType == -1) return;
      labelAngX.setText("x-Drehung um " + angX.getValue() + "°");
      labelAngY.setText("y-Drehung um " + angY.getValue() + "°");
      labelAngZ.setText("z-Drehung um " + angZ.getValue() + "°");
      if(loadingValues) return;
      ListOfObject3D.setObject3DRotation(object3dType, angX.getValue(), angY.getValue(),
          angZ.getValue());
    }
  }
  
  private class PosAct implements ChangeListener
  {
    @Override
    /**
     * changes the position
     */
   public void stateChanged(ChangeEvent e)
    {
      if(object3dType == -1) return;
      labelPosX.setText("x-Position: " + posX.getValue() + " cm");
      labelPosY.setText("y-Position: " + posY.getValue() + " cm");
      labelPosZ.setText("z-Position: " + posZ.getValue() + " cm");
      if(loadingValues) return;
      Vector3f pos = new Vector3f((float) posX.getValue() / 100f,
          (float) posY.getValue() / 100f, (float) posZ.getValue() / 100f);
      ListOfObject3D.setObjec3DPosition(object3dType, pos);
    }
  }
  
  private class SizeAct implements ChangeListener
  {
    @Override
    /**
     * changes the size
     */
    public void stateChanged(ChangeEvent e)
    {
      if(object3dType == -1) return;
      labelWidth.setText("Breite: " + width.getValue() + " cm");
      labelLength.setText("Länge: " + length.getValue() + " cm");
      labelHeight.setText("Höhe: " + height.getValue() + " cm");
      if(loadingValues) return;
      Vector3f size = new Vector3f((float) width.getValue() / 100f,
          (float) length.getValue() / 100f, (float) height.getValue() / 100f);
      ListOfObject3D.setObject3DSize(object3dType, size);
    }
  }
  
  private class ColAct implements ChangeListener
  {
    @Override
    /**
     * changes the color
     */
    public void stateChanged(ChangeEvent e)
    {
      if(object3dType == -1) return;
      labelColR.setText("Rotanteil: " + colR.getValue() + " %");
      labelColG.setText("Grünanteil: " + colG.getValue() + " %");
      labelColB.setText("Blauanteil: " + colB.getValue() + " %");
      if(loadingValues) return;
      Vector3f color = new Vector3f((float) colR.getValue() / 100f,
          (float) colG.getValue() / 100f, (float) colB.getValue() / 100f);
      ListOfObject3D.setObject3DColor(object3dType, color);
    }
  }
  
  /**
   * load the setting values in the editor
   * @param loading
   */
  private void loadObject()
  {
    if(object3dType == -1) return;
    loadingValues = true;
    Object3D toLoad = ListOfObject3D.getList()[object3dType];
    angX.setValue((int) toLoad.getEulerX());
    angY.setValue((int) toLoad.getEulerY());
    angZ.setValue((int) toLoad.getEulerZ());
    posX.setValue((int) (toLoad.getRelativePosition().getX() * 100));
    posY.setValue((int) (toLoad.getRelativePosition().getY() * 100));
    posZ.setValue((int) (toLoad.getRelativePosition().getZ() * 100));
    width.setValue((int) (toLoad.getSize().getX() * 100));
    height.setValue((int) (toLoad.getSize().getY() * 100));
    length.setValue((int) (toLoad.getSize().getZ() * 100));
    colR.setValue((int) (toLoad.getColor().getX() * 100));
    colG.setValue((int) (toLoad.getColor().getY() * 100));
    colB.setValue((int) (toLoad.getColor().getZ() * 100));
    loadingValues = false;
  }
}
