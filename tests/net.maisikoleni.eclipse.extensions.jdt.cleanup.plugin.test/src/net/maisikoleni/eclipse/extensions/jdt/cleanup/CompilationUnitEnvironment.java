package net.maisikoleni.eclipse.extensions.jdt.cleanup;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

@SuppressWarnings("restriction")
public final class CompilationUnitEnvironment {

	private boolean built;

	private String name;
	private String source;
	private IJavaProject javaProject;
	private IProgressMonitor progressMonitor;
	private ICompilationUnit iCompilationUnit;
	private CompilationUnit compilationUnit;
	private Map<String, String> additionalCompilationOptions;

	private CompilationUnitEnvironment() {
	}

	public static CompilationUnitEnvironment environment() {
		return new CompilationUnitEnvironment();
	}

	public boolean isBuilt() {
		return built;
	}

	public String getName() {
		return name;
	}

	public String getSource() {
		return source;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public IProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public ICompilationUnit getICompilationUnit() {
		return iCompilationUnit;
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public Map<String, String> getAdditionalCompilationOptions() {
		return additionalCompilationOptions;
	}

	void verify() {
		try {
			assertThat(name).isNotBlank();
			assertThat(source).isNotBlank();
			assertThat(javaProject).isNotNull();
			assertThat(iCompilationUnit).isNotNull();
			assertThat(progressMonitor).isNotNull();
			assertThat(compilationUnit).isNotNull();
			assertThat(iCompilationUnit.getResource()).isInstanceOf(IFile.class);
			assertThat(iCompilationUnit.getBuffer()).isNotNull();
			assertThat(iCompilationUnit.getBuffer().getCharacters()).isNotNull();
			assertThat(javaProject).isSameAs(iCompilationUnit.getJavaProject());
			assertThat(iCompilationUnit).isSameAs(compilationUnit.getJavaElement());
			assertThat(additionalCompilationOptions).isNotNull();
			if (!mockingDetails(iCompilationUnit).isMock())
				assertThat(iCompilationUnit.getJavaProject().getOptions(true))
						.describedAs("Non-mock ICompilationUnit most provide a java-project with compiler options.")
						.isNotNull();
		} catch (JavaModelException e) {
			fail("CompilationUnitEnvironment verification failed with exception", e);
		}
	}

	public CompilationUnitEnvironment name(String name) {
		assertNotBuilt();
		this.name = name;
		return this;
	}

	public CompilationUnitEnvironment source(String source) {
		assertNotBuilt();
		this.source = source;
		return this;
	}

	public CompilationUnitEnvironment javaProject(IJavaProject javaProject) {
		assertNotBuilt();
		this.javaProject = javaProject;
		return this;
	}

	public CompilationUnitEnvironment progressMonitor(IProgressMonitor progressMonitor) {
		assertNotBuilt();
		this.progressMonitor = progressMonitor;
		return this;
	}

	public CompilationUnitEnvironment iCompilationUnit(ICompilationUnit iCompilationUnit) {
		assertNotBuilt();
		this.iCompilationUnit = iCompilationUnit;
		return this;
	}

	public CompilationUnitEnvironment compilationUnit(CompilationUnit compilationUnit) {
		assertNotBuilt();
		this.compilationUnit = compilationUnit;
		return this;
	}

	public CompilationUnitEnvironment additionalCompilationOptions(Map<String, String> additionalCompilationOptions) {
		this.additionalCompilationOptions = additionalCompilationOptions;
		return this;
	}

	private void assertNotBuilt() {
		assertThat(built).as("CompilationUnitEnvironment has not been build yet.").isFalse();
	}

	public CompilationUnitEnvironment build() {
		if (built)
			return this;
		try {
			if (progressMonitor == null) {
				progressMonitor = new NullProgressMonitor();
			}
			if (javaProject == null) {
				javaProject = mock(IJavaProject.class);
			}
			if (additionalCompilationOptions == null) {
				additionalCompilationOptions = new HashMap<>();
			}
			setupICompilationUnit();
			if (compilationUnit == null) {
				compilationUnit = createCompilationUnit();
			}
		} catch (JavaModelException e) {
			fail("ICleanUpEnvorinment build failed.", e);
		}
		verify();
		built = true;
		return this;
	}

	private void setupICompilationUnit() throws JavaModelException {
		boolean isMock = mockingDetails(iCompilationUnit).isMock();
		// Setup compilation unit source contents
		if (iCompilationUnit != null && iCompilationUnit.getBuffer() != null
				&& iCompilationUnit.getBuffer().getCharacters() != null) {
			// If the compilation unit has the source, use / check that
			var sourceFromUnit = new String(iCompilationUnit.getBuffer().getCharacters());
			if (source != null)
				assertThat(source).describedAs(
						"source string from environment must match ICompilationUnit buffer content if both are set.")
						.isEqualTo(sourceFromUnit);
			else
				source = sourceFromUnit;
		} else {
			// Otherwise, mock the compilation unit correctly to have a source
			if (compilationUnit == null)
				iCompilationUnit = mock(ICompilationUnit.class);
			else if (!isMock)
				fail("The provided ICompilationUnit has no buffer content and is not a mock.");
			isMock = true; // Must be one at that point
			if (iCompilationUnit.getBuffer() == null)
				when(iCompilationUnit.getBuffer()).thenReturn(mock(IBuffer.class));
			else if (!mockingDetails(iCompilationUnit.getBuffer()).isMock())
				fail("The provided ICompilationUnit has no buffer content and the buffer is not a mock.");
			if (source != null)
				when(iCompilationUnit.getBuffer().getCharacters()).thenReturn(source.toCharArray());
			else
				fail("source string must be present if ICompilationUnit buffer content is not present.");
		}
		// Setup IFile as resource
		if (!(iCompilationUnit.getResource() instanceof IFile) && isMock)
			when(iCompilationUnit.getResource()).thenReturn(mock(IFile.class));
		else
			fail("The provided ICompilationUnit has no IFile as resource and is not a mock.");
		// Setup java project
		if (iCompilationUnit.getJavaProject() != javaProject && isMock)
			when(iCompilationUnit.getJavaProject()).thenReturn(javaProject);
		else
			fail("The provided ICompilationUnit has not the same project as the environment and is not a mock.");
	}

	private CompilationUnit createCompilationUnit() {
		@SuppressWarnings("deprecation")
		var parser = ASTParser.newParser(AST.JLS17);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		setCompilerOptions(parser);
		parser.setSource(source.toCharArray());
		parser.setEnvironment(null, null, null, true);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		parser.setUnitName(name);
		var node = parser.createAST(null);
		assertThat(node).isInstanceOf(CompilationUnit.class);
		return (CompilationUnit) node;
	}

	private void setCompilerOptions(ASTParser parser) {
		// Use options from the project if available
		var options = iCompilationUnit.getJavaProject().getOptions(true);
		// Otherwise use the default options
		if (options == null || options.isEmpty()) {
			options = new CompilerOptions().getMap();
			JavaCore.setComplianceOptions(JavaCore.VERSION_17, options);
			options.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.IGNORE);
		}
		options.putAll(additionalCompilationOptions);
		/*
		 * The following has the purpose to set the type root of the ASTParser whilst
		 * having no project and using a char array source. For that, we set the source
		 * temporarily to a type root that references a custom option map. This option
		 * map will set the type root of the ASTParser to the ICompilationUnit mock when
		 * the compilation starts and we are past the source type determination stage.
		 */
		options = new HashMap<>(options) {
			private static final long serialVersionUID = 1L;

			private boolean set;

			@Override
			public String get(Object key) {
				if (!set) {
					/*
					 * As set source also sets the project (which we don't want set), we set the
					 * project back to null immediately afterwards. The purpose of the flag is to
					 * only do this once.
					 */
					parser.setSource(iCompilationUnit);
					parser.setProject(null);
					set = true;
				}
				return super.get(key);
			}
		};
		var tempTypeRoot = mock(ITypeRoot.class);
		var tempProject = mock(IJavaProject.class);
		when(tempTypeRoot.getJavaProject()).thenReturn(tempProject);
		when(tempProject.getOptions(anyBoolean())).thenReturn(options);
		/*
		 * Only return the temporary project once when we are in the Map.get to keep the
		 * options we have set and configured tempProject to return. Only possible if
		 * the unit is a mock, otherwise, it must have a project with options.
		 */
		if (mockingDetails(iCompilationUnit).isMock()) {
			IJavaProject oldProject;
			when(oldProject = iCompilationUnit.getJavaProject()).thenReturn(tempProject, oldProject);
		}
		// Inject custom option implementation using a dummy type root mock
		parser.setSource(tempTypeRoot);
		// Set project again to null.
		parser.setProject(null);
	}

}