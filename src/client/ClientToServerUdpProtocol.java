package client;

public interface ClientToServerUdpProtocol {
	public void msg(String emetteur, String timestamp, String msg);
	public void msgAck(String emetteur, String timestamp);
}
