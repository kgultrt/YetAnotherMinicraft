package com.mojang.ld22;

import java.util.Random;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.PixelFont;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.gfx.Shaders;
import com.mojang.ld22.gfx.SpriteSheet;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.level.Level;
import com.mojang.ld22.level.tile.Tile;
import com.mojang.ld22.screen.DeadMenu;
import com.mojang.ld22.screen.LevelTransitionMenu;
import com.mojang.ld22.screen.Menu;
import com.mojang.ld22.screen.PauseMenu;
import com.mojang.ld22.screen.TitleMenu;
import com.mojang.ld22.screen.WonMenu;
import com.mojang.ld22.sound.Sound;
import com.mojang.ld22.sound.MusicManager;

import java.nio.ByteBuffer;

public class Game extends ApplicationAdapter {
    public static final String NAME = "Minicraft";

    public static String version = "1.0.0";

    public static int WIDTH = 320;
    public static int HEIGHT = 240;

    private static final float TICK_RATE = 60f;
    private static final float TICK_TIME = 1f / TICK_RATE;
    private static final int BASE_SHORT_SIDE = 240;

    private static final int SHAKE_TICKS = 18;

    public static final String PIXEL_FONT_PATH = "quan.ttf";

    /** 开场动画时长（tick）。30 tick = 0.5 秒。 */
    private static final int INTRO_DURATION = 30;

    /** 开场时摄像机的初始偏移量（像素）。32 相当于玩家一开始偏 4 格。 */
    private static final int INTRO_CAM_OFFSET = 32;

    /**
     * 背景暗化过渡时长（tick）。12 tick ≈ 0.2 秒。
     *
     * <p>菜单滑动动画是 24 tick，暗化是它的一半 —— 暗化先到位， 菜单滑到位时游戏画面已经暗好了，衔接更顺。
     */
    private static final int DARKEN_ANIM_TICKS = 12;

    /** 有序抖动矩阵。开场渐亮用它把"该不该画黑块"分散到不同 tick， 看起来像像素在逐块点亮，而不是整体变亮。 */
    private static final int[][] BAYER_4X4 = {
            {0, 8, 2, 10},
            {12, 4, 14, 6},
            {3, 11, 1, 9},
            {15, 7, 13, 5},
    };

    /**
     * HUD 一行的 X 坐标。全部 y=0，水平平铺。 血条 10 格（80px）从 0 起，间隔 16px 后是体力条， 再间隔 16px 后是道具槽。240 宽下也放得下（208px）。
     */
    private static final int HUD_HP_X = 0;

    private static final int HUD_STAMINA_X = 96;
    private static final int HUD_ITEM_X = 192;
    private static final int HUD_Y = 0;

    private Screen screen;
    private Screen lightScreen;
    private InputHandler input;

    /** 16 位调色板 + RGBA8888 查表。 */
    private final Palette palette = new Palette();

    private int tickCount = 0;
    public int gameTime = 0;

    private Level level;
    private Level[] levels = new Level[5];
    private int currentLevel = 3;
    public Player player;

    /** 当前稳定显示的菜单。动画期间它正在滑出。 */
    public Menu menu;

    /** 菜单状态机与切换动画。 */
    private MenuManager menus;

    private int playerDeadTime;
    private int pendingLevelChange;
    private int wonTimer = 0;
    public boolean hasWon = false;

    /** 开场动画倒计时。-1 表示不在动画中。 */
    private int introTicks = -1;

    private boolean focused = true;

    private Pixmap pixmap;
    private ByteBuffer pixmapBuffer;
    private Texture texture;
    private SpriteBatch batch;

    private boolean touchEnabled;
    private int lastScreenW = -1, lastScreenH = -1;

    private TouchControls touchControls;

    private float tickAccumulator = 0f;
    private float shaderTime = 0f;

    private Random random = new Random();
    private int shakeTicks = 0;
    private float shakeAmount = 0f;

