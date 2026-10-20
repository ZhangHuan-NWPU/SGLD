package ec.app.UAR.util;

import ec.EvolutionState;
import ec.app.UAR.UARIndividual;
import ec.app.UAR.UARNode;
import ec.app.UAR.functions.*;
import ec.app.UAR.terminals.*;
import ec.gp.GPTree;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
* 规则解析器，将lisp格式的字符串解析为GP树
* 作者：张桓
* */

public class RuleParser {
    public static UARNode parseTree(String expression) {

        // 移除前后的空格
        expression = expression.trim();

        //移除左右的括号
        if (expression.charAt(0) == '(')
            expression = expression.substring(1);

        if (expression.charAt(expression.length() - 1) == ')')
            expression = expression.substring(0, expression.length() - 1);

        //划分运算符及若干个子表达式
        String operator;
        ArrayList<String> subExpressions = new ArrayList<>();

        //找到第一个空格的位置，以确认运算符
        int spaceIndex = expression.indexOf(' ');
        operator = expression.substring(0, spaceIndex).trim();
        expression = expression.substring(spaceIndex).trim();

        //基于括号平衡找到剩余的表达式
        int balance = 0;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                balance++;
            } else if (c == ')') {
                balance--;
            }

            if (balance == 0 && c == ' ') {
                subExpressions.add(expression.substring(start, i).trim());
                start = i;
            }
        }
        subExpressions.add(expression.substring(start).trim());

        // 确认运算符
        UARNode output;
        switch (operator) {
            case "+": {
                output = new Add();
                output.children = new UARNode[2];
                break;
            }
            case "-": {
                output = new Sub();
                output.children = new UARNode[2];
                break;
            }
            case "*": {
                output = new Mul();
                output.children = new UARNode[2];
                break;
            }
            case "/": {
                output = new Div();
                output.children = new UARNode[2];
                break;
            }
            case "max": {
                output = new Max();
                output.children = new UARNode[2];
                break;
            }
            case "min": {
                output = new Min();
                output.children = new UARNode[2];
                break;
            }
            case "if": {
                output = new If();
                output.children = new UARNode[3];
                break;
            }
            default: {
                output = new Sqrt();
                output.children = new UARNode[1];
                break;
            }
        }

        for (int i = 0; i < subExpressions.size(); i++) {
            if (i >= output.children.length)
                System.out.println("错误：子表达式数量大于子节点数量");

            String subExpr = subExpressions.get(i);
            if (subExpr.charAt(0) == '(' && subExpr.charAt(subExpr.length() - 1) == ')') {
                output.children[i] = parseTree(subExpr);
            }
            else {
                switch (subExpr) {
                    case "CR": {
                        output.children[i] = new CR();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "i": {
                        output.children[i] = new i();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "M": {
                        output.children[i] = new M();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "N": {
                        output.children[i] = new N();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "1": {
                        output.children[i] = new One();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "PN": {
                        output.children[i] = new PN();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "PW": {
                        output.children[i] = new PW();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "2": {
                        output.children[i] = new Two();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "V": {
                        output.children[i] = new V();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    case "0": {
                        output.children[i] = new Zero();
                        output.children[i].children = new UARNode[0];
                        break;
                    }
                    default:
                        RandomNumber rand = new RandomNumber();
                        double value = Double.parseDouble(subExpr);
                        rand.value = value;
                        rand.initialized = true;
                        output.children[i] = rand;
                        output.children[i].children = new UARNode[0];
                        break;
                }
            }
            output.children[i].argposition = (byte) i;
            output.children[i].parent = output;
        }

        return output;
    }

    public static UARIndividual parseIndividual(ArrayList<String> expressions) {
        UARIndividual output = new UARIndividual();

        int treeNum = expressions.size();
        output.trees = new GPTree[treeNum];

        for (int i = 0; i < expressions.size(); i++) {
            GPTree tree = new GPTree();
            tree.child = parseTree(expressions.get(i));
            tree.owner = output;
            tree.child.parent = tree;
            tree.child.argposition = 0;

            output.trees[i] = tree;
        }

        return output;
    }

    public static void main(String[] args) {
        ArrayList<String> expressions = new ArrayList<>();
        expressions.add("(+ (/ (/ (+ (/ (* (max 1 PN) (- M V)) (* (max 1 1) V)) (max i 1)) (max 1 1)) (* (max 1 PN) (- M V))) i)");
        // expressions.add("(min (min (sqrt (/ N 0)) (sqrt (- i 1))) (min (min (- CR (* V CR)) (- CR (sqrt 0))) (sqrt 0)))");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out), true);

        UARIndividual ind = parseIndividual(expressions);
        for (GPTree tree: ind.trees) {
            tree.printTree(new EvolutionState(), writer);
        }
    }
}
