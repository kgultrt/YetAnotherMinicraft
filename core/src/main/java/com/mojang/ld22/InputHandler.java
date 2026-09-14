package com.mojang.ld22;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

import java.util.ArrayList;
import java.util.List;

public class InputHandler implements InputProcessor {
	public class Key {
		public int presses, absorbs;
		public boolean down, clicked;

		public Key() {
			keys.add(this);
		}

		public void toggle(boolean pressed) {
			if (pressed != down) {
				down = pressed;
			}
			if (pressed) {
				presses++;
			}
		}

		public void tick() {
			if (absorbs < presses) {
				absorbs++;
				clicked = true;
			} else {
				clicked = false;
			}
		}
	}

	public static class TouchButton {
		public final InputHandler.Key key;
		public float x, y, w, h;
		public int activePointer = -1;

		public TouchButton(InputHandler.Key key) {
			this.key = key;
		}

		public boolean contains(float px, float py) {
			return px >= x && px < x + w && py >= y && py < y + h;
		}
	}

	public static class Joystick {
		public float cx, cy;
		public float radius;
		public float knobRadius;
		public float knobX, knobY;
		public int activePointer = -1;
	}

	public List<Key> keys = new ArrayList<Key>();

	public Key up = new Key();
	public Key down = new Key();
	public Key left = new Key();
	public Key right = new Key();
	public Key attack = new Key();
	public Key menu = new Key();

	/**
	 * 暂停键。映射到 ESC 和 Android 返回键。
	 *
	 * <p>Game 在游戏中检测 {@code pause.clicked} 弹暂停菜单，
	 * PauseMenu 自己检测它来关闭。Android 上返回键的默认行为是
	 * 退出应用 —— 必须在 {@link #keyDown} 里返回 true 消费掉，
	 * 否则 libGDX 会先一步把游戏关掉。
	 */
	public Key pause = new Key();

	public Joystick joystick = new Joystick();

	public List<TouchButton> touchButtons = new ArrayList<TouchButton>();
	public TouchButton btnAttack, btnMenu;

	private int screenW = 0, screenH = 0;

	public InputHandler() {
		btnAttack = new TouchButton(attack);
		btnMenu = new TouchButton(menu);
		touchButtons.add(btnAttack);
		touchButtons.add(btnMenu);
	}

	public void releaseAll() {
		for (int i = 0; i < keys.size(); i++) {
			keys.get(i).down = false;
		}
		for (int i = 0; i < touchButtons.size(); i++) {
			touchButtons.get(i).activePointer = -1;
		}
		joystick.activePointer = -1;
		joystick.knobX = joystick.cx;
		joystick.knobY = joystick.cy;
	}

	public void tick() {
		for (int i = 0; i < keys.size(); i++) {
			keys.get(i).tick();
		}
	}

	public void layoutTouchControls(int sw, int sh) {
		this.screenW = sw;
		this.screenH = sh;

		float pad = sh * 0.05f;

		float jsRadius = sh * 0.16f;
		joystick.radius = jsRadius;
		joystick.knobRadius = jsRadius * 0.42f;
		joystick.cx = pad + jsRadius;
		joystick.cy = pad + jsRadius;
		joystick.knobX = joystick.cx;
		joystick.knobY = joystick.cy;

		float btnSize = sh * 0.15f;
		float atkSize = btnSize * 1.4f;
		float atkX = sw - pad - atkSize;
		float atkY = pad;
		set(btnAttack, atkX, atkY, atkSize, atkSize);

		float menuSize = btnSize;
		float menuX = atkX - pad - menuSize;
		float menuY = pad + (atkSize - menuSize) * 0.5f;
		set(btnMenu, menuX, menuY, menuSize, menuSize);
	}

	private static void set(TouchButton b, float x, float y, float w, float h) {
		b.x = x;
		b.y = y;
		b.w = w;
		b.h = h;
	}

	// ---------- 键盘 ----------

