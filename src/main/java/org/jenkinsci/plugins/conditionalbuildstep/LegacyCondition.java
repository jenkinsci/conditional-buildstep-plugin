/**
 * 
 */
package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Legacy condition to ease migration from old condition to new condition mecano
 * implemented by the run-condition-plugin
 * 
 * @author domi
 */
public class LegacyCondition extends RunCondition {

	private static Logger log = Logger.getLogger(LegacyCondition.class.getName());

	private final String condition;
	private final boolean invertCondition;

	@DataBoundConstructor
	public LegacyCondition(String condition, boolean invert) {
		this.condition = condition;
		this.invertCondition = invert;
	}

	@Override
	public boolean runPerform(AbstractBuild<?, ?> build, BuildListener listener) throws Exception {
		return shouldRun(build, listener);
	}

	@Override
	public boolean runPrebuild(AbstractBuild<?, ?> build, BuildListener listener) throws Exception {
		return shouldRun(build, listener);
	}

	private boolean shouldRun(AbstractBuild<?, ?> build, BuildListener listener) throws IOException, InterruptedException {
		String resolvedCondition = condition;
		try {
			resolvedCondition = TokenMacro.expand(build, listener, condition);
		} catch (MacroEvaluationException e) {

			log.log(Level.FINE, "failed to resolve condition via TokenMacro: {0}", e.getMessage());

			final VariableResolver<String> variableResolver = build.getBuildVariableResolver();
			resolvedCondition = resolveVariable(variableResolver, condition);
		}

		resolvedCondition = resolvedCondition == null ? condition : resolvedCondition;
		final boolean execute = invertCondition ? !"true".equalsIgnoreCase(resolvedCondition.trim()) : "true".equalsIgnoreCase(resolvedCondition.trim());
		listener.getLogger().println(
				"ConditionalStep  [" + condition + "] evaluated to [" + resolvedCondition + "] (invert: " + invertCondition + ") execute --> " + execute);

		return execute;

	}

	public static String resolveVariable(VariableResolver<String> variableResolver, String potentalVaraible) {
		String value = potentalVaraible;
		if (potentalVaraible != null) {
			if (potentalVaraible.startsWith("${") && potentalVaraible.endsWith("}")) {
				value = potentalVaraible.substring(2, potentalVaraible.length() - 1);
				value = variableResolver.resolve(value);
				log.log(Level.FINE, "resolve " + potentalVaraible + " to " + value);
			}
		}
		return value;
	}

	@Extension
	public static class LegacyConditionDescriptor extends RunConditionDescriptor {

		@Override
		public String getDisplayName() {
			return "Legacy condition (deprecated)";
		}

	}
}
