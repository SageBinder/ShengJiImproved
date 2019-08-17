package com.sage.shengji.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.sage.shengji.client.ShengJiGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
		config.samples = 16;
		config.forceExit = false;
		config.addIcon("icon_128x128.png", Files.FileType.Internal);
		config.addIcon("icon_32x32.png", Files.FileType.Internal);
		config.addIcon("icon_16x16.png", Files.FileType.Internal);
		new LwjglApplication(new ShengJiGame(), config);
	}
}
