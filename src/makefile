JC = javac

all : Protocol.class Client.class Server.class

Client.class : client/*.java
	$(JC) client/*.java
Server.class : server/*.java
	$(JC) server/*.java
Protocol.class : protocol/*.java
	$(JC) protocol/*.java
clean :
	rm server/*.class client/*.class protocol/*.class
