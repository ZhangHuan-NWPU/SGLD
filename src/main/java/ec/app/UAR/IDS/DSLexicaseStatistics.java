package ec.app.UAR.IDS;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.util.MapToPythonDictFormat;
import ec.simple.SimpleStatistics;
import ec.steadystate.SteadyStateStatisticsForm;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 案例选择是一种旨在选择种群中专家个体的选择方式，其特点为：全局池，随机排序和精英
 * 即每一次选择都以整个种群为候选，对所有任务或案例随机排序，逐个任务地评估个体的表现
 * 仅有最佳的一部分个体可以进行下一轮选择
 * 本类是降采样案例选择，案例列表的长度只有原来的0.25倍
 * */

public class DSLexicaseStatistics extends SimpleStatistics implements SteadyStateStatisticsForm {

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

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        state.output.println("\nBest Individual of Run:",statisticslog);

        DSLexicaseProblem.full_evaluate(state_, generalist, 0);
        fitness = generalist.fitness.fitness();

        state.output.println("Fitness:"+ fitness, statisticslog);
        generalist.printTrees(state, statisticslog);

        state.output.println("Plans:"+ MapToPythonDictFormat.output(generalist.appointment_plan), statisticslog);
        state.output.println("Objectives:"+ MapToPythonDictFormat.output(generalist.objective), statisticslog);

        Duration dur = Duration.between(start_time, LocalDateTime.now());
        state.output.println("Time: "+dur.toString(), super.statisticslog);
    }

    @Override
    /* 本函数的功能是寻找当前代的最佳个体，对其进行重新评估，并更新全局最优个体*/
    public void postEvaluationStatistics(final EvolutionState state) {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        // 寻找最佳个体
        UARIndividual best_i = null;
        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind: subpop.individuals) {
                UARIndividual ind_ = (UARIndividual) ind;

                if (best_i == null || ind_.fitness.betterThan(best_i.fitness)) {
                    best_i =  ind_;
                }
            }
        }

        // 重新而全面的评估评估
        // DSLexicaseProblem.full_evaluate(state_, best_i, 0);

        if (generalist == null || best_i.fitness.fitness() > fitness) {
            generalist = (UARIndividual) best_i.clone();
            fitness = best_i.fitness.fitness();
        }

        state.output.println("\nBest Individual of Generation "+ state.generation,statisticslog);

        state.output.println("Fitness:"+ fitness, statisticslog);
        generalist.printTrees(state, statisticslog);
    }
}