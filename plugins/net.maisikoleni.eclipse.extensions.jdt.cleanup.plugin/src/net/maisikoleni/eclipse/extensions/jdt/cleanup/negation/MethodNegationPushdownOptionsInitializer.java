package net.maisikoleni.eclipse.extensions.jdt.cleanup.negation;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

import net.maisikoleni.eclipse.extensions.jdt.cleanup.CleanUpExtensions;

public class MethodNegationPushdownOptionsInitializer implements ICleanUpOptionsInitializer {

	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		options.setOption(CleanUpExtensions.METHOD_NEGATION_PUSHDOWN, CleanUpOptions.FALSE);
		for (var possibleCandidate : MethodNegationPushdownCandidate.values()) {
			options.setOption(possibleCandidate.getId(), CleanUpOptions.TRUE);
		}
	}
}
