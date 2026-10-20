package ec.app.UAR.multitask;

import ec.EvolutionState;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.ModalProblem;
import ec.app.UAR.modal.Task;
import ec.app.UAR.util.MapToPythonDictFormat;
import ec.gp.GPIndividual;
import ec.simple.SimpleProblemForm;
import ec.simple.SimpleStatistics;
import ec.steadystate.SteadyStateStatisticsForm;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

public class MultitaskStatistics extends SimpleStatistics implements SteadyStateStatisticsForm{

    // 记录当前时间
    public LocalDateTime start_time;
    // 记录当前搜索到的最优专家个体
    public HashMap<Task, UARIndividual> specialists;

    @Override
    public void preInitializationStatistics(final EvolutionState state) {
        super.preInitializationStatistics(state);
        start_time = LocalDateTime.now();
    }

    @Override
    public void postInitializationStatistics(final EvolutionState state) {
        super.postInitializationStatistics(state);
        specialists = new HashMap<>();
    }

    @Override
    // 报告专家个体，并进行重新评估
    public void finalStatistics(final EvolutionState state, final int result) {

        ModalEvolutionState state_ = (ModalEvolutionState) state;
        if (doFinal) state.output.println("Best Specialists of Run:",statisticslog);

        for (Task task: state_.getTasks()) {
            UARIndividual ind = specialists.get(task);

            ind.objective.put(task, ModalProblem.simulate(state.random[0], task, ind));

            if (doFinal) state.output.println("\nSpecialist on Task "+task.toString()+" of Run", statisticslog);
            if (doFinal) ind.printIndividualForHumans(state, statisticslog);
            if (doFinal) state.output.println("Plan:"+Arrays.toString(ind.appointment_plan.get(task)), statisticslog);
            if (doFinal) state.output.println("Objectives:"+ind.objective.get(task), statisticslog);
        }

        Duration dur = Duration.between(start_time, LocalDateTime.now());
        state.output.println("Time: "+dur.toString(), super.statisticslog);
    }

    @Override
    // 更新当前搜索到的最优专家个体，并输出
    public void postEvaluationStatistics(final EvolutionState state) {

        // 父类的父类的函数
        for (ec.Statistics child : children) child.postEvaluationStatistics(state);

        // 记录当前代的最优专家个体
        ModalEvolutionState state_ = (ModalEvolutionState) state;
        // 新建一个字典存储当前代的最优专家个体
        HashMap<Task, UARIndividual> best_i = new HashMap<>();
        // 遍历每一个个体
        for (int p = 0; p < state.population.subpops.size(); p++) {

            if (state.population.subpops.get(p).individuals.isEmpty())
                state.output.warnOnce("Null individuals found in subpopulation");

            for (int i = 1; i < state.population.subpops.get(p).individuals.size(); i++) {

                UARIndividual ind = (UARIndividual) state.population.subpops.get(p).individuals.get(i);

                // 遍历每一个任务，若个体在当前任务上的表现优于记录的个体则更新
                for (Task task: state_.getTasks()) {
                    if (!best_i.containsKey(task) || ind.objective.get(task) < best_i.get(task).objective.get(task))
                        best_i.put(task, ind);
                }
            }
        }

        // 对比当前代与全局的最优专家个体在每一个任务上的表现
        for (Task task: state_.getTasks()) {
            if (!specialists.containsKey(task) || best_i.get(task).objective.get(task) < specialists.get(task).objective.get(task))
                specialists.put(task, (UARIndividual) best_i.get(task).clone());
        }

        // 报告全局的最优专家个体
        if (doGeneration) state.output.println("\nGeneration: " + state.generation,statisticslog);
        if (doGeneration) state.output.println("Best Specialists:",statisticslog);

        for (Task task: state_.getTasks()) {
            UARIndividual ind = specialists.get(task);
            if (doGeneration) state.output.println("\nSpecialist on Task "+task.toString()+" at Generation "+state.generation, statisticslog);
            if (doGeneration) ind.printIndividualForHumans(state, statisticslog);
            if (doGeneration) state.output.println("Plan:"+Arrays.toString(ind.appointment_plan.get(task)), statisticslog);
            if (doGeneration) state.output.println("Objectives:"+ind.objective.get(task), statisticslog);
        }
    }
}