	@Override
	public boolean keyDown(int keycode) {
		toggle(keycode, true);

		// Android 返回键：消费掉，阻止 libGDX 默认的"退出应用"。
		// 不返回 true 的话，游戏会在我们处理之前就被关掉。
		if (keycode == Input.Keys.BACK) return true;

		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		toggle(keycode, false);
		if (keycode == Input.Keys.BACK) return true;
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	private void toggle(int keycode, boolean pressed) {
		if (keycode == Input.Keys.NUMPAD_8) up.toggle(pressed);
		if (keycode == Input.Keys.NUMPAD_2) down.toggle(pressed);
		if (keycode == Input.Keys.NUMPAD_4) left.toggle(pressed);
		if (keycode == Input.Keys.NUMPAD_6) right.toggle(pressed);
		if (keycode == Input.Keys.W) up.toggle(pressed);
		if (keycode == Input.Keys.S) down.toggle(pressed);
		if (keycode == Input.Keys.A) left.toggle(pressed);
		if (keycode == Input.Keys.D) right.toggle(pressed);
		if (keycode == Input.Keys.UP) up.toggle(pressed);
		if (keycode == Input.Keys.DOWN) down.toggle(pressed);
		if (keycode == Input.Keys.LEFT) left.toggle(pressed);
		if (keycode == Input.Keys.RIGHT) right.toggle(pressed);

		if (keycode == Input.Keys.TAB) menu.toggle(pressed);
		if (keycode == Input.Keys.ALT_LEFT) menu.toggle(pressed);
		if (keycode == Input.Keys.ALT_RIGHT) menu.toggle(pressed);
		if (keycode == Input.Keys.SPACE) attack.toggle(pressed);
		if (keycode == Input.Keys.CONTROL_LEFT) attack.toggle(pressed);
		if (keycode == Input.Keys.CONTROL_RIGHT) attack.toggle(pressed);
		if (keycode == Input.Keys.NUMPAD_0) attack.toggle(pressed);
		if (keycode == Input.Keys.INSERT) attack.toggle(pressed);
		if (keycode == Input.Keys.ENTER) menu.toggle(pressed);

		if (keycode == Input.Keys.X) menu.toggle(pressed);
		if (keycode == Input.Keys.C) attack.toggle(pressed);

		// 暂停键：ESC（桌面）和 BACK（Android）
		if (keycode == Input.Keys.ESCAPE) pause.toggle(pressed);
		if (keycode == Input.Keys.BACK) pause.toggle(pressed);
	}

	// ---------- 摇杆辅助 ----------

	private float toDrawY(int screenY) {
		return screenH - screenY;
	}

	private boolean joystickContains(float px, float py) {
		float dx = px - joystick.cx;
		float dy = py - joystick.cy;
		float r = joystick.radius * 1.6f;
		return dx * dx + dy * dy <= r * r;
	}

	private void setKey(Key k, boolean pressed) {
		if (k.down != pressed) {
			k.toggle(pressed);
		}
	}

	private void resetJoystickDirection() {
		setKey(up, false);
		setKey(down, false);
		setKey(left, false);
		setKey(right, false);
	}

	private void updateJoystick(float px, float py) {
		Joystick js = joystick;
		float dx = px - js.cx;
		float dy = py - js.cy;
		float dist = (float) Math.sqrt(dx * dx + dy * dy);

		if (dist > js.radius) {
			float k = js.radius / dist;
			dx *= k;
			dy *= k;
			dist = js.radius;
		}

		js.knobX = js.cx + dx;
		js.knobY = js.cy + dy;

		float deadZone = js.radius * 0.30f;
		if (dist < deadZone) {
			resetJoystickDirection();
			return;
		}

		float nx = dx / dist;
		float ny = dy / dist;

		float t = 0.3827f;

		setKey(left,  nx < -t);
		setKey(right, nx >  t);
		setKey(up,    ny >  t);
		setKey(down,  ny < -t);
	}

	// ---------- 触控 ----------

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		float y = toDrawY(screenY);

		if (joystick.activePointer == -1 && joystickContains(screenX, y)) {
			joystick.activePointer = pointer;
			updateJoystick(screenX, y);
			return true;
		}

		for (int i = 0; i < touchButtons.size(); i++) {
			TouchButton tb = touchButtons.get(i);
			if (tb.activePointer == -1 && tb.contains(screenX, y)) {
				tb.activePointer = pointer;
				tb.key.toggle(true);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		boolean handled = false;

		if (joystick.activePointer == pointer) {
			joystick.activePointer = -1;
			joystick.knobX = joystick.cx;
			joystick.knobY = joystick.cy;
			resetJoystickDirection();
			handled = true;
		}

		for (int i = 0; i < touchButtons.size(); i++) {
			TouchButton tb = touchButtons.get(i);
			if (tb.activePointer == pointer) {
				tb.activePointer = -1;
				tb.key.toggle(false);
				handled = true;
			}
		}
		return handled;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		float y = toDrawY(screenY);

		if (joystick.activePointer == pointer) {
			updateJoystick(screenX, y);
			return true;
		}

		for (int i = 0; i < touchButtons.size(); i++) {
			TouchButton tb = touchButtons.get(i);
			if (tb.activePointer == pointer && !tb.contains(screenX, y)) {
				tb.activePointer = -1;
				tb.key.toggle(false);
			}
		}
		for (int i = 0; i < touchButtons.size(); i++) {
			TouchButton tb = touchButtons.get(i);
			if (tb.activePointer == -1 && tb.contains(screenX, y)) {
				tb.activePointer = pointer;
				tb.key.toggle(true);
				break;
			}
		}
		return false;
	}

	@Override
	public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
		return touchUp(screenX, screenY, pointer, button);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		return false;
	}
}