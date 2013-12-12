package net.ser1.stomp;

import java.util.Map;

/**
 * (c)2005 Sean Russell
 */
public interface MessageReceiver {
    
	void receive(Command command, Map<String, String> header, String body);

    void disconnect();

    boolean isClosed();
}

