package com.mojang.ld22.sound;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * 背景音乐管理器。
 *
 * <p>单例，跟 {@link Sound} 一样。加载时不会播，play() 才播。
 * 切换时旧的淡出、新的淡入，各 1.2 秒，避免硬切。
 *
 * <p>音效音量是 1.0，音乐默认 0.55 —— 背景音乐不该盖过音效。
 */
public class MusicManager {

    private static final MusicManager INSTANCE = new MusicManager();
    public static MusicManager get() { return INSTANCE; }

    private static final float DEFAULT_VOLUME = 0.55f;

    /**
     * 淡入淡出时长（秒）。1.2 秒对像素游戏刚好 ——
     * 太快像开关，太慢像两首歌在打架。
     */
    private static final float FADE_SECONDS = 1.2f;

    private final Map<String, Music> tracks = new HashMap<>();

    private String currentName;
    private Music current;

    /** 淡入剩余秒数。<= 0 表示已达目标音量。 */
    private float fadeInRemain;

    /** 正在淡出的旧曲。null 表示没有。 */
    private Music fadingOut;
    private float fadeOutRemain;

    private float volume = DEFAULT_VOLUME;

    private MusicManager() {}

    /**
     * 加载一首曲子。路径相对于 assets 根目录。
     *
     * <p>加载失败只打日志，不抛异常 —— 缺曲子不该让游戏崩。
     */
    public void load(String name, String path) {
        try {
            Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
            m.setLooping(true);
            m.setVolume(0f);
            tracks.put(name, m);
        } catch (Throwable e) {
            System.err.println("Failed to load music: " + path);
        }
    }

    /**
     * 播放指定音乐。已经在放同一首时什么都不做。
     * 切换时旧的淡出、新的淡入。
     */
    public void play(String name) {
        if (name == null) {
            stop();
            return;
        }
        if (name.equals(currentName) && current != null && current.isPlaying()) {
            return;
        }

        Music next = tracks.get(name);
        if (next == null) {
            // 找不到这首（可能没加载）—— 停掉当前的，保持安静
            if (current != null && current.isPlaying()) {
                fadingOut = current;
                fadeOutRemain = FADE_SECONDS;
            }
            current = null;
            currentName = null;
            fadeInRemain = 0;
            return;
        }

        if (current != null && current.isPlaying()) {
            fadingOut = current;
            fadeOutRemain = FADE_SECONDS;
        }

        current = next;
        currentName = name;
        next.setVolume(0f);
        next.play();
        fadeInRemain = FADE_SECONDS;
    }

    public void stop() {
        if (current != null) current.stop();
        if (fadingOut != null) {
            fadingOut.stop();
            fadingOut = null;
        }
        current = null;
        currentName = null;
        fadeInRemain = 0;
        fadeOutRemain = 0;
    }

    /** 暂停当前音乐。用于游戏暂停。 */
    public void pause() {
        if (current != null && current.isPlaying()) current.pause();
        if (fadingOut != null) fadingOut.pause();
    }

    /** 恢复当前音乐。用于游戏继续。 */
    public void resume() {
        if (current != null && !current.isPlaying()) current.play();
        if (fadingOut != null) fadingOut.play();
    }

    public void setVolume(float v) {
        if (v < 0) v = 0;
        if (v > 1) v = 1;
        this.volume = v;
    }

    public float getVolume() {
        return volume;
    }

    /** 每帧推进淡入淡出。delta 单位是秒。 */
    public void update(float delta) {
        if (current != null) {
            if (fadeInRemain > 0) {
                fadeInRemain -= delta;
                float t = 1f - Math.max(0f, fadeInRemain) / FADE_SECONDS;
                current.setVolume(volume * t);
            } else {
                current.setVolume(volume);
            }
        }

        if (fadingOut != null) {
            fadeOutRemain -= delta;
            float t = Math.max(0f, fadeOutRemain) / FADE_SECONDS;
            fadingOut.setVolume(volume * t);
            if (fadeOutRemain <= 0) {
                fadingOut.stop();
                fadingOut = null;
            }
        }
    }

    public void dispose() {
        for (Music m : tracks.values()) {
            if (m != null) m.dispose();
        }
        tracks.clear();
        current = null;
        fadingOut = null;
        currentName = null;
    }
}