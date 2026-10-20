package ec.app.UAR.DPS;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleEvaluator;

import java.util.HashMap;

public class DPSEvaluator extends SimpleEvaluator {
    @Override
    public void evaluatePopulation(final EvolutionState state) {
        super.evaluatePopulation(state);

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        // 清空
        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind : subpop.individuals) {
                DPSIndividual ind_ = (DPSIndividual) ind;
                ind_.normalization_case_fitness = new HashMap<>();
            }
        }

        // 计算每一个任务
        for (Task task: state_.getTasks()) {
            // 最大值和最小值
            double max = 0.0;
            double min = 99999.9;

            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind : subpop.individuals) {
                    double cf = ((DPSIndividual) ind).objective.get(task);

                    if (cf > max) max = cf;
                    if (cf < min) min = cf;
                }
            }

            // 计算正则适应度（目标值最小者为0.目标值最大者为1）
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind : subpop.individuals) {
                    DPSIndividual ind_ = (DPSIndividual) ind;
                    double cf = ind_.objective.get(task);

                    double ncf = (cf - min) / (max - min);

                    ind_.normalization_case_fitness.put(task, ncf);
                }
            }
        }
        // End
    }
}
