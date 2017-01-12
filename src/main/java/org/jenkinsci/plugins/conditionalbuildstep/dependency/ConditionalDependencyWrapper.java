

/*
 * The MIT License
 *
 * Copyright (c) 2013 IKEDA Yasuyuki
 * This class originates from the class with the same name in flexible publish plugin:
 * https://github.com/jenkinsci/flexible-publish-plugin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.conditionalbuildstep.dependency;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import hudson.Launcher;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.NullStream;

/**
 * Wraps {@link Dependency} and evaluates {@link RunCondition} when the dependency is triggered.
 */
public class ConditionalDependencyWrapper extends Dependency {
    private static Logger LOGGER = Logger.getLogger(ConditionalDependencyWrapper.class.getName());
    private Dependency dep;
    private RunCondition condition;
    private BuildStepRunner runner;

    public ConditionalDependencyWrapper(Dependency dep, RunCondition condition, BuildStepRunner runner) {
        super(dep.getUpstreamProject(), dep.getDownstreamProject());
        this.dep = dep;
        this.condition = condition;
        this.runner = runner;
    }

    /**
     * Determines whether the downstream project should be launched.
     *
     * {@link RunCondition} is evaluated.
     *
     * @see hudson.model.DependencyGraph.Dependency#shouldTriggerBuild(hudson.model.AbstractBuild, hudson.model.TaskListener, java.util.List)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean shouldTriggerBuild(AbstractBuild build,
                                      TaskListener listener, List<Action> actions) {
        BuildListener buildListener = null;
        if (listener instanceof BuildListener) {
            buildListener = (BuildListener)listener;
        } else {
            // Usually listener is instance of BuildListener,
            // So there may be no case entering this path.
            // If there's that case, BuildLister wrapping TaskListener should be written.
            LOGGER.warning("There is no BuildListener, and logs from RunCondition won't be recorded.");
            buildListener = new StreamBuildListener(new NullStream());
        }

        try {
            MarkPerformedBuilder marker = new MarkPerformedBuilder();

            // launcher is not used by condition or runner or marker,
            // this never cause NPE.
            Launcher launcher = null;
            runner.perform(condition, marker, build, launcher, buildListener);

            if (marker.isPerformed()) {
                return dep.shouldTriggerBuild(build, listener, actions);
            } else {
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to evaluate condition", e);
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ConditionalDependencyWrapper d = (ConditionalDependencyWrapper)obj;

        return dep.equals(d.dep) && condition.equals(d.condition);
    }

    @Override
    public int hashCode() {
        return dep.hashCode() * 23 + condition.hashCode();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public AbstractProject getDownstreamProject() {
        return dep.getDownstreamProject();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public AbstractProject getUpstreamProject() {
        return dep.getUpstreamProject();
    }

    @Override
    public boolean pointsItself() {
        return dep.pointsItself();
    }

    /**
     * Used with {@link BuildStepRunner}.
     *
     * Stores whether perform is executed.
     */
    private static class MarkPerformedBuilder extends Builder {
        private boolean performed = false;

        public boolean isPerformed() {
            return performed;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                               BuildListener listener) throws InterruptedException, IOException {
            performed = true;
            return true;
        }

        private static final Descriptor<Builder> DESCRIPTOR =
                new BuildStepDescriptor<Builder>() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                        return true;
                    }

                    @Override
                    public String getDisplayName() {
                        return "Builder to mark whether executed";
                    }
                };
        @Override
        public Descriptor<Builder> getDescriptor() {
            return DESCRIPTOR;
        }
    };
}
