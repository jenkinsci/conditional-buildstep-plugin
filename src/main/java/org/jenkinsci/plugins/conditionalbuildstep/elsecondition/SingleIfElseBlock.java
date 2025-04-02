package org.jenkinsci.plugins.conditionalbuildstep.elsecondition;

import java.util.List;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import org.jenkinsci.plugins.conditionalbuildstep.Messages;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder.SingleConditionalBuilderDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.tasks.BuildStep;
import net.sf.json.JSONObject;

public class SingleIfElseBlock extends ConditionalBlock{
	private BuildStep buildStep;
	private RunCondition runCondition;
	
	@DataBoundConstructor
	public SingleIfElseBlock(BuildStep buildStep,RunCondition runCondition) {
		this.buildStep = buildStep;
		this.runCondition = runCondition;
	}

	public BuildStep getBuildStep() {
		return buildStep;
	}

	public RunCondition getRunCondition() {
		return runCondition;
	}
	
	@Override
	public ConditionalBlockDescriptor getDescriptor() {
		return (SingleIfElseBlockDescriptor)Hudson.getInstance().getDescriptor(getClass());
	}
	
	@Extension
	public static final class SingleIfElseBlockDescriptor extends ConditionalBlockDescriptor{
		
		@Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return true;
        }
		
		public List<? extends Descriptor<? extends BuildStep>> getAllowedBuilders(AbstractProject<?, ?> project){
            final SingleConditionalBuilderDescriptor singleConditionalStepDescriptor = Hudson.getInstance().getDescriptorByType(
                    SingleConditionalBuilderDescriptor.class);
            return singleConditionalStepDescriptor.getAllowedBuilders(project);
		}
		
		@Override
		public String getDisplayName() {
			return Messages.conditionalblock_ifelseblock();
		}
		
		public List<? extends Descriptor<? extends RunCondition>> getRunConditions() {
            return RunCondition.all();
        }

        public RunCondition.RunConditionDescriptor getDefaultRunCondition() {
            return Hudson.getInstance().getDescriptorByType(AlwaysRun.AlwaysRunDescriptor.class);
        }
		
	}
}
