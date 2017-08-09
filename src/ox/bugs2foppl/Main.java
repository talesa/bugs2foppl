package ox.bugs2foppl;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.AttributeMap;
import org.renjin.sexp.Vector;
import ox.bugs2foppl.gen.bugsLexer;
import ox.bugs2foppl.gen.bugsParser;

import javax.script.ScriptEngine;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {

        // read file
        String inputFile = "examples/examples_JAGS/classic-bugs/vol1/equiv/equiv-data.R";

        Scanner in = new Scanner(new FileReader(inputFile));

        StringBuilder sb = new StringBuilder();
        while(in.hasNext()) {
            sb.append(' ');
            sb.append(in.next());
        }
        in.close();

        String data_file = sb.toString();

        String pattern = "(\"(\\S+)\"\\s*<-\\s*(?:.|\\n)*?)(?=\"\\S+\"\\s*<-|\\z)";


        System.out.println("input: " + data_file);

        System.out.println("pattern: " + pattern);

        Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
        Matcher m = p.matcher(data_file);

        List<String> Rcommands = new LinkedList<>();

        Map<String, String> varToFOPPLString = new HashMap<>();

        RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
        ScriptEngine engine = factory.getScriptEngine();

        while (m.find()) {
            String var_name = m.group(2);
            String statement = m.group(1);

            Vector res = (Vector) engine.eval(statement);

            String objectTypeName = res.getClass().getName();

            System.out.println("var_name : " + var_name);
            System.out.println("expression: " + statement);
            System.out.println("object type: " + objectTypeName);

            String expectedType = "org.renjin.sexp.DoubleArrayVector";
            if (!objectTypeName.equals(expectedType)) {
                throw new Exception("Not expected type: " + expectedType);
            } else {
                varToFOPPLString.put(var_name,
                        convertVectorToFOPPLString(res, var_name));
            }
        }
    }

    static String convertVectorToFOPPLString(Vector res, String name) throws Exception {
        if (res.hasAttributes()) {
            AttributeMap attributes = res.getAttributes();
            Vector dim = attributes.getDim();
            if (dim == null) {
                return convertVectorToFOPPLStringCaseVector(res, name);
            } else {

            }
        } else {
            return convertVectorToFOPPLStringCaseVector(res, name);
        }
        return "";
    }

    static List convertVectorToMatrix(List v, List dim) throws Exception {

    }

    static String convertVectorToFOPPLStringCaseVector(Vector res, String name) throws Exception {
        System.out.println("Result is a vector of length " + res.length());
        if (res.length() == 1) {
            return String.format("(def %s %s)", name, res.getElementAsDouble(0));
        } else if (res.length() > 1) {
            String numbers_string = "";
            for (int i=0; i<res.length(); i++) {
                numbers_string += res.getElementAsString(i);
                numbers_string += " ";
            }
            return String.format("(def %s (vector %s))", name, numbers_string);
        } else {
            throw new Exception("res.length() < 1");
        }
    }

    static void main2() throws Exception {

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