package ec.app.UAR.multifactorial;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.DoubleData;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.ModalProblem;
import ec.app.UAR.modal.Task;
import ec.app.UAR.simulator.Replication;
import ec.simple.SimpleProblemForm;
import ec.util.MersenneTwisterFast;

import java.util.Arrays;

/**
 * 多因子优化（Multifactorial Optimization, MFO）是用于多任务优化的一种技术
 * 在MFO中，每一个个体仅根据其技能因子在一个任务上进行评估，而未评估任务的目标值被设定为正无穷
 * 当个体没有技能因子时（比如刚初始化时），那么就在所有任务上评估
 * 因为标量适应度是越大越好，需使用simple fitness
 * */

public class MFProblem extends ModalProblem implements SimpleProblemForm {

    @Override
    public void evaluate(EvolutionState state_, Individual ind_, int subpopulation, int threadnum) {
        ModalEvolutionState state = (ModalEvolutionState) state_;

        MFIndividual ind = (MFIndividual) ind_;

        DoubleData input = (DoubleData) this.input;

        if (!ind.evaluated) {

            /* 产生个体的预约计划，同时完成维度设置
             * 目前版本采用单树个体，表达式为A_i = A_i-1 + M + Tree
             * 其中Tree为个体的树结构的值，表示患者预约间隔的修正项
             * 现在不是直接计算患者的预约时间，而是首先计算患者的预约间隔
             * 最后推算患者的预约时间
             * */
            ind.set_appointment_plan_and_dimension(state, threadnum, input, this);

            /* 若个体没有技能因子时（比如刚初始化时），则就在所有任务上评估*/
            if (ind.skill_factor == null) {
                // 对每一个任务进行评估
                for (Task task: state.getTasks()) {
                    ind.objective.put(task, simulate(state.random[threadnum], task, ind));
                }
            }
            /* 若个体具有技能因子，根据其技能因子在一个任务上进行评估，而未评估任务的目标值被设定为正无穷*/
            else {
                for (Task task: state.getTasks()) {
                    ind.objective.put(task, 999999.9);
                }
                ind.objective.put(ind.skill_factor, simulate(state.random[threadnum], ind.skill_factor, ind));
            }

            // 设置个体状态为已评估
            ind.evaluated = true;
        }
    }
}
