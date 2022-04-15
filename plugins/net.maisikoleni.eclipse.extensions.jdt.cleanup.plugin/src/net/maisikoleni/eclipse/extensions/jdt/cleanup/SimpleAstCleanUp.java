package net.maisikoleni.eclipse.extensions.jdt.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

@SuppressWarnings("restriction")
public abstract class SimpleAstCleanUp<V extends ASTVisitor, O extends CompilationUnitRewriteOperation>
		extends SimpleCleanUp {

	protected SimpleAstCleanUp() {
	}

	protected SimpleAstCleanUp(Map<String, String> settings) {
		super(settings);
	}

	@Override
	protected ICleanUpFix createFixForEnabled(CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit = context.getAST();
		Assert.isNotNull(compilationUnit,
				"AST must be avaiable for SimpleAstCleanUp, please adjust the clean-up requirements.");
		List<O> changes = new ArrayList<>();
		V visitor = createVisitor(changes);
		compilationUnit.accept(visitor);
		if (changes.isEmpty()) {
			return null;
		}
		return new CompilationUnitRewriteOperationsFix(getStepDescription(), compilationUnit,
				changes.toArray(CompilationUnitRewriteOperation[]::new));
	}

	protected abstract V createVisitor(List<O> changes);
}
