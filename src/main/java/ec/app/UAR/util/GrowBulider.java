package ec.app.UAR.util;

import ec.EvolutionState;
import ec.gp.*;

public class GrowBulider {
    protected static GPNode growNode(final EvolutionState state,
                              final int current,
                              final int max,
                              final GPType type,
                              final int thread,
                              final GPNodeParent parent,
                              final int argposition,
                              final GPFunctionSet set) {

        int t = type.type;
        GPNode[] terminals = set.terminals[t];
        // GPNode[] nonterminals = set.nonterminals[t];
        GPNode[] nodes = set.nodes[t];

        // pick a terminal when we're at max depth or if there are NO nonterminals
        if ((current+1 >= max) && terminals.length != 0) {
            GPNode n = (GPNode)(terminals[state.random[thread].nextInt(terminals.length)].lightClone());
            n.resetNode(state,thread);  // give ERCs a chance to randomize
            n.argposition = (byte)argposition;
            n.parent = parent;
            return n;
        }

        // else pick a random node
        else {
            GPNode n = (GPNode)(nodes[state.random[thread].nextInt(nodes.length)].lightClone());
            n.resetNode(state,thread);  // give ERCs a chance to randomize
            n.argposition = (byte)argposition;
            n.parent = parent;

            // Populate the node...
            GPType[] childtypes = n.constraints(((GPInitializer)state.initializer)).childtypes;
            for(int x=0;x<childtypes.length;x++)
                n.children[x] = growNode(state,current+1,max,childtypes[x],thread,n,x,set);

            return n;
        }
    }

    public static GPNode newRootedTree(final EvolutionState state,
                                final GPType type,
                                final int thread,
                                final GPNodeParent parent,
                                final GPFunctionSet set,
                                final int argposition) {
        GPNode n = growNode(state,0,  5,type,thread,parent,argposition,set);
        return n;
    }
}
