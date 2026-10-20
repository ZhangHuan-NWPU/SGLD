package ec.app.UAR.IDS;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.DoubleData;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.ModalProblem;
import ec.app.UAR.modal.Task;
import ec.gp.GPProblem;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;

import java.util.HashMap;

/**
 * 案例选择是一种旨在选择种群中专家个体的选择方式，其特点为：全局池，随机排序和精英
 * 即每一次选择都以整个种群为候选，对所有任务或案例随机排序，逐个任务地评估个体的表现
 * 仅有最佳的一部分个体可以进行下一轮选择
 * 本类是降采样案例选择，案例列表的长度只有原来的0.25倍
 * */

public class DSLexicaseProblem extends ModalProblem implements SimpleProblemForm {

    @Override
    public void evaluate(EvolutionState state_, Individual ind_, int subpopulation, int threadnum) {

        ModalEvolutionState state = (ModalEvolutionState) state_;

        IDSLexicaseEvaluator evaluator = (IDSLexicaseEvaluator) state.evaluator;

        UARIndividual ind = (UARIndividual) ind_;

        DoubleData input = (DoubleData) this.input;

        if (!ind.evaluated) {

            /* 产生个体的预约计划，同时完成维度设置
             * 目前版本采用单树个体，表达式为A_i = A_i-1 + M + Tree
             * 其中Tree为个体的树结构的值，表示患者预约间隔的修正项
             * 现在不是直接计算患者的预约时间，而是首先计算患者的预约间隔
             * 最后推算患者的预约时间
             * */
            ind.set_appointment_plan_and_dimension(state, threadnum, input, this);

            ind.objective = new HashMap<>();
            // 目前采用的综合适应度为GAP
            double integrated_fitness = 0.0;
            // 根据案例列表进行评估
            for (Task task: evaluator.case_list) {
                double cf = simulate(state.random[threadnum], task, ind);
                ind.objective.put(task, cf);
                integrated_fitness += ( state.getFrontier(task) - cf ) / state.getFrontier(task);
            }

            integrated_fitness = integrated_fitness / state.getTasks().size();

            /* 确定综合适应度 */
            SimpleFitness f = (SimpleFitness) ind.fitness;
            f.setFitness(state, integrated_fitness,false);
            // 设置个体状态为已评估
            ind.evaluated = true;
        }
    }

    /** 本函数的功能是对个体进行全面评估，输入个体应已具备预约计划*/
    public static void full_evaluate(ModalEvolutionState state, UARIndividual ind, int threadnum) {

        GPProblem problem = (GPProblem) state.evaluator.p_problem;
        DoubleData input = (DoubleData) problem.input;
        ind.set_appointment_plan_and_dimension(state, threadnum, input, problem);

        /* 对每一个任务进行评估 */
        ind.objective = new HashMap<>();
        // 目前采用的综合适应度为GAP
        double integrated_fitness = 0.0;
        for (Task task: state.getTasks()) {
            double cf = simulate(state.random[threadnum], task, ind);
            ind.objective.put(task, cf);
            integrated_fitness += ( state.getFrontier(task) - cf ) / state.getFrontier(task);
        }
        integrated_fitness = integrated_fitness / state.getTasks().size();

        /* 确定综合适应度 */
        SimpleFitness f = (SimpleFitness) ind.fitness;
        f.setFitness(state, integrated_fitness,false);
        // 设置个体状态为已评估
        ind.evaluated = true;
    }
}
