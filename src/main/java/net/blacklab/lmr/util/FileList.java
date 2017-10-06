package net.blacklab.lmr.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.blacklab.lib.classutil.FileClassUtil;
import net.blacklab.lmr.LittleMaidReengaged;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

public class FileList {

	public static class CommonClassLoaderWrapper extends URLClassLoader{

		public CommonClassLoaderWrapper(URL[] urls, ClassLoader parent) {
			super(urls, parent);
			// TODO 自動生成されたコンストラクター・スタブ
		}

		@Override
		public void addURL(URL url) {
			// 可視化
			if (new ArrayList(Arrays.asList(getURLs())).contains(url)) return;
			super.addURL(url);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			LittleMaidReengaged.Debug("loadClass:".concat(name));
			try {
				if (name.contains(".")) {
					// 追加のマルチモデルらしきクラス以外は普通に読み込む
					return super.loadClass(name);
				}
			} catch (NoClassDefFoundError e) {
				// noop
			}

			LittleMaidReengaged.Debug("Remapping class:".concat(name));
			String classFilename = name.replace('.', '/').concat(".class");
			LittleMaidReengaged.Debug(classFilename);
			try {
				// .classファイルをバイナリとして読み込む
				InputStream input = getResourceAsStream(classFilename);
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int data = 0;
				data = input.read();
				while (data != -1) {
					buffer.write(data);
					data = input.read();
				}
				input.close();

				byte[] originalBytecode = buffer.toByteArray();
				byte[] remappedBytecode;

				// MMM_ほにゃららクラスを参照している部分を、すべて書き換える
				remappedBytecode = rewirteBaseMMMClassName(originalBytecode);

				// DEBUG: .classファイルを出力する
				/*
				FileOutputStream fos = new FileOutputStream(name.concat(".class"));
				fos.write(remappedBytecode);
				fos.close();
				*/

				// バイナリからクラスを生成する
				return defineClass(name, remappedBytecode, 0, remappedBytecode.length);
			} catch (Exception e1) {
				// それでも読み込めないものはしょうがない
				e1.printStackTrace();
				throw new ClassNotFoundException(name);
			}
		}

		private byte[] rewirteBaseMMMClassName(byte[] bytecode) {
			ClassReader classReader = new ClassReader(bytecode);
			ClassWriter classWriter = new ClassWriter(classReader, 0);

			Remapper remapper = new MMMClassRemapper();
			classReader.accept(new RemappingClassAdapter(classWriter, remapper), ClassReader.EXPAND_FRAMES);

			return classWriter.toByteArray();
		}
	}

