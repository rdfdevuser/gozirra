package net.ser1.stomp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * A Stomp messaging implementation.
 * <p/>
 * Messages are handled in one of two ways. If subscribe was called with a
 * listener, then incoming messages are delivered to all listeners of that
 * channel, and the message is deleted from the queue. If no listener was
 * provided for that channel, then messages are placed in a queue and can be
 * retrieved with getNext(). In all cases, when messages are retrieved, they are
 * deleted from the queue.
 * <p/>
 * Notes: * FIXME: ERROR messages don't do anything.
 * <p/>
 * (c)2005 Sean Russell
 */
public abstract class Stomp {

	/**
	 * A map of channel => listener pairs. String => Listener.
	 */
	private Map<String, List> listeners = new HashMap<String, List>();
	/**
	 * Things that are listening for communication errors. Contains Listeners.
	 */
	private List<Listener> error_listeners = new ArrayList<Listener>();
	/**
	 * A message queue; where messages that have no listeners are stored.
	 * Contains Messages.
	 */
	private Stack<Message> queue = new Stack<Message>();
	/**
	 * Incoming receipts (as String IDs)
	 */
	private List<String> receipts = new ArrayList<String>();
	/**
	 * True if connected to a server; false otherwise
	 */
	protected boolean connected = false;
	/**
	 * Incoming errors (as String messages)
	 */
	private List<String> errors = new ArrayList<String>();

	private static final String ID = "id";
	private static final String THE_ID = "theid";
	private static final String DESTINATION = "destination";
	private static final String RECEIPT = "receipt";
	private static final String RECEIPT_ID = "receipt-id";

	
	
	/**
	 * Disconnect from a server, including headers. Must be implemented by the
	 * child class. Should set the _connected flag to false.
	 * 
	 * @param header
	 *            A map of key/value headers
	 */
	public abstract void disconnect(Map<String, String> header);

	/**
	 * Transmit a message to a server. Must be implemented by the child class.
	 * The implementation must handle cases where the header and/or the body are
	 * null.
	 * 
	 * @param command
	 *            The Stomp command. If null, causes an error.
	 * @param header
	 *            A map of headers. If null, an empty map is used.
	 * @param body
	 *            The body of the message. May be empty.
	 */
	protected abstract void transmit(Command command, Map<String, String> header, String body);

	/**
	 * Disconnect from a server. Must be implemented by the child class.
	 */
	public void disconnect() {
		disconnect(null);
	}

	/**
	 * Transmit a message to a server.
	 * 
	 * @param command
	 *            The Stomp command. If null, causes an error.
	 * @param header
	 *            A map of headers. If null, an empty map is used.
	 */
	protected void transmit(Command command, Map<String, String> header) {
		transmit(command, header, null);
	}

	/**
	 * Transmit a message to a server.
	 * 
	 * @param command
	 *            The Stomp command. If null, causes an error.
	 */
	protected void transmit(Command command) {
		transmit(command, null, null);
	}

	/**
	 * Begins a transaction. Messages will not be delivered to subscribers until
	 * commit() has been called.
	 */
	public void begin() {
		transmit(Command.begin);
	}

	/**
	 * Begins a transaction. Messages will not be delivered to subscribers until
	 * commit() has been called.
	 * 
	 * @param header
	 *            Additional headers to send to the server.
	 */
	public void begin(Map<String, String> header) {
		transmit(Command.begin, header);
	}

	/**
	 * Commits a transaction, causing any messages sent since begin() was called
	 * to be delivered.
	 */
	public void commit() {
		transmit(Command.commit);
	}

	/**
	 * Commits a transaction, causing any messages sent since begin() was called
	 * to be delivered.
	 * 
	 * @param header
	 *            Additional headers to send to the server.
	 */
	public void commit(Map<String, String> header) {
		transmit(Command.begin, header);
	}

