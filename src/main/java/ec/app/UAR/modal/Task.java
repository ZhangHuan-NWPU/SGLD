package ec.app.UAR.modal;

import java.util.ArrayList;

/**
 * 任务类
 * 表示进化系统中需完成的任务，即算例
 * 任务内容为预约调度，保存预约调度算例的参数
 * 总时长，患者数量，期望时长，标准差，变异系数，爽约率，无预约患者到达率
 */

public class Task{

    /* 记录预约调度任务的参数，包括总时长，患者数量，期望时长，标准差，变异系数，爽约率，无预约患者到达率*/
    final public double T = 210.0;
    final public int N;
    final public double M;
    final public double V;
    final public double CV;
    final public double PN;
    final public double PW;
    final public double CR;

    /* 构造函数 */
    public Task(double m, double cv, double pn, double pw, double cr) {
        N = (int) (this.T / m);
        M = m;
        V = m * cv;
        CV = cv;
        PN = pn;
        PW = pw;
        CR = cr;
    }

    /* 初始化，产生72个算例 */
    public static ArrayList<Task> initialize_72() {
        ArrayList<Task> instances = new ArrayList<Task>();

        double[] Ms = {21.0, 10.5};
        double[] CVs = {0.4, 0.6, 0.8};
        double[] PNs = {0.0, 0.15};
        double[] PWs = {0.0, 0.15};
        double[] CRs = {0.1, 0.5, 1.0};

        for (double m : Ms) {
            for (double cv : CVs) {
                for (double pn : PNs) {
                    for (double pw : PWs) {
                        for (double cr: CRs) {
                            instances.add(new Task(m, cv, pn, pw, cr));
                        }
                    }
                }
            }
        }

        return instances;
    }

    public static ArrayList<Task> initialize_24() {
        ArrayList<Task> instances = new ArrayList<Task>();

        double[] Ms = {21.0, 10.5};
        double[] CVs = {0.4, 0.6, 0.8};
        double[] PNs = {0.0, 0.15};
        double[] PWs = {0.0, 0.15};

        for (double m : Ms) {
            for (double cv : CVs) {
                for (double pn : PNs) {
                    for (double pw : PWs) {
                        instances.add(new Task(m, cv, pn, pw, 0.1));
                    }
                }
            }
        }

        return instances;
    }

    @Override
    public String toString() {
        return String.format("<M=%.1f,CV=%.1f,PN=%.2f,PW=%.2f,CR=%.1f>", M, CV, PN, PW, CR);
    }
}