    /**
     * 背景暗化过渡的四个状态值。
     *
     * <p>用浮点而不是整数，因为要在两个暗化档位之间平滑过渡。 {@link #currentDarken} 是当前实际值，{@link #renderFrame} 里 round
     * 成整数档位用。
     */
    private float fromDarken = 0f;

    private float toDarken = 0f;
    private float currentDarken = 0f;
    private int darkenAnimTick = DARKEN_ANIM_TICKS;

    // ---------------------------------------------------------------- 菜单

    public void setMenu(Menu newMenu) {
        setMenu(newMenu, 1);
    }

    /**
     * 带方向的菜单切换。
     *
     * @param newMenu 目标菜单；null 表示回到游戏
     * @param direction +1 前进（新菜单从右侧来），-1 返回（新菜单从左侧来）
     */
    public void setMenu(Menu newMenu, int direction) {
        menus.set(newMenu, direction);
        startDarkenAnim(newMenu != null ? newMenu.getScreenDarken() : 0);
    }

    /** 启动暗化过渡。目标跟当前目标相同时什么都不做。 */
    private void startDarkenAnim(int target) {
        if (target == toDarken) return;
        fromDarken = currentDarken;
        toDarken = target;
        darkenAnimTick = 0;
    }

    /**
     * 从标题菜单开始游戏。
     *
     * <p>跟直接 {@code setMenu(null)} 的区别：
     *
     * <ul>
     *   <li>重置世界后**硬切**菜单，不走滑动动画（滑出标题再露出游戏会拖延时间）
     *   <li>触发开场动画：黑幕渐亮 + 镜头从偏移位置滑到玩家
     * </ul>
     */
    public void startGame() {
        resetGame();
        menus.setImmediate(null);
        fromDarken = 0f;
        toDarken = 0f;
        currentDarken = 0f;
        darkenAnimTick = DARKEN_ANIM_TICKS;
        introTicks = 0;
    }

    // ---------------------------------------------------------------- 通用

    public void shake(float amount) {
        this.shakeAmount = amount;
        this.shakeTicks = SHAKE_TICKS;
    }

    public void scheduleLevelChange(int dir) {
        pendingLevelChange = dir;
    }

    public void won() {
        wonTimer = 60 * 3;
        hasWon = true;
    }

    public void changeLevel(int dir) {
        level.remove(player);
        currentLevel += dir;
        level = levels[currentLevel];
        player.x = (player.x >> 4) * 16 + 8;
        player.y = (player.y >> 4) * 16 + 8;
        level.add(player);
    }

    public void resetGame() {
        playerDeadTime = 0;
        wonTimer = 0;
        gameTime = 0;
        hasWon = false;

        levels = new Level[5];
        currentLevel = 3;

        levels[4] = new Level(128, 128, 1, null);
        levels[3] = new Level(128, 128, 0, levels[4]);
        levels[2] = new Level(128, 128, -1, levels[3]);
        levels[1] = new Level(128, 128, -2, levels[2]);
        levels[0] = new Level(128, 128, -3, levels[1]);

        level = levels[currentLevel];
        player = new Player(this, input);
        player.findStartPos(level);

        level.add(player);

        for (int i = 0; i < 5; i++) {
            levels[i].trySpawn(5000);
        }
    }

    // ---------------------------------------------------------- 生命周期

    @Override
    public void create() {
        computeInternalResolution();

        input = new InputHandler();
        Gdx.input.setInputProcessor(input);

        // 显式捕获 Android 返回键，阻止它触发 Activity 的默认退出行为。
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

        pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        pixmapBuffer = pixmap.getPixels();
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        batch = new SpriteBatch();

        Sound.loadAll();

        MusicManager.get().load("title", "music/title.ogg");

        Messages.init();
        PixelFont.init(PIXEL_FONT_PATH);
        Shaders.load();

        touchEnabled = Gdx.app.getType() == Application.ApplicationType.Android
                || Gdx.input.isPeripheralAvailable(Input.Peripheral.MultitouchScreen);

        touchControls = new TouchControls();

        init();
    }

