/**
 * 
 */
package nixexplorer.app;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import nixexplorer.App;
import nixexplorer.TextHolder;
import nixexplorer.app.components.WelcomeScreen;
import nixexplorer.app.session.AppSessionImpl;
import nixexplorer.app.settings.AppConfig;
import nixexplorer.widgets.util.Utility;

/**
 * @author subhro
 *
 */
public class MainAppFrame extends JFrame {
	private Cursor defCursor, resizeCursor;
	private JPanel content;
	private static MainAppFrame me;

	public static synchronized MainAppFrame getSharedInstance() {
		if (me == null) {
			me = new MainAppFrame();
		}
		return me;
	}

	private MainAppFrame() {
		initUI();
		AppContext.INSTANCE.setWindow(this);
	}

	private void initUI() {
		AppSidePanel sidePanel = new AppSidePanel(this);
		setIconImage(App.getAppIcon());
		defCursor = new Cursor(Cursor.DEFAULT_CURSOR);
		resizeCursor = new Cursor(Cursor.W_RESIZE_CURSOR);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (sidePanel.closeAllSessions()) {
					System.exit(0);
				}
			}
		});
		adjustWindowSize();

		// JPanel serverDisplayPanel = new ServerDisplayPanel();
		// serverDisplayPanel.setCursor(defCursor);

		MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				Dimension d = sidePanel.getPreferredSize();
				d.width = content.getX() + e.getX();
				sidePanel.setPreferredSize(d);
				sidePanel.setMaximumSize(d);
				getContentPane().revalidate();
				repaint();
			}
		};

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(new LineBorder(UIManager.getColor("DefaultBorder.color"), Utility.toPixel(1)));

		content = new JPanel(new BorderLayout());
		content.add(new WelcomeScreen(this, AppContext.INSTANCE.getConfig(), sidePanel));
		sidePanel.setListSelectionListener(e -> {
			try {
				content.removeAll();
				AppSessionImpl session = sidePanel.getSelectedSession();
				if (session == null)
					return;

				ServerDisplayPanel display = session.getDisplay();
				display.setAdapter(adapter);
//				if (display.getMouseListeners() == null
//						|| display.getMouseListeners().length < 1) {
//					display.addMouseListener(adapter);
//					display.addMouseMotionListener(adapter);
//				}
				content.add(display);
			} finally {
				revalidate();
				repaint();
			}

		});
		// serverDisplayPanel.addMouseListener(adapter);
		// serverDisplayPanel.addMouseMotionListener(adapter);
		mainPanel.add(sidePanel, BorderLayout.WEST);
		mainPanel.add(content);
		add(mainPanel);
	}

	private void adjustWindowSize() {
		AppConfig config = AppContext.INSTANCE.getConfig();
		if (config.getWindowWidth() < 1 || config.getWindowHeight() < 1) {
			Insets inset = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice().getDefaultConfiguration());

			Dimension screenD = Toolkit.getDefaultToolkit().getScreenSize();

			int screenWidth = screenD.width - inset.left - inset.right;
			int screenHeight = screenD.height - inset.top - inset.bottom;

			if (screenWidth < 800 || screenHeight < 600) {
				setSize(screenWidth, screenHeight);
			} else {
				int width = (screenWidth * 80) / 100;
				int height = (screenHeight * 80) / 100;
				setSize(width, height);
			}

		} else {
			setSize(config.getWindowWidth(), config.getWindowHeight());
			if (MAXIMIZED_BOTH == config.getWindowState()) {
				setExtendedState(MAXIMIZED_BOTH);
			}
		}

		if (config.getX() < 0 || config.getY() < 0) {
			setLocationRelativeTo(null);
		} else {
			setLocation(config.getX(), config.getY());
		}

		setTitle(TextHolder.getString("app.title"));

	}

}
