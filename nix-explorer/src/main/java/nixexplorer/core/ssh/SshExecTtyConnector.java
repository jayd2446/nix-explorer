package nixexplorer.core.ssh;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jediterm.terminal.Questioner;
import com.jediterm.terminal.TtyConnector;

import nixexplorer.app.session.SessionInfo;

public class SshExecTtyConnector implements DisposableTtyConnector {
	private InputStreamReader myInputStreamReader;
	private InputStream myInputStream = null;
	private OutputStream myOutputStream = null;
	private AtomicBoolean isInitiated = new AtomicBoolean(false);
	private SessionInfo info;
	private SshWrapper wr;
	private AtomicBoolean isCancelled = new AtomicBoolean(false);
	private AtomicBoolean stopFlag = new AtomicBoolean(false);
	private ChannelExec channel;
	private String command;
	private Dimension myPendingTermSize;
	private Dimension myPendingPixelSize;

	public SshExecTtyConnector(SessionInfo info, String command) {
		this.info = info;
		this.command = command;
	}

	@Override
	public boolean init(Questioner q) {
		System.out.println("Connecting exec tty connector");
		try {
			wr = new SshWrapper(info);
			while (!stopFlag.get()) {
				try {
					wr.connect();
					this.channel = wr.getExecChannel();
					this.channel.setCommand(command);
					break;
				} catch (Exception e) {
					e.printStackTrace();
					if (!stopFlag.get()) {
						if (JOptionPane.showConfirmDialog(null,
								"Unable to connect to server. Retry?") != JOptionPane.YES_OPTION) {
							throw new Exception("User cancelled the operation");
						}
					}
				}
			}

			if (!wr.isConnected()) {
				throw new IOException("Unable to connect");
			}

			String lang = System.getenv().get("LANG");
			channel.setEnv("LANG", lang != null ? lang : "en_US.UTF-8");
			channel.setPtyType("xterm");
			channel.setPty(true);

//			PipedOutputStream pout1 = new PipedOutputStream();
//			PipedInputStream pin1 = new PipedInputStream(pout1);
//			channel.setOutputStream(pout1);
//
//			PipedOutputStream pout2 = new PipedOutputStream();
//			PipedInputStream pin2 = new PipedInputStream(pout2);
//			channel.setInputStream(pin2);

			myInputStream = channel.getInputStream();
			myOutputStream = channel.getOutputStream();
			myInputStreamReader = new InputStreamReader(myInputStream, "utf-8");
			channel.connect();
			System.out.println("Initiated");

			// resize(termSize, pixelSize);
			isInitiated.set(true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			isInitiated.set(false);
			isCancelled.set(true);
			return false;
		}
	}

	@Override
	public void close() {
		try {
			System.out.println("Terminal wrapper disconnecting");
			wr.disconnect();
		} catch (Exception e) {
		}
	}

	@Override
	public void resize(Dimension termSize, Dimension pixelSize) {
		myPendingTermSize = termSize;
		myPendingPixelSize = pixelSize;
		if (channel != null) {
			resizeImmediately();
		}

//		if (channel == null) {
//			return;
//		}
//		System.out.println("Terminal resized");
//		channel.setPtySize(termSize.width, termSize.height, pixelSize.width, pixelSize.height);
	}

	@Override
	public String getName() {
		return "Remote";
	}

	@Override
	public int read(char[] buf, int offset, int length) throws IOException {
		return myInputStreamReader.read(buf, offset, length);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		myOutputStream.write(bytes);
		myOutputStream.flush();
	}

	@Override
	public boolean isConnected() {
		if (channel != null && channel.isConnected() && isInitiated.get()) {
			return true;
		}
		return false;
	}

	@Override
	public void write(String string) throws IOException {
		write(string.getBytes("utf-8"));
	}

	@Override
	public int waitFor() throws InterruptedException {
		System.out.println("Start waiting...");
		while (!isInitiated.get() || isRunning(channel)) {
			System.out.println("waiting");
			Thread.sleep(100); // TODO: remove busy wait
		}
		System.out.println("waiting exit");
		return channel.getExitStatus();
	}

	public boolean isRunning(Channel channel) {
		return channel != null && channel.getExitStatus() < 0 && channel.isConnected();
	}

	public boolean isBusy() {
		return channel.getExitStatus() < 0 && channel.isConnected();
	}

	public boolean isCancelled() {
		return isCancelled.get();
	}

	public void stop() {
		stopFlag.set(true);
		close();
	}

	public int getExitStatus() {
		if (channel != null) {
			return channel.getExitStatus();
		}
		return -2;
	}

	private void resizeImmediately() {
		if (myPendingTermSize != null && myPendingPixelSize != null) {
			setPtySize(channel, myPendingTermSize.width, myPendingTermSize.height, myPendingPixelSize.width,
					myPendingPixelSize.height);
			myPendingTermSize = null;
			myPendingPixelSize = null;
		}
	}

	private void setPtySize(ChannelExec channel, int col, int row, int wp, int hp) {
		System.out.println("Exec pty resized");
		channel.setPtySize(col, row, wp, hp);
	}
}
