package com.mojang.ld22.sound;

import com.badlogic.gdx.Gdx;

public class Sound {
	public static final Sound playerHurt = new Sound("playerhurt.wav");
	public static final Sound playerDeath = new Sound("death.wav");
	public static final Sound monsterHurt = new Sound("monsterhurt.wav");
	public static final Sound test = new Sound("test.wav");
	public static final Sound pickup = new Sound("pickup.wav");
	public static final Sound bossdeath = new Sound("bossdeath.wav");
	public static final Sound craft = new Sound("craft.wav");

	private final String path;
	private com.badlogic.gdx.audio.Sound gdxSound;

	private Sound(String name) {
		this.path = name;
	}

	public void load() {
		try {
			gdxSound = Gdx.audio.newSound(Gdx.files.internal(path));
		} catch (Throwable e) {
			System.err.println("Failed to load sound: " + path);
			e.printStackTrace();
		}
	}

	public void play() {
		if (gdxSound == null) return;
		try {
			gdxSound.play();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void loadAll() {
		playerHurt.load();
		playerDeath.load();
		monsterHurt.load();
		test.load();
		pickup.load();
		bossdeath.load();
		craft.load();
	}

	public static void disposeAll() {
		playerHurt.dispose();
		playerDeath.dispose();
		monsterHurt.dispose();
		test.dispose();
		pickup.dispose();
		bossdeath.dispose();
		craft.dispose();
	}

	private void dispose() {
		if (gdxSound != null) {
			gdxSound.dispose();
			gdxSound = null;
		}
	}
}