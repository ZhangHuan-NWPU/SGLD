package ec.app.UAR.repair;

import com.gurobi.gurobi.GRBException;
import ec.app.UAR.UARIndividual;
import ec.gp.GPIndividual;

public class RepairIndividual extends UARIndividual {
    public void repair(RepairEvolutionState state, int thread) {
        // 对个体进行修复
        DimensionRepairer repairer = state.repairer[thread];
        try {
            if (this.trees.length == 1)
                repairer.repair(this.trees[0], 1.0);
            else if (this.trees.length == 2) {
                repairer.repair(this.trees[0], 0.0);
                repairer.repair(this.trees[1], 1.0);
            }
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
    }
}
