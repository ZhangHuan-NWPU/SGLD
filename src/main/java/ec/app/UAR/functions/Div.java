package ec.app.UAR.functions;
import ec.EvolutionState;
import ec.Problem;
import ec.app.UAR.UARNode;
import ec.app.UAR.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;

public class Div extends UARNode {
    public String toString() { return "/"; }

    public int expectedChildren() { return 2; }

    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem)
    {
        double result;
        DoubleData rd = ((DoubleData)(input));

        children[0].eval(state,thread,input,stack,individual,problem);
        result = rd.x;

        children[1].eval(state,thread,input,stack,individual,problem);
        if (rd.x == 0){
            rd.x = result * 1000.0;
        }
        else{
            rd.x = result / rd.x;
        }

        /* Dimension Computation*/
        UARNode c1 = (UARNode) children[0];
        UARNode c2 = (UARNode) children[1];
        dimension = c1.getDimension() - c2.getDimension();
        dimension_gap = c1.getDimension_gap() + c2.getDimension_gap();
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

