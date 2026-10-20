package ec.app.UAR.repair;

import ec.EvolutionState;
import ec.Fitness;
import ec.Individual;
import ec.gp.GPIndividual;
import ec.gp.GPSpecies;

public class RepairSpecies extends GPSpecies {
    @Override
    public Individual newIndividual(EvolutionState state, int thread) {
        GPIndividual newind = ((GPIndividual)(i_prototype)).lightClone();

        // Initialize the trees
        for (int x=0;x<newind.trees.length;x++)
            newind.trees[x].buildTree(state, thread);

        // Set the fitness
        newind.fitness = (Fitness)(f_prototype.clone());
        newind.evaluated = false;

        // Set the species to me
        newind.species = this;

        /*=============================================*/

        // 对新个体进行修复
        ((RepairIndividual) newind).repair((RepairEvolutionState) state, thread);

        /*=============================================*/

        // ...and we're ready!
        return newind;
    }
}
