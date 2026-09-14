package com.mojang.ld22.android;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mojang.ld22.Game;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				showCrashDialog(e);
			}
		});

		// 从 PackageManager 读取 versionName，同步给 Game.version
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			Game.version = pInfo.versionName;
		} catch (Exception e) {
			// 读不到就用 Game 里的默认值
		}

		AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
		configuration.useImmersiveMode = true;
		initialize(new Game(), configuration);
	}

	private void showCrashDialog(final Throwable e) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				final String stackTrace = sw.toString();

				AlertDialog.Builder builder = new AlertDialog.Builder(AndroidLauncher.this);
				builder.setTitle("游戏崩溃了");
				builder.setMessage("很抱歉，游戏遇到了一个错误。\n\n" + stackTrace);
				builder.setPositiveButton("复制错误信息", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText("崩溃日志", stackTrace);
						clipboard.setPrimaryClip(clip);
						Toast.makeText(AndroidLauncher.this, "错误信息已复制到剪贴板", Toast.LENGTH_SHORT).show();
						finish();
					}
				});
				builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
				builder.setCancelable(false);
				builder.show();
			}
		});
	}
}
