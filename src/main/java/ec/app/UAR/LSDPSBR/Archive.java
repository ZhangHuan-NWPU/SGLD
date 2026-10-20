package ec.app.UAR.LSDPSBR;

import ec.app.UAR.DoubleData;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.gp.GPProblem;
import ec.simple.SimpleFitness;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Archive {
    public final ConcurrentHashMap<Task, CaseArchive> case_archives;

    /** 本函数的功能是构造函数，新建每一个任务的档案库*/
    public Archive(ModalEvolutionState state) {
        case_archives = new ConcurrentHashMap<>();
        for (Task task: state.getTasks()) {
            case_archives.put(task, new CaseArchive(task));
        }
    }

    /** 本函数的功能是向档案库中添加个体，记录其每一个任务的调度和目标值*/
    public void add(ModalEvolutionState state, UARIndividual ind) {
        for (Task task: state.getTasks()) {
            if (ind.appointment_plan.containsKey(task) && ind.objective.containsKey(task)) {
                double[] plan = ind.appointment_plan.get(task);
                double objective = ind.objective.get(task);
                case_archives.get(task).add(new Entry(task, plan, objective));
            }
        }
    }
    /** 本函数的功能是使用1NN预测和评估个体的表现*/
    public void ONE_NN(ModalEvolutionState state, UARIndividual ind, int thread) {

        GPProblem problem = (GPProblem) state.evaluator.p_problem;

        DoubleData input = (DoubleData) problem.input;

        ind.set_appointment_plan_and_dimension(state, thread, input, problem);

        /* 对每一个任务进行评估 */
        ind.objective = new HashMap<>();
        double integrated_fitness = 0.0;
        // 目前采用的综合适应度为GAP
        for (Task task: state.getTasks()) {
            double[] plan = ind.appointment_plan.get(task);
            double cf = case_archives.get(task).get_nearest_entry(plan);
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

    /** 本函数的功能是使用1NN预测个体的表现，在每一个任务上的平均表现*/
    public double ONE_NN_for_generalist(ModalEvolutionState state, UARIndividual ind, int thread) {

        GPProblem problem = (GPProblem) state.evaluator.p_problem;

        DoubleData input = (DoubleData) problem.input;

        ind.set_appointment_plan_and_dimension(state, thread, input, problem);

        double result = 0.0;
        int i = 0;
        for (Task task: state.getTasks()) {
            if (ind.appointment_plan.containsKey(task)) {
                i++;
                double[] plan = ind.appointment_plan.get(task);

                double cf = case_archives.get(task).get_nearest_entry(plan);
                result += ( state.getFrontier(task) - cf ) / state.getFrontier(task);
            }
        }
        if (i < state.getTasks().size()) System.out.println("警告：评估使用的任务数量小于72！！！");
        return result / i;
    }

    /** 本函数的功能是使用1NN预测个体的表现，在最擅长的一个任务上的表现*/
    public double ONE_NN_for_specialist(ModalEvolutionState state, UARIndividual ind, int thread) {

        GPProblem problem = (GPProblem) state.evaluator.p_problem;

        DoubleData input = (DoubleData) problem.input;

        ind.set_appointment_plan_and_dimension(state, thread, input, problem);

        double result = 0.0;
        int i = 0;
        for (Task task: state.getTasks()) {
            if (ind.appointment_plan.containsKey(task)) {
                i++;
                double[] plan = ind.appointment_plan.get(task);

                double cf = case_archives.get(task).get_nearest_entry(plan);
                cf= ( state.getFrontier(task) - cf ) / state.getFrontier(task);
                if (cf > result) result = cf;
            }
        }
        if (i < state.getTasks().size()) System.out.println("警告：评估使用的任务数量小于72！！！");
        return result;
    }

    public int size() {
        int temp = 0;
        for (CaseArchive case_archive: case_archives.values())
            temp += case_archive.entries.size();
        return temp;
    }
}

/** 本类的功能是记录某任务下的计划和表现*/
class CaseArchive {
    public final Task task;
    public final ArrayList<Entry> entries;

    public CaseArchive(Task task) {
        this.task = task;
        this.entries = new ArrayList<>();
    }

    public synchronized void add(Entry entry) {
        for (Entry member: entries) {
            if (hamming_distance(entry.plan(), member.plan()) == 0)
                return;
        }
        this.entries.add(entry);
    }

    public double get_nearest_entry(double[] input) {
        double best_o = 99999.9;
        double best_d = 999999.9;
        for (Entry entry: entries) {
            double distance = euclidean_distance(input, entry.plan());
            if (distance < best_d) {
                best_d = distance;
                best_o = entry.objective();
            }
        }
        return best_o;
    }

    public double euclidean_distance(double[] plan_1, double[] plan_2) {
        if (plan_1.length != plan_2.length)
            return 99999.9;
        double distance = 0.0;
        for (int i = 0; i < plan_1.length; i++)
            distance += (plan_1[i] - plan_2[i]) * (plan_1[i] - plan_2[i]);
        return Math.sqrt(distance);
    }

    public double hamming_distance(double[] plan_1, double[] plan_2) {
        if (plan_1.length != plan_2.length)
            return 99999.9;
        double distance = 0.0;
        for (int i = 0; i < plan_1.length; i++)
            distance += Math.abs(plan_1[i] - plan_2[i]);
        return distance;
    }
}

/** 本类的功能是记录某任务下的某个计划和表现*/
record Entry(Task task, double[] plan, double objective) {
    Entry(Task task, double[] plan, double objective) {
        this.task = task;
        this.plan = plan.clone();
        this.objective = objective;
    }
}
