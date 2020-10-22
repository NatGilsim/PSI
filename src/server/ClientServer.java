package server;

import java.util.ArrayList;

public class ClientServer {
	
	private String name;
	private String token;
	private ArrayList<Annonce> annonces = new ArrayList<Annonce>();
	
	public ClientServer(String name, String token) {
		this.name = name;
		this.token = token;
	}

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
	
	public boolean addAnnonce(Domain dom, String titre, String description, int prix, int id) {
		return this.annonces.add(new Annonce(dom, titre, description, prix, id));
	}

}