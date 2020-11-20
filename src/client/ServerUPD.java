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
	private int limitAck = 5;
	private boolean stop = false;
	
	public ServerUPD(Client client) throws IOException {
		this.senderSock = new DatagramSocket(7201);
		this.client = client;
		Timer timer = new Timer();
		TimerTask checkAknowledgement = new UpdaterAcknoledgement(this.client);
		timer.schedule(checkAknowledgement, 0, 1000);
	}
	
	@Override
	public void run() {
		String input = null;
		while (!stop) {
			data = new byte[1024];
			dpReceive = new DatagramPacket(data, data.length);
			try {
				senderSock.receive(dpReceive);
			} catch (IOException e) {
				if (stop == false)
					System.out.println("Problem.");
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
			if (!this.peers.containsKey(emetteur)) {
				this.peers.put(emetteur, ip);
			}
			this.msg(emetteur, timestamp, msg);
			break;
		case "MSG_ACK":
			emetteur = parsed[1];
			timestamp = parsed[2];
			this.msgAck(emetteur, timestamp);
			break;
		default:
			this.client.printConsole("Server UDP receive an unknow method <" + parsed[0] + ">.");
			break;
		}
	}
	
	public void stopServer() {
		this.stop = true;
		senderSock.close();
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
	
	public void sendMessage(String to, String msg, long timestamp, boolean isAck) throws IOException {
		String pck;
		pck = "MSG" + "\n";
		pck += this.client.getName() + "\n";
		pck += timestamp + "\n";
		pck += msg + "\n";
		pck += ".";
		//System.out.println("to: " + to + "  ip adress:" + peers.get(to));
		dpSend = new DatagramPacket(pck.getBytes(), pck.getBytes().length, peers.get(to), 7201);
		if (!isAck)
			this.client.addMsgToBeAcknowledge(new MessageUDP(this.client.getName(), to, msg, timestamp));
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
    		ArrayList<MessageUDP> toDelete = new ArrayList<>();
    		for (MessageUDP m : this.client.getMsgToBeAcknowledge()) {
    			if (!m.getAck()) {
    				if ((System.currentTimeMillis() - m.getTimestamp()) >= Math.pow(2, m.getCounter()) * 1000) {
    					if (m.getCounter() == (limitAck + 1)) {
    						
    						this.client.printConsole("Message <" + m.getContent() + "> with timestamp <" + m.getTimestamp() + "> was never aknowledged and is now deleted, please send again the message.");
    						toDelete.add(m);
    					} else {
		    				try {
		    					m.setTimestamp();
								sendMessage(m.getDestinataire(), m.getContent(), m.getTimestamp(), true);
							} catch (IOException e) {
								e.printStackTrace();
							}
		    				m.incrementCounter();
    					}
    				}
    			} else {
    				toDelete.add(m);
    			}
    		}
    		for (MessageUDP m : toDelete)
    			this.client.getMsgToBeAcknowledge().remove(m);
    	}
    }

    /* Client to Server UDP protocol */
    
	@Override
	public void msg(String emetteur, String timestamp, String msg) {
		this.client.printConsole("Message <" + msg + "> received from <" + emetteur + "> with timestamp <" + timestamp + "> and sending aknoledgement.");
		this.client.addChat(emetteur);
		this.client.receiveMsg(emetteur, timestamp, msg);
		try {
			this.sendAck(emetteur, timestamp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void msgAck(String emetteur, String timestamp) {
		this.client.printConsole("Message with timestamp <" + timestamp + "> send by <" + emetteur + "> is aknowledged.");
		for (MessageUDP m : this.client.getMsgToBeAcknowledge()) {
			if (m.getIdMsg().equals(emetteur + timestamp)) {
				m.setAck(true);
				return;
			}
		}
	}
	
}