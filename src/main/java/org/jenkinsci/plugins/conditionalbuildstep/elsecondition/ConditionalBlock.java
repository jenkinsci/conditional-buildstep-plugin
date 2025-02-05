package org.jenkinsci.plugins.conditionalbuildstep.elsecondition;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;

public abstract class ConditionalBlock implements Describable<ConditionalBlock>,ExtensionPoint{
	public static DescriptorExtensionList<ConditionalBlock, ConditionalBlockDescriptor> all() {
        return Hudson.getInstance().<ConditionalBlock, ConditionalBlockDescriptor>getDescriptorList(ConditionalBlock.class);
    }
	
	public abstract Descriptor<ConditionalBlock> getDescriptor();
	
	public static abstract class ConditionalBlockDescriptor extends Descriptor<ConditionalBlock>{
		
		protected ConditionalBlockDescriptor() { }

        protected ConditionalBlockDescriptor(Class<? extends ConditionalBlock> clazz) {
            super(clazz);
        }
	}
}
