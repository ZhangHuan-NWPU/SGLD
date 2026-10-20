package ec.app.UAR.functions;
import ec.EvolutionState;
import ec.Problem;
import ec.app.UAR.UARNode;
import ec.app.UAR.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;

public class If extends UARNode {
    public String toString() { return "if"; }

    public int expectedChildren() { return 3; }

    @Override
    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem)
    {
        double flag;
        double result_1;
        double result_2;
        DoubleData rd = ((DoubleData)(input));

        children[0].eval(state,thread,input,stack,individual,problem);
        flag = rd.x;

        children[1].eval(state,thread,input,stack,individual,problem);
        result_1 = rd.x;

        children[2].eval(state,thread,input,stack,individual,problem);
        result_2 = rd.x;

        if (flag > 0.0)
            rd.x = result_1;
        else
            rd.x = result_2;

        /* Dimension Computation*/
        UARNode c1 = (UARNode) children[1];
        UARNode c2 = (UARNode) children[2];
        dimension = (c1.getDimension() + c2.getDimension()) / 2.0;
        dimension_gap = c1.getDimension_gap() + c2.getDimension_gap() + Math.abs(c1.getDimension() - c2.getDimension());
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
