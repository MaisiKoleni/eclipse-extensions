package net.maisikoleni.eclipse.extensions.jdt.cleanup;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.internal.ui.preferences.cleanup.AbstractCleanUpTabPage;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import net.maisikoleni.eclipse.extensions.jdt.cleanup.negation.MethodNegationPushdownCandidate;
import net.maisikoleni.eclipse.extensions.jdt.cleanup.negation.MethodNegationPushdownCleanUp;

@SuppressWarnings("restriction")
public class CleanUpExtensionsTabPage extends AbstractCleanUpTabPage {

	public static final String ID = "net.maisikoleni.eclipse.extensions.jdt.cleanup.tabpage"; //$NON-NLS-1$

	static final String[] FALSE_TRUE = { CleanUpOptions.FALSE, CleanUpOptions.TRUE };

	@Override
	protected AbstractCleanUp[] createPreviewCleanUps(Map<String, String> values) {
		return new AbstractCleanUp[] { new MethodNegationPushdownCleanUp(values) };
	}

	@Override
	protected void doCreatePreferences(Composite composite, int numColumns) {

		Group methodNegPushGroup = createGroup(numColumns, composite, "Method Negation Pushdown"); //$NON-NLS-1$

		var methodNegPushPref = createCheckboxPref(methodNegPushGroup, numColumns,
				CleanUpExtensions.METHOD_NEGATION_PUSHDOWN_DESCRIPTION, CleanUpExtensions.METHOD_NEGATION_PUSHDOWN,
				FALSE_TRUE);

		var methodNegPushCandidates = new ArrayList<CheckboxPreference>();
		for (var candidate : MethodNegationPushdownCandidate.values()) {
			intent(methodNegPushGroup);
			var candidatePref = createCheckboxPref(methodNegPushGroup, numColumns - 1, candidate.toString(),
					candidate.getId(), FALSE_TRUE);
			methodNegPushCandidates.add(candidatePref);
		}

		registerSlavePreference(methodNegPushPref, methodNegPushCandidates.toArray(CheckboxPreference[]::new));
		registerPreference(methodNegPushPref);
	}
}