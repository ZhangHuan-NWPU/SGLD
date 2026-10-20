package ec.app.UAR.multifactorial;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleEvaluator;
import ec.simple.SimpleFitness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * 多因子优化（Multifactorial Optimization, MFO）是用于多任务优化的一种技术
 * 在MFO中，每一次评估过后都需要更新个体的因子排名，技能因子和标量适应度
 * 具体而言，首先对整个种群中的个体升序排列，获取它们的排名，并基于排名得到技能因子，最后更新标量适应度
 * */

public class MFEvaluator extends SimpleEvaluator {

    @Override
    public void evaluatePopulation(final EvolutionState state) {
        super.evaluatePopulation(state);

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        /* 更新个体的因子排名*/
        // 遍历每一个任务
        for (Task task: state_.getTasks()) {
            // 建立一个列表作为排名
            ArrayList<MFIndividual> sorted_inds = new ArrayList<>();
            // 填充所有个体
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind: subpop.individuals) {
                    sorted_inds.add((MFIndividual) ind);
                }
            }

            // 根据目标值升序排序
            sorted_inds.sort(Comparator.comparingDouble(ind -> ind.objective.get(task)));
            // 确定因子排名
            for (int i = 0; i < sorted_inds.size(); i++) {
                MFIndividual ind = sorted_inds.get(i);
                ind.factorial_rank.put(task, i + 1);
            }
        }

        // 遍历每一个个体
        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind: subpop.individuals) {
                MFIndividual ind_ = (MFIndividual) ind;
                /* 更新个体的技能因子*/
                ind_.skill_factor = ind_.factorial_rank.entrySet().stream().
                        min(HashMap.Entry.comparingByValue()).map(HashMap.Entry::getKey).orElse(null);

                /* 更新个体的标量适应度*/
                double scalar_fitness = 10.0 / ind_.factorial_rank.get(ind_.skill_factor);
                ((SimpleFitness) ind.fitness).setFitness(state, scalar_fitness,false);
            }
        }
    }
}
