package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.List;

/**
 * A builder not directly configurable via UI, instances are only created for
 * transitive usage to wrap the execution of multiple builders.
 * 
 * @author domi
 * 
 */
public class BuilderChain extends Builder {

	private final List<Builder> conditionalbuilders;

	public BuilderChain(final List<Builder> conditionalbuilders) {
		this.conditionalbuilders = conditionalbuilders;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		boolean shouldContinue = true;
		for (BuildStep buildStep : conditionalbuilders) {
			if (!shouldContinue) {
				break;
			}
			shouldContinue = buildStep.prebuild(build, listener);
		}
		return shouldContinue;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		boolean shouldContinue = true;
		for (BuildStep buildStep : conditionalbuilders) {
			if (!shouldContinue) {
				break;
			}
			shouldContinue = buildStep.perform(build, launcher, listener);
		}
		return shouldContinue;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return false;
		}

		@Override
		public String getDisplayName() {
			return "BuilderChain";
		}

	}
}
