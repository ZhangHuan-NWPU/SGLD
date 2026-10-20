package ec.app.UAR;

import ec.EvolutionState;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;

import java.util.Arrays;
import java.util.HashMap;

/**
 * UAR个体，适用于通用预约规则设计
 * 集成预约计划设置，维度设置，长度与综合适应度的帕累托支配，重复检查等功能
 * 其中预约计划，目标值都是针对每一个任务的，类型是HashMap
 * */

public class UARIndividual extends GPIndividual {

    /** 统计参数 */
    public boolean selected;

    /** 核心参数*/
    // 任务与预约计划：保存预约时间的数组，索引是患者，值是时间
    public HashMap<Task, double[]> appointment_plan = new HashMap<>();
    // 程序长度
    private int length;
    // 目标值：个体在每一个任务上的表现
    public HashMap<Task, Double> objective = new HashMap<>();
    // 维度差：目前的GP可以维持维度一致性，因此已经不需要
    public double modified_dimension_gap;
    // 任务：临时存储当前预约计划的任务，辅助预约计划设置
    public Task current_task;
    // i：临时存储预约计划中的患者索引，辅助预约计划设置
    public int i;

    /** 本函数的功能是设置个体在所有任务上的预约计划，同时完成维度设置工作*/
    public void set_appointment_plan_and_dimension (final EvolutionState state,
                                                    int threadnum,
                                                    DoubleData input,
                                                    GPProblem problem)
    {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        this.appointment_plan = new HashMap<>();

        // 对于每一个任务，设置预约计划
        for (Task task: state_.getTasks()) {
            set_appointment_plan(state_, threadnum, input, problem, task);
        }

        // 设置个体的维度和维度差
        for (int i = 0; i < this.trees.length; i++) {
            UARNode tree = (UARNode) this.trees[i].child;
            this.modified_dimension_gap += tree.getDimension_gap() + (Math.abs(tree.getDimension() - 1.0));
        }
    }

    /** 本函数的功能是设置个体在特定任务上的的预约计划*/
    public void set_appointment_plan(final EvolutionState state,
                                     int threadnum,
                                     DoubleData input,
                                     GPProblem problem,
                                     Task task) {
        // 设置当前任务
        current_task = task;
        // 新建一个计划数组
        double[] plan = new double[current_task.N];
        // 设置第一个患者的时间为0.0
        plan[0] = 0.0;
        // 设置前一个患者的时间为0.0
        double previous_time = 0.0;

        for (i = 1; i < current_task.N; i++) {
            this.trees[0].child.eval(state, threadnum, input, problem.stack, this, problem);
            /*
            * 目前版本采用单树个体，表达式为A_i = A_i-1 + M + Tree
            * 其中Tree为个体的树结构的值，表示患者预约间隔的修正项
            * 现在不是直接计算患者的预约时间，而是首先计算患者的预约间隔
            * 最后推算患者的预约时间
            * */
            double t = previous_time + current_task.M + input.x;
            // 保证预约计划的可行性
            if (t < previous_time)
                t = previous_time;
            else if (t > 210) {
                t = previous_time;
            }
            plan[i] = t;
            previous_time = t;
        }

        // 设置预约计划
        appointment_plan.put(current_task, plan);
    }

    /** 本函数的功能是设置个体在所有任务上的预约计划，同时完成维度设置工作*/
    public void set_appointment_plan_and_dimension_2trees (final EvolutionState state,
                                                           int threadnum,
                                                           DoubleData input,
                                                           GPProblem problem)
    {
        ModalEvolutionState state_ = (ModalEvolutionState) state;
        // 对于每一个任务，设置预约计划
        for (Task task: state_.getTasks()) {
            set_appointment_plan_2trees(state_, threadnum, input, problem, task);
        }

        // 设置个体的维度和维度差
        for (int i = 0; i < this.trees.length; i++) {
            UARNode tree = (UARNode) this.trees[i].child;
            this.modified_dimension_gap += tree.getDimension_gap() + (Math.abs(tree.getDimension() - 1.0));
        }
    }

