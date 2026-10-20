package ec.app.UAR.DPS;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairEvolutionState;
import ec.breed.MultiBreedingPipeline;
import ec.gp.GPIndividual;
import ec.gp.GPInitializer;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.gp.koza.CrossoverPipeline;
import ec.util.IntBag;

import java.util.ArrayList;
import java.util.HashMap;

public class DPSCrossoverPipeline extends CrossoverPipeline {

    @Override
    public int produce(final int min,
                       final int max,
                       final int subpopulation,
                       final ArrayList<Individual> inds,
                       final EvolutionState state,
                       final int thread, HashMap<String, Object> misc)

    {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        int start = inds.size();

        // how many individuals should we make?
        int n = typicalIndsProduced();
        if (n < min) n = min;
        if (n > max) n = max;


        // should we bother?
        if (!state.random[thread].nextBoolean(likelihood))
        {
            // just load from source 0 and clone 'em
            sources[0].produce(n,n,subpopulation,inds, state,thread,misc);
            return n;
        }

        IntBag[] parentparents = null;
        IntBag[] preserveParents = null;
        if (misc!=null&&misc.get(KEY_PARENTS) != null)
        {
            preserveParents = (IntBag[])misc.get(KEY_PARENTS);
            parentparents = new IntBag[2];
            misc.put(KEY_PARENTS, parentparents);
        }

        GPInitializer initializer = ((GPInitializer)state.initializer);

        for(int q=start;q<n+start; /* no increment */) { // keep on going until we're filled up
            parents.clear();

            DPSIndividual parent1 = null;
            DPSIndividual parent2 = null;
            // 首先确认第一个父亲
            sources[0].produce(1, 1, subpopulation, parents, state, thread, misc);
            parent1 = (DPSIndividual) parents.get(0);

            // 根据概率决定是否交叉
            int tries = 0;
            boolean loop = true;
            while (loop) {
                parents.clear();

                sources[1].produce(1, 1, subpopulation, parents, state, thread, misc);
                parent2 = (DPSIndividual) parents.get(0);

                tries++;

                if (allow_crossover(state_, parent1, parent2)) {
                    loop = false;
                    state_.partner_num[state.generation] ++;
                };

                int pop_size = state.population.subpops.get(subpopulation).initialSize;

                if (tries >= (pop_size / 2)) {
                    loop = false;
                    MultiBreedingPipeline source = (MultiBreedingPipeline) state.population.subpops.get(subpopulation).species.pipe_prototype;
                    source.sources[0].probability -= 1.0 / pop_size;
                    source.sources[1].probability += 1.0 / pop_size;
                }
            }
            state_.crossover_num[state.generation] ++;
            state_.try_num[state.generation] += tries;

            // 标记选中
            for (Subpopulation subpop: state.population.subpops) {
                for (Individual ind: subpop.individuals) {
                    UARIndividual ind_ = (UARIndividual) ind;
                    if (ind_.equals(parent1) && !ind_.selected) {
                        ind_.selected = true;
                        state_.parent_num[state.generation]++;
                    }
                    if (ind_.equals(parent2) && !ind_.selected) {
                        ind_.selected = true;
                        state_.parent_num[state.generation]++;
                    }
                }
            }

            if (parent2 == null) {
                sources[1].produce(1, 1, subpopulation, parents, state, thread, misc);
                parent2 = (DPSIndividual) parents.get(1);
            }

            GPIndividual[] offspring = crossover(state_, thread, subpopulation, parent1, parent2);


            for (GPIndividual ind: offspring) {
                ((DPSIndividual) ind).repair((RepairEvolutionState) state, thread);
                inds.add(ind);
                if (preserveParents != null) {
                    parentparents[0].addAll(parentparents[1]);
                    preserveParents[q] = parentparents[0];
                }
                q++;
                if (q>=n+start) break;
            }
        }
        return n;
    }

