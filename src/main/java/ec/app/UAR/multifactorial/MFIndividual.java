package ec.app.UAR.multifactorial;


import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairIndividual;
import java.util.HashMap;
/**
 * 多因子优化（Multifactorial Optimization, MFO）是用于多任务优化的一种技术
 * 其核心包括 4 个属性
 * 1）Factorial Cost：个体在每一个任务上的目标值
 * 2）Factorial Rank：个体在每一个任务上，相对种群中所有个体中的排名
 * 3）Skill Factor：个体最擅长的任务
 * 4）Scalar Fitness：个体的技能因子对应的排名的倒数（即排名越高数值越大）
 * 其中 Factorial Cost 即 UARIndividual 的 Objectives，而 Scalar Fitness 就是适应度
 * 因此本类只定义了 2 和 3
 * */

public class MFIndividual extends RepairIndividual {

    /** Factorial Rank：对于特定任务，本个体在种群中所有个体中的排名 */
    public HashMap<Task, Integer> factorial_rank = new HashMap<>();

    /** Skill Factor：本个体最擅长的任务 */
    public Task skill_factor = null;
}
