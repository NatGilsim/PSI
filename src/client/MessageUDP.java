package client;

public class MessageUDP {
	
	String destinataire;
	String msg;
	Boolean ack;
	
	public MessageUDP(String destinataire, String msg) {
		this.destinataire = destinataire;
		this.msg = msg;
		this.ack = false;
	}
	
	public String getMsg() {
		return this.msg;
	}
	
	public String getDestinataire() {
		return this.destinataire;
	}
	
	public void setAck(boolean ack) {
		this.ack = ack;
	}
	
	public boolean getAck() {
		return this.ack;
	}
}