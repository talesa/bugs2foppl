package ox.bugs2foppl;

import org.antlr.v4.misc.Graph;
import org.antlr.v4.runtime.tree.ParseTree;
import ox.bugs2foppl.gen.bugsBaseVisitor;
import ox.bugs2foppl.gen.bugsParser;

import java.util.HashMap;

/**
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public class DependencyGraphPass<T> extends bugsBaseVisitor<T> {
    HashMap<String, ParseTree> stringToRelationNode = new HashMap<String, ParseTree>();
    Graph<String> dependencyGraph = new Graph<>();
    String currentlyAssignedVariable = null;
	
//	@Override public T visitInput(bugsParser.InputContext ctx) { return visitChildren(ctx); }
//
	@Override public T visitVar(bugsParser.VarContext ctx) {
	    // direction of the edge:
        // a -> b
        // b depends on a
        dependencyGraph.addEdge(ctx.name.getText(), currentlyAssignedVariable);
        return null;
	}
	
	@Override public T visitStochasticRelation(bugsParser.StochasticRelationContext ctx) {
        currentlyAssignedVariable = ctx.var().getText();
        stringToRelationNode.put(currentlyAssignedVariable, ctx);
        T returnVal = visitChildren(ctx);
	    return returnVal;
	}
	
//	@Override public T visitInterval(bugsParser.IntervalContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitTruncated(bugsParser.TruncatedContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitVarStatement(bugsParser.VarStatementContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitDeclarationList(bugsParser.DeclarationListContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitNodeDeclaration(bugsParser.NodeDeclarationContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitDimensionsList(bugsParser.DimensionsListContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitDataStatement(bugsParser.DataStatementContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitModelStatement(bugsParser.ModelStatementContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitForLoop(bugsParser.ForLoopContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitCounter(bugsParser.CounterContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitAssignment(bugsParser.AssignmentContext ctx) { return visitChildren(ctx); }
	
	@Override public T visitDeterministicRelation(bugsParser.DeterministicRelationContext ctx) {
        currentlyAssignedVariable = ctx.var().getText();
        stringToRelationNode.put(currentlyAssignedVariable, ctx);
        T returnVal = visitChildren(ctx);
        return returnVal;
	}
	
//	@Override public T visitExpression(bugsParser.ExpressionContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitNegation(bugsParser.NegationContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitExpressionList1(bugsParser.ExpressionList1Context ctx) { return visitChildren(ctx); }
//
//	@Override public T visitExpressionList2(bugsParser.ExpressionList2Context ctx) { return visitChildren(ctx); }
//
//	@Override public T visitRangeList(bugsParser.RangeListContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitRangeElement(bugsParser.RangeElementContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitDistribution(bugsParser.DistributionContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitRelations(bugsParser.RelationsContext ctx) { return visitChildren(ctx); }
//
//	@Override public T visitRelationList1(bugsParser.RelationList1Context ctx) { return visitChildren(ctx); }
//
//	@Override public T visitRelationList2(bugsParser.RelationList2Context ctx) { return visitChildren(ctx); }
//
//	@Override public T visitRelation(bugsParser.RelationContext ctx) { return visitChildren(ctx); }
}