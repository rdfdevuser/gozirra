package net.ser1.stomp;


/**
 * (c)2005 Sean Russell
 */
public final class Command {
    public final static String ENCODING = "US-ASCII";
    private String command;
    
    private static final String SEND = "SEND";
    private static final String SUBSCRIBE = "SUBSCRIBE";
    private static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    private static final String BEGIN = "BEGIN";
    private static final String COMMIT = "COMMIT";
    private static final String ABORT = "ABORT";
    private static final String DISCONNECT = "DISCONNECT";
    private static final String CONNECT = "CONNECT";
    private static final String MESSAGE = "MESSAGE";
    private static final String RECEIPT = "RECEIPT";
    private static final String CONNECTED = "CONNECTED";
    private static final String ERROR = "ERROR";   
    
    private Command(String msg) {
        command = msg;
    }

    public static Command send = new Command(SEND),
        subscribe = new Command(SUBSCRIBE),
        unsubscribe = new Command(UNSUBSCRIBE),
        begin = new Command(BEGIN),
        commit = new Command(COMMIT),
        abort = new Command(ABORT),
        disconnect = new Command(DISCONNECT),
        connect = new Command(CONNECT);

    public static Command message = new Command(MESSAGE),
        receipt = new Command(RECEIPT),
        connected = new Command(CONNECTED),
        error = new Command(ERROR);

    public static Command valueOf(String v) {
        v = v.trim();
        if (v.equals(SEND)) return send;
        else if (v.equals(SUBSCRIBE)) return subscribe;
        else if (v.equals(UNSUBSCRIBE)) return unsubscribe;
        else if (v.equals(BEGIN)) return begin;
        else if (v.equals(COMMIT)) return commit;
        else if (v.equals(ABORT)) return abort;
        else if (v.equals(CONNECT)) return connect;
        else if (v.equals(MESSAGE)) return message;
        else if (v.equals(RECEIPT)) return receipt;
        else if (v.equals(CONNECTED)) return connected;
        else if (v.equals(DISCONNECT)) return disconnect;
        else if (v.equals(ERROR)) return error;
        throw new IllegalArgumentException("Unrecognised command " + v);
    }

    public String toString() {
        return command;
    }
}

