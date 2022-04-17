package net.maisikoleni.eclipse.extensions.jdt.cleanup.negation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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
	OBJECTS_NULL("nonNull", "isNull", "java.util.Objects", "java.lang.Object"),
	OPTIONAL("isPresent", "isEmpty", List.of( //
			"java.util.Optional", //
			"java.util.OptionalInt", //
			"java.util.OptionalLong", //
			"java.util.OptionalDouble" //
	)),
	STREAM("anyMatch", "noneMatch", //
			new Variant("java.util.stream.Stream", "java.util.function.Predicate"), //
			new Variant("java.util.stream.IntStream", "java.util.function.IntPredicate"), //
			new Variant("java.util.stream.LongStream", "java.util.function.LongPredicate"), //
			new Variant("java.util.stream.DoubleStream", "java.util.function.DoublePredicate") //
	);

	private final String id;
	private final List<Variant> variants;
	private final String methodNameA;
	private final String methodNameB;

	MethodNegationPushdownCandidate(String methodNameA, String methodNameB, Variant... variants) {
		this.id = CleanUpExtensions.METHOD_NEGATION_PUSHDOWN + "." + name().toLowerCase(Locale.ROOT);
		this.variants = List.of(variants);
		if (this.variants.isEmpty())
			throw new IllegalArgumentException("Variants may not be empty");
		this.methodNameA = Objects.requireNonNull(methodNameA);
		this.methodNameB = Objects.requireNonNull(methodNameB);
	}

	MethodNegationPushdownCandidate(String methodNameA, String methodNameB, List<String> types,
			String... parameterTypeNames) {
		this(methodNameA, methodNameB,
				types.stream().map(type -> new Variant(type, parameterTypeNames)).toArray(Variant[]::new));
	}

	MethodNegationPushdownCandidate(String methodNameA, String methodNameB, String type, String... parameterTypeNames) {
		this(methodNameA, methodNameB, List.of(type), parameterTypeNames);
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		var result = new StringBuilder();
		result.append(methodNameA);
		result.append("/");
		result.append(methodNameB);
		result.append(" for the type ");
		result.append(variants.get(0).type());
		if (variants.size() > 1) {
			result.append(" (and ");
			result.append(variants.size() - 1);
			result.append(" others)");
		}
		return result.toString();
	}

	boolean appliesTo(IMethodBinding methodBinding) {
		var name = methodBinding.getName();
		if (!name.equals(methodNameA) && !name.equals(methodNameB))
			return false;
		/*
		 * Now we know the name matches one of both possible ones, so just check the
		 * type and rest of the signature.
		 */
		return variants.stream().anyMatch(
				variant -> ASTNodes.usesGivenSignature(methodBinding, variant.type(), name, variant.parameters()));
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
		boolean disabled = !cleanUp.isEnabled() || !cleanUp.isEnabled(getId());
		return switch (this) {
			case OPTIONAL -> disabled ? //
					"boolean empty = !Optional.of(1).isPresent();" : //
					"boolean empty = Optional.of(1).isEmpty();";
			case STREAM -> disabled ? //
					"boolean anyMatch = !Stream.of(2).noneMatch(x -> x > 0);" : //
					"boolean anyMatch = Stream.of(2).anyMatch(x -> x > 0);";
			case OBJECTS_NULL -> disabled ? //
					"boolean isNull = !Objects.nonNull(3);" : //
					"boolean isNull = Objects.isNull(3);";
		};
	}

	public static void selectOnly(CleanUpOptions options, MethodNegationPushdownCandidate... candidates) {
		selectOnly(options, Set.of(candidates));
	}

	public static void selectOnly(CleanUpOptions options, Set<MethodNegationPushdownCandidate> candidates) {
		for (var candidate : values())
			options.setOption(candidate.getId(),
					candidates.contains(candidate) ? CleanUpOptions.TRUE : CleanUpOptions.FALSE);
	}

	record Variant(String type, String... parameters /* using array here because of JDT API */) {

		Variant {
			Objects.requireNonNull(type);
			Stream.of(parameters).forEach(Objects::requireNonNull);
		}

		@Override
		public String toString() {
			return type + "(" + String.join(",", parameters) + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash(type) * 31 + Arrays.hashCode(parameters);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || (obj instanceof Variant other) //
					&& Arrays.equals(parameters, other.parameters) //
					&& Objects.equals(type, other.type);
		}

	}
}