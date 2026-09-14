package com.mojang.ld22.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

public class Messages {

	public static final String NO_TEXT_FOUND = "!!!NO TEXT!!!";

	private static final String PREFS_NAME = "minicraft_settings";
	private static final String PREFS_KEY_LANG = "language";

	private static I18NBundle bundle;
	private static Languages lang;

	public static void init() {
		Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
		String code = prefs.getString(PREFS_KEY_LANG, null);
		Languages selected = fromCode(code);
		if (selected == null) {
			selected = matchSystem();
		}
		setup(selected);
	}

	public static void setup(Languages language) {
		lang = language;

		I18NBundle.setExceptionOnMissingKey(false);

		Locale locale = language.toLocale();
		String path = "i18n/messages";
		bundle = I18NBundle.createBundle(Gdx.files.internal(path), locale);

		Gdx.app.log("Messages", "language set to " + language.code() + " locale=" + locale);

		Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
		prefs.putString(PREFS_KEY_LANG, language.code());
		prefs.flush();
	}

	public static Languages lang() {
		return lang;
	}

	private static Languages fromCode(String code) {
		if (code == null) return null;
		for (Languages l : Languages.values()) {
			if (l.code().equals(code)) return l;
		}
		return null;
	}

	private static Languages matchSystem() {
		Locale sys = Locale.getDefault();
		if ("zh".equals(sys.getLanguage())) return Languages.CHINESE;
		return Languages.ENGLISH;
	}

	public static String get(Object first, Object... rest) {
		if (first == null) {
			if (rest.length == 0) return NO_TEXT_FOUND;
			String key = String.valueOf(rest[0]);
			Object[] args = tail(rest);
			return resolve(null, key, args);
		}

		if (first instanceof String) {
			String key = (String) first;
			return resolve(null, key, rest);
		}

		if (rest.length == 0) return NO_TEXT_FOUND;

		Class<?> c = (first instanceof Class) ? (Class<?>) first : first.getClass();
		String key = String.valueOf(rest[0]);
		Object[] args = tail(rest);

		return resolve(c, key, args);
	}

	private static Object[] tail(Object[] arr) {
		if (arr.length <= 1) return new Object[0];
		Object[] out = new Object[arr.length - 1];
		System.arraycopy(arr, 1, out, 0, out.length);
		return out;
	}

	private static String resolve(Class<?> c, String key, Object... args) {
		String fullKey = buildKey(c, key);
		String value = bundle.get(fullKey);

		if (value.equals(fullKey)) {
			if (c != null && c.getSuperclass() != null) {
				return resolve(c.getSuperclass(), key, args);
			}
			return NO_TEXT_FOUND;
		}

		// 有参数时用 I18NBundle.format，支持 {0} {1} 占位符
		if (args.length > 0) {
			try {
				return bundle.format(fullKey, args);
			} catch (Exception e) {
				Gdx.app.error("Messages", "format error on key: " + fullKey, e);
				return value;
			}
		}
		return value;
	}

	private static String buildKey(Class<?> c, String key) {
		if (c == null) return key.toLowerCase(Locale.ENGLISH);
		String simple = c.getSimpleName().toLowerCase(Locale.ENGLISH);
		return simple + "." + key.toLowerCase(Locale.ENGLISH);
	}
}