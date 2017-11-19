package de.tucottbus.kt.csl.retired.lcars.elements;

import java.util.ArrayList;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.retired.lcars.geometry.GSensitivityPlot;
import de.tucottbus.kt.csl.retired.lcars.messages.IObservable;
import de.tucottbus.kt.csl.retired.lcars.messages.IObserver;
import de.tucottbus.kt.csl.retired.lcars.messages.NewBeamformerMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewCeilingMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewFrequencyMsg;
import de.tucottbus.kt.csl.retired.lcars.messages.NewSlicePositionMsg;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.geometry.AGeometry;

@Deprecated
public class ESensitivityPlot extends EElement implements IObserver {

  private MicArrayState micArrayState;
  private double slicePos;
  private float frequency;
  private final GSensitivityPlot gs;
  private final IObservable obs;
  private final int orientation;

  /**
   * 
   * @param panel
   * @param x
   * @param y
   * @param w
   * @param h
   * @param slicePos
   *          The slice position: height in {@link GSensitivityPlot#ES_XY} mode,
   *          etc.
   * @param frequency
   * @param orientation
   *          The plot style, {@link GSensitivityPlot#ES_XY},
   *          {@link GSensitivityPlot#ES_YZ}, or {@link GSensitivityPlot#ES_XZ}
   *          combined with other standard element styles.
   */
  public ESensitivityPlot(Panel panel, IObservable observable, int x, int y,
      int w, int h, double slicePos, MicArrayState micArrayState,
      float frequency, int orientation) {
    super(panel, x, y, w, h, LCARS.EB_OVERDRAG, null);
    this.slicePos = slicePos;
    this.frequency = frequency;
    this.obs = observable;
    this.orientation = orientation;
    this.micArrayState = micArrayState;
    gs = new GSensitivityPlot(micArrayState, slicePos, frequency, orientation,
        getBounds());

  }

  public MicArrayState getMicArrayState() {
    return micArrayState;
  }

  private void setMicArrayState(Object arg) {
    MicArrayState state = null;
    if (arg instanceof NewBeamformerMsg) {
      NewBeamformerMsg msg = (NewBeamformerMsg) arg;
      state = msg.getMicArrayState();
    } else if (arg instanceof NewCeilingMsg) {
      NewCeilingMsg msg = (NewCeilingMsg) arg;
      state = msg.getMicArrayState();
    }

    if (state == null)
      return;

    try {
      if (micArrayState.equals(state))
        return;
    } catch (NullPointerException e) {}
    micArrayState = state;
    gs.setMicArrayState(state);
    invalidate(true);
  }

  public double getPos() {
    return slicePos;
  }

  private void setPos(NewSlicePositionMsg msg) {
    switch (orientation & GSensitivityPlot.ES_MASK) {
      case GSensitivityPlot.ES_XY:
        if (msg.getZSlicePos() != null) {
          slicePos = msg.getZSlicePos();
          gs.setSlicePos(msg.getZSlicePos());
          invalidate(true);
        }
        break;
      case GSensitivityPlot.ES_YZ:
        if (msg.getXSlicePos() != null) {
          slicePos = msg.getXSlicePos();
          gs.setSlicePos(msg.getXSlicePos());
          invalidate(true);
        }
        break;
      case GSensitivityPlot.ES_XZ:
        if (msg.getYSlicePos() != null) {
          slicePos = msg.getYSlicePos();
          gs.setSlicePos(msg.getYSlicePos());
          invalidate(true);
        }
        break;
    }
  }

  public float getFrequency() {
    return frequency;
  }

  private void setFrequency(NewFrequencyMsg msg) {
    if (msg.getFrequency() == frequency)
      return;

    this.frequency = msg.getFrequency();
    gs.setFrequency(msg.getFrequency());
    invalidate(true);
  }

  @Override
  protected ArrayList<AGeometry> createGeometriesInt() {
    ArrayList<AGeometry> geos = new ArrayList<AGeometry>();
    geos.add(gs);
    return geos;
  }

  @Override
  public void update(IObservable o, Object arg) {
    if (o != obs)
      return;

    if (arg instanceof NewSlicePositionMsg)
      setPos((NewSlicePositionMsg) arg);

    if (arg instanceof NewFrequencyMsg)
      setFrequency((NewFrequencyMsg) arg);

    if (arg instanceof NewBeamformerMsg || arg instanceof NewCeilingMsg)
      setMicArrayState(arg);

  }

}
