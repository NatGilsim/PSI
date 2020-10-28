package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

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
						this.closeConnexion();
						System.out.println("[Serveur] Client " + this.client.getName() + " s'est déconnecté (" + this.client.getToken() + ").");
						Server.delHandler(this);
					}
				} while (input != null && !input.equals("."));
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (input != null)
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
		String[] parsed = input.split("\n");
		switch(parsed[0]) {
		case "CONNECT":
			if (!this.client.getIsConnected()) {
				if (parsed[1].length() >= 50) {
					this.pw.println("CONNECT_KO");
					this.pw.println(".");
				}
				if (Server.tokenExists(parsed[1])) {
					this.client = Server.getClientFromToken(parsed[1]);
					System.out.println("[Serveur] Utilisateur " + this.client.getName() + " connecté grâce à son token (" + this.client.getToken() + ").");
					this.client.setIsConnected(true);
					this.pw.println("CONNECT_OK");
					this.pw.println(".");
				} else {
					if (parsed[1].substring(0, 1).equals("#") || Server.userExists(parsed[1])) {
						System.out.println("[Serveur] Création d'utilisateur refusé : le nom ne doit pas commencer par un dièse ou est déjà utilisé.");
						this.pw.println("CONNECT_NEW_USER_KO");
						this.pw.println(".");
					} else {
						ClientServer newClientServer = new ClientServer(parsed[1], Server.createToken());
						this.client = newClientServer;
						Server.clients.add(newClientServer);
						System.out.println("[Serveur] Nouveau token créé (" + this.client.getToken() + ") pour l'utilisateur " + this.client.getName() + " qui est connecté.");
						this.client.setIsConnected(true);
						this.pw.println("CONNECT_NEW_USER_OK");
						this.pw.println(this.client.getToken());
						this.pw.println(".");
					}
				}
			}
			break;
		case "DISCONNECT":
			this.closeConnexion();
			System.out.println("[Serveur] Client " + this.client.getName() + " s'est déconnecté (" + this.client.getToken() + ").");
			Server.delHandler(this);
			break;
		case "POST_ANC":
			int prixAnnonce = Integer.valueOf(parsed[4]);
			int idAnnonce = Server.createIdAnnonce();
			System.out.println(Domain.valueOf(parsed[1]));
			if (this.addAnnonce(Domain.valueOf(parsed[1]), parsed[2], parsed[3], prixAnnonce, idAnnonce)) {
				System.out.println("[Serveur] Client " + this.client.getName() + " a créé l'annonce avec l'id " + idAnnonce + ".");
				this.pw.println("POST_ANC_OK");
				this.pw.println(idAnnonce);
				this.pw.println(".");
			} else {
				System.out.println("[Serveur] Echec de création d'annonce par le client " + this.client.getName() + ".");
				this.pw.println("POST_ANC_KO");
				this.pw.println(".");
			}
			break;
		case "REQUEST_DOMAIN":
			if (Domain.values().length >= 1) {
				System.out.println("[Serveur] Envoie des domaines au client " + this.client.getName() + ".");
				this.pw.println("SEND_DOMAINE_OK");
				for (Domain d : Domain.values())
					this.pw.println(d);
				this.pw.println(".");
			} else {
				System.out.println("[Serveur] Echec de l'envoie des domaines au client " + this.client.getName() + ".");
				this.pw.println("SEND_DOMAIN_KO");
				this.pw.println(".");
			}
			break;
		case "REQUEST_ANC":
			if (!Server.domainExists(parsed[1])) {
				System.out.println("[Serveur] Echec de l'envoie des annonces d'un domaine au client " + this.client.getName() + " : domaine inexistant.");
				this.pw.println("SEND_ANC_KO");
				this.pw.println(".");
			} else if (Server.nbrAnnonces(parsed[1]) == 0) {
				System.out.println("[Serveur] Echec de l'envoie des annonces du domaine " + parsed[1] + " au client " + this.client.getName() + " : aucune annonces créées dans ce domaine.");
				this.pw.println("SEND_ANC_KO");
				this.pw.println(".");
			} else {
				System.out.println("[Serveur] Envoie des annonces du domaine " + parsed[1] + " au client " + this.client.getName());
				this.pw.println("SEND_ANC_OK");
				for (ClientServer cs : Server.clients) {
					for (Annonce a : cs.getAnnonces()) {
						if (a.getDomain().name().equals(parsed[1])) {
							this.pw.println(Integer.toString(a.getId()));
							this.pw.println(a.getDomain().name());
							this.pw.println(a.getTitre());
							this.pw.println(a.getDescriptif());
							this.pw.println(Integer.toString(a.getPrix()));
						}
					}
				}
				this.pw.println(".");
			}
			break;
		case "REQUEST_OWN_ANC":
			if (Server.getClientFromToken(this.client.getToken()).getAnnonces().size() == 0) {
				System.out.println("[Serveur] Le client " + this.client.getName() + " a demandé ses annonces mais il en a aucune.");
				this.pw.println("SEND_OWN_ANC_KO");
				this.pw.println(".");
			} else {
				System.out.println("[Serveur] Envoie des annonces de  " + this.client.getName() + " à lui-même.");
				this.pw.println("SEND_OWN_ANC_OK");
				for (Annonce a : Server.getClientFromToken(this.client.getToken()).getAnnonces()) {
					System.out.println(Integer.toString(a.getId()));
					this.pw.println(Integer.toString(a.getId()));
					System.out.println(a.getDomain());
					this.pw.println(a.getDomain().name());
					this.pw.println(a.getTitre());
					this.pw.println(a.getDescriptif());
					this.pw.println(Integer.toString(a.getPrix()));
				}
				this.pw.println(".");
			}
			break;
		case "MAJ_ANC":
			if (this.client.isOwnAnnonce(Integer.parseInt(parsed[1]))) {
				if (!parsed[2].equals("null"))
					this.client.getAnnonceById(Integer.parseInt(parsed[1])).setDomain(Domain.valueOf(parsed[2]));
				if (!parsed[3].equals("null"))
					this.client.getAnnonceById(Integer.parseInt(parsed[1])).setTitre(parsed[3]);
				if (!parsed[4].equals("null"))
					this.client.getAnnonceById(Integer.parseInt(parsed[1])).setDescriptif(parsed[4]);
				if (!parsed[5].equals("null"))
					this.client.getAnnonceById(Integer.parseInt(parsed[1])).setPrix(Integer.parseInt(parsed[5]));
				System.out.println("[Serveur] Le client " + this.client.getName() + " a mis à jour l'annonce avec l'id " + Integer.parseInt(parsed[1]) + ".");
				this.pw.println("MAJ_ANC_OK");
				this.pw.println(".");
			} else {
				System.out.println("[Serveur] Le client " + this.client.getName() + " veut mettre à jour une annonce qui ne lui appartient pas");
				this.pw.println("MAJ_ANC_KO");
				this.pw.println(".");
			}
			break;
		case "DELETE_ANC":
			if (this.client.isOwnAnnonce(Integer.parseInt(parsed[1]))) {
				Server.delAnnonce(Integer.parseInt(parsed[1]));
				System.out.println("[Client] Suppression de l'annonce avec l'id " + Integer.parseInt(parsed[1]) + " effectuée par le client " + this.client.getName() + ".");
				this.pw.println("DELETE_ANC_OK");
				this.pw.println(Integer.parseInt(parsed[1]));
				this.pw.println(".");
			} else {
				System.out.println("[Serveur] Le client " + this.client.getName() + " veut supprimer une annonce qui ne lui appartient pas.");
				this.pw.println("DELETE_ANC_KO");
				this.pw.print(".");
			}
			break;
		default:
			System.out.println("[Serveur] " + parsed[0] + " est une requête inconnue.");
			break;
		}
	}
	
	private void closeConnexion() throws IOException {
		this.pw.close();
		this.is.close();
		this.os.close();
		this.s.close();
		this.client.setIsConnected(false);
	}
	
	private boolean addAnnonce(Domain dom, String titre, String description, int prix, int id) {
		return this.client.addAnnonce(dom, titre, description, prix, id);
	}

}