    /** 本函数的功能是设置个体在特定任务上的的预约计划（双树表示）*/
    public void set_appointment_plan_2trees(final EvolutionState state,
                                            int threadnum,
                                            DoubleData input,
                                            GPProblem problem,
                                            Task task) {
        // 设置当前任务
        current_task = task;
        // 新建一个计划数组
        double[] plan = new double[current_task.N];
        // 设置第一个患者的时间为0.0
        plan[0] = 0.0;
        // 设置前一个患者的时间为0.0
        double previous_time = 0.0;

        for (i = 1; i < current_task.N; i++) {
            this.trees[0].child.eval(state, threadnum, input, problem.stack, this, problem);
            double k = input.x;
            this.trees[1].child.eval(state, threadnum, input, problem.stack, this, problem);
            double v = input.x;
            double t  = previous_time + k * current_task.M + v;
            // 保证预约计划的可行性
            if (t < previous_time)
                t = previous_time;
            else if (t > 210) {
                t = previous_time;
            }
            plan[i] = t;
            previous_time = t;
        }

        // 设置预约计划
        appointment_plan.put(current_task, plan);
    }

    /** 本函数的功能是设置个体的程序长度，即包含节点的数量*/
    public void setLength() {
        length = (int) size();
    }

    /** 本函数的功能是获取个体的程序长度，即包含节点的数量*/
    public int getLength() {
        return length;
    }

    /** 本函数的功能是判断本个体是否能支配另一个个体other*/
    public boolean paretoDominates(UARIndividual other) {
        if (this.fitness.betterThan(other.fitness) && this.getLength() <= other.getLength())
            return true;
        else
            return false;
    }

    /**
     * 本函数的功能是判断本个体与另一个个体other是否重复
     * 因预约计划为向量，判断方式为余弦相似度
     * 结果为两个个体在每一个任务的预约计划的余弦相似度平均值
     * 满足重复的阈值为 1
     * */
    public boolean isDuplicateTo(UARIndividual other) {
        // 临时存储结果
        double[] results = new double[appointment_plan.size()];
        // 循环每一个任务，计算个体在每一个任务的预约计划的余弦相似度
        int i = 0;
        for (Task task: appointment_plan.keySet()) {
            results[i] = CosSimilarity(other, task);
            i++;
        }

        if (results.length == 0)
            System.out.println("isDuplicateTo错误：数组长度为0！");

        // 阈值为1.0
        return (Arrays.stream(results).sum() / results.length) == 1.0;
    }

    /** 计算余弦相似度
     * 其中的元素一定大于等于0，因此计算过程作出简化*/
    private double CosSimilarity(UARIndividual other, Task task) {
        if (!this.appointment_plan.containsKey(task))
            System.out.println("CosSimilarity错误：个体的预约计划尚未初始化！");

        if (!other.appointment_plan.containsKey(task))
            System.out.println("CosSimilarity错误：个体的预约计划尚未初始化！");

        double[] plan_1 = this.appointment_plan.get(task);
        double[] plan_2 = other.appointment_plan.get(task);

        double cosSimilarity = 0.0;
        for (int i = 0; i < plan_1.length; i++) {
            cosSimilarity += plan_1[i] * plan_2[i];
        }

        // 计算两个向量的模长
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < plan_1.length; i++) {
            norm1 += plan_1[i] * plan_1[i];
            norm2 += plan_2[i] * plan_2[i];
        }

        cosSimilarity = cosSimilarity / (Math.sqrt(norm1) * Math.sqrt(norm2));
        return cosSimilarity;
    }

    /** 本函数的功能是对个体进行深度克隆*/
    @Override
    public Object clone() {
        UARIndividual myObj= (UARIndividual) super.clone();

        myObj.appointment_plan = clonePlan();
        myObj.length = length;
        myObj.objective = cloneObjective();
        myObj.modified_dimension_gap = modified_dimension_gap;

        return myObj;
    }

    /** 对HashMap的深度克隆 */
    public HashMap<Task, double[]> clonePlan() {
        HashMap<Task, double[]> cloned = new HashMap<>();
        for (Task task: appointment_plan.keySet()) {
            cloned.put(task, appointment_plan.get(task).clone());
        }
        return cloned;
    }

    /** 对HashMap的深度克隆 */
    public HashMap<Task, Double> cloneObjective() {
        HashMap<Task, Double> cloned = new HashMap<>();
        for (Task task: objective.keySet()) {
            cloned.put(task, objective.get(task));
        }
        return cloned;
    }
}