    private void computeInternalResolution() {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        float aspect = (float) sw / sh;

        int longSide = Math.round(BASE_SHORT_SIDE * Math.max(aspect, 1f / aspect));
        longSide = (longSide / 8) * 8;

        if (sw >= sh) {
            WIDTH = longSide;
            HEIGHT = BASE_SHORT_SIDE;
        } else {
            WIDTH = BASE_SHORT_SIDE;
            HEIGHT = longSide;
        }
    }

    @Override
    public void dispose() {
        Sound.disposeAll();
        MusicManager.get().dispose();
        PixelFont.dispose();
        if (touchControls != null) touchControls.dispose();
        if (batch != null) batch.dispose();
        if (texture != null) texture.dispose();
        if (pixmap != null) pixmap.dispose();
        Shaders.dispose();
    }

    @Override
    public void pause() {
        focused = false;
    }

    @Override
    public void resume() {
        focused = true;
    }

    private void init() {
        palette.build();

        Pixmap sheetPixmap = new Pixmap(Gdx.files.internal("icons.png"));
        SpriteSheet sheet = new SpriteSheet(sheetPixmap);
        sheetPixmap.dispose();

        screen = new Screen(WIDTH, HEIGHT, sheet);
        lightScreen = new Screen(WIDTH, HEIGHT, sheet);

        menus = new MenuManager(this, input);

        resetGame();
        setMenu(new TitleMenu());
    }

    // -------------------------------------------------------------- 主循环

    @Override
    public void render() {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        if (sw != lastScreenW || sh != lastScreenH) {
            lastScreenW = sw;
            lastScreenH = sh;
            input.layoutTouchControls(sw, sh);
        }

        float delta = Math.min(Gdx.graphics.getDeltaTime(), 0.25f);
        tickAccumulator += delta;
        while (tickAccumulator >= TICK_TIME) {
            tick();
            tickAccumulator -= TICK_TIME;
        }

        shaderTime += delta;
        MusicManager.get().update(delta);

        renderFrame();
        present();
    }

    public void tick() {
        tickCount++;

        updateMusic();

        if (shakeTicks > 0) shakeTicks--;

        // 暗化过渡独立推进，不受菜单状态影响。
        if (darkenAnimTick < DARKEN_ANIM_TICKS) {
            darkenAnimTick++;
            float t = darkenAnimTick / (float) DARKEN_ANIM_TICKS;
            // easeInOutQuad：两端慢、中间快，是"过渡"的手感。
            float eased = (t < 0.5f)
                    ? 2f * t * t
                    : 1f - 2f * (1f - t) * (1f - t);
            currentDarken = fromDarken + (toDarken - fromDarken) * eased;
        }

        if (!focused) {
            input.releaseAll();
            return;
        }

        input.tick();

        // 游戏中按 ESC / Android 返回键 → 弹出暂停菜单
        if (menu == null && !menus.isAnimating() && introTicks < 0) {
            if (input.pause.clicked) {
                setMenu(new PauseMenu(), +1);
                return;
            }
        }

        if (menus.isAnimating()) {
            menus.tick();
            return;
        }

        if (menu == null) {
            if (introTicks >= 0) {
                introTicks++;
                if (introTicks >= INTRO_DURATION) {
                    introTicks = -1;
                }
                return;
            }

            if (!player.removed && !hasWon) gameTime++;

            if (player.removed) {
                playerDeadTime++;
                if (playerDeadTime > 60) {
                    setMenu(new DeadMenu());
                }
            } else {
                if (pendingLevelChange != 0) {
                    setMenu(new LevelTransitionMenu(pendingLevelChange));
                    pendingLevelChange = 0;
                }
            }
            if (wonTimer > 0) {
                if (--wonTimer == 0) {
                    setMenu(new WonMenu());
                }
            }
            level.tick();
            Tile.tickCount++;
            return;
        }

        menu.tick();
    }

