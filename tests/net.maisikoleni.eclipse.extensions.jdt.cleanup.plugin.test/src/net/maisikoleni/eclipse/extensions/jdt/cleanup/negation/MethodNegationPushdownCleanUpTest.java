package net.maisikoleni.eclipse.extensions.jdt.cleanup.negation;

import static java.util.function.Predicate.not;
import static net.maisikoleni.eclipse.extensions.jdt.cleanup.CompilationUnitEnvironment.environment;
import static net.maisikoleni.eclipse.extensions.jdt.cleanup.ICleanUpAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.maisikoleni.eclipse.extensions.jdt.cleanup.CleanUpExtensions;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.CompilationUnitEnvironment;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.SimpleCleanUp;

@SuppressWarnings("restriction")
class MethodNegationPushdownCleanUpTest {

	private static CompilationUnitEnvironment environment;

	private CleanUpOptions options;
	private SimpleCleanUp cleanUp;

	@BeforeAll
	static void buildEnvironment() {
		String source = """
				import java.util.Optional;
				import java.util.stream.DoubleStream;

				public class X {
				    public static void main(String[] args) {
				        boolean present = !Optional.of(5).isEmpty();
				        boolean noNaN = !DoubleStream.of(1.0).anyMatch(Double::isNaN);
				    }
				}
				""";
		environment = environment().name("X.java").source(source).build();
	}

	@BeforeEach
	void createCleanUp() {
		options = new CleanUpOptions();
		new MethodNegationPushdownOptionsInitializer().setDefaultOptions(options);
		options.setOption(CleanUpExtensions.METHOD_NEGATION_PUSHDOWN, CleanUpOptions.TRUE);
		cleanUp = new MethodNegationPushdownCleanUp();
		cleanUp.setOptions(options);
	}

	@Test
	void testRequirements() {
		var assertCleanUp = assertThat(cleanUp).requirements();
		assertAll( //
				() -> assertCleanUp.requiresAST().isTrue(), //
				() -> assertCleanUp.requiresFreshAST().isFalse(), //
				() -> assertCleanUp.requiresChangedRegions().isFalse(), //
				() -> assertCleanUp.compilerOptions().isNullOrEmpty() //
		);
	}

	@Test
	void testStepDescriptions() {
		assertThat(cleanUp).stepDescriptions().hasSize(1).noneMatch(String::isBlank);
	}

	@Test
	void testPreviewAllSelected() {
		var previewLines = getPreviewLines();
		assertTrue(previewLines.stream().noneMatch(line -> line.contains("!")));
	}

	@Test
	void testPreviewNoneSelected() {
		MethodNegationPushdownCandidate.selectOnly(options);
		var previewLines = getPreviewLines();
		assertTrue(previewLines.stream().allMatch(line -> line.contains("!")));
	}

	@Test
	void testPreviewDisabled() {
		options.setOption(CleanUpExtensions.METHOD_NEGATION_PUSHDOWN, CleanUpOptions.FALSE);
		var previewLines = getPreviewLines();
		assertTrue(previewLines.stream().allMatch(line -> line.contains("!")));
	}

	private List<String> getPreviewLines() {
		var preview = cleanUp.getPreview();
		var<String> previewLines = preview.lines().filter(not(String::isEmpty)).collect(Collectors.toList());
		assertEquals(MethodNegationPushdownCandidate.values().length, previewLines.size());
		return previewLines;
	}

	@Test
	void testCleanUpAllSelected() {
		String expected = """
				import java.util.Optional;
				import java.util.stream.DoubleStream;

				public class X {
				    public static void main(String[] args) {
				        boolean present = Optional.of(5).isPresent();
				        boolean noNaN = DoubleStream.of(1.0).noneMatch(Double::isNaN);
				    }
				}
				""";
		var appliedCleanUp = assertThat(cleanUp).using(environment);
		appliedCleanUp.isWithoutProblems();
		appliedCleanUp.newSource().isEqualTo(expected);
	}

	@Test
	void testCleanUpNoneSelected() {
		MethodNegationPushdownCandidate.selectOnly(options);
		var appliedCleanUp = assertThat(cleanUp).using(environment);
		appliedCleanUp.isWithoutProblems();
		appliedCleanUp.isSourceUnchanged();
	}

	@Test
	void testCleanUpPreview() {
		String template = """
				import java.util.*;
				import java.util.stream.*;

				public class Previews {
				public static void main(String[] args) {
				%s
				}
				}
				""";
		MethodNegationPushdownCandidate.selectOnly(options);
		String previewsBefore = getPreviewsAsLines();
		MethodNegationPushdownCandidate.selectOnly(options, MethodNegationPushdownCandidate.values());
		String previewsAfter = getPreviewsAsLines();

		var appliedCleanUp = assertThat(cleanUp).using( //
				environment().name("Previews.java").source(template.formatted(previewsBefore)) //
		);

		appliedCleanUp.isWithoutProblems();
		appliedCleanUp.newSource().isEqualTo(template, previewsAfter);
	}

	private String getPreviewsAsLines() {
		return Stream.of(MethodNegationPushdownCandidate.values()) //
				.map(candidate -> candidate.getPreview(cleanUp)) //
				.collect(Collectors.joining("\n"));
	}
}
