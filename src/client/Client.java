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

	private void begin() {
		Scanner scn = new Scanner(System.in);
		Thread inCmd = new Thread(new Runnable() {
			@Override
			public void run() {
			String cmd = "", input = "";
				if (s != null) {
					BufferedReader buffReader = new BufferedReader(new InputStreamReader(is));
					while (isConnected) {
						cmd = "";
						try {
							do {
								input = buffReader.readLine();
								if (input != null) {
									cmd += input + "\n";
								} else {
									closeConnexion();
									System.out.println("[Client] Serveur shutdown.");
									token = null;
									printMenuAndInfos();
									break;
								}
							} while (input != null && !input.equals(".") && !s.isClosed());
						} catch (IOException e) {
							if (isConnected)
								e.printStackTrace();
						}
						if (!cmd.equals(""))
							processInput(cmd);
					}
				}
			}
		});
		Thread outCmd = new Thread(new Runnable() {
			Scanner scn = new Scanner(System.in);
			@Override
			public void run() {
				scn.useDelimiter("\n");
				String domain, title, descriptif, prix, id;
				String newDomain, newTitle, newDescriptif, newPrix;
				while (!quit) {
					do {
						int numCmd = scn.nextInt();
						if (numCmd == 3 && (s == null || s.isClosed())) {
							quit = true;
						} else if (numCmd != 1 && (s == null || s.isClosed())) {
							System.out.println("[Client] Socket non établie.");
							printMenuAndInfos();
						} else {
							switch(numCmd) {
							case 1:
								if (!isConnected) {
									try {
										openConnexion();
									} catch (UnknownHostException e) {
										System.out.println("[Client] Le serveur n'est pas opérationnel.");
										printMenuAndInfos();
									} catch (IOException e) {
										System.out.println("[Client] Le serveur n'est pas opérationnel.");
										printMenuAndInfos();
									}
									inputCmd = new Thread(inCmd);
									inputCmd.start();
									if (!isConnected) {
										break;
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
								} else {
									System.out.println("[Client] Vous êtes déjà connecté au serveur.");
								}
								break;
							case 2:
								pw.println("DISCONNECT");
								pw.println(".");
								try {
									closeConnexion();
								} catch (IOException e) {
									e.printStackTrace();
								}
								System.out.println("[Client] Vous êtes déconnecté du serveur.");
								printMenuAndInfos();
								break;
							case 3:
								if (isConnected) {
									try {
										closeConnexion();
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
								quit = true;
								break;
							case 4:
								System.out.println("Domaine ?");
								domain = scn.next();
								System.out.println("Titre ?");
								title = scn.next();
								System.out.println("Descriptif ?");
								descriptif = scn.next();
								System.out.println("Prix ?");
								prix = scn.next();
								while (!prix.matches("^[0-9]+\\.?[0-9]{0,2}")) {
									System.out.println("Saisissez un prix valide.");
									System.out.println("Prix ?");
									prix = scn.next();
								}
								pw.println("POST_ANC");
								pw.println(domain.toLowerCase());
								pw.println(title);
								pw.println(descriptif);
								pw.println(prix);
								pw.println(".");
								break;
							case 5:
								pw.println("REQUEST_DOMAIN");
								pw.println(".");
								break;
							case 6:
								System.out.println("Domaine ?");
								domain = scn.next();
								pw.println("REQUEST_ANC");
								pw.println(domain);
								pw.println(".");
								break;
							case 7:
								System.out.println("[Client] Quelle est l'id de l'annonce à modifier ?");
								id = scn.next();
								System.out.println("Domaine ?");
								newDomain = scn.next();
								System.out.println("Titre ?");
								newTitle = scn.next();
								System.out.println("Descriptif ?");
								newDescriptif = scn.next();
								System.out.println("Prix ?");
								newPrix = scn.next();
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
								break;
							case 8:
								pw.println("REQUEST_OWN_ANC");
								pw.println(".");
								break;
							case 9:
								System.out.println("[Client] Quelle est l'id de l'annonce à supprimer ?");
								id = scn.next();
								pw.println("DELETE_ANC");
								pw.println(id);
								pw.println(".");
								break;
							default:
								System.out.println("[Client] Commande inconnue.");
								break;
							}
						}
					} while (isConnected);
				}
			}
		});
		System.out.println("[Client] Nom d'utilisateur ?");
		this.name = scn.next();
		outputCmd = outCmd;
		outputCmd.start();
		this.printMenuAndInfos();
	}

	protected void processInput(String input) {
		System.out.println("Requête reçue : <" + input + ">");
		String[] parsed = input.split("\n");
		switch(parsed[0]) {
		case "CONNECT_OK":
			System.out.println("[Client] Vous êtes connecté au serveur.");
			break;
		case "CONNECT_NEW_USER_OK":
			this.token = parsed[1];
			System.out.println("[Client] Vous êtes connecté au serveur : votre token est " + this.token + ".");
			break;
		case "CONNECT_NEW_USER_KO":
			System.out.println("[Client] Le nom d'utilisateur ne doit pas commencer par dièse ou est déjà utilisé.");
			break;
		case "CONNECT_KO":
			System.out.println("[Client] Erreur de connexion.");
			break;
		case "POST_ANC_OK":
			System.out.println("[Client] Annonce créée avec succès.");
			break;
		case "POST_ANC_KO":
			System.out.println("[Client] Annonce non créée.");
			break;
		case "SEND_DOMAIN_OK":
			System.out.println("[Client] Liste des domaines :");
			for (int i = 1; i < parsed.length - 1; i++)
				System.out.println(parsed[i]);
			break;
		case "SEND_DOMAIN_KO":
			System.out.println("[Client] Aucun domaines à affiché.");
			break;
		case "SEND_ANC_OK":
			System.out.println("[Client] Liste des annonces du domaine " + parsed[2] + ":");
			for (int i = 1; i < parsed.length - 1; i++)
				System.out.println(parsed[i]);
			break;
		case "SEND_ANC_KO":
			System.out.println("[Client] Aucune annonces à affiché.");
			break;
		case "SEND_OWN_ANC_OK":
			System.out.println("[Client] Liste de vos annonces :");
			for (int i = 1; i < parsed.length - 1; i++)
				System.out.println(parsed[i]);
			break;
		case "SEND_OWN_ANC_KO":
			System.out.println("[Client] Aucunes de vos annonces à énumérées.");
			break;
		case "MAJ_ANC_KO":
			System.out.println("[Client] Annonce non mise à jour.");
			break;
		case "MAJ_ANC_OK":
			System.out.println("[Client] Annonce mise à jour avec succès.");
			break;
		case "DELETE_ANC_OK":
			System.out.println("[Client] L'annonce avec l'id " + Integer.parseInt(parsed[1]) + " a été supprimé.");
			break;
		case "DELETE_ANC_KO":
			System.out.println("[Client] L'annonce avec l'id " + Integer.parseInt(parsed[1] + " n'a pas été supprimée."));
			break;
		case "UNKNOWN_REQUEST":
			System.out.println("[Client] La requête envoyée n'est pas reconnue par le serveur.");
			break;
		case "NOT_CONNECTED":
			System.out.println("[Client] Vous n'êtes pas connecté au serveur.");
			break;
		default:
			System.out.println("[Client] Requête inconnue : " + parsed[0]);
			break;
		}
		this.printMenuAndInfos();
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
		if (!this.inputCmd.isInterrupted()) {
			this.inputCmd.interrupt();
		}
		this.s.close();
		this.pw.close();
		this.is.close();
		this.os.close();
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
