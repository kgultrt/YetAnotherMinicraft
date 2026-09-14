package com.mojang.ld22.screen;

import com.mojang.ld22.gfx.Screen;

/**
 * 关卡切换时的遮罩过渡。
 *
 * <p>遮罩以 8×8 网格为单位，每格根据它在屏幕上的位置获得一个"延迟值"，
 * 然后随着 time 递增逐个被黑色块覆盖。dir>0（下楼）从上往下扫，
 * dir<0（上楼）从下往上扫。
 *
 * <p>动画时长 60 tick（30 次 tick × 每次 +2）：
 * <ul>
 *   <li>time=30 切换关卡（遮罩覆盖最深的瞬间）</li>
 *   <li>time=60 结束，回到游戏</li>
 * </ul>
 *
 * <p>网格尺寸从 {@code screen.w/h} 动态算出，延迟公式做了归一化，
 * 保证在任意分辨率下波形不拉伸、最大延迟不超过渲染窗口。
 */
public class LevelTransitionMenu extends Menu {
	private int dir;
	private int time = 0;

	public LevelTransitionMenu(int dir) {
		this.dir = dir;
	}

	public void tick() {
		time += 2;
		if (time == 30) game.changeLevel(dir);
		if (time == 60) game.setMenu(null);
	}

	public void render(Screen screen) {
		// 动态网格：覆盖当前内部分辨率，而不是写死 20×15。
		int cols = (screen.w + 7) / 8;
		int rows = (screen.h + 7) / 8;

		// 归一化分母。原本在 20×15 下：
		//   纵向最大贡献 = 14（rows-1），横向 = 19/3 ≈ 6。
		// 现在按行列数等比缩放，波形保持一致，最大延迟稳定在 28 左右，
		// 配 30 tick 的渲染窗口刚好在 time=60 前清空。
		int maxV = Math.max(1, rows - 1);
		int maxH = Math.max(1, cols - 1);

		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				int vertical = y * 20 / maxV;
				int lateral = x * 6 / maxH;
				int dd = (vertical + x % 2 * 2 + lateral) - time;
				if (dd < 0 && dd > -30) {
					if (dir > 0)
						screen.render(x * 8, y * 8, 0, 0, 0);
					else
						screen.render(x * 8, screen.h - y * 8 - 8, 0, 0, 0);
				}
			}
		}
	}
}