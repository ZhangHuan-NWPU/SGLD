package ec.app.UAR.DPS;

import ec.EvolutionState;
import ec.Individual;
import ec.Subpopulation;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class DPSNewEvaluator extends SimpleEvaluator {

    public double q1 = 0.25;
    public double q2 = 0.50;
    public double q3 = 0.75;

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
            double max = -99999.9;
            double min = 99999.9;

            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind : subpop.individuals) {
                    double cf = ((DPSIndividual) ind).objective.get(task);

                    if (cf > max) max = cf;
                    if (cf < min) min = cf;
                }
            }

            // 计算正则适应度（目标值最小者为1.目标值最大者为0）
            for (Subpopulation subpop: state_.population.subpops) {
                for (Individual ind : subpop.individuals) {
                    DPSIndividual ind_ = (DPSIndividual) ind;
                    double cf = ind_.objective.get(task);

                    double ncf = (max - cf) / (max - min);

                    ind_.normalization_case_fitness.put(task, ncf);
                }
            }
        }
        // End

        // 计算q1，q2，q3
        ArrayList<Double> normalization_case_fitness = new ArrayList<>();
        for (Subpopulation subpop: state_.population.subpops) {
            for (Individual ind : subpop.individuals) {
                DPSIndividual ind_ = (DPSIndividual) ind;
                double ncf = ind_.normalization_case_fitness.values().stream().reduce(0.0, Double::sum) / state_.getTasks().size();
                normalization_case_fitness.add(ncf);
            }
        }

        median(normalization_case_fitness);
    }

    public void median(ArrayList<Double> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // 对列表进行排序
        list.sort(Comparator.comparingDouble(Double::doubleValue).reversed());

        int size = list.size();

        // 计算中位数 Q2
        if (size % 2 == 0) {
            q2 = (list.get(size / 2 - 1) + list.get(size / 2)) / 2.0;
        } else {
            q2 = list.get(size / 2);
        }

        // 计算 Q1 和 Q3
        int lowerHalfEnd = (size % 2 == 0) ? size / 2 - 1 : size / 2;
        int upperHalfStart = (size % 2 == 0) ? size / 2 : size / 2 + 1;

        // 计算 Q1（前半部分的中位数）
        int lowerSize = lowerHalfEnd + 1;
        if (lowerSize % 2 == 0) {
            q1 = (list.get(lowerSize / 2 - 1) + list.get(lowerSize / 2)) / 2.0;
        } else {
            q1 = list.get(lowerSize / 2);
        }

        // 计算 Q3（后半部分的中位数）
        int upperSize = size - upperHalfStart;
        if (upperSize % 2 == 0) {
            q3 = (list.get(upperHalfStart + upperSize / 2 - 1) + list.get(upperHalfStart + upperSize / 2)) / 2.0;
        } else {
            q3 = list.get(upperHalfStart + upperSize / 2);
        }
    }
}
