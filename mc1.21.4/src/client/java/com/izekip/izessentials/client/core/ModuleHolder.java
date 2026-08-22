package com.izekip.izessentials.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * "Su an aktif olan icerik modulu" referansini tutar ve canli guncelleme (hot-swap) mantigini
 * uygular. Tum metotlar client (render) thread'inden cagrilmalidir (ClientTickEvents/
 * ClientPlayConnectionEvents zaten o thread'de tetiklenir) - burada ekstra kilitleme
 * karmasikligina girmemek icin bilerek boyle tasarlandi, arka plan thread'inden asla
 * dogrudan cagrilmamali (bkz. ModuleUpdater - indirme arka planda, uygulama Minecraft.execute ile).
 */
public final class ModuleHolder {
	private static final Logger LOGGER = LoggerFactory.getLogger("aurafriendly/module");
	private static final String ENTRY_POINT_CLASS = "com.izekip.izessentials.client.ContentEntryPoint";

	private static AuraFriendlyModule current = new NoOpModule();
	private static ClassLoader currentContentLoader;

	private ModuleHolder() {
	}

	public static AuraFriendlyModule current() {
		return current;
	}

	/** Ilk acilista, ana jar'in kendi classloader'i ile icerik modulunu yukler (ayri dosya/indirme gerekmez). */
	public static void loadInitial() {
		try {
			ClassLoader ownLoader = ModuleHolder.class.getClassLoader();
			Class<?> cls = Class.forName(ENTRY_POINT_CLASS, true, ownLoader);
			AuraFriendlyModule module = (AuraFriendlyModule) cls.getDeclaredConstructor().newInstance();
			module.onLoad();
			current = module;
			currentContentLoader = ownLoader;
			LOGGER.info("Ilk icerik modulu yuklendi.");
		} catch (Throwable t) {
			LOGGER.error("Ilk icerik modulu yuklenemedi, NoOp ile devam ediliyor (mod calismayacak).", t);
		}
	}

	/**
	 * Indirilen yeni jar'dan icerik modulunu yuklemeyi dener. Basarili olursa eski modul
	 * duzgunce kapatilir (onUnload) ve eski classloader kapatilir (dosya kilidi cozulur).
	 * Herhangi bir uyumsuzluk/hata durumunda hicbir sey degistirmeden false doner - cagiran
	 * taraf (ModuleUpdater) bu durumda eski "jar'i indir + yeniden baslat" yontemine dusmeli.
	 */
	public static synchronized boolean trySwap(Path newJarPath) {
		try {
			ClassLoader parent = ModuleHolder.class.getClassLoader();
			ContentClassLoader loader = new ContentClassLoader(newJarPath.toUri().toURL(), parent);
			Class<?> cls = Class.forName(ENTRY_POINT_CLASS, true, loader);
			Object instance = cls.getDeclaredConstructor().newInstance();
			if (!(instance instanceof AuraFriendlyModule newModule)) {
				loader.close();
				LOGGER.warn("Indirilen jar'daki giris sinifi AuraFriendlyModule uygulamiyor, canli guncelleme iptal.");
				return false;
			}

			newModule.onLoad();

			AuraFriendlyModule old = current;
			ClassLoader oldLoader = currentContentLoader;
			current = newModule;
			currentContentLoader = loader;

			safeUnload(old);
			closeIfClosable(oldLoader);

			LOGGER.info("Icerik modulu canli olarak degistirildi.");
			return true;
		} catch (Throwable t) {
			LOGGER.warn("Canli guncelleme basarisiz (uyumsuzluk olabilir), yeniden baslatma gerekecek.", t);
			return false;
		}
	}

	private static void safeUnload(AuraFriendlyModule old) {
		try {
			old.onUnload(net.minecraft.client.Minecraft.getInstance());
		} catch (Throwable t) {
			LOGGER.warn("Eski modul kapatilirken hata (yok sayiliyor, yeni modul zaten devrede).", t);
		}
	}

	private static void closeIfClosable(ClassLoader loader) {
		if (loader instanceof URLClassLoader urlClassLoader) {
			try {
				urlClassLoader.close();
			} catch (IOException e) {
				LOGGER.debug("Eski classloader kapatilirken hata (onemli degil): {}", e.toString());
			}
		}
	}
}
