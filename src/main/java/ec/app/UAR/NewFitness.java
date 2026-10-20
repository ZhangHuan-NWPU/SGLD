package ec.app.UAR;
import ec.Fitness;
import ec.simple.SimpleFitness;

/**
 * 重写后的适应度
 * 浮点数，越小越好
 * 适用于多种问题，包括预约调度问题的综合适应度
 * */

public class NewFitness extends SimpleFitness {
    @Override
    public boolean betterThan(Fitness _fitness) {
        return ((NewFitness)_fitness).fitness() > fitness();
    }
}