	/**
	 * Commits a transaction, causing any messages sent since begin() was called
	 * to be delivered. This method does not return until the server has
	 * confirmed that the commit was successful.
	 */
	public void commitW() throws InterruptedException {
		commitW(null);
	}

	/**
	 * Commits a transaction, causing any messages sent since begin() was called
	 * to be delivered. This method does not return until the server has
	 * confirmed that the commit was successful.
	 */
	public void commitW(Map<String, String> header) throws InterruptedException {
		String receipt = addReceipt(header);
		transmit(Command.commit, header);
		waitOnReceipt(receipt);
	}

	/**
	 * Aborts a transaction. Messages sent since begin() was called are
	 * destroyed and are not sent to subscribers.
	 */
	public void abort() {
		transmit(Command.abort);
	}

	/**
	 * Aborts a transaction. Messages sent since begin() was called are
	 * destroyed and are not sent to subscribers.
	 * 
	 * @param header
	 *            Additional headers to send to the server.
	 */
	public void abort(Map<String, String> header) {
		transmit(Command.abort, header);
	}

	/**
	 * Subscribe to a channel.
	 * 
	 * @param name
	 *            The name of the channel to listen on
	 */
	public void subscribe(String name) {
		subscribe(name, null, null);
	}

	/**
	 * Subscribe to a channel.
	 * 
	 * @param name
	 *            The name of the channel to listen on
	 * @param header
	 *            Additional headers to send to the server.
	 */
	public void subscribe(String name, Map<String, String> header) {
		subscribe(name, null, header);
	}

	/**
	 * Subscribe to a channel.
	 * 
	 * @param name
	 *            The name of the channel to listen on
	 * @param listener
	 *            A listener to receive messages sent to the channel
	 */
	public void subscribe(String name, Listener alistener) {
		subscribe(name, alistener, null);
	}

	/**
	 * Subscribe to a channel.
	 * 
	 * @param name
	 *            The name of the channel to listen on
	 * @param headers
	 *            Additional headers to send to the server.
	 * @param alistener
	 *            A listener to receive messages sent to the channel
	 */
	public void subscribe(String name, Listener alistener, Map<String, String> headers) {
		synchronized (listeners) {
			if (alistener != null) {
				List<Listener> list = listeners.get(name);
				if (list == null) {
					list = new ArrayList<Listener>();
					listeners.put(name, list);
				}
				if (!list.contains(alistener))
					list.add(alistener);
			}
		}
		if (headers == null) {
			headers = new HashMap<String, String>();
		}
		
		headers.put(ID, THE_ID);
		headers.put(DESTINATION, name);
		transmit(Command.subscribe, headers);
	}

	private String addReceipt(Map<String, String> header) {
		if (header == null)
			header = new HashMap<String, String>();
		String receipt = String.valueOf(hashCode()) + "&" + System.currentTimeMillis();
		header.put(RECEIPT, receipt);
		return receipt;
	}

	/**
	 * Subscribe to a channel. This method blocks until it receives a receipt
	 * from the server.
	 * 
	 * @param name
	 *            The name of the channel to listen on
	 * @param headers
	 *            Additional headers to send to the server.
	 * @param alistener
	 *            A listener to receive messages sent to the channel
	 */
	public void subscribeW(String name, Listener alistener, Map<String, String> header) throws InterruptedException {
		String receipt = addReceipt(header);
		subscribe(name, alistener, header);
		waitOnReceipt(receipt);
	}

	/**
	 * Subscribe to a channel. This method blocks until it receives a receipt
	 * from the server.
	 * 
	 * @param name
	 *            The name of the channel to listen on
	 * @param alistener
	 *            A listener to receive messages sent to the channel
	 */
	public void subscribeW(String name, Listener alistener) throws InterruptedException {
		subscribeW(name, alistener, null);
	}

