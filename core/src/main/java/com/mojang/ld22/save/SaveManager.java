package com.mojang.ld22.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.mojang.ld22.Game;
import com.mojang.ld22.entity.Player;
import com.mojang.ld22.level.Level;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 存档 / 读档管理器。
 *
 * <p>文件位置：{@code saves/slot0.dat}（{@link Gdx#files} 的 local 目录）。
 */
public final class SaveManager {

    private static final int    MAGIC   = 0x4D4E4353; // "MNCS"
    private static final int    VERSION = 1;
    private static final String DIR     = "saves";
    private static final String FILE    = "slot0.dat";

    private SaveManager() {}

    // ---------------------------------------------------------------- 查询

    public static boolean exists() {
        return Gdx.files.local(DIR + "/" + FILE).exists();
    }

    public static void delete() {
        FileHandle fh = Gdx.files.local(DIR + "/" + FILE);
        if (fh.exists()) fh.delete();
    }

    // ---------------------------------------------------------------- 保存

    public static boolean save(Game game) {
        DataOutputStream out = null;
        try {
            FileHandle fh = Gdx.files.local(DIR + "/" + FILE);
            fh.parent().mkdirs();
            out = new DataOutputStream(new BufferedOutputStream(fh.write(false)));
            writeAll(out, game);
            out.flush();
            return true;
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "save failed", e);
            return false;
        } finally {
            close(out);
        }
    }

    private static void writeAll(DataOutputStream out, Game game) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(VERSION);

        out.writeInt(game.gameTime);
        out.writeInt(game.getCurrentLevel());
        out.writeBoolean(game.hasWon);
        out.writeInt(game.getWonTimer());

        Level[] levels = game.getLevels();

        // 关卡头
        out.writeInt(levels.length);
        for (Level lv : levels) {
            out.writeInt(lv.w);
            out.writeInt(lv.h);
            out.writeInt(lv.getDepth());
            out.writeInt(lv.monsterDensity);
            out.writeInt(lv.grassColor);
            out.writeInt(lv.dirtColor);
            out.writeInt(lv.sandColor);
        }

        // 地形
        for (Level lv : levels) lv.writeTiles(out);

        // 实体（不含 player）
        for (Level lv : levels) lv.writeEntities(out);

        // 玩家
        game.player.write(out);
    }

    // ---------------------------------------------------------------- 读取

    public static boolean load(Game game) {
        if (!exists()) return false;
        DataInputStream in = null;
        try {
            FileHandle fh = Gdx.files.local(DIR + "/" + FILE);
            in = new DataInputStream(new BufferedInputStream(fh.read()));
            readAll(in, game);
            return true;
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "load failed", e);
            return false;
        } finally {
            close(in);
        }
    }

    private static void readAll(DataInputStream in, Game game) throws IOException {
        int magic = in.readInt();
        if (magic != MAGIC) throw new IOException("bad magic: 0x" + Integer.toHexString(magic));
        int version = in.readInt();
        if (version != VERSION) throw new IOException("unsupported version: " + version);

        int gameTime     = in.readInt();
        int currentLevel = in.readInt();
        boolean hasWon   = in.readBoolean();
        int wonTimer     = in.readInt();

        int n = in.readInt();
        int[] ws = new int[n], hs = new int[n], ds = new int[n], md = new int[n];
        int[] gc = new int[n], dc = new int[n], sc = new int[n];
        for (int i = 0; i < n; i++) {
            ws[i] = in.readInt();
            hs[i] = in.readInt();
            ds[i] = in.readInt();
            md[i] = in.readInt();
            gc[i] = in.readInt();
            dc[i] = in.readInt();
            sc[i] = in.readInt();
        }

        Level[] levels = new Level[n];
        for (int i = 0; i < n; i++) {
            levels[i] = new Level(ws[i], hs[i], ds[i]);   // 空关卡构造器
            levels[i].monsterDensity = md[i];
            levels[i].grassColor     = gc[i];
            levels[i].dirtColor      = dc[i];
            levels[i].sandColor      = sc[i];
        }

        // 地形
        for (int i = 0; i < n; i++) levels[i].readTiles(in);

        // 实体
        for (int i = 0; i < n; i++) levels[i].readEntities(in, game);

        // 玩家
        Player player = new Player(game, game.input);
        player.read(in);

        // 回写 Game
        game.gameTime = gameTime;
        game.hasWon   = hasWon;
        game.setWonTimer(wonTimer);
        game.setLevels(levels, currentLevel);
        game.player = player;
        game.level  = levels[currentLevel];
        game.level.add(player);   // 会设置 level.player
    }

    private static void close(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) {}
    }
}