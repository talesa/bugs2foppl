package ox.bugs2foppl;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import ox.bugs2foppl.gen.bugsBaseListener;
import ox.bugs2foppl.gen.bugsParser;

public class GeneralTreePass extends bugsBaseListener {
	ParseTreeProperty<String> outputForNode = new ParseTreeProperty<>();
	
	@Override public void enterInput(bugsParser.InputContext ctx) { }
	
	@Override public void exitInput(bugsParser.InputContext ctx) {
        // TODO this needs to be fixed
        outputForNode.put(ctx, outputForNode.get(ctx.children.get(0)));

        System.out.println(outputForNode.get(ctx.children.get(0)));
    }
	
	@Override public void enterVar(bugsParser.VarContext ctx) { }
	
	@Override public void exitVar(bugsParser.VarContext ctx) {
        outputForNode.put(ctx, ctx.name.getText());
    }
	
	@Override public void enterStochasticRelation(bugsParser.StochasticRelationContext ctx) { }
	
	@Override public void exitStochasticRelation(bugsParser.StochasticRelationContext ctx) {
        String output = "";

        // TODO if it's an observed variable and the appropriate data is in data then it should result in an observe variable

        // I return whatever is inside let [...] so basically pairs of variables
        output += outputForNode.get(ctx.var());
        output += " ";
        output += "(sample ";
        output += outputForNode.get(ctx.distribution());
        output += ")";

        outputForNode.put(ctx, output);
    }
	
	@Override public void enterInterval(bugsParser.IntervalContext ctx) { }
	
	@Override public void exitInterval(bugsParser.IntervalContext ctx) { }
	
	@Override public void enterTruncated(bugsParser.TruncatedContext ctx) { }
	
	@Override public void exitTruncated(bugsParser.TruncatedContext ctx) { }
	
	@Override public void enterVarStatement(bugsParser.VarStatementContext ctx) { }
	
	@Override public void exitVarStatement(bugsParser.VarStatementContext ctx) { }
	
	@Override public void enterDeclarationList(bugsParser.DeclarationListContext ctx) { }
	
	@Override public void exitDeclarationList(bugsParser.DeclarationListContext ctx) { }
	
	@Override public void enterNodeDeclaration(bugsParser.NodeDeclarationContext ctx) { }
	
	@Override public void exitNodeDeclaration(bugsParser.NodeDeclarationContext ctx) { }
	
	@Override public void enterDimensionsList(bugsParser.DimensionsListContext ctx) { }
	
	@Override public void exitDimensionsList(bugsParser.DimensionsListContext ctx) { }
	
	@Override public void enterDataStatement(bugsParser.DataStatementContext ctx) { }
	
	@Override public void exitDataStatement(bugsParser.DataStatementContext ctx) { }
	
	@Override public void enterModelStatement(bugsParser.ModelStatementContext ctx) { }
	
	@Override public void exitModelStatement(bugsParser.ModelStatementContext ctx) {
        String output = "";
//
//        output += "(let [";
        output += outputForNode.get(ctx.relationList());
//        output += "])";
        outputForNode.put(ctx, output);
    }
	
	@Override public void enterForLoop(bugsParser.ForLoopContext ctx) { }
	
	@Override public void exitForLoop(bugsParser.ForLoopContext ctx) { }
	
	@Override public void enterCounter(bugsParser.CounterContext ctx) { }
	
	@Override public void exitCounter(bugsParser.CounterContext ctx) { }
	
	@Override public void enterAssignment(bugsParser.AssignmentContext ctx) { }
	
	@Override public void exitAssignment(bugsParser.AssignmentContext ctx) { }
	
	@Override public void enterDeterministicRelation(bugsParser.DeterministicRelationContext ctx) { }
	
	@Override public void exitDeterministicRelation(bugsParser.DeterministicRelationContext ctx) {
        String output = "";
        output += outputForNode.get(ctx.var());
        output += " ";
        output += outputForNode.get(ctx.expression());
        outputForNode.put(ctx, output);
    }
	
	@Override public void enterExpression(bugsParser.ExpressionContext ctx) { }
	
	@Override public void exitExpression(bugsParser.ExpressionContext ctx) {
        outputForNode.put(ctx, ctx.getText());
    }
	
	@Override public void enterNegation(bugsParser.NegationContext ctx) { }
	
	@Override public void exitNegation(bugsParser.NegationContext ctx) { }
	
	@Override public void enterExpressionList1(bugsParser.ExpressionList1Context ctx) { }
	
	@Override public void exitExpressionList1(bugsParser.ExpressionList1Context ctx) {
        outputForNode.put(ctx, ctx.expression().getText());
    }

    @Override public void enterExpressionList2(bugsParser.ExpressionList2Context ctx) { }

    @Override public void exitExpressionList2(bugsParser.ExpressionList2Context ctx) {
        // Just appends the consecutive expressions with spaces in between
        String output = "";
        output += outputForNode.get(ctx.expression());
        output += " ";
        output += outputForNode.get(ctx.expressionList());
        outputForNode.put(ctx, output);
    }
	
	@Override public void enterRangeList(bugsParser.RangeListContext ctx) { }
	
	@Override public void exitRangeList(bugsParser.RangeListContext ctx) { }
	
	@Override public void enterRangeElement(bugsParser.RangeElementContext ctx) { }
	
	@Override public void exitRangeElement(bugsParser.RangeElementContext ctx) { }
	
	@Override public void enterDistribution(bugsParser.DistributionContext ctx) { }
	
	@Override public void exitDistribution(bugsParser.DistributionContext ctx) {
        String distributionName = ctx.ID().getText();
        String FOPPLDistributionName = "";

        switch (distributionName) {
            case "dnorm": FOPPLDistributionName = "normal";
        }

        String output = "";
        output += "(";
        output += FOPPLDistributionName;
        output += " ";
        // TODO there might be an error here in case of dflat() distribution
        output += outputForNode.get(ctx.expressionList());
        output += ")";

        outputForNode.put(ctx, output);
    }
	
	@Override public void enterRelations(bugsParser.RelationsContext ctx) { }
	
	@Override public void exitRelations(bugsParser.RelationsContext ctx) { }
	
	@Override public void enterRelationList1(bugsParser.RelationList1Context ctx) { }

    @Override public void exitRelationList1(bugsParser.RelationList1Context ctx) {
        String output = "";
        output += "(let [";
        output += outputForNode.get(ctx.relation());
        output += "]";
        output += ")";
        outputForNode.put(ctx, output);
    }

    @Override public void enterRelationList2(bugsParser.RelationList2Context ctx) { }

    @Override public void exitRelationList2(bugsParser.RelationList2Context ctx) {
        // Just appends the consecutive expressions with spaces in between
        String output = "";
        output += "(let [";
        output += outputForNode.get(ctx.relation());
        output += "]";
        output += " ";
        output += outputForNode.get(ctx.relationList());
        output += ")";
        outputForNode.put(ctx, output);
    }
	
	@Override public void enterRelation(bugsParser.RelationContext ctx) { }
	
	@Override public void exitRelation(bugsParser.RelationContext ctx) {
        String output = "";

        output += outputForNode.get(ctx.children.get(0));

        outputForNode.put(ctx, output);
    }
	
	@Override public void enterEveryRule(ParserRuleContext ctx) { }
	
	@Override public void exitEveryRule(ParserRuleContext ctx) { }
	
	@Override public void visitTerminal(TerminalNode node) { }
	
	@Override public void visitErrorNode(ErrorNode node) { }
}