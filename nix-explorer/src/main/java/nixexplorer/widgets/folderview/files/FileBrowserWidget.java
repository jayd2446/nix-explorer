/**
 * 
 */
package nixexplorer.widgets.folderview.files;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import nixexplorer.TextHolder;
import nixexplorer.app.AppContext;
import nixexplorer.app.session.AppSession;
import nixexplorer.app.session.SessionEventAware;
import nixexplorer.app.session.SessionInfo;
import nixexplorer.widgets.Widget;
import nixexplorer.widgets.folderview.TabbedFolderViewWidget;
import nixexplorer.widgets.folderview.TabbedFolderViewWidget.TabbedFolderViewListener;
import nixexplorer.widgets.folderview.local.LocalFolderViewWidget;
import nixexplorer.widgets.folderview.remote.RemoteFolderViewWidget;
import nixexplorer.widgets.util.Utility;

/**
 * @author subhro
 *
 */
public class FileBrowserWidget extends Widget
		implements SessionEventAware, TabbedFolderViewListener {

	private JPanel bottomBar;
	private JPanel bottomPanel;
	private JSplitPane vertSplit;
	private int lastBottomDivider;
	private LocalFolderViewWidget localFileView;
	private RemoteFolderViewWidget remoteFileView;
	private MatteBorder expB, clpB;

	/**
	 * @param info
	 * @param args
	 * @param appSession
	 * @param window
	 */
	public FileBrowserWidget(SessionInfo info, String[] args,
			AppSession appSession, Window window) {
		super(info, args, appSession, window);

		vertSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		vertSplit.setBorder(new EmptyBorder(0, 0, 0, 0));

		vertSplit.setUI(new BasicSplitPaneUI() {
			@Override
			public BasicSplitPaneDivider createDefaultDivider() {
				BasicSplitPaneDivider d = new BasicSplitPaneDivider(this) {
					@Override
					public Border getBorder() {
						return null;
					}
				};
				d.setBorder(null);
				return d;
			}
		});
		vertSplit.setOpaque(false);
		vertSplit.setDividerSize(Utility.toPixel(3));
		vertSplit.setContinuousLayout(true);

		localFileView = new LocalFolderViewWidget(info, new String[] {},
				appSession, window);
		localFileView.setCloseListener(this);

		clpB = new MatteBorder(Utility.toPixel(1), Utility.toPixel(0),
				Utility.toPixel(0), Utility.toPixel(0),
				UIManager.getColor("DefaultBorder.color"));
		expB = new MatteBorder(Utility.toPixel(0), Utility.toPixel(1),
				Utility.toPixel(0), Utility.toPixel(0),
				UIManager.getColor("DefaultBorder.color"));

		add(createContentPanel1());
	}

	private JPanel createContentPanel1() {
		remoteFileView = new RemoteFolderViewWidget(info, args, appSession,
				this.getWindow(), this);
		remoteFileView.setCloseListener(this);
		JLabel lblRemoteTitle = new JLabel(
				TextHolder.getString("app.remote.title"));

		JButton btnExpandCollapse = new JButton(
				UIManager.getIcon("ExpandPanel.upIcon"));
		btnExpandCollapse.setBorderPainted(false);
		JLabel lblLocalTitle = new JLabel(
				TextHolder.getString("app.local.title"));

		Box remoteTitleBox = Box.createHorizontalBox();
		remoteTitleBox.setBorder(new EmptyBorder(Utility.toPixel(5),
				Utility.toPixel(5), Utility.toPixel(5), Utility.toPixel(5)));
		remoteTitleBox.add(Box.createRigidArea(new Dimension(Utility.toPixel(5),
				btnExpandCollapse.getPreferredSize().height)));
		remoteTitleBox.add(lblRemoteTitle);

		JPanel remotePanel = new JPanel(new BorderLayout());
		remotePanel.add(remoteTitleBox, BorderLayout.NORTH);
		remotePanel.add(remoteFileView);

		Border localTitleTopBorder = new EmptyBorder(Utility.toPixel(5),
				Utility.toPixel(5), Utility.toPixel(5), Utility.toPixel(5));
		Border localTitleBottomlBorder = new MatteBorder(Utility.toPixel(1),
				Utility.toPixel(0), Utility.toPixel(0), Utility.toPixel(0),
				UIManager.getColor("DefaultBorder.color"));

		Box localTitleBox = Box.createHorizontalBox();

		localTitleBox.add(Box.createRigidArea(new Dimension(Utility.toPixel(5),
				btnExpandCollapse.getPreferredSize().height)));
		localTitleBox.add(lblLocalTitle);
		localTitleBox.add(Box.createHorizontalGlue());
		localTitleBox.add(btnExpandCollapse);

		JPanel panel = new JPanel(new BorderLayout());
		if (AppContext.INSTANCE.getConfig().getFileBrowser().isLocalVisible()) {
			System.out.println("Local visible");
			JPanel localPanel = new JPanel(new BorderLayout());
			localTitleBox.setBorder(localTitleTopBorder);
			localPanel.add(localTitleBox, BorderLayout.NORTH);
			localPanel.add(localFileView);
			localPanel.setPreferredSize(remotePanel.getPreferredSize());
			vertSplit.setRightComponent(localPanel);
			vertSplit.setLeftComponent(remotePanel);
			vertSplit.setDividerLocation(appSession.getWindow().getWidth() / 2);
			panel.add(vertSplit);
			btnExpandCollapse
					.setIcon(UIManager.getIcon("ExpandPanel.downIcon"));
		} else {
			panel.add(remotePanel);
			panel.add(localTitleBox, BorderLayout.SOUTH);
			btnExpandCollapse.setIcon(UIManager.getIcon("ExpandPanel.upIcon"));
			localTitleBox.setBorder(localTitleBottomlBorder);
		}

		btnExpandCollapse.addActionListener(e -> {
			if (AppContext.INSTANCE.getConfig().getFileBrowser()
					.isLocalVisible()) {
				// collapse
				panel.removeAll();
				// vertSplit.removeAll();
				panel.add(remotePanel);
				panel.add(localTitleBox, BorderLayout.SOUTH);
				btnExpandCollapse
						.setIcon(UIManager.getIcon("ExpandPanel.upIcon"));
				localTitleBox.setBorder(localTitleBottomlBorder);
				AppContext.INSTANCE.getConfig().getFileBrowser()
						.setLocalVisible(false);
			} else {
				// expand
				panel.removeAll();
				JPanel localPanel = new JPanel(new BorderLayout());
				localPanel.add(localTitleBox, BorderLayout.NORTH);
				localPanel.add(localFileView);
				localPanel.setPreferredSize(remotePanel.getPreferredSize());
				// vertSplit.setDividerSize(Utility.toPixel(3));
				vertSplit.setRightComponent(localPanel);
				vertSplit.setLeftComponent(remotePanel);
				vertSplit.setDividerLocation(
						appSession.getWindow().getWidth() / 2);
				panel.add(vertSplit);
				btnExpandCollapse
						.setIcon(UIManager.getIcon("ExpandPanel.downIcon"));
				localTitleBox.setBorder(localTitleTopBorder);
				AppContext.INSTANCE.getConfig().getFileBrowser()
						.setLocalVisible(true);
			}
			doLayout();
			revalidate();
			repaint();
			AppContext.INSTANCE.getConfig().save();
		});

		return panel;
	}

	private JPanel createContentPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);

		JPanel remoteTopPanel = new JPanel(new BorderLayout());

		JLabel lblRemoteTitle = new JLabel(
				TextHolder.getString("app.remote.title"));
		lblRemoteTitle.setBorder(new EmptyBorder(Utility.toPixel(5),
				Utility.toPixel(10), Utility.toPixel(5), Utility.toPixel(5)));
		lblRemoteTitle.setFont(
				new Font(Font.DIALOG, Font.PLAIN, Utility.toPixel(14)));
		remoteTopPanel.add(lblRemoteTitle);

		JPanel remotePanel = new JPanel(new BorderLayout());
		remotePanel.add(remoteTopPanel, BorderLayout.NORTH);

		remoteFileView = new RemoteFolderViewWidget(info, args, appSession,
				this.getWindow(), this);
		remotePanel.add(remoteFileView);

		bottomBar = new JPanel(new BorderLayout());
		bottomBar.setOpaque(true);
		JLabel lblTitle = new JLabel(TextHolder.getString("app.local.title"));
		lblTitle.setBorder(new EmptyBorder(Utility.toPixel(0),
				Utility.toPixel(10), Utility.toPixel(0), Utility.toPixel(0)));
		lblTitle.setFont(
				new Font(Font.DIALOG, Font.PLAIN, Utility.toPixel(14)));
		JButton btnExpandCollapse = new JButton(
				UIManager.getIcon("ExpandPanel.upIcon"));
		btnExpandCollapse.addActionListener(e -> {
			if (btnExpandCollapse
					.getClientProperty("button.expanded") == null) {
				panel.removeAll();
				vertSplit.setLeftComponent(remotePanel);
				bottomPanel.putClientProperty("panel.size",
						bottomPanel.getPreferredSize());
				bottomPanel.removeAll();
				if (lastBottomDivider == 0) {
					lastBottomDivider = panel.getWidth() / 2;
					if (lastBottomDivider < 1) {
						lastBottomDivider = Utility.toPixel(400);
					}
				}
//				bottomPanel.setPreferredSize(new DimensionUIResource(
//						Utility.toPixel(100), Utility.toPixel(300)));
				bottomPanel.add(bottomBar, BorderLayout.NORTH);
				bottomPanel.setBorder(expB);
				btnExpandCollapse.putClientProperty("button.expanded",
						Boolean.TRUE);
				btnExpandCollapse
						.setIcon(UIManager.getIcon("ExpandPanel.downIcon"));
				bottomPanel.add(localFileView);
				vertSplit.setRightComponent(bottomPanel);
				panel.add(vertSplit);
				vertSplit.setDividerLocation(lastBottomDivider);
//				AppContext.INSTANCE.getConfig().getFileBrowser().setLocalVisible(true);
//				AppContext.INSTANCE.getConfig().save();
			} else {
				System.out.println("Collapsing...");
				lastBottomDivider = vertSplit.getDividerLocation();
				panel.removeAll();
				bottomPanel.removeAll();
				bottomPanel.setPreferredSize((Dimension) bottomPanel
						.getClientProperty("panel.size"));
				bottomPanel.add(bottomBar);
				bottomPanel.setBorder(clpB);
				btnExpandCollapse.putClientProperty("button.expanded", null);
				btnExpandCollapse
						.setIcon(UIManager.getIcon("ExpandPanel.upIcon"));
				panel.add(remotePanel);
				panel.add(bottomPanel, BorderLayout.SOUTH);
//				AppContext.INSTANCE.getConfig().getFileBrowser().setLocalVisible(true);
//				AppContext.INSTANCE.getConfig().save();
			}
			doLayout();
			revalidate();
			repaint();
		});
		btnExpandCollapse.setBorderPainted(false);
		bottomBar.add(lblTitle, BorderLayout.WEST);
		bottomBar.add(btnExpandCollapse, BorderLayout.EAST);
