package ec.app.UAR.rankaggregation;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.simple.SimpleStatistics;
import ec.steadystate.SteadyStateStatisticsForm;

import java.time.Duration;
import java.time.LocalDateTime;

public class RAStatistics extends SimpleStatistics implements SteadyStateStatisticsForm {

    // 记录当前时间
    public LocalDateTime start_time;
    // 记录全局最优通才个体
    public UARIndividual generalist;
    // 记录全局最优通才个体的适应度
    public double fitness;

    @Override
    public void preInitializationStatistics(final EvolutionState state) {
        super.preInitializationStatistics(state);
        start_time = LocalDateTime.now();
        generalist = null;
        fitness = 9999999.9;
    }

    @Override
    // 报告全局最优个体，在最后对其进行重新评估
    public void finalStatistics(final EvolutionState state, final int result) {

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        if (doFinal) state.output.println("\nBest Individual of Run:",statisticslog);


        generalist.evaluated = false;
        ((GPProblem) state.evaluator.p_problem).evaluate(state, generalist, 0, 0);

        // 目前采用的综合适应度为GAP
        double integrated_fitness = 0.0;
        for (Task task: state_.getTasks()) {
            double cf = generalist.objective.get(task);
            integrated_fitness += ( state_.getFrontier(task) - cf ) / state_.getFrontier(task);
        }
        integrated_fitness = integrated_fitness / state_.getTasks().size();
        fitness = integrated_fitness;

        if (doFinal) generalist.printIndividualForHumans(state,statisticslog);

        // finally describe the winner if there is a description
        if (doFinal && doDescription)
            if (state.evaluator.p_problem instanceof SimpleProblemForm)
                ((SimpleProblemForm)(state.evaluator.p_problem.clone())).describe(state, generalist, 0, 0, statisticslog);

        Duration dur = Duration.between(start_time, LocalDateTime.now());
        state.output.println("Time: "+dur.toString(), super.statisticslog);
    }

    @Override
    // 报告当前的全局最优个体
    public void postEvaluationStatistics(final EvolutionState state) {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind: subpop.individuals) {
                UARIndividual ind_ = (UARIndividual) ind;

                // 目前采用的综合适应度为GAP
                double integrated_fitness = 0.0;
                for (Task task: state_.getTasks()) {
                    double cf = ind_.objective.get(task);
                    integrated_fitness += ( state_.getFrontier(task) - cf ) / state_.getFrontier(task);
                }
                integrated_fitness = integrated_fitness / state_.getTasks().size();

                if (generalist == null || integrated_fitness > fitness) {
                    generalist = (UARIndividual) ind_.clone();
                    fitness = integrated_fitness;
                }
            }
        }

        state.output.println("\nGlobal Best Individuals up to Generation "  + state.generation, statisticslog);
        generalist.printIndividualForHumans(state, statisticslog);
        state.output.println("Size: " + generalist.size(), statisticslog);
    }
}
