package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * 
 * @author domi (imod)
 */
public class ConditionalBuilder extends Builder {
	private static Logger log = Logger.getLogger(ConditionalBuilder.class.getName());

	private final String condition;
	private final boolean invertCondition;

	private List<Builder> conditionalbuilders = new ArrayList<Builder>();

	@DataBoundConstructor
	public ConditionalBuilder(String condition, boolean invert) {
		this.condition = condition;
		this.invertCondition = invert;
	}

	public String getCondition() {
		return condition;
	}

	public boolean isInvertCondition() {
		return invertCondition;
	}

	public List<Builder> getConditionalbuilders() {
		return conditionalbuilders;
	}

	public void setConditionalbuilders(List<Builder> conditionalbuilders) {
		this.conditionalbuilders = conditionalbuilders;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

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
		if (!execute) {
			return true;
		}

		boolean shouldContinue = true;
		for (BuildStep buildStep : conditionalbuilders) {
			if (!shouldContinue) {
				break;
			}
			shouldContinue = buildStep.prebuild(build, listener);
		}

		// execute build step, stop processing if indicated
		for (BuildStep buildStep : conditionalbuilders) {
			if (!shouldContinue) {
				break;
			}
			shouldContinue = buildStep.perform(build, launcher, listener);
		}
		return shouldContinue;
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

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		/**
		 * Performs on-the-fly validation of the form field 'condition'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
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

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Conditional Steps";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			ConditionalBuilder instance = req.bindJSON(ConditionalBuilder.class, formData);
			instance.conditionalbuilders = Descriptor.newInstancesFromHeteroList(req, formData, "conditionalbuilders", Builder.all());
			return instance;
		}

		public static List<Descriptor<Builder>> getBuilderDescriptors(AbstractProject<?, ?> project) {
			final List<Descriptor<Builder>> descriptors = BuildStepDescriptor.filter(Builder.all(), project.getClass());
			final List<Descriptor<Builder>> filteredDescriptors = new ArrayList<Descriptor<Builder>>();
			for (Descriptor<Builder> descriptor : descriptors) {
				if (!descriptor.getClass().equals(DescriptorImpl.class)) {
					filteredDescriptors.add(descriptor);
				}
			}
			return filteredDescriptors;
		}
	}

}
