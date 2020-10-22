package server;

public class Annonce {

	private Domain dom;
	private String titre;
	private String descriptif;
	private int prix;
	private int id;
	
	public Annonce (Domain d, String titre, String descriptif, int prix, int id) {
		this.id = id;
		this.setDomain(d);
		this.setTitre(titre);
		this.setDescriptif(descriptif);
		this.setPrix(prix);
	}
	
	public Domain getDomain() {
		return this.dom;
	}

	public String getTitre() {
		return this.titre;
	}

	public String getDescriptif() {
		return this.descriptif;
	}
	
	public int getPrix() {
		return this.prix;
	}
	
	public int getId() {
		return this.id;
	}

	public void setDomain(Domain d) {
		this.dom = d;
	}
	
	public void setTitre(String titre) {
		this.titre = titre;
	}
	
	public void setDescriptif(String descriptif) {
		this.descriptif = descriptif;
	}

	public void setPrix(int prix) {
		this.prix = prix;
	}

}