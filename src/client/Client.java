package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import protocol.ClientToServerTcpProtocol;
import protocol.ServerTcpToClientProtocol;

public class Client implements ServerTcpToClientProtocol, ClientToServerTcpProtocol {

	private Socket s = null;
	private OutputStream os = null;
	private PrintWriter writer = null;
	private boolean quit = false;
	private int port = -2;
	private String name = null;
	private String token = null;
	private Thread inputCmd; // thread use to process cmd receive from server (run only when client is connected to server)
	private ClientGui gui;
	private ServerUPD serverUDP;
	//private Map<String, MessageUDP> acknowledgementMsg = new HashMap<String, MessageUDP>();
	private ArrayList<MessageUDP> msgToBeAcknowledge = new ArrayList<>();
	private Map<String, ArrayList<MessageChat>> chatMsg = new HashMap<String, ArrayList<MessageChat>>();

	public Client(int port) throws UnknownHostException, IOException {
		this.port = port;
		this.quit = false;
		//inputCmd = new Thread(inCmd);
	    this.gui = new ClientGui(this);
	    this.gui.setVisible(true);
	}

	protected void processInput(String input) throws UnknownHostException, IOException {
		String[] parsed = input.split("\n");
		switch(parsed[0]) {
		case "CONNECT_OK":
            this.connectOk(parsed[1], parsed[2]);
			break;
		case "CONNECT_NEW_USER_OK":
            this.connectNewUserOk(parsed[1]);
			break;
		case "CONNECT_NEW_USER_KO":
			this.connectNewUserKo();
			break;
		case "CONNECT_KO":
			this.connectKo();
			break;
		case "POST_ANC_OK":
			this.postAncOk(parsed[1]);
			break;
		case "POST_ANC_KO":
			this.postAncKo();
			break;
		case "SEND_DOMAIN_OK":
			this.sendDomainOk(Arrays.copyOfRange(parsed, 1, parsed.length - 1));
			break;
		case "SEND_DOMAIN_KO":
			this.sendDomainKo();
			break;
		case "SEND_ANC_OK":
			this.sendAncOk(Arrays.copyOfRange(parsed, 1, parsed.length - 1));
			break;
		case "SEND_ANC_KO":
			this.sendAncKo();
			break;
		case "SEND_OWN_ANC_OK":
			this.sendOwnAncOk(Arrays.copyOfRange(parsed, 1, parsed.length - 1));
			break;
		case "SEND_OWN_ANC_KO":
			this.sendOwnAncKo();
			break;
		case "MAJ_ANC_OK":
			this.majAncOk(parsed[1]);
			break;
		case "MAJ_ANC_KO":
			this.majAncKo();
			break;
		case "DELETE_ANC_OK":
			this.delAncOk(parsed[1]);
			break;
		case "DELETE_ANC_KO":
			this.delAncKo();
			break;
        case "REQUEST_IP_OK":
        	this.requestIpOk(parsed[1], parsed[2]);
            break;
        case "REQUEST_IP_KO":
        	this.requestIpKo();
        	break;
		case "UNKNOWN_REQUEST":
			this.unknownRequest();
			break;
		case "NOT_CONNECTED":
			this.notConnedted();
			break;
		default:
			this.printConsole("Unknown command: <" + parsed[0] + ">.");
			break;
		}
        this.gui.updateIsConnected();
	}

	private void closeConnexion() throws IOException {
		if (!this.inputCmd.isInterrupted()) {
			this.inputCmd.interrupt();
		}
		this.s.close();
		this.writer.close();
		this.os.close();
		this.gui.updateIsConnected();
	}

	private void openConnexion() throws UnknownHostException, IOException {
		this.s = new Socket(this.gui.getIPServeur(), port);
		this.os = this.s.getOutputStream();
		this.writer = new PrintWriter(this.os, true);
	}
        
    public boolean isConnected() {
        if (this.s == null || this.s.isClosed()) {
            return false;
        } else {
            return true;
        }
    }
    
    /* Client to Server TCP protocol */
    
