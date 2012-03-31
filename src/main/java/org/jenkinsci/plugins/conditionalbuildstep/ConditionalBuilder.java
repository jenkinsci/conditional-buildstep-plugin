/*
 * The MIT License
 *
 * Copyright (C) 2011 by Dominik Bartholdi
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
package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder.SingleConditionalBuilderDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * A buildstep wrapping any number of other buildsteps, controlling there execution based on a defined condition.
 * 
 * @author Dominik Bartholdi (imod)
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

    @Override
    public boolean prebuild(final AbstractBuild<?, ?> build, final BuildListener listener) {
        return runner.prebuild(runCondition, new BuilderChain(conditionalbuilders), build, listener);
    }

    @Override
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

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // No need for aggregation for matrix build with MatrixAggregatable
            // this is only supported for: {@link Publisher}, {@link JobProperty}, {@link BuildWrapper}
            return !SingleConditionalBuilder.PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return Messages.multistepbuilder_displayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
            ConditionalBuilder instance = req.bindJSON(ConditionalBuilder.class, formData);
            if (formData.opt("conditionalbuilders") != null) {
                final List all = new ArrayList(Builder.all());
                // as Any Build step also allows publishers to be used, we have to pass the publisher descriptors too...
                all.addAll(Publisher.all());
                instance.conditionalbuilders = Descriptor.newInstancesFromHeteroList(req, formData, "conditionalbuilders", all);
            }
            return instance;
        }

        public List<? extends Descriptor<? extends BuildStep>> getBuilderDescriptors(AbstractProject<?, ?> project) {
            final SingleConditionalBuilderDescriptor singleConditionalStepDescriptor = Hudson.getInstance().getDescriptorByType(
                    SingleConditionalBuilderDescriptor.class);
            return singleConditionalStepDescriptor.getAllowedBuilders(project);
        }

        public DescriptorExtensionList<BuildStepRunner, BuildStepRunner.BuildStepRunnerDescriptor> getBuildStepRunners() {
            return BuildStepRunner.all();
        }

        public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
            return RunCondition.all();
        }

    }

}