	public static class MMMClassRemapper extends Remapper {
		@Override
		public String map(String typeName) {
			/// MMM_から始まるクラスの代わりにnet.blacklab.lmr.entity.maidmodelパッケージのクラスを使う
			boolean isMMM;
			try {
				isMMM = "MMM_".equals(typeName.substring(0, 4));
			} catch (StringIndexOutOfBoundsException e) {
				isMMM = false;
			}
			if (isMMM) {
				try {
					Class<?> aClass = Class.forName("net.blacklab.lmr.entity.maidmodel.".concat(typeName.substring(4)));
					return aClass.getCanonicalName().replace(".", "/");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			return super.map(typeName);
		}

		@Override
		public String mapType(String typeName) {
			boolean isMMM;
			try {
				isMMM = "MMM_".equals(typeName.substring(0, 4));
			} catch (StringIndexOutOfBoundsException e) {
				isMMM = false;
			}
			if (isMMM) {
				try {
					Class<?> aClass = Class.forName("net.blacklab.lmr.entity.maidmodel.".concat(typeName.substring(4)));
					return aClass.getCanonicalName().replace(".", "/");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			return super.mapType(typeName);
		}
	}

	public static File dirMinecraft;
	public static File dirMods;
	public static File dirModsVersion;
	public static File dirDevClasses;
	public static File dirDevClassAssets;
	public static List<File> dirDevIncludeClasses = new ArrayList<File>();
//	public static File[] dirDevIncludeAssets = new File[]{};

	public static List<File> files;
	public static String dirMinecraftPath	= "";
//	public static File   minecraftJar	= null;	// minecraft.jarを見に行くのは昔の仕様？
	public static String assetsDir		= "";	// mods/LittleMaidX/assets
	public static boolean isDevdir;
	public static Map<String,List<File>>    fileList = new HashMap<String, List<File>>();

	public static CommonClassLoaderWrapper COMMON_CLASS_LOADER;

	static {
		Object[] injectionData = FMLInjectionData.data();
		dirMinecraft = (File) FMLInjectionData.data()[6];
		dirMinecraftPath = FileClassUtil.getLinuxAntiDotName(dirMinecraft.getAbsolutePath());
		if (dirMinecraftPath.endsWith("/")) {
			dirMinecraftPath = dirMinecraftPath.substring(0, dirMinecraftPath.lastIndexOf("/"));
		}
		dirMods = new File(dirMinecraft, "mods");

		// 開発モード
		if(DevMode.DEVMODE != DevMode.NOT_IN_DEV){
			// Linux準拠の形式に変更
			String dirProjectPath = FileClassUtil.getParentDir(dirMinecraftPath);
			
			String binPath = "";
			String assetsPath = "";

			// Game Directoryの直上のディレクトリを見に行く.
			if(DevMode.DEVMODE == DevMode.DEVMODE_ECLIPSE) {
				binPath = dirProjectPath.concat("/bin");
			} else if(DevMode.DEVMODE == DevMode.DEVMODE_NO_IDE) {
				binPath = dirProjectPath.concat("/build/classes/main");
				assetsPath = dirProjectPath.concat("/build/resources/main");
				dirDevClassAssets = new File(assetsPath);
			}
			dirDevClasses = new File(binPath);
			if(!dirDevClasses.exists() || !dirDevClasses.isDirectory())
				throw new IllegalStateException("Could not get dev class path.");

			if (DevMode.DEVMODE == DevMode.DEVMODE_ECLIPSE) {
				for(int i=0; i<DevMode.INCLUDEPROJECT.length; i++){
					String c = FileClassUtil.getParentDir(dirProjectPath)+"/"+DevMode.INCLUDEPROJECT[i]+"/bin";
					dirDevIncludeClasses.add(new File(c));
				}
			}
		}
		dirModsVersion = new File(dirMods, (String)injectionData[4]);
		LittleMaidReengaged.Debug("init FileManager.");
	}

	// TODO 今後使用しなさそう
	/*
	public static void setSrcPath(File file)
	{
		assetsDir = file.getPath() + "/assets";
		LittleMaidReengaged.Debug("mods path =" + dirMods.getAbsolutePath());

		// eclipseの環境の場合、eclipseフォルダ配下のmodsを見に行く
		isDevdir = file.getName().equalsIgnoreCase("bin");
		if(isDevdir)
		{
			dirMods = new File(file.getParent()+"/eclipse/mods");
		}
		else
		{
			dirMods = new File(file.getParent());
		}
	}
	*/

	/**
	 * modsディレクトリに含まれるファイルを全て返す。<br>
	 * バージョンごとの物も含む。
	 * @return
	 */
	/*
	public static List<File> getAllmodsFiles() {
		List<File> llist = new ArrayList<File>();
		if (dirMods.exists()) {
			for (File lf : dirMods.listFiles()) {
				llist.add(lf);
			}
		}
		if (dirModsVersion.exists()) {
			for (File lf : dirModsVersion.listFiles()) {
				llist.add(lf);
			}
		}
		files = llist;
		return llist;
	}
	public static List<File> getAllmodsFiles(ClassLoader pClassLoader) {
		List<File> llist = new ArrayList<File>();
		if (pClassLoader instanceof URLClassLoader ) {
			for (URL lurl : ((URLClassLoader)pClassLoader).getURLs()) {
				try {
					String ls = lurl.toString();
					if (ls.endsWith("/bin/") || ls.indexOf("/mods/") > -1) {
						llist.add(new File(lurl.toURI()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		files = llist;
		return llist;
	}
	*/
	public static List<File> getAllmodsFiles(ClassLoader pClassLoader, boolean pFlag) {
		List<File> llist = new ArrayList<File>();
		if (pClassLoader instanceof URLClassLoader ) {
			for (URL lurl : ((URLClassLoader)pClassLoader).getURLs()) {
				try {
					String ls = lurl.toString();
					if (ls.endsWith("/bin/") || ls.indexOf("/mods/") > -1) {
						llist.add(new File(lurl.toURI()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (pFlag) {
			if (dirMods.exists()) {
				for (File lf : dirMods.listFiles()) {
					addList(llist, lf);
				}
			}
			if (dirModsVersion.exists()) {
				for (File lf : dirModsVersion.listFiles()) {
					addList(llist, lf);
				}
			}
		}
		files = llist;
		return llist;
	}

	protected static boolean addList(List<File> pList, File pFile) {
		for (File lf : pList) {
			try {
				if (pFile.getCanonicalPath().compareTo(lf.getCanonicalPath()) == 0) {
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		pList.add(pFile);
		return true;
	}



	/**
	 * MODディレクトリに含まれる対象ファイルのオブジェクトを取得。
	 * @param pname 検索リスト名称、getFileList()で使う。
	 * @param pprefix この文字列の含まれるファイルを列挙する。
	 * @return 列挙されたファイルのリスト。
	 */
	public static List<File> getModFile(String pname, String pprefix) {
		// 検索済みかどうかの判定
		List<File> llist;
		if (fileList.containsKey(pname)) {
			llist = fileList.get(pname);
		} else {
			llist = new ArrayList<File>();
			fileList.put(pname, llist);
		}

		LittleMaidReengaged.Debug("getModFile:[%s]:%s", pname, dirMods.getAbsolutePath());
		// ファイル・ディレクトリを検索
		if(DevMode.DEVMODE != DevMode.NOT_IN_DEV){
			//開発モード時はそちらを優先
			llist.add(dirDevClasses);
			if(DevMode.DEVMODE == DevMode.DEVMODE_NO_IDE) llist.add(dirDevClassAssets);
			if(DevMode.DEVMODE == DevMode.DEVMODE_ECLIPSE){
				for(File f:dirDevIncludeClasses){
					llist.add(f);
				}
			}
		}
		try {
			if (dirMods.isDirectory()) {
				LittleMaidReengaged.Debug("getModFile-get:%d.", dirMods.list().length);
				for (File t : dirMods.listFiles()) {
					if (t.getName().indexOf(pprefix) != -1) {
						if (t.getName().endsWith(".zip") || t.getName().endsWith(".jar")) {
							llist.add(t);
							LittleMaidReengaged.Debug("getModFile-file:%s", t.getName());
						} else if (t.isDirectory()) {
							llist.add(t);
							LittleMaidReengaged.Debug("getModFile-file:%s", t.getName());
						}
					}
				}
				LittleMaidReengaged.Debug("getModFile-files:%d", llist.size());
			} else {
				// まずありえない
				LittleMaidReengaged.Debug("getModFile-fail.");
			}
		}
		catch (Exception exception) {
			LittleMaidReengaged.Debug("getModFile-Exception.");
		}
		return llist;

	}
	public static void debugPrintAllFileList()
	{
		for(String key : fileList.keySet())
		{
			List<File> list = fileList.get(key);
			for(File f : list)
			{
				System.out.println("MMMLib-AllFileList ### " + key + " : " + f.getPath());
			}
		}
	}

	public static List<File> getFileList(String pname)
	{
		return fileList.get(pname);
	}
}
