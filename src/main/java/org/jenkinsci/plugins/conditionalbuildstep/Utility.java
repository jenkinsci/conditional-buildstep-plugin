package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;

public class Utility {

	public static FormValidation doCheckCondition(@QueryParameter String value) throws IOException, ServletException {
		if (value.length() == 0) {
			return FormValidation.error("Please define a condition");
		}
		if (!value.startsWith("${")) {
			return FormValidation.warning("do you realy want to hard code the condition?");
		}
		return FormValidation.ok();
	}

}
