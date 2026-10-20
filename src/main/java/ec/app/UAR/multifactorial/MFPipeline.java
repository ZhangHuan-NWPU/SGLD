package ec.app.UAR.multifactorial;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairEvolutionState;
import ec.app.UAR.util.GrowBulider;
import ec.gp.*;
import ec.gp.koza.CrossoverPipeline;
import ec.util.IntBag;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 多因子优化（Multifactorial Optimization, MFO）是用于多任务优化的一种技术
 * 在MFO中，个体的繁殖遵循以下过程，首先判断2个个体的技能因子是否一致，若它们一致则交叉
 * 否则就根据预先设置的概率选择交叉还是变异，通过垂直文化传播机制，确定后代的技能因子
 * */

public class MFPipeline extends CrossoverPipeline {

    @Override
    public int produce(final int min,
                       final int max,
                       final int subpopulation,
                       final ArrayList<Individual> inds,
                       final EvolutionState state,
                       final int thread, HashMap<String, Object> misc)

    {
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

            // grab two individuals from our sources
            if (sources[0]==sources[1])  // grab from the same source
                sources[0].produce(2,2,subpopulation, parents, state,thread, misc);
            else { // grab from different sources
                sources[0].produce(1,1,subpopulation, parents, state,thread, misc);
                sources[1].produce(1,1,subpopulation, parents, state,thread, misc);
            }

            // at this point, parents[] contains our two selected individuals
            MFIndividual parent_1 = (MFIndividual) parents.get(0);
            MFIndividual parent_2 = (MFIndividual) parents.get(1);

            ModalEvolutionState state_ = (ModalEvolutionState) state;

            MFIndividual[] offspring = null;

            /*
             * 多因子优化（Multifactorial Optimization, MFO）是用于多任务优化的一种技术
             * 在MFO中，个体的繁殖遵循以下过程，首先判断2个个体的技能因子是否一致，若它们一致则交叉
             * 否则就根据预先设置的概率选择交叉还是变异，通过垂直文化传播机制，确定后代的技能因子
             */
            if (parent_1.skill_factor == parent_2.skill_factor)
                offspring = crossover(state_, thread, subpopulation, parent_1, parent_2);
            else {
                if (state_.random[thread].nextDouble() < 0.9)
                    offspring = crossover(state_, thread, subpopulation, parent_1, parent_2);
                else
                    offspring = new MFIndividual[] {mutate(state_, thread, subpopulation, parent_1), mutate(state_, thread, subpopulation, parent_2)};
            }

            // add the individuals to the population
            // inds.set(q, j1);
            for (MFIndividual ind: offspring) {
                ind.repair((RepairEvolutionState) state, thread);
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
    private MFIndividual[] crossover(ModalEvolutionState state, int thread, int subpopulation, MFIndividual ind_1, MFIndividual ind_2) {
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

        MFIndividual j1 = (MFIndividual) ind_1.lightClone();
        MFIndividual j2 = (MFIndividual) ind_2.lightClone();

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

        //===============确定个体的技能因子和因子目标值===============//
        Task[] factors = new Task[] {ind_1.skill_factor, ind_2.skill_factor};
        j1.skill_factor = factors[state.random[thread].nextInt(2)];
        j2.skill_factor = factors[state.random[thread].nextInt(2)];

        j1.objective = new HashMap<>();
        j2.objective = new HashMap<>();

        j1.factorial_rank = new HashMap<>();
        j2.factorial_rank = new HashMap<>();

        j1.evaluated = false;
        j2.evaluated = false;
        //======================================================//

        return new MFIndividual[] {j1, j2};
    }

    /** 本函数的功能是变异*/
    private MFIndividual mutate (ModalEvolutionState state, int thread, int subpopulation, MFIndividual ind) {

        GPInitializer initializer = ((GPInitializer)state.initializer);

        // pick random tree
        int t = state.random[thread].nextInt(ind.trees.length);

        // validity result...
        boolean res = false;

        // prepare the nodeselector
        nodeselect1.reset();

        // pick a node
        GPNode p1=null;  // the node we pick
        GPNode p2=null;

        for(int x = 0;x < numTries; x++) {
            // pick a node in individual 1
            p1 = nodeselect1.pickNode(state, subpopulation, thread, ind, ind.trees[t]);

            // generate a tree swap-compatible with p1's position
            p2 = GrowBulider.newRootedTree(state,
                                                 p1.parentType(initializer),
                                                 thread,
                                                 p1.parent,
                                                 ind.trees[t].constraints(initializer).functionset,
                                                 p1.argposition);

            // check for depth and swap-compatibility limits
            res = verify_mutate_points(p2, p1);  // p2 can fit in p1's spot  -- the order is important!

            // did we get something that had both nodes verified?
            if (res) break;
        }

        if (res) {
            p2.parent = p1.parent;
            p2.argposition = p1.argposition;
            if (p2.parent instanceof GPNode)
                ((GPNode)(p2.parent)).children[p2.argposition] = p2;
            else ((GPTree)(p2.parent)).child = p2;
        }

        //===============确定个体的技能因子和因子目标值===============//
        ind.objective = new HashMap<>();

        ind.factorial_rank = new HashMap<>();

        ind.evaluated = false;
        //======================================================//

        return ind;
    }

    /** 本函数的功能是判断变异的点是否满足深度限制*/
    private boolean verify_mutate_points(GPNode inner1, GPNode inner2) {
        return inner1.depth() + inner2.atDepth() <= maxDepth;
    }
}
