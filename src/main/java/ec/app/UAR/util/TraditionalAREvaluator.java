package ec.app.UAR.util;

import ec.app.UAR.simulator.Replication;
import ec.app.UAR.modal.Task;
import ec.util.MersenneTwisterFast;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class TraditionalAREvaluator {
    public ArrayList<Task> tasks = Task.initialize_72();

    /** IBFI规则*/
    public double[] IBFI(double m, double v) {
        int n = (int) (210 / m);
        double[] plan = new double[n];
        plan[0] = 0.0;
        for (int i = 1; i < n; i++) {
            plan[i] = plan[i-1] + m;
        }
        return plan;
    }

    /** 2BEG规则*/
    public double[] TwoBEG(double m, double v) {
        int n = (int) (210 / m);
        double[] plan = new double[n];
        plan[0] = 0.0;
        plan[1] = 0.0;
        for (int i = 2; i < n; i++) {
            plan[i] = plan[i-1] + m;
        }
        return plan;
    }

    /** MBFI规则*/
    public double[] MBFI(double m, double v) {
        int n = (int) (210 / m);
        double[] plan = new double[n];
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                plan[i] = i * m;
            } else {
                plan[i] = (i - 1) * m;
            }
        }
        return plan;
    }

    /** OFFSET规则*/
    public double[] OFFSET(double m, double v) {
        int n = (int) (210 / m);
        if (n == 10) {
            double[] plan = new double[n];
            int k = 4;
            for (int i = 0; i < n; i++) {
                if (i <= k)
                    plan[i] = Math.max(0.0, i * m + 0.15 * (i - k) * v);
                else
                    plan[i] = i * m + 0.3 * (i - k) * v;
            }
            return plan;
        }
        else {
            double[] plan = new double[n];
            int k = 8;
            for (int i = 0; i < n; i++) {
                if (i <= k)
                    plan[i] = Math.max(0.0, i * m + 0.15 * (i - k) * v);
                else
                    plan[i] = i * m + 0.3 * (i - k) * v;
            }
            return plan;
        }
    }

    /** DOME98*/
    public double[] DOME98(double m, double v, double cv, double pn, double cr_) {
        int n = (int) (210 / m);
        double[] plan = new double[n];

        double cr = cr_ * 10;

        double term1 = 1.0263 * pn;
        double term2 = 0.4963 * Math.pow(cv, 0.5);
        double term3 = 0.3418 / Math.exp(0.25 * cr);
        double term4 = 0.0591 * (0.025 * cv * cr + 10) / Math.exp(0.025 * cv * cr);
        double term5 = 0.2726 * (0.6 + 0.002 * n * cr) / Math.exp(0.002 * n * cr);
        double term6 = 1.176;
        double bracket = term1 - term2 - term3 - term4 + term5 + term6;


        double k = Math.pow(-Math.log(bracket) / 0.61, 1.0 / 3.0);

        for (int i = 1; i <= n; i++) {
            plan[i - 1] = Math.max(0.0, k * (i - 1) * m - v * Math.sqrt(i));
        }
        return plan;
    }

    /** DOME12*/
    public double[] DOME12(double m, double v, double cv, double pn, double pw, double cr_) {
        int n = (int) (210 / m);
        double[] plan = new double[n];

        double cr = cr_ * 10;

        double term1 = 0.9973;
        double term2_1 = 0.005765 * cr * (1 - pn);
        double term2_2 = Math.pow(cr * (1 - pn), -0.3481);
        double term2 = 0.103 * (term2_1+ term2_2);
        double term3 = 0.10699 * Math.pow(cv, 1.257);
        double term4 = 0.627 * Math.pow(n * (1 - pn), -0.8579);
        double term5 = 0.007574 * (Math.pow(Math.abs(cr * (1 - pw) - 2.143), 0.9682) - 0.622 * cr * (1 - pw));
        double term6 = 0.004855 * Math.pow(cr, 0.8913);
        double bracket = term1 - term2 - term3 - term4 - term5 + term6;


        double k = Math.pow(bracket, -1.898);

        for (int i = 1; i <= n; i++) {
            double pi = (n + i) / (n - 1.0);
            plan[i - 1] = Math.max(0.0, k * (i - 1) * m - v * Math.sqrt(i) * pi);
        }
        return plan;
    }

    /**NEW RULE*/
    public double[] NEW_RULE(double m, double v, double cv, double pn, double pw, double cr) {
        int n = (int) (210 / m);
        double[] plan = new double[n];

        plan[0] = 0.0;
        for (int i = 1; i < n; i++) {
            plan[i] = plan[i - 1] + m + (1.0/n - 1.0/i) * Math.sqrt(cr) * v;
        }
        return plan;
    }

    public double[] NEW_RULE_V2(double m, double v, double cv, double pn, double pw, double cr) {
        int n = (int) (210 / m);
        double[] plan = new double[n];

        plan[0] = 0.0;
        for (int i = 1; i < n; i++) {
            double term1 = (m / i) * (0.15619 * (i - 1) - cr - 2 * cr * (n - cr) * pn);
            double term2 = (1.0 / n - 1.0 / i) * Math.sqrt(cr) * v;
            plan[i] = plan[i - 1] + m + Math.max(term1, term2);
        }
        return plan;
    }

    public double[] RULE7(double m, double v, double cv, double pn, double pw, double cr) {
        int n = (int) (210 / m);
        double[] plan = new double[n];
        plan[0] = 0.0;
        // plan[1] = 0.0;
        for (int i = 1; i < n; i++) {
            plan[i] = plan[i-1] + m + 0.3 * v;
            if (plan[i] > 210)
                plan[i] = plan[i-1];
        }
        return plan;
    }

    public double[] run() {
        double[] result = new double[72];
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            double[] plan = RULE7(task.M, task.V, task.CV, task.PN, task.PW, task.CR);
            System.out.println(Arrays.toString(plan));
            MersenneTwisterFast random = new MersenneTwisterFast(1);
            Replication rep = new Replication(random, task, plan);

            int simulation_times = 15000;

            double[] WAIT_result = new double[simulation_times];
            double[] IDLE_result = new double[simulation_times];
            double[] OVER_result = new double[simulation_times];

            for (int j = 0; j < simulation_times; j++) {
                rep.run();
                WAIT_result[j] = rep.WAIT;
                IDLE_result[j] = rep.IDLE;
                OVER_result[j] = rep.OVER;
            }

            double WAIT = Arrays.stream(WAIT_result).average().orElse(0.0);
            double IDLE = Arrays.stream(IDLE_result).average().orElse(0.0);
            double OVER = Arrays.stream(OVER_result).average().orElse(0.0);

            result[i] = WAIT + task.CR * (10 * IDLE + 15 * OVER);
        }
        return result;
    }

    public static void main(String[] args) {
        TraditionalAREvaluator evaluator = new TraditionalAREvaluator();
        // 运行
        double[] obj = evaluator.run();

        // 创建一个新的Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        // 创建一个工作表
        Sheet sheet = workbook.createSheet("result");
        // 创建表头
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        Cell headerCell2 = headerRow.createCell(1);
        headerCell1.setCellValue("Scenarios");
        headerCell2.setCellValue("Obj");


        // 填充数据
        for (int i = 0; i < evaluator.tasks.size(); i++) {
            Row row = sheet.createRow(i + 1); // 数据从第2行开始
            Cell cell1 = row.createCell(0);
            Cell cell2 = row.createCell(1);
            cell1.setCellValue(evaluator.tasks.get(i).toString());
            cell2.setCellValue(obj[i]);
        }

        // 自动调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        // 保存到文件
        try (FileOutputStream fileOut = new FileOutputStream("result_RULE7.xlsx")) {
            workbook.write(fileOut);
            System.out.println("Excel file created successfully.");
        } catch (IOException ignored) { }

        // 关闭工作簿
        try {
            workbook.close();
        } catch (IOException ignored) { }
    }
}
