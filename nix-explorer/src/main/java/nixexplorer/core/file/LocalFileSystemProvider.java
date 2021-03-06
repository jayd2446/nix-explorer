package nixexplorer.core.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nixexplorer.PathUtils;
import nixexplorer.core.DataTransferProgress;
import nixexplorer.core.FileInfo;
import nixexplorer.core.FileSystemProvider;
import nixexplorer.core.FileType;

public class LocalFileSystemProvider implements FileSystemProvider {

	private static final String PROTO_LOCAL_FILE = "local";

	public void chmod(int perm, String path) throws Exception {
	}

	@Override
	public FileInfo getInfo(String path)
			throws FileNotFoundException, IOException {
		File f = new File(path);
		if (!f.exists()) {
			throw new FileNotFoundException(path);
		}
		Path p = f.toPath();
		BasicFileAttributes attrs = Files.readAttributes(p,
				BasicFileAttributes.class);
		FileInfo info = new FileInfo(f.getName(), path, f.length(),
				f.isDirectory() ? FileType.Directory : FileType.File,
				f.lastModified(), -1, PROTO_LOCAL_FILE, "",
				attrs.creationTime().toMillis(), "");
		return info;
	}

	@Override
	public String getHome() throws FileNotFoundException, IOException {
		return System.getProperty("user.home");
	}

	@Override
	public List<FileInfo> list(String path) throws Exception {
		if (path == null || path.length() < 1) {
			path = System.getProperty("user.home");
		}
		if (!path.endsWith(File.separator)) {
			path = path + File.separator;
		}
		File[] childs = new File(path).listFiles();
		List<FileInfo> list = new ArrayList<>();
		if (childs == null || childs.length < 1) {
			return list;
		}
		for (File f : childs) {

			Path p = f.toPath();
			BasicFileAttributes attrs = Files.readAttributes(p,
					BasicFileAttributes.class);
			FileInfo info = new FileInfo(f.getName(), f.getAbsolutePath(),
					f.length(),
					f.isDirectory() ? FileType.Directory : FileType.File,
					f.lastModified(), -1, PROTO_LOCAL_FILE, "",
					attrs.creationTime().toMillis(), "");
			list.add(info);
		}
		return list;
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public InputStream getInputStream(String file, long offset)
			throws FileNotFoundException, Exception {
		FileInputStream fout = new FileInputStream(file);
		fout.skip(offset);
		return fout;
	}

	@Override
	public OutputStream getOutputStream(String file)
			throws FileNotFoundException, Exception {
		return new FileOutputStream(file, true);
	}

	@Override
	public void rename(String oldName, String newName)
			throws FileNotFoundException, Exception {
		System.out.println("Renaming from " + oldName + " to: " + newName);
		if (!new File(oldName).renameTo(new File(newName))) {
			throw new FileNotFoundException();
		}
	}

	public synchronized void delete(FileInfo f) throws Exception {
		if (f.getType() == FileType.Directory) {
			List<FileInfo> list = list(f.getPath());
			if (list != null && list.size() > 0) {
				for (FileInfo fc : list) {
					delete(fc);
				}
			}
			new File(f.getPath()).delete();
		} else {
			new File(f.getPath()).delete();
		}
	}

	@Override
	public void mkdir(String path) throws Exception {
		new File(path).mkdirs();
	}

	@Override
	public void close() {
	}

	@Override
	public boolean isConnected() {
		return true;
	}

	@Override
	public boolean mkdirs(String absPath) throws Exception {
		return new File(absPath).mkdirs();
	}

//	@Override
//	public long getAllFiles(String dir, String baseDir,
//			Map<String, String> fileMap, Map<String, String> folderMap)
//			throws Exception {
//		long size = 0;
//		System.out.println("get files: " + dir);
//		String parentFolder = PathUtils.combineUnix(baseDir,
//				PathUtils.getFileName(dir));
//
//		List<FileInfo> list = ll(dir, false);
//		for (FileInfo f : list) {
//			if (f.getType() == FileType.Directory) {
//				folderMap.put(f.getPath(),
//						PathUtils.combineUnix(parentFolder, f.getName()));
//				size += getAllFiles(f.getPath(), parentFolder, fileMap,
//						folderMap);
//			} else {
//				fileMap.put(f.getPath(),
//						PathUtils.combineUnix(parentFolder, f.getName()));
//				size += f.getSize();
//			}
//		}
//		return size;
//	}

	@Override
	public long getAllFiles(String dir, String baseDir,
			Map<String, String> fileMap, Map<String, String> folderMap)
			throws Exception {
		long size = 0;
		System.out.println("get files: " + dir);
		String parentFolder = PathUtils.combineUnix(baseDir,
				PathUtils.getFileName(dir));

		folderMap.put(dir, parentFolder);

		List<FileInfo> list = list(dir);
		for (FileInfo f : list) {
			if (f.getType() == FileType.Directory) {
				folderMap.put(f.getPath(),
						PathUtils.combineUnix(parentFolder, f.getName()));
				size += getAllFiles(f.getPath(), parentFolder, fileMap,
						folderMap);
			} else {
				fileMap.put(f.getPath(),
						PathUtils.combineUnix(parentFolder, f.getName()));
				size += f.getSize();
			}
		}
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.core.FileSystemProvider#deleteFile(java.lang.String)
	 */
	@Override
	public void deleteFile(String f) throws Exception {
		new File(f).delete();
	}

	@Override
	public String getProtocol() {
		return PROTO_LOCAL_FILE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.core.FileSystemProvider#createFile(java.lang.String)
	 */
	@Override
	public void createFile(String path) throws Exception {

	}

	public void createLink(String src, String dst, boolean hardLink)
			throws Exception {

	}

	@Override
	public void connect() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyTo(String source, String dest, DataTransferProgress prg,
			int mode) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyTo(String source, OutputStream dest,
			DataTransferProgress prg, int mode, long offset) throws Exception {
	}

	@Override
	public String[] getRoots() throws Exception {
		File[] roots = File.listRoots();
		String arr[] = new String[roots.length];
		int i = 0;
		for (File f : roots) {
			arr[i++] = f.getAbsolutePath();
		}
		return arr;
	}

}
