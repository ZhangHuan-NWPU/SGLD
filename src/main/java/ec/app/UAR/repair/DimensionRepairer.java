package ec.app.UAR.repair;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import ec.EvolutionState;
import ec.app.UAR.DoubleData;
import ec.app.UAR.util.RuleParser;
import ec.app.UAR.UARNode;
import ec.app.UAR.functions.*;
import ec.app.UAR.util.GrowBulider;
import ec.gp.*;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 维度修复器是DAGP中的一个工具，功能是修复一棵树的维度。
 * 在不更改树结构的前提下，对树做出最小程度的改变。
 * 使树的内部符合维度一致性，且输出符合目标维度。
 * 作者：张桓
 * */

public class DimensionRepairer {
    /** 进化状态*/
    public EvolutionState state;
    /** 线程，GRB的环境不是线程安全的，必须每个线程一个 */
    public int thread;

    /** Gurobi环境*/
    GRBEnv env;

    /** 维度运算类型，共有5类*/
    public static int[] DIM_OPERATOR = {0, 1, 2, 3, 4};

    /** 维度取值类型，共有2类*/
    public static int[] DIM_TYPE = {0, 1};

    /** 对应每一类终端的维度取值*/
    public static Map<Integer, Double> DIM_VALUE = Map.of(0, 0.0, 1, 1.0);

