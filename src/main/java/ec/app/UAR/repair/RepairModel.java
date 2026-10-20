package ec.app.UAR.repair;

import com.gurobi.gurobi.*;
import ec.app.UAR.UARNode;
import ec.gp.GPNode;
import ec.gp.GPTree;
import java.util.*;

public class RepairModel {
    GRBModel model;

    /** 维度运算类型，共有5类*/
    public static int[] DIM_OPERATOR = {0, 1, 2, 3, 4};

    /** 维度取值类型，共有2类*/
    public static int[] DIM_TYPE = {0, 1};

    /** 对应每一类终端的维度取值*/
    public static Map<Integer, Double> DIM_VALUE = Map.of(0, 0.0, 1, 1.0);

    /* 建立节点与变量的映射 */
    public HashMap<UARNode, Integer> nodeIndex;

    /* 建立函数节点的集合 */
    public ArrayList<UARNode> functions;

    /* 建立终端节点的集合 */
    public ArrayList<UARNode> terminals;

    /* 记录节点的数量 */
    public int size;

    /* 建立问题的参数，节点的深度 */
    public int[] depth;

    /* 建立问题的参数，变动节点的成本
     * 理论上应该是深度越深，成本越低，暂时采用的方案为cost = 1 / depth */
    public double[] cost;

    /* 建立问题的参数，节点的维度运算及终端取值类型，前者只适用于函数节点，后者只适用于终端节点
     * 但是一个节点不能既是函数类型又是终端类型，因此可以使用同一个数组容纳这些信息*/
    public int[] dimInfo;

    /* 建立变量字典 */
    // 二元变量
    public GRBVar[] x;
    public GRBVar[][] y;
    public GRBVar[][] z;
    // 实数变量
    public GRBVar[] dim;

    /* 足够大的数字 */
    public double M;

    public RepairModel(GRBEnv env, GPTree tree, double target) throws GRBException {

        /* 建立GRB模型 */
        model = new GRBModel(env);
        model.set(GRB.IntParam.LogToConsole, 0);

        /* 建立节点与变量的映射 */
        nodeIndex = new HashMap<>();

        /* 建立函数节点的集合 */
        functions = new ArrayList<>();

        /* 建立终端节点的集合 */
        terminals = new ArrayList<>();

        /* 对输入的树进行遍历，填充刚刚建立的映射和集合 */
        Iterator iterator = tree.child.iterator(GPNode.NODESEARCH_ALL);
        int index = 0;
        while (iterator.hasNext()) {
            UARNode node = (UARNode) iterator.next();
            nodeIndex.put(node, index);

            if (node.expectedChildren() == 0)
                terminals.add(node);
            else
                functions.add(node);

            index += 1;
        }

        /* 记录节点的数量 */
        size = nodeIndex.size();

        /* 建立问题的参数，节点的深度 */
        depth = new int[size];

        /* 建立问题的参数，变动节点的成本
         * 理论上应该是深度越深，成本越低，暂时采用的方案为cost = 1 / depth */
        cost = new double[size];

        /* 建立问题的参数，节点的维度运算及终端取值类型，前者只适用于函数节点，后者只适用于终端节点
         * 但是一个节点不能既是函数类型又是终端类型，因此可以使用同一个数组容纳这些信息*/
        int[] dimInfo = new int[size];

        /* 填充参数 */
        for (UARNode node : nodeIndex.keySet()) {
            depth[nodeIndex.get(node)] = node.atDepth() + 1;
            cost[nodeIndex.get(node)] = 10.0 / depth[nodeIndex.get(node)];
            if (node.expectedChildren() != 0)
                dimInfo[nodeIndex.get(node)] = getDimOperator(node);
            else
                dimInfo[nodeIndex.get(node)] = getDimType(node);
        }

        /* 建立变量字典 */
        // 二元变量
        x = new GRBVar[size];
        y = new GRBVar[size][DIM_OPERATOR.length];
        z = new GRBVar[size][DIM_TYPE.length];
        // 实数变量
        dim = new GRBVar[size];

        /* 建立变量 */
        for (int i : nodeIndex.values()) {
            x[i] = model.addVar(0, 1, 0, GRB.BINARY, "x_" + i);
            dim[i] = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, "dim_" + i);
            for (int j : DIM_OPERATOR) {
                y[i][j] = model.addVar(0, 1, 0, GRB.BINARY, "y_" + i + "," + j);
            }
            for (int j : DIM_TYPE) {
                z[i][j] = model.addVar(0, 1, 0, GRB.BINARY, "z_" + i + "," + j);
            }
        }

