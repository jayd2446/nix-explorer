package nixexplorer.widgets.folderview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import nixexplorer.TextHolder;
import nixexplorer.core.FileInfo;
import nixexplorer.core.FileType;
import nixexplorer.widgets.util.Utility;

public class PermissionsDialog extends JDialog {
	private int permissions;
	private JCheckBox chkPermissons[];
	private JLabel lblOwner, lblGroup, lblOther;
	private String[] labels = new String[] { "read", "write", "execute" };
	private int dialogResult = JOptionPane.CANCEL_OPTION;
	private FileInfo details;
	private JTextField txtName, txtSize, txtType, txtOwner, txtGroup,
			txtModified, txtCreated, txtPath, txtFileCount;

	private static final String userGroupRegex = "^[^\\s]+\\s+[^\\s]+\\s+([^\\s]+)\\s+([^\\s]+)";
	private Pattern pattern;

	public PermissionsDialog(Window window, boolean multimode) {
		super(window);
		setResizable(true);
		setModal(true);
		setTitle("Properties");
		// setSize(new Dimension(Utility.toPixel(350), Utility.toPixel(350)));
		chkPermissons = new JCheckBox[9];
		for (int i = 0; i < 9; i++) {
			chkPermissons[i] = new JCheckBox(labels[i % 3]);
			chkPermissons[i].setAlignmentX(Box.LEFT_ALIGNMENT);
		}
		lblOwner = new JLabel("Owner permissions");
		lblOwner.setAlignmentX(Box.LEFT_ALIGNMENT);
		lblGroup = new JLabel("Group permissions");
		lblGroup.setAlignmentX(Box.LEFT_ALIGNMENT);
		lblOther = new JLabel("Other permissions");
		lblOther.setAlignmentX(Box.LEFT_ALIGNMENT);

		Box b = Box.createVerticalBox();

		if (multimode) {
			txtFileCount = new JTextField(30);
			b.add(addPropertyField(txtFileCount, "Total"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));
		} else {
			this.pattern = Pattern.compile(userGroupRegex);
			txtName = new JTextField(30);
			b.add(addPropertyField(txtName, "Name"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

			txtPath = new JTextField(30);
			b.add(addPropertyField(txtPath, "Path"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

			txtSize = new JTextField(30);
			b.add(addPropertyField(txtSize, "Size"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

			txtOwner = new JTextField(30);
			b.add(addPropertyField(txtOwner, "Owner"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

			txtType = new JTextField(30);
			b.add(addPropertyField(txtType, "Type"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

			txtGroup = new JTextField(30);
			b.add(addPropertyField(txtGroup, "Group"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

			txtModified = new JTextField(30);
			b.add(addPropertyField(txtModified, "Last modified"));
			b.add(Box.createVerticalStrut(Utility.toPixel(10)));

//			txtCreated = new JTextField(30);
//			b.add(addPropertyField(txtCreated, "Creation date"));
//			b.add(Box.createVerticalStrut(Utility.toPixel(10)));
		}

		b.add(lblOwner);

		for (int i = 0; i < 3; i++) {
			b.add(chkPermissons[i]);
		}
		b.add(Box.createVerticalStrut(Utility.toPixel(10)));
		b.add(lblGroup);
		for (int i = 3; i < 6; i++) {
			b.add(chkPermissons[i]);
		}
		b.add(Box.createVerticalStrut(Utility.toPixel(10)));
		b.add(lblOther);
		for (int i = 6; i < 9; i++) {
			b.add(chkPermissons[i]);
		}

		Box b2 = Box.createHorizontalBox();
		JButton btn = new JButton(TextHolder.getString("common.ok"));
		btn.addActionListener(e -> {
			dialogResult = JOptionPane.OK_OPTION;
			dispose();
		});
		JButton btn1 = new JButton(TextHolder.getString("common.cancel"));
		btn1.addActionListener(e -> {
			dialogResult = JOptionPane.CANCEL_OPTION;
			dispose();
		});
		b2.setAlignmentX(Box.LEFT_ALIGNMENT);
		b2.add(Box.createHorizontalGlue());
		b2.add(btn);
		b2.add(Box.createHorizontalStrut(Utility.toPixel(10)));
		b2.add(btn1);
		b.add(Box.createVerticalGlue());

		int w = Math.max(btn.getPreferredSize().width,
				btn1.getPreferredSize().width);
		btn.setPreferredSize(new Dimension(w, btn.getPreferredSize().height));
		btn1.setPreferredSize(new Dimension(w, btn1.getPreferredSize().height));

		b.setBorder(new EmptyBorder(Utility.toPixel(10), Utility.toPixel(10),
				Utility.toPixel(10), Utility.toPixel(10)));
		b2.setBorder(new EmptyBorder(Utility.toPixel(10), Utility.toPixel(10),
				Utility.toPixel(10), Utility.toPixel(10)));
		add(b);
		add(b2, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(null);
	}

	private boolean[] extractPermissions(int permissions) {
		boolean[] perms = new boolean[9];
		for (int i = 0; i < 9; i++) {
			perms[i] = (permissions & PermissionsDialog.perms[i]) != 0;
		}
//		perms[0] = (permissions & S_IRUSR) != 0;
//		perms[1] = (permissions & S_IWUSR) != 0;
//		perms[2] = (permissions & S_IXUSR) != 0;
//		perms[3] = (permissions & S_IRGRP) != 0;
//		perms[4] = (permissions & S_IWGRP) != 0;
//		perms[5] = (permissions & S_IXGRP) != 0;
//		perms[6] = (permissions & S_IROTH) != 0;
//		perms[7] = (permissions & S_IWOTH) != 0;
//		perms[8] = (permissions & S_IXOTH) != 0;
		return perms;
	}

	public void setDetails(FileInfo details) {
		System.out.println(details.getExtra());
		this.permissions = details.getPermission();
		if (this.pattern != null && details.getExtra() != null
				&& details.getExtra().length() > 0) {
			Matcher matcher = pattern.matcher(details.getExtra());
			if (matcher.find()) {
				String user = matcher.group(1);
				String group = matcher.group(2);

				txtOwner.setText(user);
				txtGroup.setText(group);
			}
		}
//		this.txtCreated.setText(
//				details.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
		// this.txtGroup.setText("");
		this.txtModified.setText(details.getLastModified()
				.format(DateTimeFormatter.ISO_DATE_TIME));
		this.txtName.setText(details.getName());
		this.txtPath.setText(details.getPath());
		this.txtSize.setText(details.getType() == FileType.Directory
				|| details.getType() == FileType.DirLink ? "---"
						: Utility.humanReadableByteCount(details.getSize(),
								true));
		this.txtType.setText(details.getType() == FileType.Directory
				|| details.getType() == FileType.DirLink ? "Directory"
						: "File");
		boolean[] perms = extractPermissions(permissions);
		for (int i = 0; i < 9; i++) {
			chkPermissons[i].setSelected(perms[i]);
		}
	}

	public void setMultipleDetails(FileInfo[] files) {
		int fc = 0, dc = 0;
		for (FileInfo f : files) {
			if (f.getType() == FileType.Directory
					|| f.getType() == FileType.DirLink) {
				dc++;
			} else {
				fc++;
			}
		}
		txtFileCount.setText(fc + " files, " + dc + " folders");
	}

	public int getPermissions() {
		int perms = 0;
		for (int i = 0; i < 9; i++) {
			if (chkPermissons[i].isSelected()) {
				perms |= PermissionsDialog.perms[i];
			}
		}
		return perms;
	}

	private Component addPropertyField(JTextField txt, String label) {
		txt.setEditable(false);
		txt.setBorder(null);
		JLabel lblFileName = new JLabel(label);
		lblFileName.setPreferredSize(new Dimension(Utility.toPixel(150),
				lblFileName.getPreferredSize().height));
		Box b11 = Box.createHorizontalBox();
		b11.setAlignmentX(Box.LEFT_ALIGNMENT);
		b11.add(lblFileName);
		// b11.add(Box.createHorizontalGlue());
		b11.add(txt);
		return b11;
	}

	static final int S_IRUSR = 00400; // read by owner
	static final int S_IWUSR = 00200; // write by owner
	static final int S_IXUSR = 00100; // execute/search by owner

	static final int S_IRGRP = 00040; // read by group
	static final int S_IWGRP = 00020; // write by group
	static final int S_IXGRP = 00010; // execute/search by group

	static final int S_IROTH = 00004; // read by others
	static final int S_IWOTH = 00002; // write by others
	static final int S_IXOTH = 00001; // execute/search by others

	static final int[] perms = new int[] { S_IRUSR, S_IWUSR, S_IXUSR, S_IRGRP,
			S_IWGRP, S_IXGRP, S_IROTH, S_IWOTH, S_IXOTH };

	public int getDialogResult() {
		return dialogResult;
	}

}
