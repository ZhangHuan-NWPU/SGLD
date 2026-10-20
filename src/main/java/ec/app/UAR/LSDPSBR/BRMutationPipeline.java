package ec.app.UAR.LSDPSBR;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.repair.RepairEvolutionState;
import ec.app.UAR.repair.RepairIndividual;
import ec.gp.*;
import ec.gp.koza.MutationPipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class BRMutationPipeline extends MutationPipeline {
    @Override
    public int produce(final int min,
                       final int max,
                       final int subpopulation,
                       final ArrayList<Individual> inds,
                       final EvolutionState state,
                       final int thread, HashMap<String, Object> misc)
    {
        BREvolutionState state_ = (BREvolutionState) state;

        int start = inds.size();

        // grab individuals from our source and stick 'em right into inds.
        // we'll modify them from there
        int n = sources[0].produce(min,max,subpopulation,inds, state,thread, misc);

        // should we bother?
        if (!state.random[thread].nextBoolean(likelihood)) {return n;}

        // now let's mutate 'em
        for(int q=start; q < n+start; q++) {
            GPIndividual ind = (GPIndividual)inds.get(q);

            // 繁殖重组
            // 定义一个比较器，根据 ONE_NN(state, ind) 的值对个体进行排序
            Comparator<GPIndividual> ind_comparator_for_generalist = new Comparator<GPIndividual>() {
                @Override
                public int compare(GPIndividual ind1, GPIndividual ind2) {
                    double score1 = state_.archive.ONE_NN_for_generalist(state_, (UARIndividual) ind1, thread);
                    double score2 = state_.archive.ONE_NN_for_generalist(state_, (UARIndividual) ind2, thread);
                    return Double.compare(score2, score1); // 降序排序，值越大越好
                }
            };

            Comparator<GPIndividual> ind_comparator_for_specialist = new Comparator<GPIndividual>() {
                @Override
                public int compare(GPIndividual ind1, GPIndividual ind2) {
                    double score1 = state_.archive.ONE_NN_for_specialist(state_, (UARIndividual) ind1, thread);
                    double score2 = state_.archive.ONE_NN_for_specialist(state_, (UARIndividual) ind2, thread);
                    return Double.compare(score2, score1); // 降序排序，值越大越好
                }
            };

            int br_size = 5;

            // 进行多次变异
            ArrayList<GPIndividual> all_offspring = new ArrayList<>();
            for (int i = 0; i < br_size; i++) {
                GPIndividual new_ind = (GPIndividual) ind.clone();
                all_offspring.add(mutate(state, thread, subpopulation, new_ind));
            }

            double ratio = (double) state.generation / state.numGenerations;
            if (state.random[thread].nextDouble() > ratio)
                all_offspring.sort(ind_comparator_for_specialist);
            else
                all_offspring.sort(ind_comparator_for_generalist);

            // 选择最好的1个个体
            ind = all_offspring.getFirst();

            // add the new individual, replacing its previous source\
            // ((RepairIndividual) ind).repair((RepairEvolutionState) state, thread);
            inds.set(q, ind);
        }
        return n;
    }

    public GPIndividual mutate(EvolutionState state, int thread, int subpopulation, GPIndividual ind) {

        GPInitializer initializer = ((GPInitializer)state.initializer);

        int t;
        // pick random tree
        if (tree == TREE_UNFIXED)
            if (ind.trees.length > 1) t = state.random[thread].nextInt(ind.trees.length);
            else t = 0;
        else t = tree;

        // validity result...
        boolean res = false;

        // prepare the nodeselector
        nodeselect.reset();

        // pick a node

        GPNode p1 = null;  // the node we pick
        GPNode p2 = null;

        for (int x = 0; x < numTries; x++) {
            // pick a node in individual 1
            p1 = nodeselect.pickNode(state, subpopulation, thread, ind, ind.trees[t]);

            // generate a tree swap-compatible with p1's position


            int size = GPNodeBuilder.NOSIZEGIVEN;
            if (equalSize) size = p1.numNodes(GPNode.NODESEARCH_ALL);

            p2 = builder.newRootedTree(state,
                    p1.parentType(initializer),
                    thread,
                    p1.parent,
                    ind.trees[t].constraints(initializer).functionset,
                    p1.argposition,
                    size);

            // check for depth and swap-compatibility limits
            res = verifyPoints(p2, p1);  // p2 can fit in p1's spot  -- the order is important!

            // did we get something that had both nodes verified?
            if (res) break;
        }

        if (res) {
            // we're in business
            p2.parent = p1.parent;
            p2.argposition = p1.argposition;
            if (p2.parent instanceof GPNode)
                ((GPNode) (p2.parent)).children[p2.argposition] = p2;
            else ((GPTree) (p2.parent)).child = p2;
            ind.evaluated = false;  // we've modified it
        }

        ((RepairIndividual) ind).repair((RepairEvolutionState) state, thread);
        return ind;
    }
}
