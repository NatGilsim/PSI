package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import server.Domain;

public class Client {

	private Socket s = null;
	private InputStream is = null;
	private OutputStream os = null;
	private PrintWriter pw = null;
	private boolean isConnected = false;
	private boolean quit = false;
	private int port = -1;
	private String name = null;
	private String token = null;
	private Thread outputCmd; // thread use to send cmd to server ask by the user (always running)
	private Thread inputCmd; // thread use to process cmd receive from server (run only when client is connected to server)
	//private Thread sendMessage;
	//private Thread receiveMessage;

	public Client(int port, String name) throws UnknownHostException, IOException {
		this.port = port;
		this.name = name;
		this.quit = false;
		this.begin();
	}
	
	private void begin() throws UnknownHostException, IOException {
		Scanner scn = new Scanner(System.in);
		Thread inCmd = new Thread(new Runnable() {
			@Override
			public void run() {
				String cmd = null, input = null;
				BufferedReader buffReader = new BufferedReader(new InputStreamReader(is));
				while (isConnected) {
					cmd = "";
					try {
						do {
							input = buffReader.readLine();
							cmd += input + "\n";
						} while (!input.equals("."));
					} catch (IOException e) {
						// process error in log file
						//System.out.println("[Client] Impossible de lire dans le buffReader (inCmd).");
					}
					if (!cmd.equals("")) {
						processInput(cmd);
					}
				}
			}
		});
		Thread outCmd = new Thread(new Runnable() {
			Scanner scn = new Scanner(System.in);
			@Override
			public void run() {
				while (!quit) {
					do {
						int numCmd = scn.nextInt();
						switch(numCmd) {
						case 1:
							if (!isConnected) {
								try {
									openConnexion();
									inputCmd = new Thread(inCmd);
									inputCmd.start();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if (token == null) {
								pw.println("CONNECT");
								pw.println(name);
								pw.println(".");
							} else {
								pw.println("CONNECT");
								pw.println(token);
								pw.println(".");
							}
							break;
						case 2:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								pw.println("DISCONNECT");
								pw.println(".");
								try {
									closeConnexion();
									inputCmd.interrupt();
								} catch (IOException e) {
									e.printStackTrace();
								}
								System.out.println("[Client] Vous êtes déconnecté du serveur.");
								printMenuAndInfos();
							}
							break;
						case 3:
							if (isConnected) {
								try {
									closeConnexion();
									inputCmd.interrupt();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							System.out.println("[Client] Vous êtes déconnecté du serveur.");
							System.out.println("\nUtilisateur : " + name);
							if (s == null)
								System.out.println("Socket établie : false");
							else
								System.out.println("Socket établie : " + !s.isClosed());
							System.out.println("Connecté au serveur : " + isConnected);
							quit = true;
							break;
						case 4:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								System.out.println("Domaine ?");
								String domain = scn.next();
								System.out.println("Titre ?");
								String title = scn.next();
								System.out.println("Descriptif ?");
								String descriptif = scn.next();
								System.out.println("Prix ?");
								String prix = scn.next();
								pw.println("POST_ANC");
								pw.println(domain);
								pw.println(title);
								pw.println(descriptif);
								pw.println(prix);
								pw.println(".");
							}
							break;
						case 5:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								pw.println("REQUEST_DOMAIN");
								pw.println(".");
							}
							break;
						case 6:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								System.out.println("Domaine ?");
								String domain = scn.next();
								pw.println("REQUEST_ANC");
								pw.println(domain);
								pw.println(".");
							}
							break;
						case 7:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								System.out.println("[Client] Quelle est l'id de l'annonce à modifier ?");
								String id = scn.next();
								System.out.println("Domaine ?");
								String newDomain = scn.next();
								System.out.println("Titre ?");
								String newTitle = scn.next();
								System.out.println("Descriptif ?");
								String newDescriptif = scn.next();
								System.out.println("Prix ?");
								String newPrix = scn.next();
								pw.println("MAJ_ANC");
								pw.println(id);
								if (newDomain.equals(""))
									pw.println("null");
								else
									pw.println(newDomain);
								if (newTitle.equals(""))
									pw.println("null");
								else
									pw.println(newTitle);
								if (newDescriptif.equals(""))
									pw.println("null");
								else
									pw.println(newDescriptif);
								if (newPrix.equals(""))
									pw.println("null");
								else
									pw.println(newPrix);
								pw.println(".");
							}
							break;
						case 8:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								pw.println("REQUEST_OWN_ANC");
								pw.println(".");
							}
							break;
						case 9:
							if (!isConnected) {
								System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
								printMenuAndInfos();
							} else {
								System.out.println("[Client] Quelle est l'id de l'annonce à supprimer ?");
								String id = scn.next();
								pw.println("DELETE_ANC");
								pw.println(id);
								pw.println(".");
							}
							break;
						default:
							System.out.println("[Client] Commande inconnue.");
							break;
						}
					} while (isConnected);
				}
			}
		});
		System.out.println("[Client] Nom d'utilisateur ?");
		this.name = scn.next();
		this.printMenuAndInfos();
		outputCmd = outCmd;
		outputCmd.start();
	}

	protected void processInput(String input) {
		//System.out.println("[Client] Commande reçue : " + input);
		String[] parsed = input.split("\n");
		switch(parsed[0]) {
		case "CONNECT_OK":
			System.out.println("[Client] Vous êtes connecté au serveur.");
			this.printMenuAndInfos();
			break;
		case "CONNECT_NEW_USER_OK":
			this.token = parsed[1];
			System.out.println("[Client] Vous êtes connecté au serveur : votre token est " + this.token + ".");
			this.printMenuAndInfos();
			break;
		case "CONNECT_NEW_USER_KO":
			System.out.println("[Client] Le nom d'utilisateur ne doit pas commencer par dièse.");
			this.printMenuAndInfos();
			break;
		case "CONNECT_KO":
			System.out.println("[Client] Erreur de connexion.");
			this.printMenuAndInfos();
			break;
		case "POST_ANC_OK":
			System.out.println("[Client] Annonce créée avec succès.");
			this.printMenuAndInfos();
			break;
		case "POST_ANC_KO":
			System.out.println("[Client] Annonce non créée.");
			this.printMenuAndInfos();
			break;
		case "SEND_DOMAINE_OK":
			System.out.println("[Client] Liste des domaines :");
			for (int i = 1; i < parsed.length - 1; i++)
				System.out.println(parsed[i]);
			this.printMenuAndInfos();
			break;
		case "SEND_DOMAIN_KO":
			System.out.println("[Client] Echec reception liste des domaines.");
			this.printMenuAndInfos();
			break;
		case "SEND_ANC_OK":
			System.out.println("[Client] Liste des annonces du domaine " + parsed[2] + ":");
			for (int i = 1; i < parsed.length - 1; i++)
				System.out.println(parsed[i]);
			this.printMenuAndInfos();
			break;
		case "SEND_ANC_KO":
			System.out.println("[Client] Aucune annonces à énumérées.");
			this.printMenuAndInfos();
			break;
		case "SEND_OWN_ANC_OK":
			System.out.println("[Client] Liste de vos annonces :");
			for (int i = 1; i < parsed.length - 1; i++)
				System.out.println(parsed[i]);
			this.printMenuAndInfos();
			break;
		case "SEND_OWN_ANC_KO":
			System.out.println("[Client] Aucunes de vos annonces à énumérées.");
			this.printMenuAndInfos();
			break;
		case "MAJ_ANC_KO":
			System.out.println("[Client] Annonce non mise à jour.");
			this.printMenuAndInfos();
			break;
		case "MAJ_ANC_OK":
			System.out.println("[Client] Annonce mise à jour avec succès.");
			this.printMenuAndInfos();
			break;
		case "DELETE_ANC_OK":
			System.out.println("[Client] L'annonce avec l'id " + Integer.parseInt(parsed[1]) + " a été supprimé.");
			this.printMenuAndInfos();
			break;
		case "DELETE_ANC_KO":
			System.out.println("[Client] L'annonce avec l'id " + Integer.parseInt(parsed[1] + " n'a pas été supprimée."));
			this.printMenuAndInfos();
			break;
		default:
			System.out.println("[Client] Requête inconnue.");
			this.printMenuAndInfos();
			break;
		}
	}
	
	private void printMenuAndInfos() {
		System.out.println("\nUtilisateur : " + this.name);
		if (this.s == null)
			System.out.println("Socket établie : false");
		else
			System.out.println("Socket établie : " + !this.s.isClosed());
		System.out.println("Connecté au serveur : " + this.isConnected);
		System.out.println("1 : Connexion au serveur.");
		System.out.println("2 : Deconnexion du serveur.");
		System.out.println("3 : Quitter l'application.");
		System.out.println("4 : Poster une annonce.");
		System.out.println("5 : Liste des domaines disponibles.");
		System.out.println("6 : Recevoir les annonces d'un domaine.");
		System.out.println("7 : MAJ annonce.");
		System.out.println("8 : Mes annonces.");
		System.out.println("9 : Supprimer annonce.");
		System.out.println("Que voulez-vous faire ?");
	}
	
	private void closeConnexion() throws IOException {
		this.pw.close();
		this.is.close();
		this.os.close();
		this.s.close();
		this.isConnected = false;
	}
	
	private void openConnexion() throws UnknownHostException, IOException {
		this.s = new Socket("127.0.0.1", port);
		this.is = this.s.getInputStream();
		this.os = this.s.getOutputStream();
		this.pw = new PrintWriter(this.os, true);
		this.isConnected = true;
	}

	public static void main(String[] args ) throws IOException {
		Client c = new Client(1027, "toto");
	}
	
}
