package ec.app.UAR.IFS;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleEvaluator;
import ec.simple.SimpleFitness;

import java.util.HashMap;

/**
 * 隐式适应度共享（Implicit fitness sharing， IFS） 是一种调整适应度的选择策略
 * 本研究采用的是连续模式下的适应度共享，分母是所有个体在该任务的表现总和
 * 因涉及对案例适应度的归一化，因此借用了DPSIndividual
 * */

public class IFSEvaluator extends SimpleEvaluator {
    @Override
    public void evaluatePopulation(final EvolutionState state) {
        super.evaluatePopulation(state);

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        // 记录每一个任务上所有个体的表现总和
        HashMap<Task, Double> sum_performance = new HashMap<>();

        // 计算每一个任务
        for (Task task: state_.getTasks()) {
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind : subpop.individuals) {
                    UARIndividual ind_ = (UARIndividual) ind;
                    // 将种群中所有个体在该任务上的目标值相加
                    double cf = ind_.objective.get(task);

                    if (!sum_performance.containsKey(task)) sum_performance.put(task, 0.0);
                    else {
                        double temp = sum_performance.get(task);
                        temp += cf;
                        sum_performance.put(task, temp);
                    }
                }
            }
        }

        // 计算每一个个体的综合适应度
        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind : subpop.individuals) {
                UARIndividual ind_ = (UARIndividual) ind;

                /* 确定综合适应度 */
                double implicit_sharing_fitness = 0.0;
                for (Task task: state_.getTasks()) {
                    implicit_sharing_fitness += (ind_.objective.get(task) / sum_performance.get(task));
                }

                // 设置综合适应度
                SimpleFitness f = (SimpleFitness) ind.fitness;
                f.setFitness(state, implicit_sharing_fitness,false);
                // 设置个体状态为已评估
                ind.evaluated = true;
            }
        }
        // End
    }
}
