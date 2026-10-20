package ec.app.UAR.functions;
import ec.EvolutionState;
import ec.Problem;
import ec.app.UAR.UARNode;
import ec.app.UAR.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;

public class Sqrt extends UARNode{
    public String toString() { return "sqrt"; }

    public int expectedChildren() { return 1; }

    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem)
    {
        DoubleData rd = ((DoubleData)(input));

        children[0].eval(state,thread,input,stack,individual,problem);
        if (rd.x < 0)
            rd.x = 0;
        rd.x = Math.sqrt(rd.x);

        /* Dimension Computation*/
        UARNode c1 = (UARNode) children[0];
        dimension = c1.getDimension() / 2.0;
        dimension_gap = c1.getDimension_gap();
    }

    @Override
    public double getDimension() {
        return dimension;
    }

    @Override
    public double getDimension_gap() {
        return dimension_gap;
    }
}
