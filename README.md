# SGLD

基于本地 ECJ 27 项目整理的共享代码，保留 ECJ 框架与 UAR 应用。

论文对应的正式实验结果见 [experiments/manuscript_261003](experiments/manuscript_261003/README.md)，包含筛选说明、原始结果、运行日志、论文图表映射及已知数据差异。

## 内容

- `src/main/java/ec/`：ECJ 框架；应用目录仅保留 `ec/app/UAR`。
- `src/main/java/sim/`：框架及模拟所用的工具代码。
- `src/main/java/ec/app/UAR/`：UAR 源码、参数文件、模拟器和 `util/rules.xlsx` 规则数据。
- `src/main/resources/ec/`：ECJ 框架参数资源，已排除其他应用的资源。
- `src/test/`：保留的 ECJ 框架测试，已排除其他应用的测试。
- `docs/`、`README-ECJ.md`、`CHANGES`：原有 ECJ 文档和说明。
- `local-repo/`：原项目自带的 pshecj Maven 依赖。

不含 ASR、MultiAppScheduling、hospital、hospital_v2、IDE 配置、编译产物和运行日志。根目录旧实验结果表未纳入，避免混入其他应用的结果。

## 环境与构建

使用 JDK 21 和 Maven。UAR 的 repair 模块依赖 Gurobi 11.0.0；执行相关算法需要自行安装对应运行环境并配置有效许可证。仓库不包含 Gurobi 许可证或 Gurobi 二进制包。

```sh
mvn -DskipTests compile
mvn -DskipTests -Dmaven.javadoc.skip=true package
```

## 运行

在仓库根目录执行，例如：

```sh
java -cp "target/classes;target/dependency/*" ec.Evolve -file src/main/java/ec/app/UAR/params/modal_2_ls_dpsplus_br.params
```

Linux/macOS 将 classpath 中的分号换为冒号。参数文件默认运行较大的实验，使用前请查看 jobs、generations、线程数与种群规模。

规则评估入口为 `ec.app.UAR.util.EvolvedAREvaluator`，可通过第一个命令行参数传入规则表路径；不传参时使用仓库中的 `util/rules.xlsx`。

## 整理说明

源目录：本地 `IdeaProjects/ecj`，整理日期：2026-10-20。原目录未修改。

除范围筛选外，共享副本保留了 UAR 参数文件原有的父资源相对路径，将日志输出改为当前目录的 `out.stat`，并将规则评估器的本机绝对路径改为可传参的仓库相对路径。算法逻辑沿用原代码。部分历史参数文件引用源目录中已缺失的类，具体见 `VALIDATION.md`。

ECJ 的原许可证与版权声明保留于 `LICENSE` 和各源文件中。
