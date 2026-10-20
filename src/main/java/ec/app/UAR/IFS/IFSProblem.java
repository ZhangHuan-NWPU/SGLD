package ec.app.UAR.IFS;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.DoubleData;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.app.UAR.simulator.Replication;
import ec.app.UAR.util.MapToPythonDictFormat;
import ec.gp.GPProblem;
import ec.simple.SimpleProblemForm;
import ec.util.MersenneTwisterFast;

import java.util.Arrays;
import java.util.HashMap;

/**
 * 隐式适应度共享（Implicit fitness sharing， IFS） 是一种调整适应度的选择策略
 * 本研究采用的是连续模式下的适应度共享，分母是所有个体在该任务的表现总和
 * 适应度越大越好
 * */


public class IFSProblem extends GPProblem implements SimpleProblemForm {

    @Override
    public void evaluate(final EvolutionState state_, Individual ind_, int subpopulation, int threadnum) {

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

            /* 对每一个任务进行评估 */
            ind.objective = new HashMap<>();
            for (Task task: state.getTasks()) {
                ind.objective.put(task, simulate(state.random[threadnum], task, ind));
            }

            /* 确定综合适应度 */
            // SimpleFitness f = (SimpleFitness) ind.fitness;
            // 目前采用的综合适应度的值为平均值
            // double integrated_fitness = ind.objective.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            // f.setFitness(state, integrated_fitness,false);
            // 设置个体状态为已评估
            ind.evaluated = true;
        }
    }

    @Override
    // 描述个体在每一个任务上的预约计划和目标
    public void describe(EvolutionState state, Individual ind_, int subpopulation, int threadnum, int log) {
        UARIndividual ind = (UARIndividual) ind_;

        state.output.println("\nPlans:"+ MapToPythonDictFormat.output(ind.appointment_plan), log);
        state.output.println("\nObjectives:"+ MapToPythonDictFormat.output(ind.objective), log);
    }

    /** 本函数的功能是通过仿真评估，返回个体的适应度*/
    public static double simulate(MersenneTwisterFast random, Task task, UARIndividual ind) {
        Replication rep = new Replication(random, task, ind.appointment_plan.get(task));

        int simulation_times = 15000;

        double[] WAIT_result = new double[simulation_times];
        double[] IDLE_result = new double[simulation_times];
        double[] OVER_result = new double[simulation_times];

        for (int i = 0; i < simulation_times; i++) {
            rep.run();
            WAIT_result[i] = rep.WAIT;
            IDLE_result[i] = rep.IDLE;
            OVER_result[i] = rep.OVER;
        }

        double WAIT = Arrays.stream(WAIT_result).filter(Double::isFinite).average().orElse(0.0);
        double IDLE = Arrays.stream(IDLE_result).filter(Double::isFinite).average().orElse(0.0);
        double OVER = Arrays.stream(OVER_result).filter(Double::isFinite).average().orElse(0.0);

        return (WAIT + task.CR * (10.0 * IDLE + 15.0 * OVER));
    }
}
