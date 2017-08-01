package ox.bugs2foppl;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import ox.bugs2foppl.gen.*;

import java.io.FileInputStream;

public class Main {
    public static void main(String[] args) throws java.io.IOException {

        String inputFile = "examples/PLA2_example2";

        FileInputStream fis = new FileInputStream(inputFile);
        ANTLRInputStream inputStream = new ANTLRInputStream(fis);

        bugsLexer lexer = new bugsLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        bugsParser parser = new bugsParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.input();

        System.out.println(tree.toStringTree(parser));

        ParseTreeWalker walker = new ParseTreeWalker();
        GeneralTreePass generalPass = new GeneralTreePass();
        walker.walk(generalPass, tree);
    }
}