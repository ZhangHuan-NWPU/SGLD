package ec.app.UAR;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.ModalStatistics;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.simple.SimpleStatistics;
import ec.steadystate.SteadyStateStatisticsForm;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

public class NewStatistics extends SimpleStatistics implements SteadyStateStatisticsForm {

    // 记录当前时间
    public LocalDateTime start_time;

    @Override
    public void preInitializationStatistics(final EvolutionState state) {
        super.preInitializationStatistics(state);
        start_time = LocalDateTime.now();
    }

    @Override
    // 报告全局最优个体，在最后对其进行重新评估
    public void finalStatistics(final EvolutionState state, final int result) {
        if (doFinal) state.output.println("\nBest Individual of Run:",statisticslog);
        for(int x = 0; x< state.population.subpops.size(); x++ ) {

            best_of_run[x].evaluated = false;
            ((GPProblem) state.evaluator.p_problem).evaluate(state, best_of_run[x], x, 0);

            if (doFinal) state.output.println("Subpopulation " + x + ":",statisticslog);
            if (doFinal) best_of_run[x].printIndividualForHumans(state,statisticslog);
            if (doMessage && !silentPrint) state.output.message("Subpop " + x + " best fitness of run: " + best_of_run[x].fitness.fitnessToStringForHumans());

            // finally describe the winner if there is a description
            if (doFinal && doDescription)
                if (state.evaluator.p_problem instanceof SimpleProblemForm)
                    ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, best_of_run[x], x, 0, statisticslog);
        }
        ModalEvolutionState state_ = (ModalEvolutionState) state;
        state.output.println(Arrays.toString(state_.crossover_num), statisticslog);
        state.output.println(Arrays.toString(state_.partner_num), statisticslog);
        state.output.println(Arrays.toString(state_.try_num), statisticslog);
        state.output.println(Arrays.toString(state_.parent_num), statisticslog);

        Duration dur = Duration.between(start_time, LocalDateTime.now());
        state.output.println("Time: "+dur.toString(), super.statisticslog);
    }

    @Override
    // 报告当前的全局最优个体
    public void postEvaluationStatistics(final EvolutionState state) {
        super.postEvaluationStatistics(state);

        state.output.println("\nGlobal Best Individuals up to Generation "  + state.generation, statisticslog);
        for(int x = 0; x < state.population.subpops.size(); x++) {
            state.output.println("Subpopulation " + x + ":", statisticslog);
            best_of_run[x].printIndividualForHumans(state, statisticslog);
            state.output.println("Size: " + best_of_run[x].size(), statisticslog);
        }
    }
}
