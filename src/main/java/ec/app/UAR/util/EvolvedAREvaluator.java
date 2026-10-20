package ec.app.UAR.util;

import ec.app.UAR.DoubleData;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.modal.ModalEvolutionState;
import ec.app.UAR.modal.ModalProblem;
import ec.app.UAR.modal.Task;
import ec.app.UAR.simulator.Replication;
import ec.util.MersenneTwisterFast;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class EvolvedAREvaluator {
    public ArrayList<Task> tasks;
    public ModalEvolutionState state;

    public EvolvedAREvaluator() {
        state = new ModalEvolutionState();
        // state.tasks = Task.initialize_72();
        tasks = state.getTasks();
    }

    public double[] run(UARIndividual ind) {


        ind.set_appointment_plan_and_dimension(state, 0, new DoubleData(), new ModalProblem());

        double[] result = new double[72];
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            double[] plan = ind.appointment_plan.get(task);

            MersenneTwisterFast random = new MersenneTwisterFast(10);
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

            double WAIT = Arrays.stream(WAIT_result).filter(Double::isFinite).average().orElse(0.0);
            double IDLE = Arrays.stream(IDLE_result).filter(Double::isFinite).average().orElse(0.0);
            double OVER = Arrays.stream(OVER_result).filter(Double::isFinite).average().orElse(0.0);

            result[i] = WAIT + task.CR * (10.0 * IDLE + 15.0 * OVER);
        }
        return result;
    }

    public ArrayList<String> read_rules(String path) throws IOException {
        ArrayList<String> rules = new ArrayList<>();

        FileInputStream fis = new FileInputStream(path);
        Workbook workbook = new XSSFWorkbook(fis);
        // 获取第一个工作表
        Sheet sheet = workbook.getSheetAt(0);

        // 遍历每一行
        for (Row row : sheet) {
            Cell cell = row.getCell(0);
            if (cell != null) rules.add(cell.getStringCellValue());
        }
        return rules;
    }

    public static void main(String[] args) throws IOException {
        EvolvedAREvaluator evaluator = new EvolvedAREvaluator();

        ArrayList<String> rules = evaluator.read_rules(args.length > 0 ? args[0] : "src/main/java/ec/app/UAR/util/rules.xlsx");

        // 创建一个新的Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        // 创建一个工作表
        Sheet sheet = workbook.createSheet("result");
        // 创建表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < evaluator.tasks.size(); i++) {
            Cell headerCell = headerRow.createCell(i + 1);
            headerCell.setCellValue(evaluator.tasks.get(i).toString());
        }

        // 填充数据
        for (int j = 0; j < rules.size(); j++) {
            ArrayList<String> expressions = new ArrayList<>();
            expressions.add(rules.get(j));
            UARIndividual ind = RuleParser.parseIndividual(expressions);

            double[] results = evaluator.run(ind);

            Row row = sheet.createRow(j + 1); // 数据从第2行开始
            row.createCell(0).setCellValue(j + 1);

            for (int i = 0; i < evaluator.tasks.size(); i++) {

                Cell cell = row.createCell(i + 1);
                cell.setCellValue(results[i]);
            }
        }

        // 保存到文件
        try (FileOutputStream fileOut = new FileOutputStream("results.xlsx")) {
            workbook.write(fileOut);
            System.out.println("Excel file created successfully.");
        } catch (IOException ignored) { }

        // 关闭工作簿
        try {
            workbook.close();
        } catch (IOException ignored) { }
    }
}
