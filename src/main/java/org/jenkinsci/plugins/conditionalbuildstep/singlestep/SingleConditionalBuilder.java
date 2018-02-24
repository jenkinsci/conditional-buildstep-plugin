/*
 * The MIT License
 *
 * Copyright (C) 2011 by Anthony Robinson
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

package org.jenkinsci.plugins.conditionalbuildstep.singlestep;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.DependencyGraph;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jenkins.model.DependencyDeclarer;
import net.sf.json.JSONObject;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkinsci.plugins.conditionalbuildstep.Messages;
import org.jenkinsci.plugins.conditionalbuildstep.dependency.ConditionalDependencyGraphWrapper;
import org.jenkinsci.plugins.conditionalbuildstep.elsecondition.SingleIfElseBlock;
import org.jenkinsci.plugins.conditionalbuildstep.elsecondition.SingleIfElseBlock.SingleIfElseBlockDescriptor;
import org.jenkinsci.plugins.conditionalbuildstep.lister.BuilderDescriptorLister;
import org.jenkinsci.plugins.conditionalbuildstep.lister.DefaultBuilderDescriptorLister;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * @author Anthony Robinson
 * @author Dominik Bartholdi (imod)
 */
public class SingleConditionalBuilder extends Builder implements DependencyDeclarer {

    public static final String PROMOTION_JOB_TYPE = "hudson.plugins.promoted_builds.PromotionProcess";

    private final RunCondition condition;
    private final BuildStep buildStep;
    private final BuildStepRunner runner;
    private List<SingleIfElseBlock> conditionalBlocks;
    private boolean useElse;
    private BuildStep elseBuildStep;

    @DataBoundConstructor
    public SingleConditionalBuilder(final BuildStep buildStep, final RunCondition condition, final BuildStepRunner runner,final List<SingleIfElseBlock> conditionalBlocks,final boolean useElse,final BuildStep elseBuildStep) {
        this.buildStep = buildStep;
        this.condition = condition;
        this.runner = runner;
        this.conditionalBlocks = conditionalBlocks;
        this.useElse = useElse;
        this.elseBuildStep = elseBuildStep;
    }

    public BuildStep getBuildStep() {
        return buildStep;
    }

    public RunCondition getCondition() {
        return condition;
    }

    public BuildStepRunner getRunner() {
        return runner;
    }
    
    public List<SingleIfElseBlock> getConditionalBlocks(){
    	if(conditionalBlocks==null) this.conditionalBlocks = new ArrayList<SingleIfElseBlock>();
    	return conditionalBlocks;
    }
    
    public boolean getUseElse() {
        return useElse;
    }

    public BuildStep getElseBuildStep() {
        return elseBuildStep;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return buildStep == null ? BuildStepMonitor.NONE : buildStep.getRequiredMonitorService();
    }

    @Override
    public Collection getProjectActions(final AbstractProject<?, ?> project) {
        return buildStep == null ? Collections.emptyList() : buildStep.getProjectActions(project);
    }

    @Override
    public boolean prebuild(final AbstractBuild<?, ?> build, final BuildListener listener) {
		try {
			if(condition.runPerform(build, listener)){
				return buildStep.prebuild(build, listener);
			}
			if(conditionalBlocks!=null){
				for(SingleIfElseBlock block: conditionalBlocks){
					RunCondition runCondition = block.getRunCondition();
					if(runCondition!=null && runCondition.runPerform(build, listener)){
						BuildStep ifElseBuildStep = block.getBuildStep();
						if(ifElseBuildStep!=null){
							return ifElseBuildStep.prebuild(build, listener);
						}
					}
				}
			}
			if(useElse && elseBuildStep!=null){
				return elseBuildStep.prebuild(build, listener);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return false;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
		try {
			if(condition.runPerform(build, listener)){
				return buildStep.perform(build, launcher, listener);
			}
			if(conditionalBlocks!=null){
				for(SingleIfElseBlock block: conditionalBlocks){
					RunCondition runCondition = block.getRunCondition();
					if(runCondition!=null && runCondition.runPerform(build, listener)){
						BuildStep ifElseBuildStep = block.getBuildStep();
						if(ifElseBuildStep!=null){
							return ifElseBuildStep.perform(build, launcher, listener);
						}
					}
				}
			}
			if(useElse&&elseBuildStep!=null){
				return elseBuildStep.perform(build, launcher, listener);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return false;
    }
    
    public void buildDependencyGraph(AbstractProject project, DependencyGraph graph) {
        if(buildStep != null) {
            if(buildStep instanceof DependencyDeclarer) {
                DependencyDeclarer dependencyDeclarer = (DependencyDeclarer) buildStep;
                dependencyDeclarer.buildDependencyGraph(project, new ConditionalDependencyGraphWrapper(graph, condition, runner));
            }
        }
    }

    @Extension(ordinal = Integer.MAX_VALUE - 500)
    public static class SingleConditionalBuilderDescriptor extends BuildStepDescriptor<Builder> {

        public static DescriptorExtensionList<BuilderDescriptorLister, Descriptor<BuilderDescriptorLister>> getAllBuilderDescriptorListers() {
            return Hudson.getInstance().<BuilderDescriptorLister, Descriptor<BuilderDescriptorLister>> getDescriptorList(BuilderDescriptorLister.class);
        }

        private BuilderDescriptorLister builderLister;

        @DataBoundConstructor
        public SingleConditionalBuilderDescriptor(final BuilderDescriptorLister builderLister) {
            this.builderLister = builderLister;
        }

        public SingleConditionalBuilderDescriptor() {
            load();
            if (builderLister == null)
                builderLister = new DefaultBuilderDescriptorLister();
        }

        public BuilderDescriptorLister getBuilderLister() {
            return builderLister;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            final SingleConditionalBuilderDescriptor newConfig = req.bindJSON(SingleConditionalBuilderDescriptor.class, json);
            if (newConfig.builderLister != null)
                builderLister = newConfig.builderLister;
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.singlestepbuilder_displayName();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // No need for aggregation for matrix build with MatrixAggregatable
            // this is only supported for: {@link Publisher}, {@link JobProperty}, {@link BuildWrapper}
            return !PROMOTION_JOB_TYPE.equals(aClass.getCanonicalName());
        }

        public DescriptorExtensionList<BuildStepRunner, BuildStepRunner.BuildStepRunnerDescriptor> getBuildStepRunners() {
            return BuildStepRunner.all();
        }

        public BuildStepRunner.BuildStepRunnerDescriptor getDefaultBuildStepRunner() {
            return Hudson.getInstance().getDescriptorByType(BuildStepRunner.Fail.FailDescriptor.class);
        }
        
        public SingleIfElseBlockDescriptor getIfElseBlockDescriptor() {
            return Hudson.getInstance().getDescriptorByType(SingleIfElseBlockDescriptor.class);
        }

        public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
            return RunCondition.all();
        }

        public RunCondition.RunConditionDescriptor getDefaultRunCondition() {
            return Hudson.getInstance().getDescriptorByType(AlwaysRun.AlwaysRunDescriptor.class);
        }

        public List<? extends Descriptor<? extends BuildStep>> getAllowedBuilders(AbstractProject<?, ?> project) {
            if (project == null)
                project = Stapler.getCurrentRequest().findAncestorObject(AbstractProject.class);
            return builderLister.getAllowedBuilders(project);
        }

        public Object readResolve() {
            if (builderLister == null)
                builderLister = new DefaultBuilderDescriptorLister();
            return this;
        }

    }

}