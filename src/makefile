JC = javac

all : Client.class Server.class

Client.class : client/*.java
	$(JC) client/*.java
Server.class : server/*.java
	$(JC) server/*.java
clean :
	rm server/*.class client/*.class
