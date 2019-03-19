/**
 * 
 */
package nixexplorer.app.session;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author subhro
 *
 */
public class SessionInfo extends NamedItem {
	private String host, user, password, localFolder, remoteFolder;
	private int port = 22;
	private List<String> favouriteFolders = new ArrayList<>();
	private String privateKeyFile;

	@Override
	public String toString() {
		return name;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the localFolder
	 */
	public String getLocalFolder() {
		return localFolder;
	}

	/**
	 * @param localFolder the localFolder to set
	 */
	public void setLocalFolder(String localFolder) {
		this.localFolder = localFolder;
	}

	/**
	 * @return the remoteFolder
	 */
	public String getRemoteFolder() {
		return remoteFolder;
	}

	/**
	 * @param remoteFolder the remoteFolder to set
	 */
	public void setRemoteFolder(String remoteFolder) {
		this.remoteFolder = remoteFolder;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the favouriteFolders
	 */
	public List<String> getFavouriteFolders() {
		return favouriteFolders;
	}

	/**
	 * @param favouriteFolders the favouriteFolders to set
	 */
	public void setFavouriteFolders(List<String> favouriteFolders) {
		this.favouriteFolders = favouriteFolders;
	}

	/**
	 * @return the privateKeyFile
	 */
	public String getPrivateKeyFile() {
		return privateKeyFile;
	}

	/**
	 * @param privateKeyFile the privateKeyFile to set
	 */
	public void setPrivateKeyFile(String privateKeyFile) {
		this.privateKeyFile = privateKeyFile;
	}

	public SessionInfo copy() {
		SessionInfo info = new SessionInfo();
		info.setId(UUID.randomUUID().toString());
		info.setHost(this.host);
		info.setPort(this.port);
		info.getFavouriteFolders().addAll(favouriteFolders);
		info.setLocalFolder(this.localFolder);
		info.setRemoteFolder(this.remoteFolder);
		info.setPassword(this.password);
		info.setPrivateKeyFile(privateKeyFile);
		info.setUser(user);
		info.setName(name);
		return info;
	}
}
