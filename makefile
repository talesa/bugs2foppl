antlr4=java org.antlr.v4.gui.TestRig
grun=java -Xmx500M org.antlr.v4.Tool

default: out/bugsParser.java
	cd out; $(antlr4) bugs input -tokens ../examples/v1_rats

out/bugsParser.java: bugs.g4
	$(grun) bugs.g4 -o out
	javac out/*.java