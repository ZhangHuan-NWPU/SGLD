package ec.app.UAR.LSDPSBR;

import ec.app.UAR.DoubleData;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.gp.GPIndividual;
import ec.gp.GPProblem;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Comparator;

public class BroodRecombination {

    public final BREvolutionState state;
    public final int thread;
    public final ArrayList<GPIndividual> inds;
    public ArrayList<Triple<UARIndividual, Task, Double>> sorted_inds;

    public BroodRecombination(BREvolutionState state, int thread, ArrayList<GPIndividual> inds) {
        this.state = state;
        this.thread = thread;
        this.inds = inds;
        sorted_inds = new ArrayList<>();
    }

    public void evaluate_for_generalist() {
        sorted_inds.clear();
        for (GPIndividual ind: inds) {
            UARIndividual ind_ = (UARIndividual) ind;
            double fitness = state.archive.ONE_NN_for_generalist(state, ind_, thread);
            sorted_inds.add(Triple.of(ind_, null, fitness));
        }
    }

    public void evaluate_for_specialist() {
        sorted_inds.clear();
        for (GPIndividual ind: inds) {
            UARIndividual ind_ = (UARIndividual) ind;
            double fitness = state.archive.ONE_NN_for_specialist(state, ind_, thread);
            sorted_inds.add(Triple.of(ind_, null, fitness));
        }
    }

    public void evaluate_for_specialist_with_skill_factor() {
        sorted_inds.clear();
        for (GPIndividual ind: inds) {
            UARIndividual ind_ = (UARIndividual) ind;
            Triple<UARIndividual, Task, Double> x = ONE_NN(state, ind_, thread);
            sorted_inds.add(x);
        }
    }

    /** 本函数的功能是使用1NN预测个体的表现，在最擅长的一个任务上的表现，使用tabu*/
    public Triple<UARIndividual, Task, Double> ONE_NN(BREvolutionState state, UARIndividual ind, int thread) {

        GPProblem problem = (GPProblem) state.evaluator.p_problem;

        DoubleData input = (DoubleData) problem.input;

        ind.set_appointment_plan_and_dimension(state, thread, input, problem);

        double result = 0.0;
        Task skill_factor = null;
        for (Task task: state.getTasks()) {
            if (ind.appointment_plan.containsKey(task)) {
                double[] plan = ind.appointment_plan.get(task);

                double cf = state.archive.case_archives.get(task).get_nearest_entry(plan);
                cf= ( state.getFrontier(task) - cf ) / state.getFrontier(task);
                if (cf > result) {
                    result = cf;
                    skill_factor = task;
                }
            }
        }
        return Triple.of(ind, skill_factor, result);
    }

    public static ArrayList<UARIndividual> get_generalist(BREvolutionState state, int thread, ArrayList<GPIndividual> inds) {
        // 建立BR
        BroodRecombination br = new BroodRecombination(state, thread, inds);

        // 评估和排序
        br.evaluate_for_generalist();
        br.sorted_inds.sort(Comparator.comparing(Triple<UARIndividual, Task, Double>::getRight).reversed());

        // 提取结果
        ArrayList<UARIndividual> offspring = new ArrayList<>();
        offspring.add(br.sorted_inds.get(0).getLeft()); // 最好的个体
        offspring.add(br.sorted_inds.get(1).getLeft()); // 第二好的个体
        return offspring;
    }

    public static ArrayList<UARIndividual> get_specialist(BREvolutionState state, int thread, ArrayList<GPIndividual> inds) {
        // 建立BR
        BroodRecombination br = new BroodRecombination(state, thread, inds);

        // 评估和排序
        br.evaluate_for_specialist();
        br.sorted_inds.sort(Comparator.comparing(Triple<UARIndividual, Task, Double>::getRight).reversed());

        // 提取结果
        ArrayList<UARIndividual> offspring = new ArrayList<>();
        offspring.add(br.sorted_inds.get(0).getLeft()); // 最好的个体
        offspring.add(br.sorted_inds.get(1).getLeft()); // 第二好的个体
        return offspring;
    }

    public static ArrayList<UARIndividual> get_unique_specialist(BREvolutionState state, int thread, ArrayList<GPIndividual> inds) {
        // 建立BR
        BroodRecombination br = new BroodRecombination(state, thread, inds);

        // 评估和排序
        br.evaluate_for_specialist_with_skill_factor();
        br.sorted_inds.sort(Comparator.comparing(Triple<UARIndividual, Task, Double>::getRight).reversed());

        // 提取结果
        ArrayList<UARIndividual> offspring = new ArrayList<>();
        offspring.add(br.sorted_inds.get(0).getLeft()); // 最好的个体
        Task skill_factor = br.sorted_inds.get(0).getMiddle();

        for (int i = 1; i < br.sorted_inds.size(); i++) {
            Triple<UARIndividual, Task, Double> x = br.sorted_inds.get(i);
            if (!skill_factor.equals(x.getMiddle())) {
                offspring.add(br.sorted_inds.get(i).getLeft()); // 第二好的个体
                break;
            }
        }

        return offspring;
    }
}