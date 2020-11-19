package protocol;

public interface ClientToServerTcpProtocol {
    public void connect(String name);
    public void disconnect();
    public void requestDomain();
    public void requestAnnonce(String domain);
    public void requestOwnAnnonce();
    public void postAnc(String domain, String title, String descriptif, String price);
    public void majAnc(String id, String domain, String title, String descriptif, String price);
    public void delAnc(String id);
    public void requestIP(String id);
}