    /** 本函数的功能是交叉*/
    private GPIndividual[] crossover(ModalEvolutionState state, int thread, int subpopulation, GPIndividual ind_1, GPIndividual ind_2) {
        GPInitializer initializer = ((GPInitializer)state.initializer);

        // pick random trees  -- their GPTreeConstraints must be the same
        int t1 = state.random[thread].nextInt(ind_1.trees.length);
        int t2 = state.random[thread].nextInt(ind_2.trees.length);

        // validity results...
        boolean res1 = false;
        boolean res2 = false;

        // prepare the nodeselectors
        nodeselect1.reset();
        nodeselect2.reset();

        // pick some nodes
        GPNode p1 = null;
        GPNode p2 = null;

        for(int x = 0; x < numTries; x++) {
            // pick a node in individual 1
            p1 = nodeselect1.pickNode(state,subpopulation,thread,ind_1,ind_1.trees[t1]);

            // pick a node in individual 2
            p2 = nodeselect2.pickNode(state,subpopulation,thread,ind_2,ind_2.trees[t2]);

            // check for depth and swap-compatibility limits
            res1 = verifyPoints(initializer,p2,p1);  // p2 can fill p1's spot -- order is important!
            res2 = verifyPoints(initializer,p1,p2);  // p1 can fill p2's spot -- order is important!

            // did we get something that had both nodes verified?
            if (res1 && res2) break;
        }

        // Create some new individuals based on the old ones -- since
        // GPTree doesn't deep-clone, this should be just fine.  Perhaps we
        // should change this to proto off of the main species prototype, but
        // we have to then copy so much stuff over; it's not worth it.

        GPIndividual j1 = (GPIndividual) ind_1.lightClone();
        GPIndividual j2 = (GPIndividual) ind_2.lightClone();

        // Fill in various tree information that didn't get filled in there
        j1.trees = new GPTree[ind_1.trees.length];
        j2.trees = new GPTree[ind_2.trees.length];

        // at this point, p1 or p2, or both, may be null.
        // If not, swap one in.  Else just copy the parent.

        for(int x=0;x<j1.trees.length;x++) {
            // we've got a tree with a kicking cross position!
            if (x==t1 && res1) {
                j1.trees[x] = ind_1.trees[x].lightClone();
                j1.trees[x].owner = j1;
                j1.trees[x].child = ind_1.trees[x].child.cloneReplacing(p2, p1);
                j1.trees[x].child.parent = j1.trees[x];
                j1.trees[x].child.argposition = 0;
                j1.evaluated = false;
            }  // it's changed
            else {
                j1.trees[x] = ind_1.trees[x].lightClone();
                j1.trees[x].owner = j1;
                j1.trees[x].child = (GPNode) ind_1.trees[x].child.clone();
                j1.trees[x].child.parent = j1.trees[x];
                j1.trees[x].child.argposition = 0;
            }
        }


        for(int x=0;x<j2.trees.length;x++) {
            // we've got a tree with a kicking cross position!
            if (x == t2 && res2) {
                j2.trees[x] = ind_2.trees[x].lightClone();
                j2.trees[x].owner = j2;
                j2.trees[x].child = ind_2.trees[x].child.cloneReplacing(p1, p2);
                j2.trees[x].child.parent = j2.trees[x];
                j2.trees[x].child.argposition = 0;
                j2.evaluated = false;
            } // it's changed
            else {
                j2.trees[x] = ind_2.trees[x].lightClone();
                j2.trees[x].owner = j2;
                j2.trees[x].child = (GPNode) ind_2.trees[x].child.clone();
                j2.trees[x].child.parent = j2.trees[x];
                j2.trees[x].child.argposition = 0;
            }
        }
        return new GPIndividual[] {j1, j2};
    }

    public boolean allow_crossover(ModalEvolutionState state, DPSIndividual recipient, DPSIndividual doner) {

        // 计算
        double alpha = 0.0;
        for (Task task: state.getTasks()) {
            double d = doner.normalization_case_fitness.get(task);
            double r = recipient.normalization_case_fitness.get(task);
            alpha += (r - d) / Math.max(r, d);
        }
        double beta = 0.0;

        return (alpha > beta);
    }
}
