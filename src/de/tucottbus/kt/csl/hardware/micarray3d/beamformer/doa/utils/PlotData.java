package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils;

import org.jfree.data.xy.XYSeries;

/**
 * This is a object which can be plotted with the {@link Plotter} class.
 * @author Martin Birth
 *
 * @param <T>
 */
public class PlotData<T> {
  private final XYSeries series;
  private final T data;

  public PlotData(XYSeries series, T data) {
    this.series = series;
    this.data = data;
  }

  public XYSeries getSeries() {
    return series;
  }

  public T getData() {
    return data;
  }
  
  public boolean isEmpty(){
    if(data==null)
      return true;
    return series.isEmpty();
  }

  @Override
  public int hashCode() {
    return series.hashCode() ^ data.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof PlotData))
      return false;
    PlotData<?> pairo = (PlotData<?>) o;
    return this.series.equals(pairo.getSeries())
        && this.data.equals(pairo.getSeries());
  }

}
