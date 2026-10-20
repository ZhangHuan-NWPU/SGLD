package ec.app.UAR.LSDPSBR;

import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.breed.MultiBreedingPipeline;
import ec.simple.SimpleBreeder;

public class BRBreeder extends SimpleBreeder {
    @Override
    public Population breedPopulation(EvolutionState state) {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        for (Subpopulation subpop: state.population.subpops) {
            MultiBreedingPipeline source = (MultiBreedingPipeline) subpop.species.pipe_prototype;
            source.sources[0].probability = 0.9;
            source.sources[1].probability = 0.1;
        }

        return super.breedPopulation(state);
    }

    @Override
    protected int nextSubpopulationSize(EvolutionState state, int subpop) {
        int br_size = ((BREvolutionState) state).br_size;
        return br_size * state.population.subpops.get(subpop).initialSize;
    }
}
