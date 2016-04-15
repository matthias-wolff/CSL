package de.tucottbus.kt.csl.hardware;

import java.util.ArrayList;
import java.util.Collection;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_010;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_011;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_012;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ESector;
import de.tucottbus.kt.lcars.swt.ColorMeta;

/**
 * Root of the CSL hardware tree.
 * 
 * @author Matthias Wolff
 */
public final class CslHardware extends ACompositeHardware 
{
  // -- Singleton implementation --

  private static volatile CslHardware singleton = null;

  /**
   * Returns the singleton instance. 
   */
  public static synchronized CslHardware getInstance()
  {
    if (singleton==null)
      singleton = new CslHardware();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private CslHardware() 
  {
  }

  // -- Implementation of AHardware --

  @Override
  public String getName() 
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AHardware getParent() 
  {
    return null;
  }

  @Override
  public Collection<AHardware> getChildren() 
  {
    ArrayList<AHardware> children = new ArrayList<AHardware>();
    children.add(MicArray3D.getInstance());
    children.add(IpPowerSocket_010.getInstance());
    children.add(IpPowerSocket_011.getInstance());
    children.add(IpPowerSocket_012.getInstance());
    return children;
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }

  // -- Operations --
  
  /**
   * Starts all hardware wrappers.
   * 
   * @see #dispose()
   */
  public void startAll()
  {
    getChildren();
  }
  
  /**
   * Stops and disposes of all hardware wrappers.
   */
  public void stopAll()
  {
    dispose();
  }
 
  // -- CSL hardware panel --
  
  public static class CslHardwarePanel extends HardwareAccessPanel
  {

    public CslHardwarePanel(IScreen iscreen) 
    {
      super(iscreen, CslHardware.class);
    }

    @Override
    protected void createSubPanels() 
    {
      final ColorMeta grey = new ColorMeta(0x44,0x44,0x44);
      
      EElbo eElbo = new EElbo(this,677,140,50,50,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
      eElbo.setArmWidths(1,1); eElbo.setArcWidths(0,0);
      eElbo.setColor(grey);
      add(eElbo);
      
      ELabel eLabel = new ELabel(this,684,147,150,25,LCARS.ES_STATIC|LCARS.ES_LABEL_NW,"677,140");
      eLabel.setColor(grey);
      add(eLabel);
      
      final int style = LCARS.EF_HEAD1|LCARS.ES_LABEL_NW|LCARS.EC_SECONDARY|LCARS.ES_STATIC;
      final int x = 1050;
      final int y = 550;
      
      add(new ESector(this,x,y,110,60,-45,90,7,style,null));
      add(new ESector(this,x,y,110,60,90,225,7,style,null));
      add(new ESector(this,x,y,110,60,225,270,7,style,null));
      add(new ESector(this,x,y,110,60,-90,-45,7,style,null));
      add(new ELabel(this,x+134,y-5,797,40,style,"COGNITIVE SYSTEMS LAB"));

      eElbo = new EElbo(this,1847,888,50,50,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
      eElbo.setArmWidths(1,1); eElbo.setArcWidths(0,0);
      eElbo.setColor(grey);
      add(eElbo);
      
      eLabel = new ELabel(this,1740,906,150,25,LCARS.ES_STATIC|LCARS.ES_LABEL_SE,"1897,938");
      eLabel.setColor(grey);
      add(eLabel);
    
    }
  }

  // == Main method ==

  /**
   * Starts LCARS with the {@link CslHardwarePanel}.
   * 
   * @param args
   *          -- not used --
   */
  public static void main(String[] args)
  {      
    args = LCARS.setArg(args,"--panel=",CslHardwarePanel.class.getName());
    LCARS.main(args);
  }
}
