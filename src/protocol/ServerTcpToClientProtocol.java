package protocol;

public interface ServerTcpToClientProtocol {
	public void connectOk(String token, String utilisateur);
	public void connectNewUserOk(String token);
	public void connectNewUserKo();
	public void connectKo();
	public void postAncOk(String idAnnonce);
	public void postAncKo();
	public void sendDomainOk(String[] domain);
	public void sendDomainKo();
	public void sendAncOk(String[] annonces);
	public void sendAncKo();
	public void sendOwnAncOk(String[] annonces);
	public void sendOwnAncKo();
	public void majAncOk(String idAnnonce);
	public void majAncKo();
	public void delAncOk(String idAnnonce);
	public void delAncKo();
	public void requestIpOk(String ip, String destinataire);
	public void requestIpKo();
	public void unknownRequest();
	public void notConnedted();
}
