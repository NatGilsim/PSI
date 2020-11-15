package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ServerUPD extends Thread implements ClientToServerUdpProtocol {
	
	private DatagramSocket senderSock = null;
	private byte[] data = null;
	private DatagramPacket dpReceive = null;
	private DatagramPacket dpSend = null;
	private Client client;
	private Map<String, InetAddress> peers = new HashMap<String, InetAddress>();
	
	public ServerUPD(Client client) throws IOException {
		this.senderSock = new DatagramSocket(7201);
		this.client = client;
		Timer timer = new Timer();
		TimerTask task = new UpdaterAcknoledgement(this.client);
		timer.schedule(task, 0, 1000);
	}
	
	@Override
	public void run() {
		String input = null;
		while (true) {
			data = new byte[1024];
			dpReceive = new DatagramPacket(data, data.length);
			try {
				senderSock.receive(dpReceive);
			} catch (IOException e) {
				e.printStackTrace();
			}
			input = new String(dpReceive.getData(), 0, dpReceive.getLength());
			processInput(input, dpReceive.getAddress());
		}
	}
	
	private void processInput(String input, InetAddress ip) {
		String[] parsed = input.split("\n");
		String emetteur = null, timestamp = null, msg = null;
		switch(parsed[0]) {
		case "MSG":
			emetteur = parsed[1];
			timestamp = parsed[2];
			msg = parsed[3];
			if (!this.peers.containsKey(emetteur))
    			this.peers.put(emetteur, ip);
			this.client.printConsole("Message <" + msg + "> received from <" + emetteur + "> with timestamp <" + timestamp + "> and sending aknoledgement.");
			this.client.addChat(emetteur);
			this.client.receiveMsg(emetteur, " [From] " + msg);
			try {
				this.sendAck(emetteur, timestamp);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case "MSG_ACK":
			emetteur = parsed[1];
			timestamp = parsed[2];
			if (this.client.getAcknowledgementMap().containsKey(emetteur + timestamp)) {
				this.client.printConsole("Message from <" + emetteur + "> with timestamp <" + timestamp + "> is aknowledged.");
				this.client.getAcknowledgementMap().get(emetteur + timestamp).setAck(true);
			}
			break;
		default:
			this.client.printConsole("Server UDP receive an unknow method <" + parsed[0] + ">.");
			break;
		}
	}

	private void sendAck(String emetteur, String timestamp) throws IOException {
		String pck;
		pck = "MSG_ACK" + "\n";
		pck += emetteur + "\n";
		pck += timestamp + "\n";
		pck += ".";
		dpSend = new DatagramPacket(pck.getBytes(), pck.getBytes().length, peers.get(emetteur), 7201);
		senderSock.send(dpSend);
	}
	
	public void sendMessage(String to, String msg) throws IOException {
		String pck;
		long timestamp;
		pck = "MSG" + "\n";
		pck += this.client.getName() + "\n";
		timestamp = System.currentTimeMillis();
		pck += timestamp + "\n";
		pck += msg + "\n";
		pck += ".";
		dpSend = new DatagramPacket(pck.getBytes(), pck.getBytes().length, peers.get(to), 7201);
		this.client.getAcknowledgementMap().put(this.client.getName() + timestamp, new MessageUDP(to, msg));
		this.client.printConsole("Message <" + msg + "> from <" + this.client.getName() + "> with timestamp <" + timestamp + "> send to <" + to + "> waiting for aknowledgement.");
		senderSock.send(dpSend);
	}
	
	public void addPeer(String name, InetAddress inetAddress) {
		this.peers.put(name, inetAddress);
	}
    
    private class UpdaterAcknoledgement extends TimerTask {
    	
    	private Client client;
    	
    	public UpdaterAcknoledgement(Client client) {
    		this.client = client;
    	}
    	
    	public void run() {
    		ArrayList<String> toDelete = new ArrayList<>();
    		for (Map.Entry<String, MessageUDP> entry : this.client.getAcknowledgementMap().entrySet()) {
    			if (!entry.getValue().getAck()) {
    				try {
						sendMessage(entry.getValue().getDestinataire(), entry.getValue().getMsg());
					} catch (IOException e) {
						e.printStackTrace();
					}
    			} else {
    				toDelete.add(entry.getKey());
    			}
    		}
    		for (String s : toDelete)
    			this.client.getAcknowledgementMap().remove(s);
    	}
    }

	@Override
	public void msg(String emetteur, String timestamp, String msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void msgAck(String emetteur, String timestamp) {
		// TODO Auto-generated method stub
		
	}
	
}