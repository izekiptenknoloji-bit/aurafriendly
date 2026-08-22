package com.izekip.izessentials.client.core;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Icerik paketini (com.izekip.izessentials.client.* - "core" alt paketi haric) CHILD-FIRST
 * yukler: Java'nin normal PARENT-FIRST davranisiyla yuklenirse, indirilen yeni jar'daki
 * guncel siniflar yerine ana jar'in classloader'inda zaten yuklu ESKI siniflar kullanilir
 * ve canli guncelleme hicbir sey degistirmemis gibi olur.
 *
 * Her sey baska (Minecraft/Fabric/JDK/Gson siniflari VE AuraFriendlyModule arayuzunun kendisi)
 * normal sekilde parent'a delege edilir - boylece Minecraft'in kendi sinif kimlikleri bozulmaz
 * ve AuraFriendlyModule tek/ortak bir tip olarak kalir (yoksa instanceof/cast calismaz).
 */
final class ContentClassLoader extends URLClassLoader {
	private static final String CONTENT_PREFIX = "com.izekip.izessentials.client.";
	private static final String CORE_PREFIX = "com.izekip.izessentials.client.core.";

	ContentClassLoader(URL jarUrl, ClassLoader parent) {
		super(new URL[] { jarUrl }, parent);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded == null) {
				if (name.startsWith(CONTENT_PREFIX) && !name.startsWith(CORE_PREFIX)) {
					try {
						loaded = findClass(name);
					} catch (ClassNotFoundException e) {
						loaded = super.loadClass(name, false);
					}
				} else {
					loaded = super.loadClass(name, false);
				}
			}
			if (resolve) {
				resolveClass(loaded);
			}
			return loaded;
		}
	}
}
