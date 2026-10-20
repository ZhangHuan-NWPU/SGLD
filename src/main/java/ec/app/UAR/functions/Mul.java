package ec.app.UAR.functions;
import ec.EvolutionState;
import ec.Problem;
import ec.app.UAR.UARNode;
import ec.app.UAR.DoubleData;
import ec.gp.ADFStack;
import ec.gp.GPData;
import ec.gp.GPIndividual;

public class Mul extends UARNode {
    public String toString() { return "*"; }

    /*
      public void checkConstraints(final EvolutionState state,
      final int tree,
      final GPIndividual typicalIndividual,
      final Parameter individualBase)
      {
      super.checkConstraints(state,tree,typicalIndividual,individualBase);
      if (children.length!=2)
      state.output.error("Incorrect number of children for node " +
      toStringForError() + " at " +
      individualBase);
      }
    */
    public int expectedChildren() { return 2; }

    @Override
    public void eval(final EvolutionState state,
                     final int thread,
                     final GPData input,
                     final ADFStack stack,
                     final GPIndividual individual,
                     final Problem problem)
    {
        double result;
        DoubleData rd = ((DoubleData)(input));

        UARNode c1 = (UARNode) children[0];
        UARNode c2 = (UARNode) children[1];

        c1.eval(state,thread,input,stack,individual,problem);
        result = rd.x;

        c2.eval(state,thread,input,stack,individual,problem);
        rd.x = result * rd.x;

        dimension = c1.getDimension() + c2.getDimension();
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

