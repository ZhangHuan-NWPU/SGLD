package ec.app.UAR.LSDPSBR;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.DoubleData;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalProblem;
import ec.app.UAR.modal.Task;
import ec.simple.SimpleFitness;
import ec.simple.SimpleProblemForm;
import java.util.HashMap;

/**
 * 增加了储存档案库功能的问题
 * */


public class BRProblem extends ModalProblem implements SimpleProblemForm {

    @Override
    public void evaluate(final EvolutionState state_, Individual ind_, int subpopulation, int threadnum) {

        BREvolutionState state = (BREvolutionState) state_;

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
            // 设置个体状态为未选中
            ind.selected = false;

            // 保存信息
            state.archive.add(state, ind);
            // System.out.println(state.archive.size());
        }
    }
}