        /* 添加约束 */
        //
        M = Math.pow(2, tree.child.depth());

        //1. 每一个函数节点只能有1个运算类型
        for (UARNode func : functions) {
            GRBLinExpr leftExpr = new GRBLinExpr();
            int i = nodeIndex.get(func);
            for (int j : DIM_OPERATOR) {
                leftExpr.addTerm(1.0, y[i][j]);
            }
            model.addConstr(leftExpr, GRB.EQUAL, 1, "constr1_" + i);
        }

        //2. 禁止一个函数节点的不可能的运算方式
        for (UARNode func : functions) {
            int i = nodeIndex.get(func);
            for (int j : getDisabledDimOperatorSet(func)) {
                model.addConstr(y[i][j], GRB.EQUAL, 0, "constr2_" + i);
            }
        }

        //3. 每一个终端节点只能有1个维度类型
        for (UARNode term : terminals) {
            GRBLinExpr leftExpr = new GRBLinExpr();
            int i = nodeIndex.get(term);
            for (int j : DIM_TYPE) {
                leftExpr.addTerm(1.0, z[i][j]);
            }
            model.addConstr(leftExpr, GRB.EQUAL, 1, "constr3_" + i);
        }

        //4. 当x为0，即节点不变时，函数节点只能采用原来的运算；当x为1时函数节点可以采用任意运算
        for (UARNode func : functions) {
            int i = nodeIndex.get(func);
            GRBLinExpr leftExpr = new GRBLinExpr();
            leftExpr.addTerm(1.0, x[i]);
            leftExpr.addTerm(1.0, y[i][dimInfo[i]]);
            model.addConstr(leftExpr, GRB.GREATER_EQUAL, 1, "constr4_" + i);
        }

        //5. 当x为0，即节点不变时，终端节点只能保持原来的维度；当x为1时终端节点可以采用任意维度
        for (UARNode term : terminals) {
            int i = nodeIndex.get(term);
            GRBLinExpr leftExpr = new GRBLinExpr();
            leftExpr.addTerm(1.0, x[i]);
            leftExpr.addTerm(1.0, z[i][dimInfo[i]]);
            model.addConstr(leftExpr, GRB.GREATER_EQUAL, 1, "constr5_" + i);
        }

        //6. 那些固定不变的节点x必须为0
        for (UARNode node : nodeIndex.keySet()) {
            if (isUnchangeable(node)) {
                int i = nodeIndex.get(node);
                model.addConstr(x[i], GRB.EQUAL, 0, "constr6_" + i);
            }
        }

        //7. 终端节点的维度与其类型相符
        for (UARNode term : terminals) {
            GRBLinExpr leftExpr = new GRBLinExpr();
            int i = nodeIndex.get(term);
            for (int j : DIM_TYPE) {
                leftExpr.addTerm(DIM_VALUE.get(j), z[i][j]);
            }
            model.addConstr(leftExpr, GRB.EQUAL, dim[i], "constr7_" + i);
        }

