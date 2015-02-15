package pipeline.misc_util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides mapping between OS name as reported by Java, folder name that we use to store
 * dynamic libraries for the corresponding architecture, and system suffixes for dynamic libraries.
 * Currently FreeBSD, OS X and Linux are covered.
 *
 */
public class DylibInfo {
	public String osName;
	public String directoryName;
	public String suffix;

	static String localOsName = System.getProperty("os.name");

	static Map<String, DylibInfo> dylibs = getDylibInfos();

	public static DylibInfo getDylibInfo() {
		if (!dylibs.containsKey(localOsName)) {
			throw new Error("Unrecognized os name " + localOsName);
		}
		return dylibs.get(localOsName);
	}

	public static Map<String, DylibInfo> getDylibInfos() {
		Map<String, DylibInfo> infos = new HashMap<>();
		DylibInfo osx = new DylibInfo();
		osx.osName = "Mac OS X";
		osx.directoryName = "macosx";
		osx.suffix = "dylib";
		infos.put(osx.osName, osx);

		DylibInfo freebsd = new DylibInfo();
		freebsd.osName = "FreeBSD";
		freebsd.directoryName = "freebsd";
		freebsd.suffix = "so";
		infos.put(freebsd.osName, freebsd);

		DylibInfo linux = new DylibInfo();
		linux.osName = "Linux";
		linux.directoryName = "linux";
		linux.suffix = "so";
		infos.put(linux.osName, linux);

		return infos;
	}

}