	/**
	 * Unsubscribe from a channel. Automatically unregisters all listeners of
	 * the channel. To re-subscribe with listeners, subscribe must be passed the
	 * listeners again.
	 * 
	 * @param name
	 *            The name of the channel to unsubscribe from.
	 */
	public void unsubscribe(String name) {
		unsubscribe(name, (HashMap<String, String>) null);
	}

	/**
	 * Unsubscribe a single listener from a channel. This does not send a
	 * message to the server unless the listener is the only listener of this
	 * channel.
	 * 
	 * @param name
	 *            The name of the channel to unsubscribe from.
	 * @param listener
	 *            The listener to unsubscribe
	 */
	public void unsubscribe(String name, Listener alistener) {
		synchronized (listeners) {
			List list = listeners.get(name);
			if (list != null) {
				list.remove(alistener);
				if (list.size() == 0) {
					unsubscribe(name);
				}
			}
		}
	}

	/**
	 * Unsubscribe from a channel. Automatically unregisters all listeners of
	 * the channel. To re-subscribe with listeners, subscribe must be passed the
	 * listeners again.
	 * 
	 * @param name
	 *            The name of the channel to unsubscribe from.
	 * @param header
	 *            Additional headers to send to the server.
	 */
	public void unsubscribe(String name, Map<String, String> header) {
		if (header == null)
			header = new HashMap<String, String>();
		synchronized (listeners) {
			listeners.remove(name);
		}
		header.put(DESTINATION, name);
		transmit(Command.unsubscribe, header);
	}

	/**
	 * Unsubscribe from a channel. Automatically unregisters all listeners of
	 * the channel. To re-subscribe with listeners, subscribe must be passed the
	 * listeners again. This method blocks until a receipt is received from the
	 * server.
	 * 
	 * @param name
	 *            The name of the channel to unsubscribe from.
	 */
	public void unsubscribeW(String name) throws InterruptedException {
		unsubscribe(name, (HashMap<String, String>) null);
	}

	/**
	 * Unsubscribe from a channel. Automatically unregisters all listeners of
	 * the channel. To re-subscribe with listeners, subscribe must be passed the
	 * listeners again. This method blocks until a receipt is received from the
	 * server.
	 * 
	 * @param name
	 *            The name of the channel to unsubscribe from.
	 */
	public void unsubscribeW(String name, Map<String, String> header) throws InterruptedException {
		String receipt = addReceipt(header);
		unsubscribe(name, (HashMap<String, String>) null);
		waitOnReceipt(receipt);
	}

	/**
	 * Send a message to a channel synchronously. This method does not return
	 * until the server acknowledges with a receipt.
	 * 
	 * @param dest
	 *            The name of the channel to send the message to
	 * @param mesg
	 *            The message to send.
	 */
	public void sendW(String dest, String mesg) throws InterruptedException {
		sendW(dest, mesg, null);
	}

	/**
	 * Send a message to a channel synchronously. This method does not return
	 * until the server acknowledges with a receipt.
	 * 
	 * @param dest
	 *            The name of the channel to send the message to
	 * @param mesg
	 *            The message to send.
	 */
	public void sendW(String dest, String mesg, Map<String, String> header) throws InterruptedException {
		String receipt = addReceipt(header);
		send(dest, mesg, header);
		waitOnReceipt(receipt);
	}

	/**
	 * Send a message to a channel.
	 * 
	 * @param dest
	 *            The name of the channel to send the message to
	 * @param mesg
	 *            The message to send.
	 */
	public void send(String dest, String mesg) {
		send(dest, mesg, null);
	}

	/**
	 * Send a message to a channel.
	 * 
	 * @param dest
	 *            The name of the channel to send the message to
	 * @param mesg
	 *            The message to send.
	 * @param header
	 *            Additional headers to send to the server.
	 */
	public void send(String dest, String mesg, Map<String, String> header) {
		if (header == null)
			header = new HashMap<String, String>();
		header.put(DESTINATION, dest);
		transmit(Command.send, header, mesg);
	}

