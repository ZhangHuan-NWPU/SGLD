# 验证记录

验证日期：2026-10-20；环境：Windows、JDK 21.0.1、本机 Maven 依赖缓存。

- `mvn -o -DskipTests compile`：成功，编译 460 个 Java 源文件。
- UAR：保留 95 个 Java 源文件、28 个参数文件和 `util/rules.xlsx`。
- 所有 UAR 参数文件保留原有父资源路径，并验证目标文件存在。
- 已排除其他应用代码、对应资源与测试，以及 9 个直接依赖 TSP、knapsack、coevolve2 应用类的框架测试。
- `mvn -o -Djacoco.skip=true test`：168 项测试中 148 项通过、20 项错误、0 项断言失败。错误涉及 Windows 下不可用的 `/tmp/a.txt`、`/dev/null` 路径，以及 SPEA2 测试引用的已排除 moosuite 参数资源。未修改这些原有测试逻辑。
- `mvn -o -DskipTests -Dmaven.javadoc.skip=true package`：JAR 生成成功；后续依赖复制插件因离线缓存缺少插件依赖未完成，完整 package 尚未验证通过。
- 未执行完整 UAR 实验或验证 Gurobi 运行许可证；编译成功不代表所有实验参数均可直接运行。

## 原目录中已缺失的类

以下历史参数原样保留算法配置，使用前需要补回相应类或由作者确认替代配置：

| 参数文件 | 缺失的类 |
| --- | --- |
| `modal_0_benchmark_gap.params` | `ec.app.UAR.modal.ModalProblemGap` |
| `modal_14_ls_dpsplus.params` | `ec.app.UAR.DPS.DPSNewEvaluatorGap` |
| `modal_17_ls_dpsplus_2trees.params` | `ec.app.UAR.DPS.DPSNewEvaluatorGap` |
| `modal_18_ls_dpsplus_rep.params` | `ec.app.UAR.DPS.DPSNewEvaluatorGap` |
| `modal_19_ls_dpsplus_elite.params` | `ec.app.UAR.DPS.DPSNewEvaluatorGap` |
| `modal_20_ls_dpsplus_es.params` | `ec.app.UAR.DPS.DPSNewEvaluatorGap` |

原 `pom.xml` 中重复的 commons-math3 声明和旧版 JaCoCo 配置保留，构建会有相应提示。JDK 21 下执行测试可先用 `-Djacoco.skip=true` 跳过旧版覆盖率代理。
