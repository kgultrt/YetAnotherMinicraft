package com.mojang.ld22;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 触屏摇杆与按钮的渲染，以及程序化生成的圆形 / 图标纹理。
 * 只在需要时创建纹理，{@link #dispose()} 统一释放。
 */
public class TouchControls {

    private final Texture circle;
    private final Texture attack;
    private final Texture menuIcon;

    public TouchControls() {
        circle = createCircleTexture();
        attack = createAttackTexture();
        menuIcon = createMenuTexture();
    }

    public void render(SpriteBatch batch, InputHandler input) {
        renderJoystick(batch, input);

        for (int i = 0; i < input.touchButtons.size(); i++) {
            InputHandler.TouchButton tb = input.touchButtons.get(i);
            batch.setColor(0f, 0f, 0f, tb.activePointer != -1 ? 0.6f : 0.35f);
            batch.draw(circle, tb.x, tb.y, tb.w, tb.h);
        }

        batch.setColor(1f, 1f, 1f, 0.9f);
        drawIcon(batch, input.btnAttack, attack);
        drawIcon(batch, input.btnMenu, menuIcon);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void renderJoystick(SpriteBatch batch, InputHandler input) {
        InputHandler.Joystick js = input.joystick;
        boolean active = js.activePointer != -1;

        batch.setColor(0f, 0f, 0f, active ? 0.5f : 0.28f);
        batch.draw(circle,
                js.cx - js.radius,
                js.cy - js.radius,
                js.radius * 2,
                js.radius * 2);

        batch.setColor(1f, 1f, 1f, active ? 0.75f : 0.42f);
        batch.draw(circle,
                js.knobX - js.knobRadius,
                js.knobY - js.knobRadius,
                js.knobRadius * 2,
                js.knobRadius * 2);

        batch.setColor(1f, 1f, 1f, 1f);
    }

    private static void drawIcon(SpriteBatch batch, InputHandler.TouchButton tb, Texture icon) {
        if (tb == null || icon == null) return;
        float pad = tb.w * 0.25f;
        batch.draw(icon, tb.x + pad, tb.y + pad, tb.w - pad * 2, tb.h - pad * 2);
    }

    public void dispose() {
        circle.dispose();
        attack.dispose();
        menuIcon.dispose();
    }

    // ------------------------------------------------------- 纹理生成

    private static Texture wrap(Pixmap pm) {
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private static Texture createCircleTexture() {
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(size / 2, size / 2, size / 2 - 1);
        return wrap(pm);
    }

    private static Texture createAttackTexture() {
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillTriangle(26, 12, 38, 12, 32, 4);
        pm.fillRectangle(28, 12, 8, 30);
        pm.fillRectangle(18, 42, 28, 4);
        pm.fillRectangle(29, 46, 6, 10);
        pm.fillRectangle(26, 56, 12, 4);
        return wrap(pm);
    }

    private static Texture createMenuTexture() {
        int size = 64;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillRectangle(16, 18, 32, 6);
        pm.fillRectangle(16, 29, 32, 6);
        pm.fillRectangle(16, 40, 32, 6);
        return wrap(pm);
    }
}