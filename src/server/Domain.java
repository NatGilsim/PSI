package server;

import java.util.stream.Stream;

public enum Domain {
	Téléphone,
	Voiture,
	Maison,
	Appartement,
	Meuble,
	Vêtement,
	Ordinateur;
	
	static public String[] getTabDomains() {
		return Stream.of(Domain.values()).map(Domain::name).toArray(String[]::new);
	}
}
