package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread extends Thread {
	private Socket client;
	private ObjectInputStream in;// from client
	private ObjectOutputStream out;// to client
	private boolean isRunning = false;
	private Room currentRoom;// what room we are in, should be lobby by default
	private String clientName;
	private final static Logger log = Logger.getLogger(ServerThread.class.getName());
	private String color;
	List<String> mutedClients = new ArrayList<String>();
	public String getClientName;

	public boolean isMuted(String clientName) {
		Iterator<String> iter = mutedClients.iterator();
		while (iter.hasNext()) {
			String mutedC = iter.next();
			if (mutedC.equals(clientName)) {
				return true;
			}
		}
		return false;
	}

	public String getClientName() {
		return clientName;
	}

	protected synchronized Room getCurrentRoom() {
		return currentRoom;
	}

	protected synchronized void setCurrentRoom(Room room) {
		if (room != null) {
			currentRoom = room;
		} else {
			log.log(Level.INFO, "Passed in room was null, this shouldn't happen");
		}
	}

	public ServerThread(Socket myClient, Room room) throws IOException {
		this.client = myClient;
		this.currentRoom = room;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
	}

	/***
	 * Sends the message to the client represented by this ServerThread
	 * 
	 * @param message
	 * @return
	 */
	@Deprecated
	protected boolean send(String message) {
		// added a boolean so we can see if the send was successful
		try {
			out.writeObject(message);
			return true;
		} catch (IOException e) {
			log.log(Level.INFO, "Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	/***
	 * Replacement for send(message) that takes the client name and message and
	 * converts it into a payload
	 * 
	 * @param clientName
	 * @param message
	 * @return
	 */
	protected boolean send(String clientName, String message) {
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.MESSAGE);
		payload.setClientName(clientName);
		payload.setMessage(processSpecialMessage(message));

		return sendPayload(payload);
	}

	protected String processSpecialMessage(String str) {
		int countBold = 0;
		int countItalic = 0;
		int countUnderline = 0;
		int countColor = 0;
		int targetChar = 0;
		for (int i = 0; i < str.length(); i++) {

			if (str.charAt(i) == '*') {
				countBold++;
			}

			if (str.charAt(i) == '#') {
				countItalic++;
			}

			if (str.charAt(0) == '_') {
				countUnderline++;
			}

			if (str.charAt(i) == '&') {
				countColor++;
				targetChar = str.indexOf('&');
				if (targetChar != -1) {
					color = str.substring(0, targetChar).toLowerCase();
				}
			}
		}

		if (countBold >= 2) {
			str = str.replace("*", "<b>");
			str = str.replace("<b> ", "</b> ");
		}
		if (countItalic >= 2) {
			str = str.replace("#", "<i>");
			str = str.replace("<i> ", "</i> ");
		}
		if (countUnderline >= 2) {
			str = str.replace("_", "<u>");
			str = str.replace("<u> ", "</u> ");
		}
		if (countColor >= 2 || color != null) {
			str = str.replace(str.substring(0, targetChar), "");
			str = str.replace("&", "<font style=color:" + color + ">");
			str = str.replace("<font style=color:" + color + "> ", "</font> ");
		}
		return str;
	}

	protected boolean sendConnectionStatus(String clientName, boolean isConnect, String message) {
		Payload payload = new Payload();
		if (isConnect) {
			payload.setPayloadType(PayloadType.CONNECT);
			payload.setMessage(message);
		} else {
			payload.setPayloadType(PayloadType.DISCONNECT);
			payload.setMessage(message);
		}
		payload.setClientName(clientName);
		return sendPayload(payload);
	}

	protected boolean sendClearList() {
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.CLEAR_PLAYERS);
		return sendPayload(payload);
	}

	private boolean sendPayload(Payload p) {
		try {
			out.writeObject(p);
			return true;
		} catch (IOException e) {
			log.log(Level.INFO, "Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}

	/***
	 * Process payloads we receive from our client
	 * 
	 * @param p
	 */
	private void processPayload(Payload p) {
		switch (p.getPayloadType()) {
		case CONNECT:
			// here we'll fetch a clientName from our client
			String n = p.getClientName();
			if (n != null) {
				clientName = n;
				log.log(Level.INFO, "Set our name to " + clientName);
				if (currentRoom != null) {
					currentRoom.joinLobby(this);
				}
			}
			break;
		case DISCONNECT:
			isRunning = false;// this will break the while loop in run() and clean everything up
			break;
		case MESSAGE:
			currentRoom.sendMessage(this, p.getMessage());
			break;
		case CLEAR_PLAYERS:
			// we currently don't need to do anything since the UI/Client won't be sending
			// this
			break;
		default:
			log.log(Level.INFO, "Unhandled payload on server: " + p);
			break;
		}
	}

	@Override
	public void run() {
		try {
			isRunning = true;
			Payload fromClient;
			while (isRunning && // flag to let us easily control the loop
					!client.isClosed() // breaks the loop if our connection closes
					&& (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream (null would
			// likely mean a disconnect)
			) {
				System.out.println("Received from client: " + fromClient);
				processPayload(fromClient);
			} // close while loop
		} catch (Exception e) {
			// happens when client disconnects
			e.printStackTrace();
			log.log(Level.INFO, "Client Disconnected");
		} finally {
			isRunning = false;
			log.log(Level.INFO, "Cleaning up connection for ServerThread");
			cleanup();
		}
	}

	private void cleanup() {
		if (currentRoom != null) {
			log.log(Level.INFO, getName() + " removing self from room " + currentRoom.getName());
			currentRoom.removeClient(this);
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				log.log(Level.INFO, "Input already closed");
			}
		}
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				log.log(Level.INFO, "Client already closed");
			}
		}
		if (client != null && !client.isClosed()) {
			try {
				client.shutdownInput();
			} catch (IOException e) {
				log.log(Level.INFO, "Socket/Input already closed");
			}
			try {
				client.shutdownOutput();
			} catch (IOException e) {
				log.log(Level.INFO, "Socket/Output already closed");
			}
			try {
				client.close();
			} catch (IOException e) {
				log.log(Level.INFO, "Client already closed");
			}
		}
	}
}