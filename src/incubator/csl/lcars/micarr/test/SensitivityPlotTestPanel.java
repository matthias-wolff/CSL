package incubator.csl.lcars.micarr.test;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import incubator.csl.lcars.micarr.elements.ESensitivityPlot;

/**
 * -- <i>for testing only</i> --
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class SensitivityPlotTestPanel extends Panel
{
  ESensitivityPlot eSpxy;
  ESensitivityPlot eSpxz;
  ESensitivityPlot eSpyz;
  
  public SensitivityPlotTestPanel(IScreen iscreen)
  {
    super(iscreen);
  }

  @Override
  public void init()
  {
    super.init();
    
    MicArrayState state = MicArrayState.getCurrentState();
    
    eSpxy = new ESensitivityPlot(this,100,100,-1,-1,ESensitivityPlot.SLICE_XY,state);
    eSpxy.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDrag(EEvent ee)
      {
        touchDown(ee);
      }
      
      @Override
      public void touchDown(EEvent ee)
      {
        setSlicePositions(eSpxy.elementToCsl(ee.pt));
      }
    });
    add(eSpxy);

    eSpxz = new ESensitivityPlot(this,100,543,-1,-1,ESensitivityPlot.SLICE_XZ,state);
    eSpxz.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDrag(EEvent ee)
      {
        touchDown(ee);
      }
      
      @Override
      public void touchDown(EEvent ee)
      {
        setSlicePositions(eSpxz.elementToCsl(ee.pt));
      }
    });
    add(eSpxz);

    eSpyz = new ESensitivityPlot(this,543,543,-1,-1,ESensitivityPlot.SLICE_YZ,state);
    eSpyz.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDrag(EEvent ee)
      {
        touchDown(ee);
      }
      
      @Override
      public void touchDown(EEvent ee)
      {
        setSlicePositions(eSpyz.elementToCsl(ee.pt));
      }
    });
    add(eSpyz);
  }
  
  protected void setSlicePositions(Point3d point)
  {
    DoAEstimator.getInstance().setTargetSource(point);
    MicArrayState state = MicArrayState.getCurrentState();
    eSpxy.setMicArrayState(state);
    eSpxy.setSlicePos(point.z);
    eSpxz.setMicArrayState(state);
    eSpxz.setSlicePos(point.y);
    eSpyz.setMicArrayState(state);
    eSpyz.setSlicePos(point.x);
  }

  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",SensitivityPlotTestPanel.class.getName());
    LCARS.main(args);
  }

}

// EOF
