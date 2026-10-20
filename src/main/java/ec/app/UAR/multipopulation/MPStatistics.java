package ec.app.UAR.multipopulation;

import ec.EvolutionState;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.app.UAR.multitask.MultitaskStatistics;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 多种群是求解多任务优化的一类策略，具体而言，在初始化的时候，为每一个任务创建一个种群
 * 每一个种群中的个体负责解决这个种群的任务，也只评估在这个任务上的表现
 * 该策略使用单源交叉，根据概率决定是否跨种群交叉，若交叉的父母来自不同种群
 * 则抛弃主要遗传物质不是这个种群的后代
 */

public class MPStatistics extends MultitaskStatistics {

    @Override
    public void finalStatistics(final EvolutionState state, final int result) {

        ModalEvolutionState state_ = (ModalEvolutionState) state;
        if (doFinal) state.output.println("Best Specialists of Run:",statisticslog);

        for (Task task: state_.getTasks()) {
            UARIndividual ind = specialists.get(task);
            if (doFinal) state.output.println("\nSpecialist on Task "+task.toString()+" of Run", statisticslog);
            if (doFinal) ind.printIndividualForHumans(state, statisticslog);
            if (doFinal) state.output.println("Plan:"+Arrays.toString(ind.appointment_plan.get(task)), statisticslog);
            if (doFinal) state.output.println("Objectives:"+ind.fitness.fitness(), statisticslog);
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
        // 遍历每一个种群
        for (int p = 0; p < state.population.subpops.size(); p++) {

            MPSubpopulation subpop = (MPSubpopulation) state.population.subpops.get(p);
            Task task = subpop.task_to_evaluate;

            if (subpop.individuals.isEmpty())
                state.output.warnOnce("Null individuals found in subpopulation");

            // 若个体在当前任务上的表现优于记录的个体则更新
            for (int i = 1; i < subpop.individuals.size(); i++) {

                UARIndividual ind = (UARIndividual) subpop.individuals.get(i);

                if (!best_i.containsKey(task) || ind.fitness.betterThan(best_i.get(task).fitness))
                    best_i.put(task, ind);
            }
        }

        // 对比当前代与全局的最优专家个体在每一个任务上的表现
        for (Task task: state_.getTasks()) {
            if (!specialists.containsKey(task) || best_i.get(task).fitness.betterThan(specialists.get(task).fitness))
                specialists.put(task, (UARIndividual) best_i.get(task).clone());
        }

        // 报告全局的最优专家个体
        if (doGeneration) state.output.println("\nGeneration: " + state.generation,statisticslog);
        if (doGeneration) state.output.println("Best Specialists:",statisticslog);

        for (Task task: state_.getTasks()) {
            UARIndividual ind = specialists.get(task);
            if (doGeneration) state.output.println("\nSpecialist on Task "+task.toString()+" at Generation "+state.generation, statisticslog);
            if (doGeneration) ind.printIndividualForHumans(state, statisticslog);
            if (doGeneration) state.output.println("Plan:"+ Arrays.toString(ind.appointment_plan.get(task)), statisticslog);
            if (doGeneration) state.output.println("Objectives:"+ind.fitness.fitness(), statisticslog);
        }
    }
}
