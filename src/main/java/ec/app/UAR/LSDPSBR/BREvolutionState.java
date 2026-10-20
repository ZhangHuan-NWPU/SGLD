package ec.app.UAR.LSDPSBR;

import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.Task;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 初始化档案库和禁忌的进化状态
 * */

public class BREvolutionState extends ModalEvolutionState {
    public final Archive archive = new Archive(this);
    public final int br_size = 7;
}
