package com.mojang.ld22.i18n;

import java.util.Locale;

public enum Languages {
	ENGLISH("en", "English", Status.OFFICIAL),
	CHINESE("zh_CN", "简体中文", Status.OFFICIAL);

	public enum Status {
		/** 开发者原生语言 */
		OFFICIAL,
		/** 完整翻译并已审阅 */
		COMPLETE,
		/** 翻译完整但未审阅 */
		UNREVIEWED,
		/** 翻译未完成 */
		UNFINISHED
	}

	private final String code;
	private final String label;
	private final Status status;

	Languages(String code, String label, Status status) {
		this.code = code;
		this.label = label;
		this.status = status;
	}

	public String code() { return code; }
	public String label() { return label; }
	public Status status() { return status; }

	public Locale toLocale() {
		if (this == ENGLISH) return Locale.ENGLISH;
		String[] parts = code.split("_");
		if (parts.length == 2) return new Locale(parts[0], parts[1]);
		return new Locale(code);
	}
}