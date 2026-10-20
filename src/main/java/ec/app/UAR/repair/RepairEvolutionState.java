package ec.app.UAR.repair;

import ec.simple.SimpleEvolutionState;

/**
 * 维度修复进化状态
 * 保存修复器（每一个线程一个），以便执行维度修复算子
 * */

public class RepairEvolutionState extends SimpleEvolutionState {
    public DimensionRepairer[] repairer;
    @Override
    public void startFresh() {
        repairer = new DimensionRepairer[this.breedthreads];
        for (int i = 0; i < this.breedthreads; i++)
            repairer[i] = new DimensionRepairer(this, i);
        super.startFresh();
    }
}