//		tabs1.add(Box.createRigidArea(
//				new Dimension(Utility.toPixel(10), Utility.toPixel(30))));
//		bottomBar.setBorder(
//				new EmptyBorder(Utility.toPixel(2), Utility.toPixel(2), Utility.toPixel(2), Utility.toPixel(2)));

		Component cx = Box
				.createRigidArea(btnExpandCollapse.getPreferredSize());
		remoteTopPanel.add(cx, BorderLayout.EAST);

		bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBorder(clpB);

		if (AppContext.INSTANCE.getConfig().getFileBrowser().isLocalVisible()) {
			bottomPanel.putClientProperty("panel.size",
					bottomPanel.getPreferredSize());
			lastBottomDivider = Utility.toPixel(500);
			bottomPanel.add(bottomBar, BorderLayout.NORTH);
			btnExpandCollapse.putClientProperty("button.expanded",
					Boolean.TRUE);
			btnExpandCollapse
					.setIcon(UIManager.getIcon("ExpandPanel.downIcon"));
			bottomPanel.add(localFileView);
			vertSplit.setLeftComponent(remotePanel);
			vertSplit.setRightComponent(bottomPanel);
			vertSplit.setDividerLocation(lastBottomDivider);
			btnExpandCollapse.putClientProperty("button.expanded",
					Boolean.TRUE);
			panel.add(vertSplit);
		} else {
			bottomPanel.add(bottomBar);
			panel.add(remotePanel);
			panel.add(bottomPanel, BorderLayout.SOUTH);
		}
		return panel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.TabbedChild#tabSelected()
	 */
	@Override
	public void tabSelected() {
		remoteFileView.requestFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.TabbedChild#getIcon()
	 */
	@Override
	public Icon getIcon() {
		return remoteFileView.getIcon();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.TabbedChild#getTitle()
	 */
	@Override
	public String getTitle() {
		return remoteFileView.getTitle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.DisposableView#viewClosing()
	 */
	@Override
	public boolean viewClosing() {
		return remoteFileView.viewClosing();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.app.components.DisposableView#viewClosed()
	 */
	@Override
	public void viewClosed() {
		super.viewClosed();
		remoteFileView.viewClosed();
		appSession.unregisterSessionAwareComponent(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.widgets.Widget#reconnect()
	 */
	@Override
	public void reconnect() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nixexplorer.widgets.Widget#close()
	 */
	@Override
	public void close() {
		remoteFileView.close();
	}

	@Override
	public void configChanged() {
		remoteFileView.configChanged();
	}

	@Override
	public void fileSystemUpdated(String path) {
		remoteFileView.fileSystemUpdated(path);
		localFileView.fileSystemUpdated(path);
	}

	@Override
	public void allTabsClosed(TabbedFolderViewWidget w) {
		System.out.println("allTabsClosed called");
		System.out.println("closing");
		appSession.closeTab(this);
	}

}
