package net.ser1.stomp;

import java.util.Map;

/**
 * (c)2005 Sean Russell
 */
public interface Listener {
    void message(Map<String, String> headers, String body);
}
