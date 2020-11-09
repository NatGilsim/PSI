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
	private boolean quit = false;
	private int port = -2;
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
									System.out.println("[Client] Connexion interrompue avec le serveur.");
									token = null; // token must be reinitialize since server has lost all tokens
									printMenuAndInfos();
									break;
								}
							} while (!input.equals("."));
						} catch (IOException e) {
							// socket is close but it should not
							if (!quit)
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
					do {
						int numCmd = scn.nextInt();
						//s.setSoTimeout(3600 * 1000); // reset timeout
							switch(numCmd) {
							case 1:
								pw.println("DISCONNECT");
								pw.println(".");
								quit = true;
								break;
							case 2:
								System.out.println("Domaine ?");
								domain = scn.next();
								System.out.println("Titre ?");
								title = scn.next();
								System.out.println("Descriptif ?");
								descriptif = scn.next();
								System.out.println("Prix ?");
								prix = scn.next();
								// regex to make sure that prix is indeed a price
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
							case 3:
								pw.println("REQUEST_DOMAIN");
								pw.println(".");
								break;
							case 4:
								System.out.println("Domaine ?");
								domain = scn.next();
								pw.println("REQUEST_ANC");
								pw.println(domain.toLowerCase());
								pw.println(".");
								break;
							case 5:
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
									pw.println(newDomain.toLowerCase());
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
							case 6:
								pw.println("REQUEST_OWN_ANC");
								pw.println(".");
								break;
							case 7:
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
					} while (!quit);
				try {
					closeConnexion();
					System.out.println("[Client] Vous êtes déconnecté du serveur.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		try {
			openConnexion();
		} catch (UnknownHostException e) {
			System.out.println("[Client] Le serveur n'est pas opérationnel.");
			return;
		} catch (IOException e) {
			System.out.println("[Client] Le serveur n'est pas opérationnel.");
			return;
		}
		System.out.println("[Client] Nom d'utilisateur ?");
		this.name = scn.next();
		if (this.name.substring(0, 1).equals("#")) {
			this.token = name;
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
		inputCmd = new Thread(inCmd);
		inputCmd.start();
		outputCmd = outCmd;
		outputCmd.start();
		//this.printMenuAndInfos();
	}

	protected void processInput(String input) {
		//System.out.println("Requête reçue : <" + input + ">");
		String[] parsed = input.split("\n");
		switch(parsed[0]) {
		case "CONNECT_OK":
			System.out.println("[Client] Vous êtes connecté au serveur avec votre précédent token.");
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
			System.out.println("[Client] Liste des annonces du domaine demandé :");
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
			System.out.println("[Client] Aucune annonce à énumérer.");
			break;
		case "MAJ_ANC_KO":
			System.out.println("[Client] Annonce non mise à jour.");
			break;
		case "MAJ_ANC_OK":
			System.out.println("[Client] Annonce avec l'id " + parsed[1] + " mise à jour avec succès.");
			break;
		case "DELETE_ANC_OK":
			System.out.println("[Client] L'annonce avec l'id " + Integer.parseInt(parsed[1]) + " a été supprimé.");
			break;
		case "DELETE_ANC_KO":
			System.out.println("[Client] L'annonce n'a pas été supprimée.");
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
		System.out.println("1 : Quitter l'application.");
		System.out.println("2 : Poster une annonce.");
		System.out.println("3 : Liste des domaines disponibles.");
		System.out.println("4 : Recevoir les annonces d'un domaine.");
		System.out.println("5 : MAJ annonce.");
		System.out.println("6 : Mes annonces.");
		System.out.println("7 : Supprimer annonce.");
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
	}

	private void openConnexion() throws UnknownHostException, IOException {
		this.s = new Socket("127.0.0.1", port);
		this.is = this.s.getInputStream();
		this.os = this.s.getOutputStream();
		this.pw = new PrintWriter(this.os, true);
	}

	public static void main(String[] args ) throws IOException {
		Client c = new Client(1027, "toto");
	}

}
