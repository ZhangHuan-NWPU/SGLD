package ec.app.UAR.multifactorial;

import ec.*;
import ec.steadystate.SteadyStateExchangerForm;
import ec.util.Parameter;

public class MFExchanger extends Exchanger implements SteadyStateExchangerForm {
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

    /** 本函数的功能是截断种群，只保留 */
    public Population preBreedingExchangePopulation(final EvolutionState state)
    {
        Population p = state.population;

        Population newpop = p.emptyClone();
        for (int x = 0; x < p.subpops.size(); x++) {

            p.subpops.get(x).individuals.sort(null);

            for (Individual ind: p.subpops.get(x).individuals) {
                newpop.subpops.get(x).individuals.add(ind);
                if (newpop.subpops.get(x).individuals.size() >= p.subpops.get(x).initialSize)
                    break;
            }
        }
        state.population = newpop;
        return state.population;
    }

    /** Simply returns state.population. */
    public Population postBreedingExchangePopulation(final EvolutionState state) {
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
