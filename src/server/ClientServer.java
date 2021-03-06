package server;

import java.util.ArrayList;

public class ClientServer {

	private String name = null;
	private String token = null;
	private String ip;
	private ArrayList<Annonce> annonces = new ArrayList<Annonce>();
	private boolean isConnected = false;

	public ClientServer(String name, String token, String ip) {
		this.name = name;
		this.token = token;
		this.ip = ip;
	}

	public ClientServer() {}

	public String getName() {
		return this.name;
	}

	public String getToken() {
		return this.token;
	}

	public ArrayList<Annonce> getAnnonces() {
		return this.annonces;
	}

	public boolean isOwnAnnonce(int idAnnonce) {
		for (Annonce a : this.annonces)
			if (a.getId() == idAnnonce)
				return true;
		return false;
	}

	public Annonce getAnnonceById(int idAnnonce) {
		for (Annonce a : this.annonces)
			if (a.getId() == idAnnonce)
				return a;
		return null;
	}

	public boolean addAnnonce(Domain dom, String titre, String description, double prix, int id) {
		return this.annonces.add(new Annonce(dom, titre, description, prix, id));
	}

	public void setIsConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	public boolean getIsConnected() {
		return this.isConnected;
	}
	
	public String getIP() {
		return this.ip;
	}

}
