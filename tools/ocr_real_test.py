#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用 RapidOCR（PC 端轻量中文 OCR，onnxruntime）直接识别 `I:/coc/游戏截图` 里的
原生 1280x720 截图，验证「截图→识别」这条链路在真实 OCR 引擎下的中文识别效果。
注：这只是 PC 侧的一个真实 OCR 引擎，用来直观看识别能力；App 端主路径是 ML Kit，
但二者输入都是同一张截图，识别效果可直接类比参考。

用法：cd tools && python ocr_real_test.py
"""
import os
import sys
from rapidocr_onnxruntime import RapidOCR

SCREEN_DIR = r"I:\coc\游戏截图"
# 选取几张含明确中文文案的图
TEST_FILES = [
    "主世界-部落聊天框-援兵已就绪.png",
    "主世界-报告首领-存在已完成的任务.png",
    "每周精选-宝石3.png",
    "都城-主界面.png",
    "主世界-打鱼-搜索到1个.png",
    "主世界-部落战-准备日1.png",
]


def main():
    engine = RapidOCR()
    for fname in TEST_FILES:
        path = os.path.join(SCREEN_DIR, fname)
        if not os.path.exists(path):
            print(f"[跳过] {fname}")
            continue
        sys.stdout.flush()
        result, _ = engine(path)
        print("=" * 60)
        print(f"图片: {fname}")
        if not result:
            print("  (未识别出文字)")
            continue
        # 只打印置信度较高的中文/含中文结果
        for box, text, score in result:
            sc = float(score)
            if sc >= 0.4:
                print(f"  [{sc:.2f}] {text}")


if __name__ == "__main__":
    main()
