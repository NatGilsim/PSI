package client;

public class MessageChat implements Comparable<MessageChat> {
	
	private String emetteur;
	private String content;
	private long timestamp;
	
	public MessageChat(String emetteur, String content, long timestamp) {
		this.emetteur = emetteur;
		this.content = content;
		this.timestamp = timestamp;
	}
	
	public String getContent() {
		return this.content;
	}

	public void setTimestamp() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public String getEmetteur() {
		return this.emetteur;
	}
	
	/*
	public String getIdMsg() {
		return this.emetteur + this.timestamp;
	}
	 */
	
	@Override
	public int compareTo(MessageChat m) {
		return this.timestamp > m.timestamp ? 1 : -1;
	}
	
}
