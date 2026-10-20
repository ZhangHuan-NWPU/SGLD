package ec.app.UAR.LSDPSBR;

import ec.*;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.lexicase.StaticLS;
import ec.util.Parameter;

import java.util.ArrayList;

public class BRExchanger extends Exchanger {
    public void setup(final EvolutionState state, final Parameter base) { }

    /** Doesn't do anything. */
    public void initializeContacts(final EvolutionState state)
    {
        // don't care
        return;
    }

    /** Doesn't do anything. */
    public void reinitializeContacts(final EvolutionState state)
    {
        // don't care
        return;
    }

    /** Simply returns state.population. */
    public Population preBreedingExchangePopulation(final EvolutionState state)
    {
        // don't care
        return state.population;
    }

    /** Simply returns state.population. */
    public Population postBreedingExchangePopulation(final EvolutionState state) {
        BREvolutionState state_ = (BREvolutionState) state;

        for (int i = 0; i < state.population.subpops.size(); i++)  {
            Subpopulation subpop = state.population.subpops.get(i);

            ArrayList<Individual> new_inds = new ArrayList<>();

            // 粗略评估
            for (Individual ind: subpop.individuals) {
                state_.archive.ONE_NN(state_, (UARIndividual) ind, 0);
            }

            // 建立候选集合
            ArrayList<Integer> candidates = new ArrayList<>();
            for (int j = 0; j < subpop.individuals.size(); j++) candidates.add(j);

            // 重复案例评估
            for (int j = 0; j < subpop.initialSize; j++) {
                Integer index = StaticLS.LS(state_, i, candidates);
                candidates.remove(index);

                Individual ind = subpop.individuals.get(index);
                ind.evaluated = false;
                new_inds.add(ind);
            }

            subpop.individuals = new_inds;
        }
        // don't care
        return state.population;
    }

    /** Doesn't do anything. */
    public void closeContacts(final EvolutionState state, final int result)
    {
        // don't care
        return;
    }

    /** Always returns null */
    public String runComplete(final EvolutionState state)
    {
        return null;
    }
}
