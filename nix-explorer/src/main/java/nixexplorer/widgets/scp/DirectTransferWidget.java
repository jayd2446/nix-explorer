/**
 * 
 */
package nixexplorer.widgets.scp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import nixexplorer.App;
import nixexplorer.PathUtils;
import nixexplorer.TextHolder;
import nixexplorer.app.components.DisposableView;
import nixexplorer.app.session.AppSession;
import nixexplorer.app.session.SessionInfo;
import nixexplorer.app.session.SessionManagerPanel;
import nixexplorer.core.ssh.SshUtility;
import nixexplorer.core.ssh.SshWrapper;
import nixexplorer.widgets.console.TerminalDialog;
import nixexplorer.widgets.util.Utility;

/**
 * @author subhro
 *
 */
public class DirectTransferWidget extends JDialog implements DisposableView {
	/**
	 * 
	 */
	private AppSession appSession;
	private SessionInfo info;
	private JButton btnSave, btnImport, btnConnect, btnDelete, btnNew;
	private JTextField txtHost, txtUser, txtFolder;
	private JSpinner spPort;
	private JLabel lblError;
	private SshServerInfo serverInfo;
	private SshItemTableModel connectionTableModel;
	private JTable connectionTable;
	private List<String> files = new ArrayList<>(), folders = new ArrayList<>();
	private JPanel contentPage;
	private Window window;
	protected AtomicBoolean widgetClosed = new AtomicBoolean(Boolean.FALSE);
	private JComboBox<String> cmbTransferMode;
	private JTextField txtTempDir;
	private String tmpFile;
	private AtomicBoolean stopFlag = new AtomicBoolean(Boolean.FALSE);
	private String srcDir;

	// private List<ScpServerInfo> serverList = new ArrayList<>();

	public DirectTransferWidget(SessionInfo info, List<String> files,
			List<String> folders, String sourceDirectory, AppSession appSession,
			Window window) {
		super(window);
		setIconImage(App.getAppIcon());
		this.info = info;
		this.window = window;
		this.appSession = appSession;
		this.srcDir = sourceDirectory;
		init();
		setSize(Utility.toPixel(640), Utility.toPixel(480));
		setLocationRelativeTo(null);
		this.files.addAll(files);
		this.folders.addAll(folders);
	}