        //8-12. 函数节点的运算与其类型相符（5种类型）
        for (UARNode func : functions) {
            UARNode[] children = Arrays.stream(func.children).map(node -> (UARNode) node).toArray(UARNode[]::new);
            int i = nodeIndex.get(func);
            // 对于维度运算类型0-2
            if (dimInfo[i] == 0 || dimInfo[i] == 1 || dimInfo[i] == 2) {
                // 8.1 dim_i - dim_j <= M * ( 1 - y_i0 )
                GRBLinExpr leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[0])]);
                GRBLinExpr rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][0]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr8.1_" + i);

                // 8.2 dim_j - dim_i <= M * ( 1 - y_i0 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(-1.0, dim[i]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[0])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][0]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr8.2_" + i);

                // 8.3 dim_i - dim_k <= M * ( 1 - y_i0 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][0]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr8.3_" + i);

                // 8.4 dim_k - dim_i <= M * ( 1 - y_i0 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(-1.0, dim[i]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][0]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr8.4_" + i);

                // 测试性的约束
                /*---------------------------------*/
                /*
                // dim_j - dim_k <= M * ( 1 - y_i0 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[0])]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][0]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr8.5_" + i);
                // dim_k - dim_j <= M * ( 1 - y_i0 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[0])]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][0]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr8.6_" + i);
                 */
                /*---------------------------------*/

                // 9.1 dim_i - ( dim_j + dim_k ) <= M * ( 1 - y_i1 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[0])]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][1]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr9.1_" + i);

                // 9.2 ( dim_j + dim_k ) - dim_i <= M * ( 1 - y_i1 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(-1.0, dim[i]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[0])]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][1]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr9.2_" + i);

                // 10.1 dim_i - ( dim_j - dim_k ) <= M * ( 1 - y_i1 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[0])]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][2]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr10.1_" + i);

                // 10.2 ( dim_j - dim_k ) - dim_i <= M * ( 1 - y_i1 )
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(-1.0, dim[i]);
                leftExpr.addTerm(1.0, dim[nodeIndex.get(children[0])]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[1])]);
                rightExpr = new GRBLinExpr();
                rightExpr.addConstant(M);
                rightExpr.addTerm(-M, y[i][2]);
                model.addConstr(leftExpr, GRB.LESS_EQUAL, rightExpr, "constr10.2_" + i);
            }

            // 对于维度运算类型3
            if (dimInfo[i] == 3) {
                // 11.1 dim_i - 0.5*dim_j == 0
                GRBLinExpr leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-0.5, dim[nodeIndex.get(children[0])]);
                model.addConstr(leftExpr, GRB.EQUAL, 0, "constr11.1_" + i);
            }

            // 对于维度运算类型4
            if (dimInfo[i] == 4) {
                // 12.1 dim_i - dim_k == 0
                GRBLinExpr leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[1])]);
                model.addConstr(leftExpr, GRB.EQUAL, 0, "constr12.1_" + i);
                // 12.2 dim_i - dim_l == 0
                leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, dim[i]);
                leftExpr.addTerm(-1.0, dim[nodeIndex.get(children[2])]);
                model.addConstr(leftExpr, GRB.EQUAL, 0, "constr12.2_" + i);
            }
        }

        // 13. 根节点的维度必须等于0
        GRBLinExpr leftExpr = new GRBLinExpr();
        leftExpr.addTerm(1.0, dim[nodeIndex.get(tree.child)]);
        model.addConstr(leftExpr, GRB.EQUAL, target, "constr13");

        // 设置目标函数：最小化改变成本
        GRBLinExpr obj = new GRBLinExpr();
        for (UARNode node : nodeIndex.keySet()) {
            int i = nodeIndex.get(node);
            obj.addTerm(cost[i], x[i]);
        }
        model.setObjective(obj, GRB.MINIMIZE);

        // 执行优化
        model.optimize();
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

    /** 本函数的功能为获取一个UARNode的不可用维度运算类型，这些变化将禁止*/
    public int[] getDisabledDimOperatorSet(UARNode node) {
        if (node.expectedChildren() == 0)
            System.out.println("错误：输入节点非函数类型！信息：" + node);

        switch (getDimOperator(node)) {
            case 0, 1, 2: {
                return new int[] {3, 4};
            }
            case 3: {
                return  new int[] {0, 1, 2, 4};
            }
            default: {
                return  new int[] {0, 1, 2, 3};
            }
        }
    }

    /**
     * 本函数的功能是判断一个节点是否不能改变（否则可能导致树结构的改变）
     * 当一个节点为函数节点，且维度运算类型为3或4时，不能改变
     * 因为修改为其他维度运算，必须增加或减少子节点数量
     * */
    public boolean isUnchangeable(UARNode node) {
        if (node.expectedChildren() > 0) {
            int dimOperator = getDimOperator(node);
            if (dimOperator == 3 || dimOperator == 4)
                return true;
        }
        return false;
    }

    /** 本函数的功能是清理模型*/
    public void dispose() {
        model.dispose();
    }
}