	/**
	 * Get the next unconsumed message in the queue. This is non-blocking.
	 * 
	 * @return the next message in the queue, or null if the queue contains no
	 *         messages. This is non-blocking.
	 */
	public Message getNext() {
		synchronized (queue) {
			return queue.pop();
		}
	}

	/**
	 * Get the next unconsumed message for a particular channel. This is
	 * non-blocking.
	 * 
	 * @param name
	 *            the name of the channel to search for
	 * @return the next message for the channel, or null if the queue contains
	 *         no messages for the channel.
	 */
	public Message getNext(String name) {
		synchronized (queue) {
			for (int idx = 0; idx < queue.size(); idx++) {
				Message m = queue.get(idx);
				if (m.headers().get("destination").equals(name)) {
					queue.remove(idx);
					return m;
				}
			}
		}
		return null;
	}

	public void addErrorListener(Listener alistener) {
		synchronized (error_listeners) {
			error_listeners.add(alistener);
		}
	}

	public void delErrorListener(Listener alistener) {
		synchronized (error_listeners) {
			error_listeners.remove(alistener);
		}
	}

	/**
	 * Checks to see if a receipt has come in.
	 * 
	 * @param receipt_id
	 *            the id of the receipts to find
	 */
	public boolean hasReceipt(String receipt_id) {
		synchronized (receipts) {
			for (Iterator<String> i = receipts.iterator(); i.hasNext();) {
				String o = i.next();
				if (o.equals(receipt_id))
					return true;
			}
		}
		return false;
	}

	/**
	 * Deletes all receipts with a given ID
	 * 
	 * @param receipt_id
	 *            the id of the receipts to delete
	 */
	public void clearReceipt(String receipt_id) {
		synchronized (receipts) {
			for (Iterator<String> i = receipts.iterator(); i.hasNext();) {
				String o = i.next();
				if (o.equals(receipt_id))
					i.remove();
			}
		}
	}

	/**
	 * Remove all of the receipts
	 */
	public void clearReceipts() {
		synchronized (receipts) {
			receipts.clear();
		}
	}

	public void waitOnReceipt(String receipt_id) throws java.lang.InterruptedException {
		synchronized (receipts) {
			while (!hasReceipt(receipt_id))
				receipts.wait();
		}
	}

	public boolean waitOnReceipt(String receipt_id, long timeout) throws java.lang.InterruptedException {
		synchronized (receipts) {
			while (!hasReceipt(receipt_id))
				receipts.wait(timeout);
			if (receipts.contains(receipt_id)) {
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public String nextError() {

		synchronized (errors) {
			if (errors.size() == 0)
				return null;
			return errors.remove(0);
		}
	}

	public void receive(Command command, Map<String, String> headers, String body) {

		if (command == Command.message) {
			String destination = headers.get(DESTINATION);
			synchronized (listeners) {
				List listenersList = listeners.get(destination);
				if (listenersList != null) {
					listenersList = new ArrayList(listenersList);
					for (Iterator i = listenersList.iterator(); i.hasNext();) {
						Listener l = (Listener) i.next();
						try {
							l.message(headers, body);
						} catch (Exception e) {
							// Don't let listeners screw us over by throwing
							// exceptions
						}
					}
				} else {
					queue.push(new Message(command, headers, body));
				}
			}

		} else if (command == Command.connected) {
			connected = true;

		} else if (command == Command.receipt) {
			receipts.add(headers.get(RECEIPT_ID));
			synchronized (receipts) {
				receipts.notify();
			}

		} else if (command == Command.error) {
			if (error_listeners.size() > 0) {
				synchronized (error_listeners) {
					for (Iterator<Listener> i = error_listeners.iterator(); i.hasNext();) {
						try {
							i.next().message(headers, body);
						} catch (Exception e) {
							// Don't let listeners screw us over by throwing
							// exceptions
						}
					}
				}
			} else {
				synchronized (errors) {
					errors.add(body);
				}
			}
		} else {
			// FIXME
		}
	}
}
