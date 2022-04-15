package net.maisikoleni.eclipse.extensions.jdt.cleanup;

import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.FactoryBasedNavigableListAssert;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.StringAssert;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.core.util.SimpleDocument;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

@SuppressWarnings("restriction")
public class ICleanUpAssert extends AbstractObjectAssert<ICleanUpAssert, ICleanUp> {

	public ICleanUpAssert(ICleanUp actual) {
		super(actual, ICleanUpAssert.class);
	}

	protected ICleanUpAssert(ICleanUp actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public static ICleanUpAssert assertThat(ICleanUp cleanUp) {
		return new ICleanUpAssert(cleanUp);
	}

	public BetterListAssert<String, StringAssert> stepDescriptions() {
		return new BetterListAssert<>(StringAssert::new, actual.getStepDescriptions());
	}

	public CleanUpRequirementsAssert requirements() {
		return new CleanUpRequirementsAssert(actual.getRequirements());
	}

	public static class CleanUpRequirementsAssert
			extends AbstractAssert<CleanUpRequirementsAssert, CleanUpRequirements> {

		protected CleanUpRequirementsAssert(CleanUpRequirements actual) {
			super(actual, CleanUpRequirementsAssert.class);
		}

		public BooleanAssert requiresAST() {
			return new BooleanAssert(actual.requiresAST());
		}

		public BooleanAssert requiresFreshAST() {
			return new BooleanAssert(actual.requiresFreshAST());
		}

		public BooleanAssert requiresChangedRegions() {
			return new BooleanAssert(actual.requiresChangedRegions());
		}

		public MapAssert<String, String> compilerOptions() {
			return new MapAssert<>(actual.getCompilerOptions());
		}

		public static CleanUpRequirementsAssert assertThat(CleanUpRequirements cleanUpRequirements) {
			return new CleanUpRequirementsAssert(cleanUpRequirements);
		}
	}

	public BoundICleanUpAssert using(CompilationUnitEnvironment environment) {
		return new BoundICleanUpAssert(actual, environment);
	}

	public class BoundICleanUpAssert extends ICleanUpAssert {

		private CompilationUnitEnvironment environment;
		private Exception exception;
		private CompilationUnitChange change;
		private String modifiedSource;
		private RefactoringStatus preStatus;
		private RefactoringStatus postStatus;

		protected BoundICleanUpAssert(ICleanUp actual, CompilationUnitEnvironment environment) {
			super(actual, BoundICleanUpAssert.class);
			this.environment = environment.build();
			applyCleanUp();
		}

		private void applyCleanUp() {
			try {
				var iCompilationUnit = environment.getICompilationUnit();
				var compilationUnit = environment.getCompilationUnit();
				var iCompilationUnits = new ICompilationUnit[] { iCompilationUnit };
				var progressMonitor = environment.getProgressMonitor();
				var cleanUpContext = new CleanUpContext(iCompilationUnit, compilationUnit);
				// precondition check
				preStatus = actual.checkPreConditions(environment.getJavaProject(), iCompilationUnits, progressMonitor);
				// apply fix if possible
				if (!preStatus.hasError()) {
					var fix = actual.createFix(cleanUpContext);
					var newSource = new SimpleDocument(environment.getSource());
					if (fix != null) {
						change = fix.createChange(progressMonitor);
						change.getEdit().apply(newSource);
					}
					modifiedSource = newSource.get();
					// postcondition check
					postStatus = actual.checkPostConditions(progressMonitor);
				}
			} catch (Exception e) {
				exception = e;
			}
		}

		public BoundICleanUpAssert isWithoutProblems() {
			assertAll( //
					() -> exception().isNull(), //
					() -> environment().problems().isEmpty(), //
					() -> preStatus().isOK().isTrue(), //
					() -> postStatus().isOK().isTrue() //
			);
			return this;
		}

		public BoundICleanUpAssert isSourceUnchanged() {
			newSource().describedAs("source of '%s' has not been modified", environment.getName())
					.isEqualTo(environment.getSource());
			return this;
		}

		public ThrowableAssert exception() {
			return new ThrowableAssert(exception);
		}

		public ObjectAssert<CompilationUnitChange> change() {
			return new ObjectAssert<>(change);
		}

		public CompilationUnitEnvironmentAssert environment() {
			return new CompilationUnitEnvironmentAssert(environment);
		}

		public StringAssert newSource() {
			return new StringAssert(modifiedSource);
		}

		public RefactoringStatusAssert preStatus() {
			return new RefactoringStatusAssert(preStatus);
		}

		public RefactoringStatusAssert postStatus() {
			return new RefactoringStatusAssert(postStatus);
		}
	}

	public static class RefactoringStatusAssert
			extends AbstractObjectAssert<RefactoringStatusAssert, RefactoringStatus> {

		public RefactoringStatusAssert(RefactoringStatus actual) {
			super(actual, RefactoringStatusAssert.class);
		}

		public IntegerAssert severity() {
			return new IntegerAssert(actual.getSeverity());
		}

		public BooleanAssert isOK() {
			return new BooleanAssert(actual.isOK());
		}

		public BooleanAssert hasInfo() {
			return new BooleanAssert(actual.hasInfo());
		}

		public BooleanAssert hasWarning() {
			return new BooleanAssert(actual.hasWarning());
		}

		public BooleanAssert hasError() {
			return new BooleanAssert(actual.hasError());
		}

		public BooleanAssert hasFatalError() {
			return new BooleanAssert(actual.hasFatalError());
		}

		public BooleanAssert hasEntries() {
			return new BooleanAssert(actual.hasEntries());
		}

		public ListAssert<RefactoringStatusEntry> entries() {
			return new ListAssert<>(Arrays.asList(actual.getEntries()));
		}
	}

	public static class CompilationUnitEnvironmentAssert
			extends AbstractObjectAssert<CompilationUnitEnvironmentAssert, CompilationUnitEnvironment> {

		public CompilationUnitEnvironmentAssert(CompilationUnitEnvironment actual) {
			super(actual, CompilationUnitEnvironmentAssert.class);
		}

		public BetterListAssert<IProblem, IProblemAssert> problems() {
			return new BetterListAssert<>(IProblemAssert::new, actual.getCompilationUnit().getProblems());
		}

		public static class IProblemAssert extends AbstractObjectAssert<IProblemAssert, IProblem> {

			public IProblemAssert(IProblem actual) {
				super(actual, IProblemAssert.class);
			}

			public BetterListAssert<String, StringAssert> arguments() {
				return new BetterListAssert<>(StringAssert::new, actual.getArguments());
			}

			public IntegerAssert id() {
				return new IntegerAssert(actual.getID());
			}

			public StringAssert message() {
				return new StringAssert(actual.getMessage());
			}

			public IntegerAssert sourceStart() {
				return new IntegerAssert(actual.getSourceStart());
			}

			public IntegerAssert sourceEnd() {
				return new IntegerAssert(actual.getSourceEnd());
			}

			public IntegerAssert sourceLineNumber() {
				return new IntegerAssert(actual.getSourceLineNumber());
			}

			public BooleanAssert isError() {
				return new BooleanAssert(actual.isError());
			}

			public BooleanAssert isWarning() {
				return new BooleanAssert(actual.isWarning());
			}

			public BooleanAssert isInfo() {
				return new BooleanAssert(actual.isInfo());
			}
		}
	}

	public static class BetterListAssert<E, A extends AbstractAssert<A, E>>
			extends FactoryBasedNavigableListAssert<BetterListAssert<E, A>, List<E>, E, A> {

		public BetterListAssert(AssertFactory<E, A> assertFactory, List<E> actual) {
			super(actual, BetterListAssert.class, assertFactory);
		}

		@SafeVarargs
		public BetterListAssert(AssertFactory<E, A> assertFactory, E... actual) {
			this(assertFactory, actual == null ? null : Arrays.asList(actual));
		}
	}
}
