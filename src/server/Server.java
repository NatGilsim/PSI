package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {
	
	private ServerSocket ss = null;
	private boolean isRunning = true;
	static public ArrayList<ClientServer> clients = new ArrayList<ClientServer>();
	static public ArrayList<ClientHandler> handlers = new ArrayList<ClientHandler>();
	static public int idClient = 0;
	static public int idAnnonce = 0;

	public Server(int port) throws IOException {
		this.ss = new ServerSocket(1027);
		this.begin();
	}
	
	private void begin() throws IOException {
		System.out.println("[Serveur] Serveur démarré.");
		while (this.isRunning) {
			Socket s = ss.accept();
			System.out.println("[Serveur] Nouvelle requête client : " + s);
			ClientHandler ch = new ClientHandler(s);
			Thread t = new Thread(ch);
			Server.addHandler(ch);
			t.start();
		}
	}
	
	public static synchronized boolean addHandler(ClientHandler ch) {
		System.out.println("[Serveur] Nouvelle connexion géré par le serveur.");
		return Server.handlers.add(ch);
	}

	public static synchronized boolean delHandler(ClientHandler ch) {
		System.out.println("[Serveur] Une connexion a été arrêtée.");
		return Server.handlers.remove(ch);
	}

	public static synchronized boolean tokenExists(String newToken) {
		for (ClientServer cs : Server.clients)
			if (newToken.equals(cs.getToken()))
				return true;
		return false;
	}
	
	public static synchronized boolean userExists(String newUser) {
		for (ClientServer cs : Server.clients)
			if (cs.getName().equals(newUser))
				return true;
		return false;
	}
	
	public static synchronized ClientServer getClientFromToken(String token) {
		for (ClientServer cs : Server.clients)
			if (cs.getToken().equals(token))
				return cs;
		return null;
	}
	
	public static synchronized void delAnnonce(int idAnnonce) {
		for (ClientServer cs : Server.clients) {
			for (Iterator<Annonce> iterator = cs.getAnnonces().iterator(); iterator.hasNext();) {
				Annonce a = iterator.next();
				if (a.getId() == idAnnonce)
					iterator.remove();;
			}
		}
	}

	public static synchronized int nbrAnnonces(String domain) {
		int cnt = 0;
		for (ClientServer cs : Server.clients) {
			for (Annonce a : cs.getAnnonces()) {
				if (a.getDomain().name().equals(domain)) {
					cnt++;
				}
			}
		}
		return cnt;
	}

	public static synchronized boolean domainExists(String domain) {
		for (Domain d : Domain.values())
			if (d.name().equals(domain))
				return true;
		return false;
	}

	public static synchronized String createToken() {
		return "#" + (++Server.idClient);
	}
	
	public static synchronized int createIdAnnonce() {
		return ++Server.idAnnonce;
	}
	
	public static void main(String[] args) throws IOException {
		Server s = new Server(1027);
	}
}
