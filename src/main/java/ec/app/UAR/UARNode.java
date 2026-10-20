package ec.app.UAR;
import ec.gp.GPNode;

public abstract class UARNode extends GPNode {
    public double dimension;
    public double dimension_gap;

    public abstract double getDimension();

    public abstract double getDimension_gap();
}
