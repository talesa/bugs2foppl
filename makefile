antlr4=java org.antlr.v4.gui.TestRig
grun=java -Xmx500M org.antlr.v4.Tool

default: bugsParser.java
	$(antlr4) bugs input -tokens examples/v1_rats

bugsParser.java: bugs.g4
	$(grun) bugs.g4
	javac *.java