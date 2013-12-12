package net.ser1.stomp;

import java.util.Map;

/**
 * A client that is connected directly to a server.  Messages sent via
 * this client do not go through a network interface, except when being
 * delivered to clients connected via the network... all messages to
 * other IntraVMClients are delivered entirely in memory.
 * <p/>
 * (c)2005 Sean Russell
 */
public class IntraVMClient extends Stomp implements Listener, Authenticatable {
    private Server server;
    private static final String INTRA_VM_CLIENT = "IntraVMClient";

    protected IntraVMClient(Server server) {
        this.server = server;
        connected = true;
    }

    public boolean isClosed() {
        return false;
    }


    public Object token() {
        return INTRA_VM_CLIENT;
    }


    /**
     * Transmit a message to clients and listeners.
     */
    public void transmit(Command command, Map<String, String> headers, String body) {
        this.server.receive(command, headers, body, this);
    }


    public void disconnect(Map<String, String> headers) {
        this.server.receive(Command.disconnect, null, null, this);
        this.server = null;
    }

    public void message(Map<String, String> headers, String body) {
        receive(Command.message, headers, body);
    }

    public void receipt(Map<String, String> headers) {
        receive(Command.receipt, headers, null);
    }

    public void error(Map<String, String> headers, String body) {
        receive(Command.error, headers, body);
    }
}