    /** 构造函数，新建一个GRB环境，储存进化状态和线程信息*/
    public DimensionRepairer(EvolutionState state, int thread) {
        try {
            env = new GRBEnv(true);
            env.start();
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
        this.state = state;
        this.thread = thread;
    }

    /** 修复，输入一棵树（其节点必须是UARNode），以及目标维度，输出修改后的树*/
    public void repair(GPTree tree, double target) throws GRBException {
        RepairModel model;
        while (true) {

            //PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out), true);
            //tree.printTree(new EvolutionState(), writer);

            // 记录树的根节点
            UARNode root = (UARNode) tree.child;

            /* 计算树内各个节点的维度差，及总维度差 */
            DoubleData data = new DoubleData();
            GPProblem problem = (GPProblem) state.evaluator.p_problem;
            ((RepairIndividual) tree.owner).set_appointment_plan_and_dimension(state, thread, data, problem);
            // root.eval(state, thread, data, problem.stack, tree.owner, problem);
            double modified_dimension_gap = root.getDimension_gap() + Math.abs(root.getDimension() - target);

            /* 当总维度差为0时，说明树不存在维度不合理情况，直接结束维修*/
            if (modified_dimension_gap == 0)
                return;

            /* 建立模型求解 */
            model = new RepairModel(env, tree, target);

            if (model.model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL)
                break;
            else
                mutate(tree, target);
        }

        /* 建立取值类型到节点的映射 */
        GPFunctionSet functionSet = tree.constraints((GPInitializer) state.initializer).functionset;
        // 导出终端集合
        UARNode[] terminalSet = Arrays.stream(functionSet.terminals).flatMap(Arrays::stream).
                map(node -> (UARNode) node).toArray(UARNode[]::new);

        // 建立取值类型到终端的映射
        HashMap<Integer, ArrayList<UARNode>> typeToNode = new HashMap<>();
        for (int type: DIM_TYPE)
            typeToNode.put(type, new ArrayList<>());

        for (UARNode node: terminalSet) {
            int type = getDimType(node);
            if (typeToNode.containsKey(type)) {
                typeToNode.get(type).add(node);
            }
            else {
                typeToNode.put(type, new ArrayList<>());
                typeToNode.get(type).add(node);
            }
        }

        /* 基于优化结果对树进行修复 */
        for (UARNode node: model.nodeIndex.keySet()) {
            int i = model.nodeIndex.get(node);
            // 当节点需要改变
            if (model.x[i].get(GRB.DoubleAttr.X) == 1.0) {
                // 对于终端节点
                if (node.expectedChildren() == 0) {
                    // 寻找改变方式
                    int newType = -1;
                    for (int j: DIM_TYPE) {
                        if (model.z[i][j].get(GRB.DoubleAttr.X ) == 1.0) {
                            newType = j;
                            break;
                        }
                    }

                    if (newType == -1)
                        System.out.println("错误：变量z的取值中没有值等于1！");

                    // 改变
                    UARNode newNode = getNewTerminal(typeToNode, getDimType(node), newType);
                    newNode.argposition = node.argposition;
                    newNode.parent = node.parent;
                    if (node.parent instanceof GPTree)
                        ((GPTree) node.parent).child = newNode;
                    else ((GPNode) node.parent).children[node.argposition] = newNode;
                }

                // 对于函数节点
                else {
                    // 寻找改变方式
                    int newType = -1;
                    for (int j: DIM_OPERATOR) {
                        if (model.y[i][j].get(GRB.DoubleAttr.X ) == 1.0) {
                            newType = j;
                            break;
                        }
                    }

                    if (newType == -1)
                        System.out.println("错误：变量y的取值中没有值等于1！");

                    // 改变
                    UARNode newNode = getNewFunction(getDimOperator(node), newType);
                    newNode.argposition = node.argposition;
                    newNode.parent = node.parent;
                    if (newNode.children.length != node.children.length) {
                        System.out.println("错误：尝试改变函数，但是子节点数量不一致！");
                        System.out.print(newType);
                        System.out.print(node);
                        System.out.print(" to " + newNode);
                    }

                    for (int j = 0; j < newNode.children.length; j++) {
                        newNode.children[j] = node.children[j];
                        newNode.children[j].parent = newNode;
                    }

                    if (node.parent instanceof GPTree)
                        ((GPTree) node.parent).child = newNode;
                    else ((GPNode) node.parent).children[node.argposition] = newNode;
                }
            }
        }
        // 修复完毕

        // 清理模型
        model.dispose();
    }

    /** 本函数的功能为获取一个UARNode的维度运算类型*/
    public int getDimOperator(UARNode node) {
        if (node.expectedChildren() == 0)
            System.out.println("错误：输入节点非函数类型！");

        int type;
        switch (node.toString()) {
            case "+", "-", "max", "min": {
                type = 0;
                break;
            }
            case "*": {
                type = 1;
                break;
            }
            case "/": {
                type = 2;
                break;
            }
            case "sqrt": {
                type = 3;
                break;
            }
            default: {
                type = 4;
                break;
            }
        }
        return type;
    }

    /** 本函数的功能为获取一个UARNode的维度取值类型*/
    public int getDimType(UARNode node) {
        if (node.expectedChildren() != 0)
            System.out.println("错误：输入节点非终端类型！信息：" + node);

        int type;
        switch (node.toString()) {
            case "M", "V": {
                type = 1;
                break;
            }
            default: {
                type = 0;
                break;
            }
        }
        return type;
    }

    /**
     * 本函数的功能是给定旧运算类型和新运算类型，返回一个对应的新的UARNode函数
     * 逻辑是当变成1或2或3或4时，直接返回mul或div或sqrt或if即可
     * 当变成0时，若旧类型是1，返回add,若旧类型是2，返回sub
     * */
    public UARNode getNewFunction(int oldType, int newType) {
        if (IntStream.of(DIM_OPERATOR).noneMatch(value -> value == newType))
            System.out.println("错误：不合法的输入！函数名：getNewFunction");

        UARNode node;
        if (newType == 1) {
            node = new Mul();
        }
        else if (newType == 2) {
            node = new Div();
        }
        else if (newType == 3) {
            node = new Sqrt();
        }
        else if (newType == 4) {
            node = new If();
        }
        else {
            if (oldType == 1) {
                node = new Add();
            }
            else {
                node = new Sub();
            }
        }

        node.children = new GPNode[node.expectedChildren()];
        return node;
    }

    /**
     * 本函数的功能是给定旧取值类型和新取值类型，返回一个对应的新的UARNode终端
     * 逻辑是当新取值为0时，随机选择一个，当新取值为1时，随机选择一个
     * 根据终端的出现频率比例设置一个终端的选择概率
     * */
    public UARNode getNewTerminal(HashMap<Integer, ArrayList<UARNode>> typeToNode, int oldType, int newType) {
        // 合法性检查
        if (IntStream.of(DIM_TYPE).noneMatch(value -> value == newType))
            System.out.println("错误：不合法的输入！函数名：getNewTerminal");

        UARNode node;

        int index = state.random[thread].nextInt(typeToNode.get(newType).size());
        node = (UARNode) typeToNode.get(newType).get(index).lightClone();

        node.children = new GPNode[node.expectedChildren()];
        return node;

    }

    /**
     * 本函数的功能是对树进行变异，但是选择过程与变异算子不同
     * 仅选择存在维度差的节点
     * */
    public void mutate(GPTree tree, double target) {
        // 记录初始化器
        GPInitializer initializer = (GPInitializer) state.initializer;

        /* 列举树内各个存在维度差的节点 */
        ArrayList<UARNode> candidateNodes = new ArrayList<>();
        Iterator iterator = tree.child.iterator(GPNode.NODESEARCH_ALL);
        while (iterator.hasNext()) {
            UARNode node = (UARNode) iterator.next();
            if (node.getDimension_gap() > 0)
                candidateNodes.add(node);
        }

        UARNode root = (UARNode) tree.child;
        if (root.getDimension() != target && !candidateNodes.contains(root)) {
            candidateNodes.add(root);
        }

        /* 随机选择，验证合法性 */
        boolean res = false;

        UARNode p1 = null;
        UARNode p2 = null;

        for (int x = 0; x < 2; x++) {
            p1 = candidateNodes.get(state.random[thread].nextInt(candidateNodes.size()));

            p2 = (UARNode) GrowBulider.newRootedTree(state,
                                                        p1.parentType(initializer),
                                                        thread,
                                                        p1.parent,
                                                        tree.constraints(initializer).functionset,
                                                        p1.argposition);
            if (p1.atDepth() + p2.depth() <= 8) {
                res = true;
                break;
            }
        }

        /* 执行变异 */
        if (res) {
            p2.parent = p1.parent;
            p2.argposition = p1.argposition;
            if (p2.parent instanceof GPNode)
                ((GPNode) (p2.parent)).children[p2.argposition] = p2;
            else ((GPTree) (p2.parent)).child = p2;
        }
    }

    public static void main(String[] args) throws GRBException {
        DimensionRepairer repairer = new DimensionRepairer(new EvolutionState(), 0);

        //String s = "(+ M N)";
        String s = "(- (- (max i (/ (sqrt M) (max (+ PW N) CR))) (- (+ 2 PN) i)) (- V (max (+ M (* CR PN)) i)))";
        UARNode node = (UARNode) RuleParser.parseTree(s);
        GPTree tree = new GPTree();
        tree.child = node;
        tree.child.argposition = 0;
        node.parent = tree;

        repairer.repair(tree, 0);
    }
}
