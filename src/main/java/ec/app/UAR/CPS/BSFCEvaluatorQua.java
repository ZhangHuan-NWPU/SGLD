package ec.app.UAR.CPS;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleEvaluator;

import java.util.ArrayList;
import java.util.HashMap;

public class BSFCEvaluatorQua extends SimpleEvaluator {
    @Override
    public void evaluatePopulation(final EvolutionState state) {
        super.evaluatePopulation(state);

        ModalEvolutionState state_ = (ModalEvolutionState) state;

        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind: subpop.individuals) {
                BSFCIndividual ind_ = (BSFCIndividual) ind;
                ind_.bsfc = new HashMap<>();
            }
        }

        // 计算每一个任务的平均表现
        for (Task task: state_.getTasks()) {
            // 建立一个列表记录表现
            ArrayList<Double> performance = new ArrayList<>();
            // 填充所有个体
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind: subpop.individuals) {
                    UARIndividual ind_ = (UARIndividual) ind;
                    performance.add(ind_.objective.get(task));
                }
            }

            // 计算平均表现（Q1）
            double mean_performance = quartile(performance);

            // 更新所有个体
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind: subpop.individuals) {
                    BSFCIndividual ind_ = (BSFCIndividual) ind;
                    if (ind_.objective.containsKey(task) && ind_.objective.get(task) <= mean_performance)
                        ind_.bsfc.put(task, 1);
                    else
                        ind_.bsfc.put(task, 0);
                }
            }
        }
    }

    public double quartile(ArrayList<Double> list) {
        if (list == null || list.isEmpty()) {
            return 0.0;
        }

        // 对列表进行排序
        list.sort(null);

        int size = list.size();
        if (size % 4 == 0) {
            return (list.get(size / 4 - 1) + list.get(size / 4)) / 2;
        } else {
            return list.get(size / 4);
        }
    }
}
