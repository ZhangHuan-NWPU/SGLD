package ec.app.UAR.multipopulation;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.DoubleData;
import ec.app.UAR.NewFitness;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.ModalProblem;
import ec.app.UAR.modal.Task;
import ec.app.UAR.simulator.Replication;
import ec.simple.SimpleProblemForm;
import ec.util.MersenneTwisterFast;

import java.util.Arrays;

/**
 * 多种群是求解多任务优化的一类策略，具体而言，在初始化的时候，为每一个任务创建一个种群
 * 每一个种群中的个体负责解决这个种群的任务，也只评估在这个任务上的表现
 * 该策略使用单源交叉，根据概率决定是否跨种群交叉，若交叉的父母来自不同种群
 * 则抛弃主要遗传物质不是这个种群的后代
 */

public class MPProblem extends ModalProblem implements SimpleProblemForm {

    @Override
    public void evaluate(EvolutionState state_, Individual ind_, int subpopulation, int threadnum) {
        ModalEvolutionState state = (ModalEvolutionState) state_;

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

            // 只评估对应子种群的任务
            Task task = ((MPSubpopulation) state.population.subpops.get(subpopulation)).task_to_evaluate;

            NewFitness f = (NewFitness) ind.fitness;

            f.setFitness(state, simulate(state.random[threadnum], task, ind),false);
            // 设置个体状态为已评估
            ind.evaluated = true;
        }
    }
}
