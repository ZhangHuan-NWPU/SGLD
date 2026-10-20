package ec.app.UAR.multipopulation;

import ec.EvolutionState;
import ec.Population;
import ec.app.UAR.modal.ModalEvolutionState;

/**
 * 多种群是求解多任务优化的一类策略，具体而言，在初始化的时候，为每一个任务创建一个种群
 * 每一个种群中的个体负责解决这个种群的任务，也只评估在这个任务上的表现
 * 该策略使用单源交叉，根据概率决定是否跨种群交叉，若交叉的父母来自不同种群
 * 则抛弃主要遗传物质不是这个种群的后代
 */

public class MPPopulation extends Population {

    @Override
    public void populate(EvolutionState state, int thread) {
        ModalEvolutionState state_ = (ModalEvolutionState) state;

        super.populate(state, thread);

        for(int x = 0; x< this.subpops.size(); x++)
            ((MPSubpopulation) this.subpops.get(x)).task_to_evaluate = state_.getTasks().get(x);
    }
}
