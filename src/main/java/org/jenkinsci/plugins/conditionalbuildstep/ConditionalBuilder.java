package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A buildstep wrapping any number of other buildsteps, controlling there
 * execution based on a defined condition.
 * 
 * @author domi (imod)
 */
public class ConditionalBuilder extends Builder {
	private static Logger log = Logger.getLogger(ConditionalBuilder.class.getName());

	// retaining backward compatibility
	private transient String condition;
	private transient boolean invertCondition;

	private final BuildStepRunner runner;
	private RunCondition runCondition;
	private List<Builder> conditionalbuilders = new ArrayList<Builder>();

	@DataBoundConstructor
	public ConditionalBuilder(RunCondition runCondition, final BuildStepRunner runner) {
		this.runner = runner;
		this.runCondition = runCondition;
	}

	public BuildStepRunner getRunner() {
		return runner;
	}

	public RunCondition getRunCondition() {
		return runCondition;
	}

	public List<Builder> getConditionalbuilders() {
		return conditionalbuilders;
	}

	public void setConditionalbuilders(List<Builder> conditionalbuilders) {
		this.conditionalbuilders = conditionalbuilders;
	}

	public boolean prebuild(final AbstractBuild<?, ?> build, final BuildListener listener) {
		return runner.prebuild(runCondition, new ListBuilder(conditionalbuilders), build, listener);
	}

	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
		return runner.perform(runCondition, new ListBuilder(conditionalbuilders), build, launcher, listener);
	}

	public Object readResolve() {
		if (condition != null) {
			// retaining backward compatibility
			this.runCondition = new LegacyBuildstepCondition(condition, invertCondition);
		}
		return this;
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

		public DescriptorExtensionList<BuildStepRunner, BuildStepRunner.BuildStepRunnerDescriptor> getBuildStepRunners() {
			return BuildStepRunner.all();
		}

		public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
			return RunCondition.all();
		}

	}

}
