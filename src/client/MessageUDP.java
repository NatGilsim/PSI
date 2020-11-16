package client;

public class MessageUDP {
	
	private String destinataire;
	private String content;
	private long timestamp;
	private Boolean ack;
	private int counter;
	
	public MessageUDP(String destinataire, String msg, long timestamp) {
		this.destinataire = destinataire;
		this.content = msg;
		this.ack = false;
		this.timestamp = timestamp;
		this.counter = 0;
	}
	
	public String getContent() {
		return this.content;
	}
	
	public String getDestinataire() {
		return this.destinataire;
	}
	
	public void setAck(boolean ack) {
		this.ack = ack;
	}
	
	public void setTimestamp() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public boolean getAck() {
		return this.ack;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public int getCounter() {
		return this.counter;
	}
	
	public void incrementCounter() {
		this.counter++;
	}
}