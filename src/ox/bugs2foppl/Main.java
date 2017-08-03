package ox.bugs2foppl;

import org.antlr.v4.misc.Graph;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import ox.bugs2foppl.gen.*;

import javax.swing.*;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws java.io.IOException {

        String inputFile = "examples/PLA2_example3";

        FileInputStream fis = new FileInputStream(inputFile);
        ANTLRInputStream inputStream = new ANTLRInputStream(fis);

        bugsLexer lexer = new bugsLexer(inputStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        bugsParser parser = new bugsParser(tokens);
        parser.setBuildParseTree(true);
        ParseTree tree = parser.input();

        System.out.println(tree.toStringTree(parser));

        ParseTreeWalker walker = new ParseTreeWalker();
        GeneralTreePass gp = new GeneralTreePass();
//        walker.walk(generalPass, tree);

        DependencyGraphPass<Void> dgp = new DependencyGraphPass<>();
        dgp.visit(tree);

        List<String> dependencyList = dgp.dependencyGraph.sort();
        // nonefficient
        Collections.reverse(dependencyList);

        System.out.println(dependencyList);

//        System.out.println(dgp.stringToRelationNode);

        List<String> relationsList = new LinkedList<>();

        ParseTree node;

        for(String var : dependencyList) {
            node = dgp.stringToRelationNode.get(var);
            walker.walk(gp, node);
            relationsList.add(gp.outputForNode.get(node));
        }

        System.out.println(getOutputForRelations(relationsList));
    }

    static String getOutputForRelations(List<String> relationsList) {
        String output = "";
        output += "let([";
        for (String relation : relationsList) {
            output += relation;
            output += "\n";
        }
        output += "]";
        output += ")";
        return output;
    }
}