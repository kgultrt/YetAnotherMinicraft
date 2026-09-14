package com.mojang.ld22.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class Shaders {

	public static ShaderProgram post;
	public static boolean postOk = false;

	public static void load() {
		post = compile("shaders/post", "post");
		postOk = (post != null && post.isCompiled());
	}

	private static ShaderProgram compile(String basePath, String tag) {
		FileHandle vf = Gdx.files.internal(basePath + ".vert");
		FileHandle ff = Gdx.files.internal(basePath + ".frag");

		if (!vf.exists() || !ff.exists()) {
			Gdx.app.error("Shaders", tag + ": file missing (" + vf.path() + " / " + ff.path() + ")");
			return null;
		}

		String vert = vf.readString("UTF-8");
		String frag = ff.readString("UTF-8");

		ShaderProgram p = new ShaderProgram(vert, frag);
		if (!p.isCompiled()) {
			Gdx.app.error("Shaders", tag + ": compile failed\n" + p.getLog());
			return null;
		}

		Gdx.app.log("Shaders", tag + ": compiled OK");
		return p;
	}

	public static void dispose() {
		if (post != null) {
			post.dispose();
			post = null;
		}
	}
}