package com.mojang.ld22.ui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;

/**
 * 文字绘制工具。
 */
public final class Text {

    private Text() {}

    /** 在屏幕上水平居中绘制。 */
    public static void centered(Screen screen, String msg, int y, int col) {
        int x = (screen.w - Font.measure(msg)) / 2;
        Font.draw(msg, screen, x, y, col);
    }

    /** 在指定的水平中心绘制。 */
    public static void centeredAt(Screen screen, String msg, int cx, int y, int col) {
        int x = cx - Font.measure(msg) / 2;
        Font.draw(msg, screen, x, y, col);
    }

    /**
     * 自动换行绘制，左对齐。返回实际绘制的行数。
     *
     * <p>断行规则：优先在空格处断（英文）；没有空格可选时按字符断（中文）。
     * 假设字体等宽（每个字符 8px）—— Minicraft 的 Font 是等宽的，
     * 用长度 × 8 估算宽度，不需要逐字符 measure。
     *
     * @param maxLines 最多画几行；超出的文本直接截断，不做任何提示
     */
    public static int wrapped(Screen screen, String text, int x, int y,
                              int maxWidth, int col, int maxLines) {
        int lineHeight = 8;
        int charW = 8;
        int maxChars = maxWidth / charW;

        List<String> lines = wrapLines(text, maxChars);

        int n = Math.min(lines.size(), maxLines);
        for (int i = 0; i < n; i++) {
            Font.draw(lines.get(i), screen, x, y + i * lineHeight, col);
        }
        return n;
    }

    /**
     * 把文本按最大字符数折行。返回每行的内容。
     */
    public static List<String> wrapLines(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty() || maxChars <= 0) {
            result.add("");
            return result;
        }

        int start = 0;
        int len = text.length();

        while (start < len) {
            // 跳过行首空格
            while (start < len && text.charAt(start) == ' ') start++;
            if (start >= len) break;

            int end = start + maxChars;
            if (end >= len) {
                result.add(text.substring(start));
                break;
            }

            // 在 end 附近往前找空格
            int breakAt = -1;
            for (int i = end; i > start; i--) {
                if (text.charAt(i - 1) == ' ') {
                    breakAt = i - 1;
                    break;
                }
            }

            if (breakAt > start) {
                result.add(text.substring(start, breakAt));
                start = breakAt + 1;
            } else {
                // 整段没有空格（中文），按字符断
                result.add(text.substring(start, end));
                start = end;
            }
        }

        return result;
    }
}