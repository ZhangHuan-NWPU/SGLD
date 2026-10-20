package ec.app.UAR.DPS;

import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairIndividual;

import java.util.HashMap;

public class DPSIndividual extends RepairIndividual {
    public HashMap<Task, Double> normalization_case_fitness = new HashMap<>();
}