    @Override
	public void connect(String name) {
    	Thread inCmd = new Thread(new Runnable() {
			@Override
			public void run() {
			String cmd = "", input = "";
			BufferedReader buffReader = null;
				if (s != null) {
					try {
						buffReader = new BufferedReader(new InputStreamReader(s.getInputStream()));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					while (!s.isClosed()) {
						cmd = "";
						try {
							do {
								input = buffReader.readLine();
								if (input != null) {
									// connection is brutaly interrupted from the server
									cmd += input + "\n";
								} else {
									closeConnexion();
									printConsole("Connexion with server shutdown.");
									token = null; // token must be reinitialize since server has lost all tokens
									break;
								}
							} while (!input.equals("."));
						} catch (IOException e) {
							if (!quit)
								 // socket is close but it should not
								e.printStackTrace();

						}
						if (!cmd.equals(""))
							try {
								processInput(cmd);
							} catch (IOException e) {
								e.printStackTrace();
							}
					}
				}
			}
		});
		this.name = name;
		try {
			openConnexion();
		} catch (UnknownHostException e) {
			this.printConsole("Server is off.");
			return;
		} catch (IOException e) {
			this.printConsole("Server is off.");
			return;
		}
		if (this.name.substring(0, 1).equals("#")) {
			this.token = name;
		}
		if (token == null) {
			this.writer.println("CONNECT");
			this.writer.println(name);
			this.writer.println(".");
		} else {
			this.writer.println("CONNECT");
			this.writer.println(token);
			this.writer.println(".");
		}
		inputCmd = new Thread(inCmd);
		//System.out.println("alive:" + inputCmd.isAlive());
		//System.out.println("interrupt: " + inputCmd.isAlive());
		inputCmd.start();
	}
    
    @Override
    public void disconnect() {
    	this.writer.println("DISCONNECT");
    	this.writer.println(".");
    	this.inputCmd.interrupt();
    	this.serverUDP.stopServer();
        this.quit = true;
    }

	@Override
	public void requestDomain() {
		this.printConsole("Demande des domaines disponible.");
		this.writer.println("REQUEST_DOMAIN");
		this.writer.println(".");
	}
	
	@Override
	public void requestAnnonce(String domain) {
		this.writer.println("REQUEST_ANC");
		this.writer.println(domain);
		this.writer.println(".");
	}
	
	@Override
    public void requestOwnAnnonce() {
    	this.writer.println("REQUEST_OWN_ANC");
    	this.writer.println(".");
	}
	
    @Override
    public void postAnc(String domain, String title, String descriptif, String price) {
    	this.writer.println("POST_ANC");
    	this.writer.println(domain);
    	this.writer.println(title);
    	this.writer.println(descriptif);
    	this.writer.println(price);
    	this.writer.println(".");
    }

    @Override
    public void majAnc(String id, String domain, String title, String descriptif, String price) {
    	this.writer.println("MAJ_ANC");
    	this.writer.println(id);
    	this.writer.println(domain);
    	this.writer.println(title);
    	this.writer.println(descriptif);
    	this.writer.println(price);
    	this.writer.println(".");
    }

    @Override
    public void delAnc(String id) {
        this.writer.println("DELETE_ANC");
        this.writer.println(id);
        this.writer.println(".");
    }

    @Override
    public void requestIP(String id) {
        this.writer.println("REQUEST_IP");
        this.writer.println(id);
        this.writer.println(".");
    }
    
    /* Server to Client protocol */
    
    @Override
	public void connectOk(String token, String utilisateur) {
    	this.gui.printConsole("You are connected with your token with user name " + this.name + ".");
    	this.token = token;
    	this.name = utilisateur;
        this.gui.basePerspective();
        try {
			this.serverUDP = new ServerUPD(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
        this.serverUDP.start();
	}

	@Override
	public void connectNewUserOk(String token) {
		this.token = token;
		this.gui.printConsole("You are connected as " + this.name + " and you're token is " + token + ".");
        this.gui.basePerspective();
        try {
			this.serverUDP = new ServerUPD(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
        this.serverUDP.start();
	}

	@Override
	public void connectNewUserKo() {
		this.printConsole("The user name chosen shouldn't start by diese or is already used.");
	}

	@Override
	public void connectKo() {
		this.printConsole("Connexion to server error.");
	}

	@Override
	public void postAncOk(String idAnnonce) {
		this.printConsole("Annonce with id <" + idAnnonce + "> succesfully created.");
	}

	@Override
	public void postAncKo() {
		this.printConsole("Annonce not created.");
	}

	@Override
	public void sendDomainOk(String[] domains) {
		this.printConsole("Domain list succesfully received.");
        this.gui.updateDomains(domains);
	}

	@Override
	public void sendDomainKo() {
		this.printConsole("No domain to print.");
	}

	@Override
	public void sendAncOk(String[] annonces) {
		this.printConsole("Annonces list succesfully received.");
		this.gui.updateAnnoncesList(annonces);
	}

	@Override
	public void sendAncKo() {
		this.printConsole("Annonce list is empty.");
		this.gui.updateAnnoncesList(null);
	}

	@Override
	public void sendOwnAncOk(String[] annonces) {
		this.printConsole("Own annonces received.");
		this.gui.updateAnnoncesList(annonces);
        this.gui.allowUpdateOwnAnnonce();
		
	}

	@Override
	public void sendOwnAncKo() {
		this.printConsole("Own annonce list is empty.");
	}

	@Override
	public void majAncOk(String idAnnonce) {
		this.printConsole("Annonce with id <" + idAnnonce + "> is updated.");
		
	}

	@Override
	public void majAncKo() {
		this.printConsole("Annonce is not updated.");
	}

	@Override
	public void delAncOk(String idAnnonce) {
		this.printConsole("Annonce with id " + idAnnonce + " is removed.");
	}

	@Override
	public void delAncKo() {
		this.printConsole("Annonce not removed.");
	}

	@Override
	public void requestIpOk(String ip, String destinataire) {
		this.printConsole("User " + destinataire + " has ip address " + ip + ".");
    	try {
			this.serverUDP.addPeer(destinataire, InetAddress.getByName(ip));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
    	this.addChat(destinataire);
	}
	
	@Override
	public void requestIpKo() {
		this.printConsole("Failed asking ip of client.");
	}

	@Override
	public void unknownRequest() {
		this.printConsole("Method send to the server is unknown.");
	}

	@Override
	public void notConnedted() {
		this.printConsole("You are not connected to the server.");
	}
    
    public void sendCustomCommand(String cmd) throws UnknownHostException, IOException {
    	this.processInput(cmd);
    }
    
    public String getName() {
    	return this.name;
    }
    
    public void addChat(String destinataire) {
    	if (!this.gui.existsConv(destinataire)) {
			this.gui.addConv(destinataire);
			this.chatMsg.put(destinataire, new ArrayList<MessageChat>());
    	}
    }
    
    public void sendMessage(String to, String msg) throws IOException {
    	//this.gui.writeTabbedPane(to, " [To] " + msg);
    	//this.chatMsg.put(to, new MessageUDP(msg, System.currentTimeMillis()));
    	long timestamp = System.currentTimeMillis();
    	this.addMsgChat(to, new MessageChat(this.name, msg, timestamp));
    	this.serverUDP.sendMessage(to, msg, timestamp, false);
    }

    public void printConsole(String msg) {
    	this.gui.printConsole(msg);
    }
    
    private String buildConv(String conv) {
    	String c = "";
    	Collections.sort(this.chatMsg.get(conv));
    	for (MessageChat mc : this.chatMsg.get(conv)) {
    		if (mc.getEmetteur().equals(this.name))
    			c += "[TO] ";
    		else
    			c += "[FROM] ";
    		c += mc.getContent() + "\n";
    	}
    	return c;
    }
    
    public void addMsgChat(String conv, MessageChat msg) {
    	this.chatMsg.get(conv).add(msg);
    	this.buildConv(conv);
    	this.gui.refreshConv(conv, this.buildConv(conv));
    }
    
    public void addMsgToBeAcknowledge(MessageUDP msg) {
    	this.msgToBeAcknowledge.add(msg);
    }
    
    public ArrayList<MessageUDP> getMsgToBeAcknowledge() {
    	return this.msgToBeAcknowledge;
    }
    
    public static void main(String[] args ) throws IOException {
		Client c = new Client(1027);
	}
    
	public void receiveMsg(String emetteur, String timestamp, String msg) {
		if (!this.gui.existsConv(emetteur))
			this.gui.addConv(emetteur);
		this.addMsgChat(emetteur, new MessageChat(emetteur, msg, Long.parseLong(timestamp)));
	}
}
