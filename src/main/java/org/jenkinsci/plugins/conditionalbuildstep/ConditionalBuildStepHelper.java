package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.model.Project;
import hudson.tasks.BuildStep;

import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;

/**
 * Helper to work with {@link BuildStep}s wrapped by {@link ConditionalBuilder} or {@link SingleConditionalBuilder}.
 * 
 * @author Dominik Bartholdi (imod)
 * 
 */
public class ConditionalBuildStepHelper {

    private ConditionalBuildStepHelper() {
    }

    /**
     * Gets the list of all buildsteps wrapped within any {@link ConditionalBuilder} or {@link SingleConditionalBuilder} from within the given project.
     * 
     * @param p
     *            the project to get all wrapped builders for
     * @param type
     *            the type of builders to search for
     * @return a list of all buildsteps, never <code>null</code>
     */
    public static <T extends BuildStep> List<T> getContainedBuilders(Project<?, ?> p, Class<T> type) {

        List<T> r = new ArrayList<T>();

        List<ConditionalBuilder> cbuilders = p.getBuildersList().getAll(ConditionalBuilder.class);
        for (ConditionalBuilder conditionalBuilder : cbuilders) {
            final List<BuildStep> cbs = conditionalBuilder.getConditionalbuilders();
            if (cbs != null) {
                for (BuildStep buildStep : cbs) {
                    if (type.isInstance(buildStep)) {
                        r.add(type.cast(buildStep));
                    }
                }
            }
        }

        List<SingleConditionalBuilder> scb = p.getBuildersList().getAll(SingleConditionalBuilder.class);
        for (SingleConditionalBuilder singleConditionalBuilder : scb) {
            BuildStep buildStep = singleConditionalBuilder.getBuildStep();
            if (buildStep != null && type.isInstance(buildStep)) {
                r.add(type.cast(buildStep));
            }
        }

        return r;
    }

}
