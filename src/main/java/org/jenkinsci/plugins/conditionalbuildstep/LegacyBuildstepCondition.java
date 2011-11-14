/**
 * 
 */
package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Extension;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Legacy condition to ease migration from old condition to new condition mecano
 * implemented by the run-condition-plugin
 * 
 * @author domi
 * @deprecated keep to retain backward compatibility
 */
@Deprecated
public class LegacyBuildstepCondition extends RunCondition {

	private static Logger log = Logger.getLogger(LegacyBuildstepCondition.class.getName());

	private final String condition;
	private final boolean invertCondition;

	@DataBoundConstructor
	public LegacyBuildstepCondition(String condition, boolean invert) {
		this.condition = condition;
		this.invertCondition = invert;
	}

	public String getCondition() {
		return condition;
	}

	public boolean isInvertCondition() {
		return invertCondition;
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
			return Messages.legacycondition_displayName();
		}

		/**
		 * Performs on-the-fly validation of the form field 'condition'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation.
		 */
		public FormValidation doCheckCondition(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0) {
				return FormValidation.error("Please define a condition");
			}
			if (!value.startsWith("${")) {
				return FormValidation.warning("do you realy want to hard code the condition?");
			}
			return FormValidation.ok();
		}

	}
}
