/**
 * 
 */
package nixexplorer.app.session;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nixexplorer.App;
import nixexplorer.Constants;

/**
 * @author subhro
 *
 */
public class SessionStore {
	public synchronized static SavedSessionTree load() {
		File file = new File(App.getConfig("app.dir"), Constants.SESSION_DB_FILE);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(file,
					new TypeReference<SavedSessionTree>() {
					});
		} catch (IOException e) {
			e.printStackTrace();
			SessionFolder rootFolder = new SessionFolder();
			rootFolder.setName("My sites");
			SavedSessionTree tree = new SavedSessionTree();
			tree.setFolder(rootFolder);
			return tree;
		}
	}

	public synchronized static void save(SessionFolder folder,
			String lastSelectionPath) {
		File file = new File(App.getConfig("app.dir"), Constants.SESSION_DB_FILE);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			SavedSessionTree tree = new SavedSessionTree();
			tree.setFolder(folder);
			tree.setLastSelection(lastSelectionPath);
			objectMapper.writeValue(file, tree);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static synchronized SessionFolder convertModelFromTree(
			DefaultMutableTreeNode node) {
		SessionFolder folder = new SessionFolder();
		folder.setName(node.getUserObject() + "");
		Enumeration<TreeNode> childrens = node.children();
		while (childrens.hasMoreElements()) {
			DefaultMutableTreeNode c = (DefaultMutableTreeNode) childrens
					.nextElement();
			if (c.getUserObject() instanceof SessionInfo) {
				folder.getItems().add((SessionInfo) c.getUserObject());
			} else {
				folder.getFolders().add(convertModelFromTree(c));
			}
		}
		return folder;
	}

	public synchronized static DefaultMutableTreeNode getNode(
			SessionFolder folder) {
		NamedItem item = new NamedItem();
		item.setName(folder.getName());
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(item);
		for (SessionInfo info : folder.getItems()) {
			DefaultMutableTreeNode c = new DefaultMutableTreeNode(info);
			c.setAllowsChildren(false);
			node.add(c);
		}

		for (SessionFolder folderItem : folder.getFolders()) {
			node.add(getNode(folderItem));
		}
		return node;
	}

	public synchronized static void store(SessionFolder folder) {
		File file = new File(Constants.CONFIG_DIR, Constants.SESSION_DB_FILE);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			objectMapper.writeValue(file, folder);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
