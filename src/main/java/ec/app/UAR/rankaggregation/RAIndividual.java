package ec.app.UAR.rankaggregation;

import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairIndividual;
import java.util.HashMap;

/**
 * Rank aggregation 策略采用了新的适应度计算方式
 * */

public class RAIndividual extends RepairIndividual {
    /** Rank：对于特定任务，本个体在种群中所有个体中的排名 */
    public HashMap<Task, Integer> rank = new HashMap<>();
}
