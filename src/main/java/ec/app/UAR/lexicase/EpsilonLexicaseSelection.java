package ec.app.UAR.lexicase;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.select.LexicaseSelection;
import ec.select.SelectDefaults;
import ec.util.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 案例选择是一种旨在选择种群中专家个体的选择方式，其特点为：全局池，随机排序和精英
 * 即每一次选择都以整个种群为候选，对所有任务或案例随机排序，逐个任务地评估个体的表现
 * 仅有最佳的一部分个体可以进行下一轮选择
 * 本类实现了epsilon-案例选择，适用于无明确下界的连续目标值，可以略微放宽差距
 * */

public class EpsilonLexicaseSelection extends LexicaseSelection {

    public static final String P_LEXICASESELECT = "epsilon-lexicaseselect";

    @Override
    public Parameter defaultBase() { return SelectDefaults.base().push(P_LEXICASESELECT); }

    @Override
    public int produce(final int subpopulation, final EvolutionState state_, final int thread) {

        ModalEvolutionState state = (ModalEvolutionState) state_;

        final ArrayList<Individual> pop = state.population.subpops.get(subpopulation).individuals;

        // 初始化种群
        final ArrayList<Integer> candidates = new ArrayList<Integer>();

        // 初始化候选个体列表
        for (int i = 0; i < pop.size(); i++)
            candidates.add(i);

        final int num_cases = state.getTasks().size();

        final int[] case_order = EpsilonLexicaseSelection.shuffle(state, thread, num_cases);

        // 遍历每一个案例
        for (int i = 0; i < case_order.length; i++) {

            Task current_case = state.getTasks().get(case_order[i]);

            // 寻找该案例上的个体的最佳目标值和中位绝对偏差
            double[] fitness_list = new double[candidates.size()];

            double best = 99999.9;

            for (int j = 0; j < candidates.size(); j++) {
                double case_fitness = ((UARIndividual) pop.get(candidates.get(j))).objective.get(current_case);
                fitness_list[j] = case_fitness;
                if (case_fitness < best) best = case_fitness;
            }

            double mad = median_absolute_deviation(fitness_list);

            // 计算阈值，因为是最小化问题，阈值增加为 best + mad
            double threshold = best + mad;

            // Reduce candidates to the subset that performs best on the current test case
            final Iterator<Integer> it = candidates.iterator();
            while (it.hasNext()) {
                double case_fitness = ((UARIndividual) pop.get(it.next())).objective.get(current_case);
                if (case_fitness > threshold) // if strictly worse than best
                    it.remove();
            }

            // If only one individual is left, return it
            if (candidates.size() == 1) {
                return candidates.get(0);
            }

            // If this was the last test case, return a random candidate
            if (i == case_order.length - 1) {
                return candidates.get(state.random[0].nextInt(candidates.size()));
            }
        }
        throw new IllegalStateException();
    }

    /** 本函数的功能是计算中位绝对偏差*/
    private static double median_absolute_deviation (double[] array) {

        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array is empty or null");
        }

        int n = array.length;

        // 计算中位数
        Arrays.sort(array);
        double med;
        if (n % 2 == 1) med =  array[n / 2];
        else med =  (array[n / 2 - 1] + array[n / 2]) / 2.0;

        // 计算每一个数与中位数的差的绝对值
        double[] absolute_deviation_array = new double[n];
        for (int i = 0; i < n; i++) absolute_deviation_array[i] = Math.abs(array[i] - med);

        // 计算绝对偏差数量的中位数
        Arrays.sort(absolute_deviation_array);
        if (n % 2 == 1) return absolute_deviation_array[n / 2];
        else return (absolute_deviation_array[n / 2 - 1] + absolute_deviation_array[n / 2]) / 2.0;
    }

    /** 本函数的功能是对一个长度为size且内容是0-size-1的列表洗牌*/
    public static int[] shuffle(final EvolutionState state, int thread, int size) {
        int[] res = new int[size];

        for (int i = 0; i < size; i++)
            res[i] = i;

        // 使用Fisher-Yates洗牌算法
        for (int i = size - 1; i > 0; i--) {
            int j = state.random[thread].nextInt(i+1); // 生成0到i之间的随机索引
            // 交换res[i]和res[j]
            int temp = res[i];
            res[i] = res[j];
            res[j] = temp;
        }

        return  res;
    }
}
