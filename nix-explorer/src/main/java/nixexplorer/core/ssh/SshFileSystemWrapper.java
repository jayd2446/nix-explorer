package nixexplorer.core.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import nixexplorer.PathUtils;
import nixexplorer.app.session.SessionInfo;
import nixexplorer.core.DataTransferProgress;
import nixexplorer.core.FileInfo;
import nixexplorer.core.FileSystemProvider;
import nixexplorer.core.FileType;
import nixexplorer.core.ssh.SshUtility.SftpContext;

public class SshFileSystemWrapper implements FileSystemProvider {

	private Object lock = new Object();

	private SessionInfo info;
	private SshWrapper wrapper;
	private ChannelSftp sftp;
	private AtomicBoolean stopFlag = new AtomicBoolean(false);

	public static final String PROTO_SFTP = "sftp";

	public SshFileSystemWrapper(SessionInfo info) {
		this.info = info;
	}

	public ChannelSftp getSftp() throws Exception {
		ensureConnected();
		return sftp;
	}

	private void ensureConnected() throws Exception {
		if (sftp != null && sftp.isConnected()) {
			return;
		}
		connect();
	}

	@Override
	public synchronized void delete(FileInfo f) throws Exception {
		ensureConnected();
		if (f.getType() == FileType.Directory) {
			List<FileInfo> list = list(f.getPath());
			if (list != null && list.size() > 0) {
				for (FileInfo fc : list) {
					delete(fc);
				}
			}
			this.sftp.rmdir(f.getPath());
		} else {
			this.sftp.rm(f.getPath());
		}
	}

	@Override
	public void chmod(int perm, String path) throws Exception {
		ensureConnected();
		this.sftp.chmod(perm, path);
	}

	@Override
	public synchronized void connect() throws Exception {
		synchronized (lock) {
			System.out.println("Connecting to: " + info);
			SftpContext res = SshUtility.connectSftp(info, stopFlag);
			if (stopFlag.get()) {
				close();
				throw new Exception("Operation cancelled");
			}
			wrapper = res.getWrapper();
			this.sftp = res.getSftp();
		}
	}

	@Override
	public synchronized List<FileInfo> list(String path) throws Exception {
		ensureConnected();
		return listFiles(path);
	}

	private FileInfo resolveSymlink(String name, String pathToResolve,
			SftpATTRS attrs, String longName) throws Exception {
		try {
			System.out.println("Following symlink: " + pathToResolve);
			while (true) {
				String str = sftp.readlink(pathToResolve);
				System.out
						.println("Read symlink: " + pathToResolve + "=" + str);
				pathToResolve = str.startsWith("/") ? str
						: PathUtils.combineUnix(pathToResolve, str);
				System.out.println("Getting link attrs: " + pathToResolve);
				attrs = sftp.stat(pathToResolve);

				if (!attrs.isLink()) {
					FileInfo e = new FileInfo(name, pathToResolve,
							(attrs.isDir() ? -1 : attrs.getSize()),
							attrs.isDir() ? FileType.DirLink
									: FileType.FileLink,
							(long) attrs.getMTime() * 1000,
							attrs.getPermissions(), PROTO_SFTP,
							attrs.getPermissionsString(), attrs.getATime(),
							longName);
					return e;
				}
			}
		} catch (SftpException e) {
			if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE
					|| e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
				return new FileInfo(name, pathToResolve, 0, FileType.FileLink,
						(long) attrs.getMTime() * 1000, attrs.getPermissions(),
						PROTO_SFTP, attrs.getPermissionsString(),
						attrs.getATime(), longName);
			}
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	private List<FileInfo> listFiles(String path) throws Exception {
		synchronized (sftp) {
			System.out.println("Listing file: " + path);
			List<FileInfo> childs = new ArrayList<>();
			try {
				if (path == null || path.length() < 1) {
					path = sftp.getHome();
				}
				Vector<?> files = sftp.ls(path);
				if (files.size() > 0) {
					for (int i = 0; i < files.size(); i++) {
						ChannelSftp.LsEntry ent = (LsEntry) files.get(i);
						if (ent.getFilename().equals(".")
								|| ent.getFilename().equals("..")) {
							continue;
						}
						SftpATTRS attrs = ent.getAttrs();
						if (attrs.isLink()) {
							childs.add(resolveSymlink(ent.getFilename(),
									PathUtils.combineUnix(path,
											ent.getFilename()),
									attrs, ent.getLongname()));
						} else {
							FileInfo e = new FileInfo(ent.getFilename(),
									PathUtils.combineUnix(path,
											ent.getFilename()),
									(attrs.isDir() ? -1 : attrs.getSize()),
									attrs.isDir() ? FileType.Directory
											: FileType.File,
									(long) attrs.getMTime() * 1000,
									ent.getAttrs().getPermissions(), PROTO_SFTP,
									ent.getAttrs().getPermissionsString(),
									attrs.getATime(), ent.getLongname());
							childs.add(e);
						}
					}
				}
			} catch (SftpException e) {
				e.printStackTrace();
				if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
					throw new FileNotFoundException(path);
				}
				if (e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
					throw new AccessDeniedException(path);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e);
			}
			return childs;
		}
	}

	@Override
	public void close() throws Exception {
		stopFlag.set(true);
		System.out.println("Inside fs wrapper: " + stopFlag.get());
		synchronized (lock) {
			if (wrapper != null) {
				System.out.println("Closing wrapper");
				wrapper.close();
			}
		}
	}

