antlr4=java org.antlr.v4.gui.TestRig
grun=java -Xmx500M org.antlr.v4.Tool

default: HelloParser.java
	$(antlr4) Hello input -tokens input

HelloParser.java: Hello.g4
	$(grun) Hello.g4
	javac *.java