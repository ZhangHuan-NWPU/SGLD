package ec.app.UAR;

import ec.gp.GPData;

public class DoubleData extends GPData{
    public double x;    // return value

    public void copyTo(final GPData gpd)   // copy my stuff to another DoubleData
    { ((DoubleData) gpd).x = x; }
}
