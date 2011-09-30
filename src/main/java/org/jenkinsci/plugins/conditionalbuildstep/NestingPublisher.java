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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.conditionalbuildstep.ConditionalPublisher.DescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author domi
 * 
 */
public class NestingPublisher extends Notifier {

	private static Logger log = Logger.getLogger(ConditionalBuilder.class.getName());

	private final String condition;
	private final boolean invertCondition;

	private List<Publisher> nestingpublishers = new ArrayList<Publisher>();

	@DataBoundConstructor
	public NestingPublisher(String condition, boolean invert) {
		this.condition = condition;
		this.invertCondition = invert;
	}

	public String getCondition() {
		return condition;
	}

	public boolean isInvertCondition() {
		return invertCondition;
	}

	public List<Publisher> getNestingpublishers() {
		return nestingpublishers;
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
			super(NestingPublisher.class);
		}

		@Override
		public String getDisplayName() {
			return "Nesting Publisher";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			NestingPublisher instance = req.bindJSON(NestingPublisher.class, formData);
			instance.nestingpublishers = Descriptor.newInstancesFromHeteroList(req, formData, "nestingpublishers", Publisher.all());
			return instance;
		}

		public static List<Descriptor<Publisher>> getBuilderDescriptors(AbstractProject<?, ?> project) {
			final List<Descriptor<Publisher>> descriptors = new ArrayList<Descriptor<Publisher>>();
			final Descriptor conditionalPublisherDescriptor = BuildStepDescriptor.find(ConditionalPublisher.DescriptorImpl.class.getName());
			descriptors.add(conditionalPublisherDescriptor);
			return descriptors;
		}

	}
}