	public void run() {
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		contentPage.removeAll();
		JLabel lbl = new JLabel("Creating file list");
		lbl.setHorizontalAlignment(JLabel.CENTER);
		lbl.setHorizontalTextPosition(JLabel.CENTER);
		lbl.setVerticalAlignment(JLabel.CENTER);
		lbl.setVerticalTextPosition(JLabel.CENTER);
		contentPage.add(lbl);

		this.tmpFile = PathUtils.combineUnix(serverInfo.getTemp(),
				UUID.randomUUID().toString());

		System.out.println("Temp file: " + this.tmpFile);

		new Thread(() -> {
			try {

				createFileList();
				SwingUtilities.invokeLater(() -> {
					String transferCommand = genrateTransferCmd();
					System.out.println("Transfer command: " + transferCommand);
					TerminalDialog dlg = new TerminalDialog(info,
							new String[] { "-c", transferCommand }, appSession,
							window, "Command window", false, true);
					this.dispose();
					dlg.setLocationRelativeTo(window);
					dlg.setVisible(true);
					return;
				});
			} catch (AccessDeniedException e) {
				e.printStackTrace();
				if (!stopFlag.get()) {
					JOptionPane.showMessageDialog(this,
							"Temporary directory is not writable "
									+ serverInfo.getTemp());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			SwingUtilities.invokeLater(() -> {
				this.dispose();
			});
		}).start();
	}

	private void createFileList() throws IOException, AccessDeniedException {
		while (true) {
			try (SshWrapper wrapper = SshUtility.connectWrapper(info,
					stopFlag)) {
				wrapper.connect();
				ChannelSftp sftp = wrapper.getSftpChannel();
				OutputStream os = sftp.put(tmpFile);
				String data = null;
				switch (serverInfo.getTransferMode()) {
				case 0:
					data = createSftpFileList();
					break;
				case 1:
					data = createScpFileList();
					break;
				case 2:
					data = createSshFileList();
				}
				if (data != null) {
					os.write(data.getBytes());
				}
				os.close();
				sftp.disconnect();
				wrapper.disconnect();
				return;
			} catch (SftpException e) {
				e.printStackTrace();
				if (e.id == ChannelSftp.SSH_FX_PERMISSION_DENIED) {
					throw new AccessDeniedException(tmpFile);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (stopFlag.get()) {
				throw new IOException();
			}
			if (JOptionPane.showConfirmDialog(null,
					"Unable to connect to server. Retry?") != JOptionPane.YES_OPTION) {
				throw new IOException("User cancelled the operation");
			}
		}
	}

	private String createSftpFileList() {
		StringBuilder sb = new StringBuilder();
		sb.append("cd \"" + serverInfo.getFolder() + "\"\n");

		if (folders.size() > 0) {
			for (String folder : folders) {
				String name = PathUtils.getFileName(folder);
				sb.append("mkdir \"" + name + "\"\n");
				sb.append("put -r \"" + name + "\"\n");
			}
		}

		if (files.size() > 0) {
			for (String file : files) {
				sb.append("put -P \"" + file + "\"\n");
			}
			sb.append("bye\n");
		}
		return sb.toString();
	}

	private String createSshFileList() {
		StringBuilder sb = new StringBuilder();
		sb.append("\"" + this.srcDir + "\"");
		if (folders.size() > 0) {
			for (String folder : folders) {
				String name = PathUtils.getFileName(folder);
				sb.append(" \"" + name + "\" ");
			}
		}

		if (files.size() > 0) {
			for (String file : files) {
				String name = PathUtils.getFileName(file);
				sb.append(" \"" + name + "\" ");
			}
		}
		return sb.toString();
	}

	private String createScpFileList() {
		StringBuilder sb = new StringBuilder();
		if (folders.size() > 0) {
			for (String folder : folders) {
				if (sb.length() > 0) {
					sb.append("; ");
				}
				sb.append("scp -pr \"" + folder + "\" "
						+ (serverInfo.getUser() + "@" + serverInfo.getHost()
								+ ":\"'" + serverInfo.getFolder() + "'\""));
			}
		}

		if (files.size() > 0) {
			if (sb.length() > 0) {
				sb.append("; ");
			}
			sb.append("scp ");
			for (String file : files) {
				sb.append(" \"" + file + "\" ");
			}
			sb.append((serverInfo.getUser() + "@" + serverInfo.getHost()
					+ ":\"'" + serverInfo.getFolder() + "'\""));
			sb.append(";exit");
		}
		return sb.toString();
	}

	private String genrateTransferCmd() {
		switch (serverInfo.getTransferMode()) {
		case 0:
			return createSftpCommand();
		case 1:
			return createScpCommand();
		case 2:
			return createSshCommand();

		}
		return "";
	}

	private String createSftpCommand() {
		return "sftp \"" + serverInfo.getUser() + "@" + serverInfo.getHost()
				+ "\" < \"" + tmpFile + "\"";
	}

	private String createSshCommand() {
		return "cat \"" + tmpFile + "\"|xargs tar -cf - -C |ssh \""
				+ serverInfo.getUser() + "@" + serverInfo.getHost()
				+ "\" \" ( cd '" + serverInfo.getFolder() + "'; tar -xf - ) \"";
	}

	private String createScpCommand() {
		return "sh \"" + tmpFile + "\"";
	}

	private void init() {
		contentPage = new JPanel(new BorderLayout());
		setTitle(TextHolder.getString("filetransfer.title"));
		setSize(new Dimension(Utility.toPixel(640), Utility.toPixel(480)));
		setLocationRelativeTo(null);
		JPanel connectionPanel = new JPanel(new BorderLayout());
		connectionPanel.add(new JLabel("Connections"), BorderLayout.NORTH);
		lblError = new JLabel("");
		lblError.setForeground(Color.RED);
		connectionPanel.add(lblError, BorderLayout.SOUTH);

		connectionTableModel = new SshItemTableModel(appSession, info);

		connectionTable = new JTable(connectionTableModel);
		connectionTable.setShowGrid(false);
		connectionTable.setFillsViewportHeight(true);
		connectionTable.setRowHeight(Utility.toPixel(30));
		connectionTable.setIntercellSpacing(new Dimension(0, 0));
		connectionTable.getSelectionModel().addListSelectionListener(e -> {
			int index = e.getFirstIndex();
			if (index != -1) {
				SshServerInfo info = connectionTableModel.getItemAt(index);
				txtUser.setText(info.getUser());
				txtHost.setText(info.getHost());
				txtFolder.setText(info.getFolder());
				spPort.setValue(info.getPort());
				txtTempDir.setText(info.getTemp());
				cmbTransferMode.setSelectedIndex(info.getTransferMode());
			}
		});

		JScrollPane jsp = new JScrollPane(connectionTable);
		jsp.setBorder(new LineBorder(UIManager.getColor("DefaultBorder.color"),
				Utility.toPixel(1)));

		connectionPanel.add(jsp);

		Box cb = Box.createVerticalBox();
		cb.setBorder(new EmptyBorder(Utility.toPixel(5), Utility.toPixel(5),
				Utility.toPixel(5), Utility.toPixel(5)));

		JLabel lblHost = new JLabel("Host");
		lblHost.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(lblHost);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		txtHost = new JTextField(20);
		adjustSize(txtHost);
		txtHost.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(txtHost);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(10), Utility.toPixel(10))));

		JLabel lblPort = new JLabel("Port");
		lblPort.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(lblPort);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		spPort = new JSpinner(
				new SpinnerNumberModel(22, 1, Short.MAX_VALUE, 1));
		spPort.setPreferredSize(txtHost.getPreferredSize());
		adjustSize(spPort);
		spPort.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(spPort);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(10), Utility.toPixel(10))));

		JLabel lblUser = new JLabel("User");
		lblUser.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(lblUser);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		txtUser = new JTextField(20);
		txtUser.setAlignmentX(Box.LEFT_ALIGNMENT);
		adjustSize(txtUser);
		cb.add(txtUser);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(10), Utility.toPixel(10))));

		JLabel lblDir = new JLabel("Remote directory");
		lblDir.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(lblDir);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		txtFolder = new JTextField(20);
		txtFolder.setAlignmentX(Box.LEFT_ALIGNMENT);
		adjustSize(txtFolder);
		cb.add(txtFolder);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(10), Utility.toPixel(10))));

		JLabel lblTransferMode = new JLabel("Transfer mode");
		lblTransferMode.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(lblTransferMode);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		cmbTransferMode = new JComboBox<String>(
				new String[] { "SFTP", "SCP", "SSH" });
		cmbTransferMode.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				txtFolder.getPreferredSize().height));
		cmbTransferMode.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(cmbTransferMode);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(10), Utility.toPixel(10))));

		JLabel lblTempDir = new JLabel("Temporary directory");
		lblTempDir.setAlignmentX(Box.LEFT_ALIGNMENT);
		cb.add(lblTempDir);

		cb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		txtTempDir = new JTextField(20);
		txtTempDir.setAlignmentX(Box.LEFT_ALIGNMENT);
		adjustSize(txtTempDir);
		cb.add(txtTempDir);

		cb.add(Box.createVerticalGlue());

		Box bb = Box.createHorizontalBox();

		btnImport = new JButton("Import");
		btnImport.addActionListener(e -> {
			SessionInfo info = new SessionManagerPanel().newSession();
			if (info == null) {
				return;
			}
			txtHost.setText(info.getHost());
			spPort.setValue((Integer) info.getPort());
			txtUser.setText(info.getUser());
		});
		bb.add(btnImport);
		bb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		btnNew = new JButton("New");
		btnNew.addActionListener(e -> {
			clearInput();
		});
		bb.add(btnNew);
		bb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));
		btnSave = new JButton("Save");
		btnSave.addActionListener(e -> {
			updateAndSave();
		});
		bb.add(btnSave);
		bb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		btnDelete = new JButton("Delete");

		bb.add(btnDelete);

		bb.add(Box.createRigidArea(
				new Dimension(Utility.toPixel(5), Utility.toPixel(5))));

		bb.add(Box.createHorizontalGlue());

		btnConnect = new JButton("Send");
		btnConnect.addActionListener(e -> {
			serverInfo = updateAndSave();
			if (serverInfo != null) {
				run();
			}
		});
		bb.add(btnConnect);

		bb.setAlignmentX(Box.LEFT_ALIGNMENT);

		cb.add(bb);

