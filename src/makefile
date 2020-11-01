JC = javac

all : Client.class Server.class

Client.class : client/Client.java
	$(JC) client/Client.java
Server.class : server/Annonce.java server/ClientHandler.java server/ClientServer.java server/Domain.java server/Server.java
	$(JC) server/*.java
clean :
	rm server/*.class client/*.class
