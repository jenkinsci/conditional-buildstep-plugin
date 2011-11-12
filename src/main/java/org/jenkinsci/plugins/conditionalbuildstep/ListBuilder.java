package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.List;

public class ListBuilder extends Builder {

	private final List<Builder> conditionalbuilders;

	public ListBuilder(final List<Builder> conditionalbuilders) {
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

}