    /**
     * 根据当前游戏状态决定播哪首 BGM。每 tick 调一次。
     *
     * <p>规则：
     *
     * <ul>
     *   <li>标题菜单 → title
     *   <li>游戏中地表（level 0）→ surface
     *   <li>游戏中洞穴（level &lt; 0）→ cave
     *   <li>游戏中天空（level 1）→ sky
     * </ul>
     */
    private void updateMusic() {
        if (menu instanceof TitleMenu) {
            MusicManager.get().play("title");
            return;
        }

        if (menu == null) {
            if (introTicks >= 0) {
                // 开场动画期间保持当前（通常是从标题来的 title 还在淡出）
                return;
            }
            if (currentLevel < 0) {
                MusicManager.get().play("cave");
            } else if (currentLevel == 0) {
                MusicManager.get().play("surface");
            } else {
                MusicManager.get().play("sky");
            }
            return;
        }
    }

    // -------------------------------------------------------------- 渲染

    public void renderFrame() {
        int shakeX = 0, shakeY = 0;
        if (shakeTicks > 0) {
            shakeX = Math.round((random.nextFloat() * 2f - 1f) * shakeAmount);
            shakeY = Math.round((random.nextFloat() * 2f - 1f) * shakeAmount);
        }

        int camX = player.x;
        int camY = player.y;

        if (introTicks >= 0) {
            float p = introTicks / (float) (INTRO_DURATION - 1);
            float pr = p < 0.5f
                    ? 2f * p * p
                    : 1f - 2f * (1f - p) * (1f - p);
            float offset = INTRO_CAM_OFFSET * (1f - pr);
            camX += Math.round(offset);
            camY += Math.round(offset);
        }

        int xScroll = camX - screen.w / 2 + shakeX;
        int yScroll = camY - (screen.h - 8) / 2 + shakeY;
        if (xScroll < 16) xScroll = 16;
        if (yScroll < 16) yScroll = 16;
        if (xScroll > level.w * 16 - screen.w - 16) xScroll = level.w * 16 - screen.w - 16;
        if (yScroll > level.h * 16 - screen.h - 16) yScroll = level.h * 16 - screen.h - 16;

        if (currentLevel > 3) {
            int col = Color.get(20, 20, 121, 121);
            for (int y = 0; y < screen.h / 8; y++)
                for (int x = 0; x < screen.w / 8; x++) {
                    screen.render(x * 8 - ((xScroll / 4) & 7), y * 8 - ((yScroll / 4) & 7), 0, col, 0);
                }
        }

        level.renderBackground(screen, xScroll, yScroll);
        level.renderSprites(screen, xScroll, yScroll);

        if (currentLevel < 3) {
            lightScreen.clear(0);
            level.renderLight(lightScreen, xScroll, yScroll);
            screen.overlay(lightScreen, xScroll, yScroll);
        }

        renderGui();

        if (introTicks >= 0) {
            renderIntroMask();
        }

        // 压暗游戏画面。currentDarken 是浮点，round 成整数档位。
        // 必须在 menus.render 之前 —— 这样菜单不受影响。
        // 用软件混合（查表替换索引），菜单颜色仍然是原色。
        int darkenLevel = Math.round(currentDarken);
        if (darkenLevel > 0) {
            screen.darken(palette, darkenLevel);
        }

        menus.render(screen);

        if (!focused) renderFocusNagger();
    }

