# AI 提示词模板

按设计约定，AI 提示词模板放在本目录，由 `modules/ai` 统一读取与管理。

规划文件（在 AI 能力层实现阶段补充）：

- `grade-answer.md`：简答批改
- `interview-question.md`：面试题补生成
- `interview-score.md`：AI 面试逐题评分
- `interview-report.md`：面试报告生成

要求：

1. 模板只包含提示词文本，不写业务代码；
2. 变量占位符使用 `{{variable}}` 形式，由 `ai` 模块渲染；
3. 模型、密钥、base-url 一律走配置与环境变量，不进入本目录。
