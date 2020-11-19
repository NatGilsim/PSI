package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import protocol.ClientToServerTcpProtocol;
import protocol.ServerTcpToClientProtocol;

public class ClientHandler implements Runnable, ServerTcpToClientProtocol, ClientToServerTcpProtocol {

	private Socket s = null;
	private InputStream is = null;
	private OutputStream os = null;
	private PrintWriter pw = null;
	private ClientServer client = null;

	public ClientHandler(Socket s) throws IOException {
		this.s = s;
		this.is = s.getInputStream();
		this.os = s.getOutputStream();
		this.pw = new PrintWriter(this.os, true);
		this.s.setSoTimeout(3600 * 1000); // 1 hour
		this.client = new ClientServer();
	}

	@Override
	public void run() {
		String cmd = "", input = "";
		BufferedReader buffReader = new BufferedReader(new InputStreamReader(is));
		while (!s.isClosed()) {
			cmd = "";
			try {
				do {
					input = buffReader.readLine();
					if (input != null) {
						cmd += input + "\n";
					} else {
						// connection is brutaly interrupted from the client
						this.closeConnexion();
						System.out.println("[Serveur] Client <" + this.client.getName() + "> s'est déconnecté (" + this.client.getToken() + ") brutalement.");
						ServerTCP.delHandler(this);
						break;
					}
				} while (!input.equals("."));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (!cmd.equals(""))
					processInput(cmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			is.close();
			os.close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		pw.close();
	}

	protected void processInput(String input) throws IOException {
		//System.out.println("Requête reçue : <" + input + ">");
		//this.s.setSoTimeout(3600 * 1000); // reset timeout
		String[] parsed = input.split("\n");
		if (!this.client.getIsConnected() && !parsed[0].equals("CONNECT")) {
			this.pw.println("NOT_CONNECTED");
			this.pw.println(".");
		} else {
			switch(parsed[0]) {
			case "CONNECT":
				this.connect(parsed[1]);
				break;
			case "DISCONNECT":
				this.disconnect();
				break;
			case "POST_ANC":
				this.postAnc(parsed[1], parsed[2], parsed[3], parsed[4]);
				break;
			case "REQUEST_DOMAIN":
				this.requestDomain();
				break;
			case "REQUEST_ANC":
				this.requestAnnonce(parsed[1]);
				break;
			case "REQUEST_OWN_ANC":
				this.requestOwnAnnonce();
				break;
			case "MAJ_ANC":
				this.majAnc(parsed[1], parsed[2], parsed[3], parsed[4], parsed[5]);
				break;
			case "DELETE_ANC":
				this.delAnc(parsed[1]);
				break;
			case "REQUEST_IP":
				this.requestIP(parsed[1]);
				break;
			default:
				this.unknownRequest();
				break;
			}
		}
	}

	private void closeConnexion() throws IOException {
		this.pw.close();
		this.is.close();
		this.os.close();
		this.s.close();
		this.client.setIsConnected(false);
	}

	private boolean addAnnonce(Domain dom, String titre, String description, double prix, int id) {
		return this.client.addAnnonce(dom, titre, description, prix, id);
	}

	/* Client to Server TCP protocol */
	
	@Override
	public void connect(String arg) {
		if (!this.client.getIsConnected()) {
			if (arg.length() >= 50) {
				this.connectKo();
			}
			if (ServerTCP.tokenExists(arg)) {
				this.client = ServerTCP.getClientFromToken(arg);
				System.out.println("[Serveur] Utilisateur " + this.client.getName() + " connecté grâce à son token (" + this.client.getToken() + ").");
				this.client.setIsConnected(true);
				this.connectOk(this.client.getToken(), this.client.getName());				
			} else {
				if (arg.substring(0, 1).equals("#") || ServerTCP.clientExists(arg)) {
					System.out.println("[Serveur] Création d'utilisateur refusé : le nom ne doit pas commencer par un dièse ou est déjà utilisé.");
					this.connectNewUserKo();
				} else {
					String ip = (((InetSocketAddress) this.s.getRemoteSocketAddress()).getAddress()).toString().replace("/","");
					ClientServer newClientServer = new ClientServer(arg, ServerTCP.createToken(), ip);
					this.client = newClientServer;
					ServerTCP.clients.add(newClientServer);
					System.out.println("[Serveur] Nouveau token créé (" + this.client.getToken() + ") pour l'utilisateur " + this.client.getName() + " qui est connecté.");
					this.client.setIsConnected(true);
					this.connectNewUserOk(this.client.getToken());
				}
			}
		}
	}

	@Override
	public void disconnect() {
		try {
			this.closeConnexion();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("[Serveur] Client " + this.client.getName() + " s'est déconnecté (" + this.client.getToken() + ").");
		ServerTCP.delHandler(this);
	}

	@Override
	public void requestDomain() {
		if (Domain.values().length >= 1) {
			this.sendDomainOk(Domain.getTabDomains());
		} else {
			this.sendDomainKo();
		}
	}

	@Override
	public void requestAnnonce(String domain) {
		if (!ServerTCP.domainExists(domain.toLowerCase()))
			this.sendAncKo();
		else if (ServerTCP.nbrAnnonces(domain.toLowerCase()) == 0)
			this.sendAncKo();
		else
			this.sendAncOk(ServerTCP.getAnnoncesOfDomain(domain));
	}

	@Override
	public void requestOwnAnnonce() {
		if (ServerTCP.getClientFromToken(this.client.getToken()).getAnnonces().size() == 0)
			this.sendOwnAncKo();
		else
			this.sendOwnAncOk(ServerTCP.getOwnAnnonce(this.client.getToken()));
	}

	@Override
	public void postAnc(String domain, String title, String descriptif, String price) {
		double prixAnnonce = Double.valueOf(price);
		if (!ServerTCP.domainExists(domain.toLowerCase())) {
			this.postAncKo();
		} else {
			int idAnnonce = ServerTCP.createIdAnnonce();
			if (this.addAnnonce(Domain.valueOf(domain), title, descriptif, prixAnnonce, idAnnonce)) {
				this.postAncOk(String.valueOf(idAnnonce));
			} else {
				this.postAncKo();
			}
		}
	}

	@Override
	public void majAnc(String id, String domain, String title, String descriptif, String price) {
		if (this.client.isOwnAnnonce(Integer.parseInt(id))) {
			int id_converted = Integer.parseInt(id);
			if (!domain.equals("null"))
				this.client.getAnnonceById(id_converted).setDomain(Domain.valueOf(domain));
			if (!title.equals("null"))
				this.client.getAnnonceById(id_converted).setTitre(title);
			if (!descriptif.equals("null"))
				this.client.getAnnonceById(id_converted).setDescriptif(descriptif);
			if (!price.equals("null"))
				this.client.getAnnonceById(id_converted).setPrix(Double.parseDouble(price));
			this.majAncOk(id);
		}
		else
			this.majAncKo();
	}

	@Override
	public void delAnc(String id) {
		if (this.client.isOwnAnnonce(Integer.parseInt(id))) {
			ServerTCP.delAnnonce(Integer.parseInt(id));
			this.delAncOk(id);
		} else
			this.delAncKo();
	}

	@Override
	public void requestIP(String id) {
		ClientServer c = ServerTCP.getClientFromIdAnnonce(id);
		if (c.getIsConnected())
			this.requestIpOk(c.getIP(), c.getName());
		else {
			this.requestIpKo();
		}
	}
	
	//* Server TCP to Client Protocol */

	@Override
	public void connectOk(String token, String utilisateur) {
		this.pw.println("CONNECT_OK");
		this.pw.println(token);
		this.pw.println(utilisateur);
		this.pw.println(".");
	}

	@Override
	public void connectNewUserOk(String token) {
		this.pw.println("CONNECT_NEW_USER_OK");
		this.pw.println(token);
		this.pw.println(".");
	}

	@Override
	public void connectNewUserKo() {
		this.pw.println("CONNECT_NEW_USER_KO");
		this.pw.println(".");	
	}

	@Override
	public void connectKo() {
		this.pw.println("CONNECT_KO");
		this.pw.println(".");
	}

	@Override
	public void postAncOk(String idAnnonce) {
		System.out.println("[Serveur] Client " + this.client.getName() + " a créé l'annonce avec l'id " + idAnnonce + ".");
		this.pw.println("POST_ANC_OK");
		this.pw.println(idAnnonce);
		this.pw.println(".");
	}

	@Override
	public void postAncKo() {
		System.out.println("[Serveur] Echec de création d'annonce par le client " + this.client.getName() + ".");
		this.pw.println("POST_ANC_KO");
		this.pw.println(".");
	}

	@Override
	public void sendDomainOk(String[] domain) {
		System.out.println("[Serveur] Envoie des domaines au client " + this.client.getName() + ".");
		this.pw.println("SEND_DOMAIN_OK");
		for (String d : domain)
			this.pw.println(d);
		this.pw.println(".");
	}

	@Override
	public void sendDomainKo() {
		System.out.println("[Serveur] Echec de l'envoie des domaines au client " + this.client.getName() + ".");
		this.pw.println("SEND_DOMAIN_KO");
		this.pw.println(".");
	}

	@Override
	public void sendAncOk(String[] annonces) {
		System.out.println("[Serveur] Envoie des annonces au client <" + this.client.getName() + ">.");
		this.pw.println("SEND_ANC_OK");
		for (String s : annonces)
			this.pw.println(s);
		this.pw.println(".");
		
	}

	@Override
	public void sendAncKo() {
		System.out.println("[Serveur] Echec de l'envoie des annonces d'un domaine au client <" + this.client.getName() + ">.");
		this.pw.println("SEND_ANC_KO");
		this.pw.println(".");
	}

	@Override
	public void sendOwnAncOk(String[] annonces) {
		System.out.println("[Serveur] Envoie des annonces de  " + this.client.getName() + " à lui-même.");
		this.pw.println("SEND_OWN_ANC_OK");
		for (String s : annonces)
			this.pw.println(s);
		this.pw.println(".");
	}

	@Override
	public void sendOwnAncKo() {
		System.out.println("[Serveur] Le client " + this.client.getName() + " n'a créé aucune annonces.");
		this.pw.println("SEND_OWN_ANC_KO");
		this.pw.println(".");
	}

	@Override
	public void majAncOk(String idAnnonce) {
		System.out.println("[Serveur] Le client <" + this.client.getName() + "> a mis à jour son annonce avec l'id <" + idAnnonce + ">.");
		this.pw.println("MAJ_ANC_OK");
		this.pw.println(idAnnonce);
		this.pw.println(".");
	}

	@Override
	public void majAncKo() {
		System.out.println("[Serveur] Le client <" + this.client.getName() + "> veut mettre à jour une annonce qui ne lui appartient pas.");
		this.pw.println("MAJ_ANC_KO");
		this.pw.println(".");
	}

	@Override
	public void delAncOk(String idAnnonce) {
		System.out.println("[Serveur] Suppression de l'annonce avec l'id <" + idAnnonce + "> effectuée par le client <" + this.client.getName() + ">.");
		this.pw.println("DELETE_ANC_OK");
		this.pw.println(idAnnonce);
		this.pw.println(".");
	}

	@Override
	public void delAncKo() {
		System.out.println("[Serveur] Le client <" + this.client.getName() + "> veut supprimer une annonce qui ne lui appartient pas.");
		this.pw.println("DELETE_ANC_KO");
		this.pw.println(".");
	}

	@Override
	public void requestIpOk(String ip, String destinataire) {
		this.pw.println("REQUEST_IP_OK");
		this.pw.println(ip);
		this.pw.println(destinataire);
		this.pw.println(".");
	}

	@Override
	public void unknownRequest() {
		System.out.println("[Serveur] Requête inconnue.");
		this.pw.println("UNKNOWN_REQUEST");
		this.pw.println(".");
	}

	@Override
	public void notConnedted() {
		this.pw.println("NOT_CONNECTED");
		this.pw.println(".");
	}

	@Override
	public void requestIpKo() {
		this.pw.println("REQUEST_IP_KO");
		this.pw.println(".");
	}

}
