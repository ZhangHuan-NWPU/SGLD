package ec.app.UAR.repair;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.UARIndividual;
import ec.gp.*;
import ec.gp.koza.MutationPipeline;

import java.util.ArrayList;
import java.util.HashMap;

public class RepairMutationPipeline extends MutationPipeline {
    @Override
    public int produce(final int min,
                       final int max,
                       final int subpopulation,
                       final ArrayList<Individual> inds,
                       final EvolutionState state,
                       final int thread, HashMap<String, Object> misc)
    {
        int start = inds.size();

        // grab individuals from our source and stick 'em right into inds.
        // we'll modify them from there
        int n = sources[0].produce(min,max,subpopulation,inds, state,thread, misc);

        // should we bother?
        if (!state.random[thread].nextBoolean(likelihood))
        {
            return n;
        }

        GPInitializer initializer = ((GPInitializer)state.initializer);

        // now let's mutate 'em
        for(int q=start; q < n+start; q++)
        {
            GPIndividual i = (GPIndividual)inds.get(q);

            if (tree!=TREE_UNFIXED && (tree<0 || tree >= i.trees.length))
                // uh oh
                state.output.fatal("GP Mutation Pipeline attempted to fix tree.0 to a value which was out of bounds of the array of the individual's trees.  Check the pipeline's fixed tree values -- they may be negative or greater than the number of trees in an individual");


            int t;
            // pick random tree
            if (tree==TREE_UNFIXED)
                if (i.trees.length>1) t = state.random[thread].nextInt(i.trees.length);
                else t = 0;
            else t = tree;

            // validity result...
            boolean res = false;

            // prepare the nodeselector
            nodeselect.reset();

            // pick a node

            GPNode p1=null;  // the node we pick
            GPNode p2=null;

            for(int x=0;x<numTries;x++)
            {
                // pick a node in individual 1
                p1 = nodeselect.pickNode(state,subpopulation,thread,i,i.trees[t]);

                // generate a tree swap-compatible with p1's position


                int size = GPNodeBuilder.NOSIZEGIVEN;
                if (equalSize) size = p1.numNodes(GPNode.NODESEARCH_ALL);

                p2 = builder.newRootedTree(state,
                        p1.parentType(initializer),
                        thread,
                        p1.parent,
                        i.trees[t].constraints(initializer).functionset,
                        p1.argposition,
                        size);

                // check for depth and swap-compatibility limits
                res = verifyPoints(p2,p1);  // p2 can fit in p1's spot  -- the order is important!

                // did we get something that had both nodes verified?
                if (res) break;
            }

            if (res)  // we're in business
            {
                p2.parent = p1.parent;
                p2.argposition = p1.argposition;
                if (p2.parent instanceof GPNode)
                    ((GPNode)(p2.parent)).children[p2.argposition] = p2;
                else ((GPTree)(p2.parent)).child = p2;
                i.evaluated = false;  // we've modified it
            }

            // add the new individual, replacing its previous source\
            ((RepairIndividual) i).repair((RepairEvolutionState) state, thread);
            inds.set(q, i);

        }
        return n;
    }
}
