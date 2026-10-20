package ec.app.UAR.rankaggregation;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.NewFitness;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleEvaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Rank aggregation 策略采用了新的适应度计算方式
 * */

public class RAEvaluator extends SimpleEvaluator {
    @Override
    public void evaluatePopulation(final EvolutionState state) {
        super.evaluatePopulation(state);

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind: subpop.individuals) {
                RAIndividual ind_ = (RAIndividual) ind;
                ind_.rank = new HashMap<>();
            }
        }

        /* 更新个体的排名*/
        // 遍历每一个任务
        for (Task task: state_.getTasks()) {
            // 建立一个列表作为排名
            ArrayList<RAIndividual> sorted_inds = new ArrayList<>();
            // 填充所有个体
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind: subpop.individuals) {
                    sorted_inds.add((RAIndividual) ind);
                }
            }

            // 根据目标值升序排序
            sorted_inds.sort(Comparator.comparingDouble(ind -> ind.objective.get(task)));
            // 确定因子排名
            for (int i = 0; i < sorted_inds.size(); i++) {
                RAIndividual ra_ind = sorted_inds.get(i);
                ra_ind.rank.put(task, i + 1);
            }
        }

        // 遍历每一个个体，计算适应度
        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind: subpop.individuals) {
                RAIndividual ind_ = (RAIndividual) ind;
                int rank_aggregation = 0;

                for (Task task: state_.getTasks()) {
                    rank_aggregation = rank_aggregation + ind_.rank.get(task) * ind_.rank.get(task);
                }

                ((NewFitness) ind.fitness).setFitness(state, rank_aggregation, false);
            }
        }
    }
}
