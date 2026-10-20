package ec.app.UAR.LSDPSBR;

import ec.EvolutionState;
import ec.Individual;
import ec.app.UAR.DPS.DPSIndividual;
import ec.app.UAR.DPS.DPSNewEvaluator;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairEvolutionState;
import ec.breed.MultiBreedingPipeline;
import ec.gp.GPIndividual;
import ec.gp.GPInitializer;
import ec.gp.GPNode;
import ec.gp.GPTree;
import ec.gp.koza.CrossoverPipeline;
import ec.util.IntBag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class DPSBRCrossoverPipeline extends CrossoverPipeline {

    @Override
    public int produce(final int min,
                       final int max,
                       final int subpopulation,
                       final ArrayList<Individual> inds,
                       final EvolutionState state,
                       final int thread, HashMap<String, Object> misc)

    {
        BREvolutionState state_ = (BREvolutionState) state;

        int start = inds.size();

        // how many individuals should we make?
        int n = typicalIndsProduced();
        if (n < min) n = min;
        if (n > max) n = max;


        // should we bother?
        if (!state.random[thread].nextBoolean(likelihood))
        {
            // just load from source 0 and clone 'em
            sources[0].produce(n,n,subpopulation,inds, state,thread,misc);
            return n;
        }

        IntBag[] parentparents = null;
        IntBag[] preserveParents = null;
        if (misc!=null&&misc.get(KEY_PARENTS) != null)
        {
            preserveParents = (IntBag[])misc.get(KEY_PARENTS);
            parentparents = new IntBag[2];
            misc.put(KEY_PARENTS, parentparents);
        }

        GPInitializer initializer = ((GPInitializer)state.initializer);

        for(int q=start;q<n+start; /* no increment */) { // keep on going until we're filled up
            parents.clear();

            DPSIndividual parent1 = null;
            DPSIndividual parent2 = null;
            // 首先确认第一个父亲
            sources[0].produce(1, 1, subpopulation, parents, state, thread, misc);
            parent1 = (DPSIndividual) parents.get(0);

            // 决定是否繁殖重组
            boolean allow_br = true;
            // 根据概率决定是否交叉
            int tries = 0;
            boolean loop = true;
            while (loop) {
                parents.clear();

                sources[1].produce(1, 1, subpopulation, parents, state, thread, misc);
                parent2 = (DPSIndividual) parents.get(0);

                tries++;

                double hit_of_parent1 = parent1.normalization_case_fitness.values().stream().reduce(0.0, Double::sum);
                double hit_of_parent2 = parent2.normalization_case_fitness.values().stream().reduce(0.0, Double::sum);

                if (hit_of_parent1 < hit_of_parent2) {
                    DPSIndividual temp = parent1;
                    parent1 = parent2;
                    parent2 = temp;
                }

                if (allow_crossover(state_, parent1, parent2)) {
                    loop = false;
                    allow_br = true;
                    state_.partner_num[state.generation] ++;
                };

                int pop_size = state.population.subpops.get(subpopulation).initialSize;

                if (tries >= (pop_size / 2)) {
                    loop = false;
                    MultiBreedingPipeline source = (MultiBreedingPipeline) state.population.subpops.get(subpopulation).species.pipe_prototype;
                    source.sources[0].probability -= 1.0 / pop_size;
                    source.sources[1].probability += 1.0 / pop_size;
                }
            }
            state_.crossover_num[state.generation] ++;
            state_.try_num[state.generation] += tries;

            // 繁殖重组
            // 定义一个比较器，根据 ONE_NN(state, ind) 的值对个体进行排序
            Comparator<GPIndividual> ind_comparator_for_generalist = new Comparator<GPIndividual>() {
                @Override
                public int compare(GPIndividual ind1, GPIndividual ind2) {
                    double score1 = state_.archive.ONE_NN_for_generalist(state_, (UARIndividual) ind1, thread);
                    double score2 = state_.archive.ONE_NN_for_generalist(state_, (UARIndividual) ind2, thread);
                    return Double.compare(score2, score1); // 降序排序，值越大越好
                }
            };

            Comparator<GPIndividual> ind_comparator_for_specialist = new Comparator<GPIndividual>() {
                @Override
                public int compare(GPIndividual ind1, GPIndividual ind2) {
                    double score1 = state_.archive.ONE_NN_for_specialist(state_, (UARIndividual) ind1, thread);
                    double score2 = state_.archive.ONE_NN_for_specialist(state_, (UARIndividual) ind2, thread);
                    return Double.compare(score2, score1); // 降序排序，值越大越好
                }
            };

            // 定义繁殖规模
            int br_size;
            if (allow_br) br_size = 7;
            else br_size = 1;

            // 进行多次交叉
            ArrayList<GPIndividual> all_offspring = new ArrayList<>();
            for (int i = 0; i < br_size; i++) {
                all_offspring.addAll(Arrays.asList(crossover(state_, thread, subpopulation, parent1, parent2)));
            }
            // System.out.println(all_offspring.size());
            double ratio = (double) state.generation / state.numGenerations;
            if (state.random[thread].nextDouble() > ratio)
                all_offspring.sort(ind_comparator_for_specialist);
            else
                all_offspring.sort(ind_comparator_for_generalist);

            // 选择最好的两个个体
            ArrayList<GPIndividual> offspring = new ArrayList<>();
            if (all_offspring.size() >= 2) {
                offspring.add(all_offspring.get(0)); // 最好的个体
                offspring.add(all_offspring.get(1)); // 第二好的个体
            } else {
                // 如果个体数量不足两个，直接将所有个体加入
                offspring.addAll(all_offspring);
            }

            for (GPIndividual ind: offspring) {
                // ((DPSIndividual) ind).repair((RepairEvolutionState) state, thread);
                inds.add(ind);
                if (preserveParents != null) {
                    parentparents[0].addAll(parentparents[1]);
                    preserveParents[q] = parentparents[0];
                }
                q++;
                if (q>=n+start) break;
            }
        }
        return n;
    }

    /** 本函数的功能是交叉*/
    private GPIndividual[] crossover(ModalEvolutionState state, int thread, int subpopulation, GPIndividual ind_1, GPIndividual ind_2) {
        GPInitializer initializer = ((GPInitializer)state.initializer);

        // pick random trees  -- their GPTreeConstraints must be the same

        int tree_index = state.random[thread].nextInt(ind_1.trees.length);
        // int t1 = tree_index;
        // int t2 = tree_index;

        // validity results...
        boolean res1 = false;
        boolean res2 = false;

        // prepare the nodeselectors
        nodeselect1.reset();
        nodeselect2.reset();

        // pick some nodes
        GPNode p1 = null;
        GPNode p2 = null;

        for(int x = 0; x < numTries; x++) {
            // pick a node in individual 1
            p1 = nodeselect1.pickNode(state,subpopulation,thread,ind_1,ind_1.trees[tree_index]);

            // pick a node in individual 2
            p2 = nodeselect2.pickNode(state,subpopulation,thread,ind_2,ind_2.trees[tree_index]);

            // check for depth and swap-compatibility limits
            res1 = verifyPoints(initializer,p2,p1);  // p2 can fill p1's spot -- order is important!
            res2 = verifyPoints(initializer,p1,p2);  // p1 can fill p2's spot -- order is important!

            // did we get something that had both nodes verified?
            if (res1 && res2) break;
        }

        // Create some new individuals based on the old ones -- since
        // GPTree doesn't deep-clone, this should be just fine.  Perhaps we
        // should change this to proto off of the main species prototype, but
        // we have to then copy so much stuff over; it's not worth it.

        GPIndividual j1 = ind_1.lightClone();
        GPIndividual j2 = ind_2.lightClone();

        // Fill in various tree information that didn't get filled in there
        j1.trees = new GPTree[ind_1.trees.length];
        j2.trees = new GPTree[ind_2.trees.length];

        // at this point, p1 or p2, or both, may be null.
        // If not, swap one in.  Else just copy the parent.

        for(int x=0;x<j1.trees.length;x++) {
            // we've got a tree with a kicking cross position!
            if (x==tree_index && res1) {
                j1.trees[x] = ind_1.trees[x].lightClone();
                j1.trees[x].owner = j1;
                j1.trees[x].child = ind_1.trees[x].child.cloneReplacing(p2, p1);
                j1.trees[x].child.parent = j1.trees[x];
                j1.trees[x].child.argposition = 0;
                j1.evaluated = false;
            }  // it's changed
            else {
                j1.trees[x] = ind_2.trees[x].lightClone();
                j1.trees[x].owner = j1;
                j1.trees[x].child = (GPNode) ind_2.trees[x].child.clone();
                j1.trees[x].child.parent = j1.trees[x];
                j1.trees[x].child.argposition = 0;
            }
        }

        for(int x=0;x<j2.trees.length;x++) {
            // we've got a tree with a kicking cross position!
            if (x == tree_index && res2) {
                j2.trees[x] = ind_2.trees[x].lightClone();
                j2.trees[x].owner = j2;
                j2.trees[x].child = ind_2.trees[x].child.cloneReplacing(p1, p2);
                j2.trees[x].child.parent = j2.trees[x];
                j2.trees[x].child.argposition = 0;
                j2.evaluated = false;
            } // it's changed
            else {
                j2.trees[x] = ind_1.trees[x].lightClone();
                j2.trees[x].owner = j2;
                j2.trees[x].child = (GPNode) ind_1.trees[x].child.clone();
                j2.trees[x].child.parent = j2.trees[x];
                j2.trees[x].child.argposition = 0;
            }
        }
        ((DPSIndividual) j1).repair((RepairEvolutionState) state, thread);
        ((DPSIndividual) j2).repair((RepairEvolutionState) state, thread);
        return new GPIndividual[] {j1, j2};
    }

    public boolean allow_crossover(ModalEvolutionState state, DPSIndividual recipient, DPSIndividual doner) {

        // 计算
        double advantage = 0.0;
        double sum_doner = 0.0;
        double ncf_recipient_worse = 0.0;
        double num_doner_better = 0.0;
        for (Task task: state.getTasks()) {
            double d = doner.normalization_case_fitness.get(task);
            double r = recipient.normalization_case_fitness.get(task);
            if (d > r) advantage += (d - r);
            if (d > r) num_doner_better++;
            if (d > r) ncf_recipient_worse += r;
            sum_doner += d;
        }

        // double alpha = (advantage / num_doner_better) / (1.0 - (ncf_recipient_worse / num_doner_better));
        // double beta = (state.getTasks().size() - num_doner_better) / state.getTasks().size();
        // double alpha = advantage / ncf_recipient_worse;
        // double beta = num_doner_better / state.getTasks().size();

        // double q1 = ((DPSNewEvaluator) state.evaluator).q1;
        return (sum_doner / state.getTasks().size() > 0.5); // best
        // return (alpha > beta) || (sum_doner / state.getTasks().size() > 0.5);
        // return (alpha > beta);
    }
}
