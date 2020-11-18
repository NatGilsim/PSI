package client;

public class MessageUDP extends MessageChat {
	
	private String destinataire;
	private Boolean ack;
	private int counter;
	
	public MessageUDP(String emetteur, String destinataire, String content, long timestamp) {
		super(emetteur, content, timestamp);
		this.destinataire = destinataire;
		this.ack = false;
		this.counter = 0;
	}
	
	public void setAck(boolean ack) {
		this.ack = ack;
	}
	
	public boolean getAck() {
		return this.ack;
	}
	
	public int getCounter() {
		return this.counter;
	}
	
	public void incrementCounter() {
		this.counter = this.counter + 1;
	}
	
	public String getDestinataire() {
		return this.destinataire;
	}
}