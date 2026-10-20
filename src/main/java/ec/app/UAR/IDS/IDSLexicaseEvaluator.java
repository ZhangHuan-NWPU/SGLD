package ec.app.UAR.IDS;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.gp.GPIndividual;
import ec.simple.SimpleEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 案例选择是一种旨在选择种群中专家个体的选择方式，其特点为：全局池，随机排序和精英
 * 即每一次选择都以整个种群为候选，对所有任务或案例随机排序，逐个任务地评估个体的表现
 * 仅有最佳的一部分个体可以进行下一轮选择
 * 本类是降采样案例选择，案例列表的长度只有原来的0.25倍
 * */

public class IDSLexicaseEvaluator extends SimpleEvaluator {

    // 记录本代的案例列表
    public ArrayList<Task> case_list = null;

    @Override
    public void evaluatePopulation(final EvolutionState state) {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        // 首先随机选择部分个体，进行全面评估
        ArrayList<Integer> index = random_select(state, 0, 0.05);

        ArrayList<UARIndividual> sample = new ArrayList<>();
        for (int i: index) {
            UARIndividual ind = (UARIndividual) state.population.subpops.get(0).individuals.get(i);
            DSLexicaseProblem.full_evaluate(state_, ind, 0);
            sample.add(ind);
        }

        // 设置当前代的案例列表
        //int size = (int) (state_.getTasks().size() * 0.25);
        case_list = get_case_list_intelligently(state_, sample, 0.25);

        super.evaluatePopulation(state);
    }

    /** 本函数的功能是随机选择size个任务*/
    public int[] get_case_list(final ModalEvolutionState state, int size) {
        // 定义随机数生成器
        // 使用HashSet来存储随机数，避免重复
        HashSet<Integer> tasks = new HashSet<>();

        // 循环直到HashSet中存储了size个随机数
        while (tasks.size() < size) {
            // 生成一个0到71之间的随机数
            int randomNumber = state.random[0].nextInt(state.getTasks().size()); // 0到71，共72个数
            tasks.add(randomNumber);
        }

        // 将HashSet转换为数组
        int[] result = new int[size];
        int index = 0;
        for (Integer t : tasks) result[index++] = t;

        return  result;
    }

    /** 本函数的功能是从当前种群中随机选择若干个体（索引）*/
    public ArrayList<Integer> random_select(EvolutionState state, int subpopulation, double size) {
        int pop_size = state.population.subpops.get(subpopulation).individuals.size();
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < pop_size; i++) candidates.add(i);
        Collections.shuffle(candidates);
        return new ArrayList<>(candidates.subList(0, (int) (pop_size * size)));
    }

    /** 本函数的功能是根据若干已经被全面评估的个体确定最能甄别个体的任务清单*/
    public ArrayList<Task> get_case_list_intelligently(ModalEvolutionState state, ArrayList<UARIndividual> inds, double size) {
        // 确认求解向量
        HashMap<Task, double[]> solve_vectors = new HashMap<>();
        for (Task task: state.getTasks()) {
            double[] vector = new double[inds.size()];

            double max = -99999.9;
            double min = 999999.9;
            for (int i = 0; i < inds.size(); i++) {
                UARIndividual ind = inds.get(i);
                vector[i] = ind.objective.getOrDefault(task, 99999.9);

                if (vector[i] > max) max = vector[i];
                if (vector[i] < min) min = vector[i];
            }
            // 归一化
            for (int i = 0; i < inds.size(); i++) {
                vector[i] = (max - vector[i]) / (max - min);
            }
            solve_vectors.put(task, vector);
        }

        // 确定样本大小
        int sample_size = (int) (state.getTasks().size() * size);

        // 确定整个case list
        ArrayList<Task> case_list = new ArrayList<>();

        // 随机选择一个任务
        int task_index = state.random[0].nextInt(state.getTasks().size());
        case_list.add(state.getTasks().get(task_index));

        // 其余任务
        for (int i = 1; i < sample_size; i++) {
            Task best_task = null;
            double longest_distance = -99999.9;
            for (Task task: state.getTasks()) {
                if (!case_list.contains(task)) {
                    // 计算汉明距离
                    double distance = 0.0;
                    for (int j = 0; j < inds.size(); j++) {
                        double x = solve_vectors.get(case_list.get(i - 1))[j];
                        double y = solve_vectors.get(task)[j];
                        distance += Math.abs(x - y);
                    }

                    if (distance > longest_distance) {
                        longest_distance = distance;
                        best_task = task;
                    }
                }
            }
            case_list.add(best_task);
        }
        return case_list;
    }
}
