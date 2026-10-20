package ec.app.UAR.modal;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.util.MapToPythonDictFormat;
import ec.simple.SimpleProblemForm;
import ec.simple.SimpleStatistics;
import ec.steadystate.SteadyStateStatisticsForm;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

public class ModalStatistics extends SimpleStatistics implements SteadyStateStatisticsForm{

    // 记录当前时间
    public LocalDateTime start_time;
    // 记录全局最优通才个体
    public UARIndividual generalist = null;
    // 记录全局最优通才个体的适应度
    public double fitness;

    @Override
    public void preInitializationStatistics(final EvolutionState state) {
        super.preInitializationStatistics(state);
        start_time = LocalDateTime.now();
    }

    @Override
    public void postInitializationStatistics(final EvolutionState state) {
        super.postInitializationStatistics(state);
    }

    @Override
    /* 本函数的功能是输出全局最优个体*/
    public void finalStatistics(final EvolutionState state, final int result) {

        state.output.println("\nBest Individual of Run:",statisticslog);

        state.output.println("Fitness:"+ fitness, statisticslog);
        generalist.printTrees(state, statisticslog);

        state.output.println("Plans:"+ MapToPythonDictFormat.output(generalist.appointment_plan), statisticslog);
        state.output.println("Objectives:"+ MapToPythonDictFormat.output(generalist.objective), statisticslog);

        Duration dur = Duration.between(start_time, LocalDateTime.now());
        state.output.println("Time: "+dur.toString(), super.statisticslog);
    }

    @Override
    /* 本函数的功能是寻找全局最优个体*/
    public void postEvaluationStatistics(final EvolutionState state) {
        if (state.generation == (state.numGenerations - 1)) {
            ModalEvolutionState state_ = (ModalEvolutionState) state;

            generalist = null;
            fitness = 999999.9;

            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind: subpop.individuals) {
                    UARIndividual ind_ = (UARIndividual) ind;

                    double objective = ind_.objective.values().stream().mapToDouble(Double::doubleValue).sum() / state_.getTasks().size();

                    if (generalist == null || objective < fitness) {
                        generalist = (UARIndividual) ind_.clone();
                        fitness = objective;
                    }
                }
            }
        }
    }
}
