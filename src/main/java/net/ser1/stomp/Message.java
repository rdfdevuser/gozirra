package net.ser1.stomp;

import java.util.Map;

/**
 * (c)2005 Sean Russell
 */
public class Message {

	private Command command;
	private Map<String, String> headers;
	private String body;

	protected Message(Command acommand, Map<String, String> aheaders, String abody) {
		command = acommand;
		headers = aheaders;
		body = abody;
	}

	public Map<String, String> headers() {
		return headers;
	}

	public String body() {
		return body;
	}

	public Command command() {
		return command;
	}
}
