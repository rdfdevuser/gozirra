package net.ser1.stomp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;

/**
 * Implements a Stomp client connection to a Stomp server via the network.
 * <p/>
 * Example:
 * 
 * <pre>
 *   Client c = new Client( "localhost", 61626, "ser", "ser" );
 *   c.subscribe( "/my/channel", new Listener() { ... } );
 *   c.subscribe( "/my/other/channel", new Listener() { ... } );
 *   c.send( "/other/channel", "Some message" );
 *   // ...
 *   c.disconnect();
 * </pre>
 * 
 * @see Stomp <p/>
 *      (c)2005 Sean Russell
 */
public class Client extends Stomp implements MessageReceiver {
	private Thread listener;
	private OutputStream output;
	private InputStream input;
	private Socket socket;
	private static final String ACCEPT_VERSION = "accept-version";
	private static final String VERSION_NUMBER = "1.1";
	private static final String HOST = "host";
	private static final String LOGIN = "login";
	private static final String PASSCODE = "passcode";
	private static final String HEARTBEAT = "heart-beat";
	private static final String HEARTBEAT_CONFIG = "0,1000";

	/**
	 * Connects to a server
	 * <p/>
	 * Example:
	 * 
	 * <pre>
	 *   Client stomp_client = new Client( "host.com", 61626 );
	 *   stomp_client.subscribe( "/my/messages" );
	 *   ...
	 * </pre>
	 * 
	 * @param server
	 *            The IP or host name of the server
	 * @param port
	 *            The port the server is listening on
	 * @see Stomp
	 */
	public Client(String server, int port, String login, String pass) throws IOException, LoginException {
		socket = new Socket(server, port);
		input = socket.getInputStream();
		output = socket.getOutputStream();

		listener = new Receiver(this, input);
		listener.start();

		// Connect to the server
		LinkedHashMap<String, String> header = new LinkedHashMap<String, String>();
		header.put(ACCEPT_VERSION, VERSION_NUMBER);
		header.put(HOST, server);
		header.put(LOGIN, login);
		header.put(PASSCODE, pass);
		header.put(HEARTBEAT, HEARTBEAT_CONFIG);

		transmit(Command.connect, header, null);
		try {
			String error = null;
			while (!isConnected() && ((error = nextError()) == null)) {
				Thread.sleep(100);
			}
			if (error != null)
				throw new LoginException(error);
		} catch (InterruptedException e) {
		}
	}

	public boolean isClosed() {
		return socket.isClosed();
	}

	public void disconnect(Map<String, String> header) {
		if (!isConnected())
			return;
		transmit(Command.disconnect, header, null);
		listener.interrupt();
		Thread.yield();
		try {
			input.close();
		} catch (IOException e) {/* We ignore these. */
		}
		try {
			output.close();
		} catch (IOException e) {/* We ignore these. */
		}
		try {
			socket.close();
		} catch (IOException e) {/* We ignore these. */
		}
		connected = false;
	}

	/**
	 * Transmit a message to the server
	 */
	public void transmit(Command command, Map<String, String> header, String body) {
		try {
			Transmitter.transmit(command, header, body, output);
		} catch (Exception e) {
			receive(Command.error, null, e.getMessage());
		}
	}
}
