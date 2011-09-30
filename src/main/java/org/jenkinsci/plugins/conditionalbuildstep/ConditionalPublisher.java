/**
 * 
 */
package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author domi
 * 
 */
public class ConditionalPublisher extends Notifier {

	private static Logger log = Logger.getLogger(ConditionalBuilder.class.getName());

	private final String condition;
	private final boolean invertCondition;

	private List<Publisher> conditionalpublishers = new ArrayList<Publisher>();

	@DataBoundConstructor
	public ConditionalPublisher(String condition, boolean invert) {
		this.condition = condition;
		this.invertCondition = invert;
	}

	public String getCondition() {
		return condition;
	}

	public boolean isInvertCondition() {
		return invertCondition;
	}

	public List<Publisher> getConditionalpublishers() {
		return conditionalpublishers;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		System.out.println("-->trigger notifiers...");
		listener.getLogger().println("trigger notifiers...");
		return true;
	}

	/**
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(ConditionalPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Conditional Publisher";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return false;
		}

		/**
		 * Performs on-the-fly validation of the form field 'condition'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation.
		 */
		public FormValidation doCheckCondition(@QueryParameter String value) throws IOException, ServletException {
			return Utility.doCheckCondition(value);
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			ConditionalPublisher instance = req.bindJSON(ConditionalPublisher.class, formData);
			instance.conditionalpublishers = Descriptor.newInstancesFromHeteroList(req, formData, "conditionalpublishers", Publisher.all());
			return instance;
		}

		public static List<Descriptor<Publisher>> getBuilderDescriptors(AbstractProject<?, ?> project) {
			final List<Descriptor<Publisher>> descriptors = BuildStepDescriptor.filter(Publisher.all(), project.getClass());
			final List<Descriptor<Publisher>> filteredDescriptors = new ArrayList<Descriptor<Publisher>>();
			for (Descriptor<Publisher> descriptor : descriptors) {
				if (!descriptor.getClass().equals(DescriptorImpl.class) && !descriptor.getClass().equals(NestingPublisher.DescriptorImpl.class)) {
					filteredDescriptors.add(descriptor);
				}
			}
			return filteredDescriptors;
		}

	}
}
