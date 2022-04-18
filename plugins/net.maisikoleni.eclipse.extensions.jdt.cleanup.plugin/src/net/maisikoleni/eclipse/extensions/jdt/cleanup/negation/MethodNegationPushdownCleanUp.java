package net.maisikoleni.eclipse.extensions.jdt.cleanup.negation;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.text.edits.TextEditGroup;

import net.maisikoleni.eclipse.extensions.jdt.cleanup.CleanUpExtensions;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.SimpleAstCleanUp;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.negation.MethodNegationPushdownCleanUp.MethodNegationPushdownChange;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.negation.MethodNegationPushdownCleanUp.MethodNegationPushdownVisitor;

@SuppressWarnings("restriction")
public class MethodNegationPushdownCleanUp
		extends SimpleAstCleanUp<MethodNegationPushdownVisitor, MethodNegationPushdownChange> {

	public MethodNegationPushdownCleanUp() {
	}

	public MethodNegationPushdownCleanUp(Map<String, String> settings) {
		super(settings);
	}

	@Override
	protected String getId() {
		return CleanUpExtensions.METHOD_NEGATION_PUSHDOWN;
	}

	@Override
	protected String getStepDescription() {
		return CleanUpExtensions.METHOD_NEGATION_PUSHDOWN_DESCRIPTION;
	}

	@Override
	protected CleanUpRequirements getRequirementsWhenEnabled() {
		return new CleanUpRequirements(true, false, false, null);
	}

	@Override
	public String getPreview() {
		var preview = new StringBuilder();
		for (var candidate : MethodNegationPushdownCandidate.values()) {
			preview.append(candidate.getPreview(this));
			preview.append("\n");
		}
		return preview.toString();
	}

	@Override
	protected MethodNegationPushdownVisitor createVisitor(List<MethodNegationPushdownChange> changes) {
		var candidates = extractCandidateSetFromOptions();
		return new MethodNegationPushdownVisitor(changes, candidates);
	}

	Set<MethodNegationPushdownCandidate> extractCandidateSetFromOptions() {
		var candidates = EnumSet.noneOf(MethodNegationPushdownCandidate.class);
		for (var possibleCandidate : MethodNegationPushdownCandidate.values()) {
			if (isEnabled(possibleCandidate.getId()))
				candidates.add(possibleCandidate);
		}
		return candidates;
	}

	static class MethodNegationPushdownVisitor extends ASTVisitor {

		private final List<MethodNegationPushdownChange> changes;
		private final Set<MethodNegationPushdownCandidate> candidates;

		public MethodNegationPushdownVisitor(List<MethodNegationPushdownChange> changes,
				Set<MethodNegationPushdownCandidate> candidates) {
			this.candidates = candidates;
			this.changes = changes;
		}

		@Override
		public boolean visit(PrefixExpression node) {
			var operand = ASTNodes.getUnparenthesedExpression(node.getOperand());
			if (node.getOperator() == Operator.NOT && operand.getNodeType() == ASTNode.METHOD_INVOCATION) {
				MethodInvocation methodInvocation = (MethodInvocation) operand;
				var methodBinding = methodInvocation.resolveMethodBinding();
				var match = candidates.stream().filter(candidate -> candidate.appliesTo(methodBinding)).findFirst();
				if (match.isPresent()) {
					changes.add(new MethodNegationPushdownChange(node, methodInvocation, match.get()));
				}
			}
			return true;
		}

	}

	static class MethodNegationPushdownChange extends CompilationUnitRewriteOperation {

		private final PrefixExpression notPrefixExpr;
		private final MethodInvocation methodInvocation;
		private final MethodNegationPushdownCandidate operation;

		public MethodNegationPushdownChange(PrefixExpression notPrefixExpr, MethodInvocation methodInvocation,
				MethodNegationPushdownCandidate operation) {
			this.notPrefixExpr = notPrefixExpr;
			this.methodInvocation = methodInvocation;
			this.operation = operation;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite = cuRewrite.getASTRewrite();
			AST ast = cuRewrite.getRoot().getAST();
			TextEditGroup group = operation.rewriteToCounterpart(rewrite, ast, methodInvocation,
					label -> createTextEditGroup(label, cuRewrite));
			Expression newMethodInvocation = ASTNodes.createMoveTarget(rewrite, methodInvocation);
			ASTNodes.replaceButKeepComment(rewrite, notPrefixExpr, newMethodInvocation, group);
		}
	}

}
