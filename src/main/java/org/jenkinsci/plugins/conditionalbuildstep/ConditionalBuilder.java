package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkinsci.plugins.conditionalbuildstep.lister.BuilderDescriptorLister;
import org.jenkinsci.plugins.conditionalbuildstep.lister.DefaultBuilderDescriptorLister;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
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
		return runner.prebuild(runCondition, new BuilderChain(conditionalbuilders), build, listener);
	}

	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
		return runner.perform(runCondition, new BuilderChain(conditionalbuilders), build, launcher, listener);
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

		private BuilderDescriptorLister builderLister;

		public DescriptorImpl() {
			if (builderLister == null) {
				builderLister = new DefaultBuilderDescriptorLister();
			}
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

		public List<? extends Descriptor<? extends BuildStep>> getBuilderDescriptors(AbstractProject<?, ?> project) {
			if (project == null)
				project = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
			return builderLister.getAllowedBuilders(project);
		}

		public DescriptorExtensionList<BuildStepRunner, BuildStepRunner.BuildStepRunnerDescriptor> getBuildStepRunners() {
			return BuildStepRunner.all();
		}

		public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
			return RunCondition.all();
		}

		// public List<? extends Descriptor<? extends BuildStep>>
		// getAllowedBuilders(AbstractProject<?, ?> project) {
		// if (project == null)
		// project =
		// Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
		// return builderLister.getAllowedBuilders(project);
		// }

		public Object readResolve() {
			if (builderLister == null) {
				builderLister = new DefaultBuilderDescriptorLister();
			}
			return this;
		}

	}

}
