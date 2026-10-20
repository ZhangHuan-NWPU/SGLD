package ec.app.UAR.terminals;
import ec.EvolutionState;
import ec.Problem;
import ec.app.UAR.UARNode;
import ec.app.UAR.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;

public class One extends UARNode {
    public String toString() { return "1"; }

    public int expectedChildren() { return 0; }

    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem)
    {
        DoubleData rd = ((DoubleData)(input));
        rd.x = 1.0;
    }

    @Override
    public double getDimension() {
        return 0.0;
    }

    @Override
    public double getDimension_gap() {
        return 0.0;
    }
}
