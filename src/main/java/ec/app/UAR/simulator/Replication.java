package ec.app.UAR.simulator;

import ec.app.UAR.modal.Task;
import ec.util.MersenneTwisterFast;
import sim.util.distribution.Exponential;
import sim.util.distribution.Normal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class Replication {
    // 输入
    // 仿真时长固定为210分钟
    public MersenneTwisterFast random;
    public final double M;
    public final double V;
    public final double PN;
    public final double PW;
    public final double[] appointment_plan;

    // 输出
    public double WAIT = 0.0;
    public double IDLE = 0.0;
    public double OVER = 0.0;

    public Replication(MersenneTwisterFast random, Task task, double[] appointment_plan) {
        this.random = random;
        this.M = task.M;
        this.V = task.V;
        this.PN = task.PN;
        this.PW = task.PW;
        this.appointment_plan = appointment_plan;
    }

    public void run() {
        this.WAIT = 0.0;
        this.IDLE = 0.0;
        this.OVER = 0.0;

        //Step1，实现对数正态分布和指数分布
        Exponential intervalGenerator = new Exponential(PW / M, random);
        double sigma2 = Math.log(1 + (V * V)/(M * M));
        double mu = Math.log(M) - 0.5 * sigma2;
        Normal timeGenerator = new Normal(mu, Math.sqrt(sigma2), random);

        //Step2，初始化患者等待队列，没有爽约的预约患者按照固定顺序接受服务
        LinkedList<Patient> patient_list = new LinkedList<>();
        for (double t : appointment_plan) {
            if (random.nextDouble() >= PN) {
                patient_list.add(new Patient(t, Math.exp(timeGenerator.nextDouble())));
            }
        }
        //记录实际到达的患者数量
        int patient_num = patient_list.size();

        //Step3，初始化未来事件列表，向未来事件列表添加开始服务第一个预约患者事件，添加第一个无预约患者到达事件
        PriorityQueue<Event> event_list = new PriorityQueue<>();
        if (!patient_list.isEmpty())
            event_list.add(new Event(1, patient_list.getFirst().arrival_time));
        if (PW > 0) {
            double temp = intervalGenerator.nextDouble();
            if (temp <= 210)
                event_list.add(new Event(2, temp));
        }

        //Step4，开始仿真
        double clock = 0.0;
        // 记录正在服务的患者
        while (!event_list.isEmpty()) {
            //时间推进
            Event e = event_list.poll();
            clock = e.time;

            //事件处理
            //当开始服务第一个预约患者
            if (e.type == 1) {
                // 获取患者p
                Patient p = patient_list.removeFirst();
                //记录患者的等待时间
                WAIT += clock - p.arrival_time;

                //向事件列表添加一个完成服务事件
                event_list.add(new Event(3, clock + p.service_time));

            } else if (e.type == 2) {
                //当无预约患者到达，向患者队列添加一个患者，最多为第3个
                if (patient_list.size() < 3)
                    patient_list.add(new Patient(clock, Math.exp(timeGenerator.nextDouble())));
                else
                    patient_list.add(3, new Patient(clock, Math.exp(timeGenerator.nextDouble())));
                //向事件列表添加一个无预约患者到达事件，当时钟小于等于210时
                double temp = clock + intervalGenerator.nextDouble();
                if (temp <= 210)
                    event_list.add(new Event(2, temp));
                //改变实际到达的患者数量
                patient_num++;

            } else {
                //向事件队列添加一个开始服务事件并记录医生的空闲时间
                if (!patient_list.isEmpty()) {
                    double temp = Math.max(0, patient_list.getFirst().arrival_time - clock);
                    event_list.add(new Event(1, clock + temp));
                    if (clock < 210)
                        IDLE += temp;
                } else {
                    if (clock < 210)
                        IDLE += Math.max(0, 210 - clock);
                }
            }
        }
        OVER += Math.max(0, clock - 210);

        WAIT = WAIT / patient_num;
        IDLE = IDLE / patient_num;
        OVER = OVER / patient_num;
    }

    private record Patient(double arrival_time, double service_time) {
        @Override
        public String toString() {
            return String.format("Patient{arrival at=%.2f, service time=%.2f}", arrival_time, service_time);
        }
    }

    //使用type表示事件类型，1为开始服务队列的第一个患者，2为无预约患者到达，3为完成服务
    private record Event(int type, double time) implements Comparable<Event> {

        @Override
        public int compareTo(Event e) {
            return Double.compare(this.time, e.time);
        }

        @Override
        public String toString() {
            return String.format("Event{type=%d, time=%.2f}", type, time);
        }
    }

    public static void main(String[] args) {
        double[] plan = {0, 21, 42, 63, 84, 105, 126, 147, 168, 189};
        //double[] plan = {0};

        Task task = new Task(21.0, 0.4, 0.0, 0.0, 0.1);
        Replication rep = new Replication(new MersenneTwisterFast(1), task, plan);
        int simulation_times = 15000;

        double[] WAIT_result = new double[simulation_times];
        double[] IDLE_result = new double[simulation_times];
        double[] OVER_result = new double[simulation_times];

        for (int i = 0; i < simulation_times; i++) {
            rep.run();
            WAIT_result[i] = rep.WAIT;
            IDLE_result[i] = rep.IDLE;
            OVER_result[i] = rep.OVER;
        }

        double WAIT = Arrays.stream(WAIT_result).average().orElse(0.0);
        double IDLE = Arrays.stream(IDLE_result).average().orElse(0.0);
        double OVER = Arrays.stream(OVER_result).average().orElse(0.0);

        double obj = WAIT + IDLE + 1.5*OVER;

        System.out.println(WAIT);
        System.out.println(IDLE);
        System.out.println(OVER);
        System.out.println(obj);
    }
}