    private void present() {
        int[] src = screen.pixels;
        int n = src.length;
        int[] rgbaPalette = palette.rgba;

        for (int i = 0; i < n; i++) {
            int cc = src[i];
            int rgba = (cc < 255) ? rgbaPalette[cc] : 0;

            int off = i << 2;
            pixmapBuffer.put(off, (byte) (rgba >> 24));
            pixmapBuffer.put(off + 1, (byte) (rgba >> 16));
            pixmapBuffer.put(off + 2, (byte) (rgba >> 8));
            pixmapBuffer.put(off + 3, (byte) rgba);
        }
        texture.draw(pixmap, 0, 0);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        float scale = Math.min(sw / WIDTH, sh / HEIGHT);
        if (scale < 1f) scale = 1f;
        float dw = WIDTH * scale;
        float dh = HEIGHT * scale;
        float dx = (sw - dw) / 2f;
        float dy = (sh - dh) / 2f;

        batch.begin();

        if (Shaders.postOk) {
            batch.setShader(Shaders.post);

            if (Shaders.post.hasUniform("u_time")) {
                Shaders.post.setUniformf("u_time", shaderTime);
            }
            if (Shaders.post.hasUniform("u_resolution")) {
                Shaders.post.setUniformf("u_resolution", sw, sh);
            }

            batch.draw(texture, dx, dy, dw, dh);
            batch.setShader(null);
        } else {
            batch.draw(texture, dx, dy, dw, dh);
        }

        if (touchEnabled) {
            touchControls.render(batch, input);
        }

        batch.end();
    }

    /**
     * HUD 一行平铺、全透明、顶置。
     *
     * <p>注意：HUD 在压暗之前绘制，所以暂停时 HUD 也会变暗。 想让 HUD 保持高亮的话，把 renderGui() 挪到 screen.darken 之后。
     */
    private void renderGui() {
        for (int i = 0; i < 10; i++) {
            if (i < player.health)
                screen.render(HUD_HP_X + i * 8, HUD_Y, 0 + 12 * 32, Color.get(-1, 200, 500, 533), 0);
            else
                screen.render(HUD_HP_X + i * 8, HUD_Y, 0 + 12 * 32, Color.get(-1, 100, 000, 000), 0);

            int sx = HUD_STAMINA_X + i * 8;
            if (player.staminaRechargeDelay > 0) {
                if (player.staminaRechargeDelay / 4 % 2 == 0)
                    screen.render(sx, HUD_Y, 1 + 12 * 32, Color.get(-1, 555, 000, 000), 0);
                else
                    screen.render(sx, HUD_Y, 1 + 12 * 32, Color.get(-1, 110, 000, 000), 0);
            } else {
                if (i < player.stamina)
                    screen.render(sx, HUD_Y, 1 + 12 * 32, Color.get(-1, 220, 550, 553), 0);
                else
                    screen.render(sx, HUD_Y, 1 + 12 * 32, Color.get(-1, 110, 000, 000), 0);
            }
        }

        if (player.activeItem != null) {
            player.activeItem.renderInventory(screen, HUD_ITEM_X, HUD_Y);
        }
    }

    /**
     * 开场黑幕。用有序抖动让黑块逐块消失，看起来像像素在点亮。
     *
     * <p>直接写 pixels 数组，不走 {@code screen.render} —— tile 0 在图集里 是空的。palette index 0 是近黑 0x0A0A0A。
     */
    private void renderIntroMask() {
        float p = introTicks / (float) (INTRO_DURATION - 1);
        int threshold = Math.round(p * 16f);

        int w = screen.w;
        int h = screen.h;
        int[] pixels = screen.pixels;

        int cols = w / 8;
        int rows = h / 8;

        for (int ty = 0; ty < rows; ty++) {
            int by = ty & 3;
            int y0 = ty * 8;
            for (int tx = 0; tx < cols; tx++) {
                int bayer = BAYER_4X4[by][tx & 3];
                if (bayer >= threshold) {
                    int x0 = tx * 8;
                    for (int y = y0; y < y0 + 8; y++) {
                        int row = y * w;
                        for (int x = x0; x < x0 + 8; x++) {
                            pixels[row + x] = 0;
                        }
                    }
                }
            }
        }
    }

    private void renderFocusNagger() {
        String msg = "Click to focus!";
        int xx = (screen.w - Font.measure(msg)) / 2;
        int yy = (screen.h - 8) / 2;

        Font.draw(msg, screen, xx, yy,
                ((tickCount / 20) % 2 == 0) ? Color.get(5, 333, 333, 333) : Color.get(5, 555, 555, 555));
    }
}