//		spPort.setMaximumSize(new Dimension(txtUser.getPreferredSize().width,
//				spPort.getPreferredSize().height));

		contentPage.add(cb, BorderLayout.EAST);

		((JComponent) getContentPane()).setBorder(
				new EmptyBorder(Utility.toPixel(5), Utility.toPixel(5),
						Utility.toPixel(5), Utility.toPixel(5)));

		contentPage.add(connectionPanel);

		this.add(contentPage);

		clearInput();

//	panels = new JPanel[][] {
//			{ createSftpUploadPanel(), createScpUploadPanel(),
//					createFtpUploadPanel() },
//			{ createSftpDownloadPanel(), createScpDownloadPanel(),
//					createFtpDownloadPanel() } };
//	content = new JPanel(new BorderLayout());
//
//	cmbMode = new JComboBox<>(new String[] {
//			"Upload files from this server to a remote server",
//			"Download files from a remote server to this server" });
//
//	cmbSaveSites = new JComboBox<>(new String[] { "New stie" });
////	cmbSaveSites.setPreferredSize(new Dimension(
////			cmbSaveSites.getPreferredSize().width
////					+ cmbSaveSites.getPreferredSize().width / 2,
////			cmbSaveSites.getPreferredSize().height));
//
//	cmbProtocol.setPreferredSize(new Dimension(
//			cmbProtocol.getPreferredSize().width
//					+ cmbProtocol.getPreferredSize().width / 2,
//			cmbProtocol.getPreferredSize().height));
//	JPanel jp = new JPanel(new BorderLayout(5, 5));
//	jp.setAlignmentX(Box.LEFT_ALIGNMENT);
//	jp.add(cmbMode);
////	jp.add(cmbSaveSites, BorderLayout.EAST);
//
////	Box b2 = Box.createHorizontalBox();
////	b2.setAlignmentX(Box.LEFT_ALIGNMENT);
////	b2.add(new JLabel("Action"));
////	b2.add(Box.createHorizontalStrut(Utility.toPixel(5)));
////	b2.add(cmbMode);
////	b2.add(Box.createHorizontalGlue());
////	b2.add(btnSession);
////	b2.add(Box.createHorizontalGlue());
////	
////	b2.add(new JLabel("Protocol"));
////	b2.add(Box.createHorizontalStrut(Utility.toPixel(5)));
////	b2.add(cmbProtocol);
//
//	Box box1 = Box.createHorizontalBox();
//	box1.setAlignmentX(Box.LEFT_ALIGNMENT);
//	box1.add(cmbSaveSites);
//	// box1.add(Box.createHorizontalGlue());
//	box1.add(btnSave);
//
//	Box box2 = Box.createHorizontalBox();
//	box2.setAlignmentX(Box.LEFT_ALIGNMENT);
//	box2.add(new JLabel("Protocol"));
//	box2.add(Box.createHorizontalStrut(Utility.toPixel(5)));
//	box2.add(cmbProtocol);
//	box2.add(new JLabel("Host"));
//	box2.add(txtHost);
//	box2.add(new JLabel("Port"));
//	box2.add(spPort);
//	box2.add(new JLabel("User"));
//	box2.add(txtUser);
//	box2.add(Box.createHorizontalGlue());
//	box2.add(btnConnect);
//
//	Box b1 = Box.createVerticalBox();
//	b1.add(jp);
//	b1.add(box1);
//	b1.add(box2);
//
//	b1.setBorder(new EmptyBorder(Utility.toPixel(5), Utility.toPixel(5),
//			Utility.toPixel(5), Utility.toPixel(5)));
//
//	cmbMode.addActionListener(e -> {
//		updateContent();
//	});
//
//	cmbProtocol.addActionListener(e -> {
//		updateContent();
//	});
//
//	frontPanel = new JPanel(new BorderLayout());
//	backPanel = new JPanel(new BorderLayout());
//
//	frontPanel.add(b1, BorderLayout.NORTH);
//	frontPanel.add(content);
//
//	add(frontPanel);

	}

	private void adjustSize(JComponent c) {
		Dimension d = c.getPreferredSize();
		c.setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
	}

	/**
	 * 
	 */
	private void clearInput() {
		connectionTable.clearSelection();
		txtHost.setText("");
		txtUser.setText("");
		txtFolder.setText(".");
		cmbTransferMode.setSelectedIndex(0);
		txtTempDir.setText("/tmp");
		spPort.setValue(22);
	}

	/**
	 * 
	 */
	private SshServerInfo updateAndSave() {
		if (txtUser.getText().length() < 1) {
			lblError.setText("User can not be blank");
			return null;
		}

		if (txtHost.getText().length() < 1) {
			lblError.setText("Host can not be blank");
			return null;
		}

		if (txtTempDir.getText().length() < 1) {
			lblError.setText("Temp dir can not be blank");
			return null;
		}

		if (txtFolder.getText().length() < 1) {
			lblError.setText("Target dir can not be blank");
			return null;
		}

		int index = connectionTable.getSelectedRow();

		SshServerInfo scpItem = new SshServerInfo();
		scpItem.setFolder(txtFolder.getText());
		scpItem.setHost(txtHost.getText());
		scpItem.setUser(txtUser.getText());
		scpItem.setPort((Integer) spPort.getValue());
		scpItem.setTransferMode(cmbTransferMode.getSelectedIndex());
		scpItem.setTemp(txtTempDir.getText());

		if (index != -1) {
			connectionTableModel.updateItem(index, scpItem);
		} else {
			connectionTableModel.addItem(scpItem);
		}

		return scpItem;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.DisposableView#viewClosing()
	 */
	@Override
	public boolean viewClosing() {
		stopFlag.set(Boolean.TRUE);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.DisposableView#viewClosed()
	 */
	@Override
	public void viewClosed() {
	}

	@Override
	public boolean getWidgetClosed() {
		return widgetClosed.get();
	}

	@Override
	public void setWidgetClosed(boolean widgetClosed) {
		this.widgetClosed.set(widgetClosed);
	}

	@Override
	public boolean closeView() {
		dispose();
		return true;
	}

}
