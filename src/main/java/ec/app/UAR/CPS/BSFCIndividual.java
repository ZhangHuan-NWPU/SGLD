package ec.app.UAR.CPS;

import ec.app.UAR.modal.Task;
import ec.app.UAR.repair.RepairIndividual;

import java.util.HashMap;

public class BSFCIndividual extends RepairIndividual {
    /* BSFC 是个体的二进制字符串适应度表征，表示个体的表现优劣
     * 若个体的表现高于平均值，则为1，否则为0
     * */
    public HashMap<Task, Integer> bsfc = new HashMap<>();
}
