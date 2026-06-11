---
name: read_pdf
description: 读取中文 PDF 文件并提取文本/表格内容。在 Windows 平台下正确处理 GBK 编码冲突，确保中文内容完整输出。当用户要求读取、查看、打开 PDF 文件，或需要提取 PDF 中的文字/表格时使用此技能。
---

# 读取中文 PDF

## 概述

此技能专门处理在 Windows 环境下读取中文 PDF 的场景，解决 GBK 终端编码与 Unicode 字符冲突的问题。

## 核心原则

**永远不要直接将中文 PDF 提取结果 `print()` 到 Windows 终端。** pdfplumber 本身能正确提取中文，但 Windows 默认 GBK 编码无法处理部分 Unicode 字符（如项目符号 `•`、全角符号等），会导致 `UnicodeEncodeError`。

## 标准工作流

### 步骤 1：确保依赖安装

```bash
pip install pdfplumber pypdf -q
```

### 步骤 2：编写提取脚本并写入 UTF-8 文件

```python
import sys
import pdfplumber

# 关键：重设 stdout 为 UTF-8
sys.stdout.reconfigure(encoding='utf-8')

pdf_path = "目标文件.pdf"

with pdfplumber.open(pdf_path) as pdf:
    print(f'总页数: {len(pdf.pages)}')
    print('=' * 80)
    for i, page in enumerate(pdf.pages):
        print(f'\n=== 第 {i+1} 页 / 共 {len(pdf.pages)} 页 ===\n')
        text = page.extract_text()
        if text:
            print(text)
        else:
            print('[此页无文本内容]')
        print()
        # 也可提取表格
        tables = page.extract_tables()
        if tables:
            for j, table in enumerate(tables):
                print(f'--- 表格 {j+1} ---')
                for row in table:
                    print(row)
                print()
        print('-' * 60)
```

### 步骤 3：重定向输出到文件

```bash
python read_script.py > pdf_output.txt 2>&1
```

### 步骤 4：用 Read 工具读取输出文件

使用 Read 工具分段读取 `pdf_output.txt`，获得完整内容展示给用户。

## 备选方案

如果 pdfplumber 对某些 PDF 文本提取效果不佳：
- 尝试 `pypdf`：`from pypdf import PdfReader`
- 对扫描版 PDF，使用 `pytesseract` + `pdf2image` 进行 OCR
- 对于已经确认是纯文本的 PDF，可以简化为只提取文本、不做表格检测

## 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `UnicodeEncodeError: 'gbk'` | print() 到 Windows 终端 | stdout UTF-8 或写文件 |
| 某些中文字符显示为乱码 | PDF 使用了非标准字体编码 | 尝试 pypdf 或 OCR |
| 表格提取不完整 | PDF 表格无明确边框 | 使用 pdfplumber 的 TableSettings |
| exit code 49 / exit code 1 | Windows Python 编码异常退出 | 检查 stdout 编码设置 |
