package de.tucottbus.kt.csl.lcars.components.threeDim;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.vecmath.Vector3f;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.lcars.messages.IObservable;
import de.tucottbus.kt.csl.lcars.messages.IObserver;
import de.tucottbus.kt.csl.lcars.messages.NewCeilingMsg;
import de.tucottbus.kt.csl.lcars.messages.NewCube3dViewMsg;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * This class is used to create a 3D space, to represent the CogntiveSystemsLab.
 * The point with the highest energy of the speaker is thus shown.
 * 
 * @author Martin Birth
 *
 */
public class TestCube3d implements IObservable, ActionListener {

  private Cube3d cube3d = null;

  private float array2Pos = 0;

  private final float ANGLE = 45;

  // observable objects
  private boolean changed = false;
  private final Vector<IObserver> obs;

  /**
   * Buttons for anything.
   */
  private JMenuItem movableView;
  private JMenuItem onTopView;
  private JMenuItem exit;
  private JMenuItem reset;
  private JCheckBox arrayButton;

  private boolean ceiling = true;

  private boolean movable = true;

  private final MicArrayState state;

  /**
   * Direction of array2
   */
  private boolean direction = true;

  /**
   * The constructor with the updating thread for new point positions.
   */
  public TestCube3d() {
    obs = new Vector<IObserver>();
    cube3d = new Cube3d(this);
    addObserver(cube3d);
    initFrame();
    
    state = MicArrayState.getCurrentState();
    MicArray3D.getInstance().dispose();
    
    Thread positionThread = new Thread() {
      @Override
      public void run() {
        while (true) {

          if (direction) {
            if (array2Pos < 0.7)
              array2Pos = (float) (array2Pos + 0.1);
            else
              direction = false;
          } else {
            if (array2Pos > -0.5)
              array2Pos = (float) (array2Pos - 0.1);
            else
              direction = true;
          }

          notifyObservers(new NewCeilingMsg(state, ceiling));
          setChanged();

          try {
            Thread.sleep(300);
          } catch (InterruptedException e2) {
            Log.err("InterruptedException", e2);
          }
        }
      }
    };
    positionThread.setName("Room3d:Positioning");
    positionThread.start();
  }

  /**
   * Initializes the frame with all elements on it.
   */
  private void initFrame() {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("CognitiveSystems Lab 3D Room");
    frame.setSize(1000, 1000);
    frame.setLocationRelativeTo(null);
    frame.setLayout(new BorderLayout());

    // Set up the menu bar, which appears above the content pane.
    JMenuBar menuBar = new JMenuBar();

    // default menu
    JMenu menu = new JMenu("SliceRoom");
    exit = new JMenuItem("Exit");
    exit.addActionListener(this);
    menu.add(exit);
    menuBar.add(menu);

    // view menu
    JMenu view = new JMenu("View");
    onTopView = new JMenuItem("On Top");
    onTopView.addActionListener(this);
    view.add(onTopView);
    movableView = new JMenuItem("Movable");
    movableView.addActionListener(this);
    view.add(movableView);
    menuBar.add(view);

    // point menu
    JMenu point = new JMenu("Objects");
    arrayButton = new JCheckBox("Ceiling Array", true);
    arrayButton.addItemListener(new CheckBoxListener());
    point.add(arrayButton);
    JSeparator sep2 = new JSeparator();
    point.add(sep2);
    reset = new JMenuItem(" Reset Point");
    reset.addActionListener(this);
    point.add(reset);
    menuBar.add(point);

    // add canvas to jFrame
    frame.setJMenuBar(menuBar);
    frame.add(cube3d.getCanvas3d(new Vector3f(0f, 0, 5f), ANGLE, false));

    frame.setVisible(true);
  }

  private class CheckBoxListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getSource() == arrayButton) {
        if (!arrayButton.isSelected()) {
          ceiling = false;
        } else {
          ceiling = true;
        }
      }
    }
  }

  /**
   * ActionListener for the button.
   */
  @Override
  public void actionPerformed(ActionEvent ae) {
    if (ae.getSource() == this.movableView) {
      setChanged();
      notifyObservers(new NewCube3dViewMsg(null, null, movable));
      movable = !movable;
    }

    if (ae.getSource() == this.onTopView) {
      notifyObservers("reset on top");
      setChanged();
    }

    if (ae.getSource() == this.exit)
      System.exit(0);

    if (ae.getSource() == this.reset) {
      notifyObservers("reset");
    }
  }

  @Override
  public synchronized void addObserver(IObserver o) {
    if (o == null)
      throw new NullPointerException();
    if (!obs.contains(o)) {
      obs.addElement(o);
    }
  }

  @Override
  public synchronized void deleteObserver(IObserver o) {
    obs.removeElement(o);
  }

  @Override
  public void notifyObservers() {
    notifyObservers(null);
  }

  @Override
  public void notifyObservers(Object arg) {
    Object[] arrLocal;

    synchronized (this) {

      if (!changed)
        return;
      arrLocal = obs.toArray();
      clearChanged();
    }

    for (Object local : arrLocal)
      ((IObserver) local).update(this, arg);
  }

  @Override
  public synchronized void deleteObservers() {
    obs.removeAllElements();
  }

  @Override
  public synchronized void setChanged() {
    changed = true;
  }

  protected synchronized void clearChanged() {
    changed = false;
  }

  @Override
  public synchronized boolean hasChanged() {
    return changed;
  }

  @Override
  public synchronized int countObservers() {
    return obs.size();
  }

  public static void main(String[] args) {
    new TestCube3d();
    return;
  }

}