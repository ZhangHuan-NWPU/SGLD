package ec.app.UAR.CPS;

import ec.EvolutionState;
import ec.Population;
import ec.Subpopulation;
import ec.breed.MultiBreedingPipeline;
import ec.es.MuPlusLambdaBreeder;
import ec.simple.SimpleBreeder;

public class CPSBreederES extends MuPlusLambdaBreeder {
    @Override
    public Population breedPopulation(EvolutionState state) {
        for (Subpopulation subpop: state.population.subpops) {
            MultiBreedingPipeline source = (MultiBreedingPipeline) subpop.species.pipe_prototype;
            source.sources[0].probability = 0.9;
            source.sources[1].probability = 0.1;
        }

        return super.breedPopulation(state);
    }
}
