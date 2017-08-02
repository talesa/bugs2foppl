antlr4=java org.antlr.v4.gui.TestRig
grun=java -Xmx500M org.antlr.v4.Tool

base_dir=src/ox/bugs2foppl
antlr_gen=$(base_dir)/gen
antlr_grammar=$(base_dir)/grammar
package_name=ox.bugs2foppl.gen

default: $(antlr_gen)/bugsParser.java
#	cd $(antlr_gen); $(antlr4) bugs input -tokens ../examples/v1_rats

$(antlr_gen)/bugsParser.java: $(antlr_grammar)/bugs.g4
	cd $(antlr_grammar); $(grun) bugs.g4 -o ../gen -package $(package_name) -visitor -listener
	javac $(antlr_gen)/*.java