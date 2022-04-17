package net.maisikoleni.eclipse.extensions.jdt.cleanup;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Has a status and only one action that can be either enabled or not.
 */
@SuppressWarnings("restriction")
public abstract class SimpleCleanUp extends AbstractCleanUp {

	protected static final String[] NO_STEP_DESCRIPTIONS = new String[0];

	private RefactoringStatus status;

	protected SimpleCleanUp() {
	}

	protected SimpleCleanUp(Map<String, String> settings) {
		super(settings);
	}

	protected final RefactoringStatus getStatus() {
		return status;
	}

	/**
	 * Returns the unique id for this clean-up.
	 */
	protected abstract String getId();

	/**
	 * Returns whether this clean-up with the {@link #getId()} is enabled based on
	 * the current options.
	 */
	public final boolean isEnabled() {
		return isEnabled(getId());
	}

	@Override
	public final String[] getStepDescriptions() {
		if (isEnabled())
			return new String[] { getStepDescription() };
		return NO_STEP_DESCRIPTIONS;
	}

	@Override
	public abstract String getPreview();

	/**
	 * @see ICleanUp#getStepDescriptions()
	 */
	protected abstract String getStepDescription();

	@Override
	public final CleanUpRequirements getRequirements() {
		if (isEnabled())
			return getRequirementsWhenEnabled();
		return super.getRequirements();
	}

	/**
	 * @see ICleanUp#getRequirements()
	 */
	protected abstract CleanUpRequirements getRequirementsWhenEnabled();

	@Override
	public final RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {
		if (isEnabled()) {
			status = new RefactoringStatus();
		}
		return new RefactoringStatus();
	}

	@Override
	public final ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		if (isEnabled() && context.getCompilationUnit() != null)
			return createFixForEnabled(context);
		return null;
	}

	/**
	 * @see ICleanUp#createFix(CleanUpContext)
	 */
	protected abstract ICleanUpFix createFixForEnabled(CleanUpContext context) throws CoreException;

	@Override
	public final RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		try {
			// TODO this seems like it is not needed.
			if (status == null || status.isOK()) {
				return new RefactoringStatus();
			}
			return status;
		} finally {
			status = null;
		}
	}

	@Override
	public boolean isEnabled(String key) {
		return super.isEnabled(key);
	}
}
