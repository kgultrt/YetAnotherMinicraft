package com.mojang.ld22.ui;

import com.mojang.ld22.gfx.Screen;

/**
 * 能画在物品栏格子里的东西。
 *
 * <p>从 screen 包搬到 ui —— 它是 UI 概念，跟 Screen 渲染器是一层的。
 */
public interface ListItem {
    void renderInventory(Screen screen, int x, int y);
}