	@Override
	public synchronized String getHome() throws Exception {
		ensureConnected();
		return sftp.getHome();
	}

	@Override
	public String[] getRoots() throws Exception {
		return new String[] { "/" };
	}

	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public synchronized FileInfo getInfo(String path) throws Exception {
		ensureConnected();
		try {
			SftpATTRS attrs = sftp.stat(path);
			if (attrs.isLink()) {
				return resolveSymlink(PathUtils.getFileName(path), path, attrs,
						null);
			} else {
				FileInfo e = new FileInfo(PathUtils.getFileName(path), path,
						(attrs.isDir() ? -1 : attrs.getSize()),
						attrs.isDir() ? FileType.Directory : FileType.File,
						(long) attrs.getMTime() * 1000, attrs.getPermissions(),
						PROTO_SFTP, attrs.getPermissionsString(),
						attrs.getATime(), null);
				return e;
			}
		} catch (SftpException e) {
			if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
				throw new FileNotFoundException(path);
			}
			throw e;
		}

	}

	@Override
	public synchronized void copyTo(String source, String dest,
			DataTransferProgress prg, int mode) throws Exception {
		ensureConnected();
		this.sftp.get(source, dest, prg, mode);
	}

	@Override
	public synchronized void copyTo(String source, OutputStream dest,
			DataTransferProgress prg, int mode, long offset) throws Exception {
		ensureConnected();
		this.sftp.get(source, dest, prg, mode, offset);
	}

	@Override
	public void createLink(String src, String dst, boolean hardLink)
			throws Exception {
		ensureConnected();
		if (hardLink) {
			this.sftp.hardlink(src, dst);
		} else {
			this.sftp.symlink(src, dst);
		}
	}

	@Override
	public void deleteFile(String f) throws Exception {
		ensureConnected();
		this.sftp.rm(f);
	}

	@Override
	public void createFile(String path)
			throws AccessDeniedException, Exception {
		ensureConnected();
		try {
			sftp.put(path).close();
		} catch (SftpException e) {
			if (e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
				throw new AccessDeniedException(path);
			}
		} catch (Exception e) {
			if (sftp.isConnected()) {
				throw new FileNotFoundException(e.getMessage());
			}
			throw new Exception(e);
		}
	}

	@Override
	public OutputStream getOutputStream(String file)
			throws FileNotFoundException, Exception {
		ensureConnected();
		synchronized (sftp) {
			return sftp.put(file, null, ChannelSftp.APPEND, 0);
		}
	}

	@Override
	public InputStream getInputStream(String file, long offset)
			throws FileNotFoundException, Exception {
		ensureConnected();
		synchronized (sftp) {
			try {
				return sftp.get(file, null, offset);
			} catch (Exception e) {
				if (sftp.isConnected()) {
					throw new FileNotFoundException();
				}
				throw new Exception();
			}
		}
	}

	@Override
	public void rename(String oldName, String newName)
			throws FileNotFoundException, Exception {
		ensureConnected();
		try {
			synchronized (sftp) {
				sftp.rename(oldName, newName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (sftp.isConnected()) {
				throw new FileNotFoundException();
			}
			throw new Exception();
		}
	}

	@Override
	public void mkdir(String path) throws AccessDeniedException, Exception {
		ensureConnected();
		try {
			sftp.mkdir(path);
		} catch (SftpException e) {
			if (e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
				throw new AccessDeniedException(path);
			}
		} catch (Exception e) {
			if (sftp.isConnected()) {
				throw new FileNotFoundException(e.getMessage());
			}
			throw new Exception(e);
		}
	}

	@Override
	public boolean mkdirs(String absPath) throws Exception {
		ensureConnected();
		System.out.println("mkdirs: " + absPath);
		if (absPath.equals("/")) {
			return true;
		}

		try {
			sftp.stat(absPath);
			return false;
		} catch (Exception e) {
			if (!sftp.isConnected()) {
				throw e;
			}
		}

		System.out.println("Folder does not exists: " + absPath);

		String parent = PathUtils.getParent(absPath);

		mkdirs(parent);
		sftp.mkdir(absPath);

		return true;
	}

	@Override
	public long getAllFiles(String dir, String baseDir,
			Map<String, String> fileMap, Map<String, String> folderMap)
			throws Exception {
		ensureConnected();
		long size = 0;
		System.out.println("get files: " + dir);
		String parentFolder = PathUtils.combine(baseDir,
				PathUtils.getFileName(dir), File.separator);

		folderMap.put(dir, parentFolder);

		List<FileInfo> list = list(dir);
		for (FileInfo f : list) {
			if (f.getType() == FileType.Directory) {
				folderMap.put(f.getPath(), PathUtils.combine(parentFolder,
						f.getName(), File.separator));
				size += getAllFiles(f.getPath(), parentFolder, fileMap,
						folderMap);
			} else {
				fileMap.put(f.getPath(), PathUtils.combine(parentFolder,
						f.getName(), File.separator));
				size += f.getSize();
			}
		}
		return size;
	}

	@Override
	public boolean isConnected() {
		return this.wrapper != null && this.wrapper.isConnected();
	}

	@Override
	public String getProtocol() {
		return PROTO_SFTP;
	}

	/**
	 * @return the wrapper
	 */
	public SshWrapper getWrapper() {
		return wrapper;
	}

}