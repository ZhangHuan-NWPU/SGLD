package ec.app.UAR.CPS;

import ec.EvolutionState;
import ec.Individual;
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

public class CPSPlusOneCrossoverPipeline extends CrossoverPipeline {

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

        for(int q=start;q<n+start; /* no increment */) { // keep on going until we're filled up
            parents.clear();

            BSFCIndividual parent1;
            BSFCIndividual parent2 = null;
            // 首先确认第一个父亲
            sources[0].produce(1, 1, subpopulation, parents, state, thread, misc);
            parent1 = (BSFCIndividual) parents.get(0);

            // 根据概率决定是否交叉
            int tries = 0;
            boolean loop = true;
            while (loop) {
                parents.clear();

                sources[1].produce(1, 1, subpopulation, parents, state, thread, misc);
                parent2 = (BSFCIndividual) parents.get(0);

                tries++;

                int hit_of_parent1 = parent1.bsfc.values().stream().reduce(0, Integer::sum);
                int hit_of_parent2 = parent2.bsfc.values().stream().reduce(0, Integer::sum);

                if (hit_of_parent1 < hit_of_parent2) {
                    BSFCIndividual temp = parent1;
                    parent1 = parent2;
                    parent2 = temp ;
                }

                if (allow_crossover(state_, parent1, parent2)) loop = false;

                int pop_size = state.population.subpops.get(subpopulation).initialSize;

                if (tries >= (pop_size / 2)) {
                    loop = false;
                    MultiBreedingPipeline source = (MultiBreedingPipeline) state.population.subpops.get(subpopulation).species.pipe_prototype;
                    source.sources[0].probability -= 1.0 / pop_size;
                    source.sources[1].probability += 1.0 / pop_size;
                }
            }

            if (parent2 == null) {
                sources[1].produce(1, 1, subpopulation, parents, state, thread, misc);
                parent2 = (BSFCIndividual) parents.get(1);
            }

            GPIndividual[] offspring = crossover(state_, thread, subpopulation, parent1, parent2);

            for (GPIndividual ind: offspring) {
                ((BSFCIndividual) ind).repair((RepairEvolutionState) state, thread);
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

        // Fill in various tree information that didn't get filled in there
        j1.trees = new GPTree[ind_1.trees.length];

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

        return new GPIndividual[] {j1};
    }

    /* 计算交叉概率*/
    public boolean allow_crossover(ModalEvolutionState state, BSFCIndividual recipient, BSFCIndividual doner) {

        // 计算
        double sum_recipient = 0;
        double advantage = 0;
        for (Task task: state.getTasks()) {
            int d = doner.bsfc.get(task);
            int r = recipient.bsfc.get(task);
            if (r > 0.5) sum_recipient++;
            if (r < 0.5 && d > 0.5) advantage ++;
        }

        if (state.getTasks().size() == sum_recipient)
            return false;

        double alpha = advantage / (state.getTasks().size() - sum_recipient);
        double beta = 1.0 - sum_recipient / state.getTasks().size();

        return (alpha > beta);
    }
}
