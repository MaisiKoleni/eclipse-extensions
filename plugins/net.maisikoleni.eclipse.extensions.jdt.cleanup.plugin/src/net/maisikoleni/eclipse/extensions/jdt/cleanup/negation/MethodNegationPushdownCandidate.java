package net.maisikoleni.eclipse.extensions.jdt.cleanup.negation;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.text.edits.TextEditGroup;

import net.maisikoleni.eclipse.extensions.jdt.cleanup.CleanUpExtensions;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.SimpleCleanUp;

@SuppressWarnings("restriction")
public enum MethodNegationPushdownCandidate {
	OPTIONAL("java.util.Optional", "isPresent", "isEmpty"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	STREAM("java.util.stream.Stream", "anyMatch", "noneMatch", "java.util.function.Predicate"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private final String id;
	private final String ownerName;
	private final String methodNameA;
	private final String methodNameB;
	private final String[] parameterTypeNames;

	MethodNegationPushdownCandidate(String ownerName, String methodNameA, String methodNameB,
			String... parameterTypeNames) {
		this.id = CleanUpExtensions.METHOD_NEGATION_PUSHDOWN + "." + name().toLowerCase(Locale.ROOT);
		this.ownerName = ownerName;
		this.methodNameA = methodNameA;
		this.methodNameB = methodNameB;
		this.parameterTypeNames = parameterTypeNames;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return ownerName + ": " + methodNameA + " <---> " + methodNameB;
	}

	boolean appliesTo(IMethodBinding methodBinding) {
		return ASTNodes.usesGivenSignature(methodBinding, ownerName, methodNameA, parameterTypeNames)
				|| ASTNodes.usesGivenSignature(methodBinding, ownerName, methodNameB, parameterTypeNames);
	}

	TextEditGroup rewriteToCounterpart(ASTRewrite rewrite, AST ast, MethodInvocation methodInvocation,
			Function<String, TextEditGroup> textEditGroupCreator) {
		SimpleName oldSimpleName = methodInvocation.getName();
		String oldSimpleNameId = oldSimpleName.getIdentifier();
		String counterpartNameId = methodNameA.equals(oldSimpleNameId) ? methodNameB : methodNameA;
		TextEditGroup group = textEditGroupCreator
				.apply(String.format("Push down negation: !%s -> %s", oldSimpleNameId, counterpartNameId));
		ASTNodes.replaceButKeepComment(rewrite, oldSimpleName, ast.newSimpleName(counterpartNameId), group);
		return group;
	}

	public String getPreview(SimpleCleanUp cleanUp) {
		boolean enabled = cleanUp.isEnabled(getId());
		switch (this) {
			case OPTIONAL:
				if (enabled)
					return "boolean empty = Optional.of(1).isEmpty();";
				return "boolean empty = !Optional.of(1).isPresent();";
			case STREAM:
				if (enabled)
					return "boolean anyMatch = Stream.of(2).anyMatch(x -> x > 0);";
				return "boolean anyMatch = !Stream.of(2).noneMatch(x -> x > 0);";
			default:
				throw new IllegalStateException(name());
		}
	}

	public static void selectOnly(CleanUpOptions options, MethodNegationPushdownCandidate... candidates) {
		selectOnly(options, Set.of(candidates));
	}

	public static void selectOnly(CleanUpOptions options, Set<MethodNegationPushdownCandidate> candidates) {
		for (var candidate : values())
			options.setOption(candidate.getId(),
					candidates.contains(candidate) ? CleanUpOptions.TRUE : CleanUpOptions.FALSE);
	}
}