JC = javac

all : Client.class Server.class Protocol.class

Client.class : client/*.java
	$(JC) client/*.java
Server.class : server/*.java
	$(JC) server/*.java
Protocol.class : protocol/*.java
	$(JC) protocol/*.java
clean :
	rm server/*.class client/*.class protocol/*.class
