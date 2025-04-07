package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.BooleanCondition;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class ConfigFileBuildWrapperTest {

    @Test
    void conditionalBuildersInMavenProjectMustBeResolvable(JenkinsRule j) throws Exception {
        final MavenModuleSet p = j.createProject(MavenModuleSet.class, "mvn");
        p.setRunHeadless(true);

        ConditionalBuilder cBuilder = new ConditionalBuilder(new BooleanCondition("true"), new BuildStepRunner.Run());
        Shell shell = new Shell("ls");
        cBuilder.getConditionalbuilders().add(shell);
        p.getPrebuilders().add(cBuilder);

        final List<Shell> containedBuilders = ConditionalBuildStepHelper.getContainedBuilders(p, Shell.class);
        assertNotNull(containedBuilders, "no builders returned");
        assertEquals(1, containedBuilders.size(), "not correct number of builders returned");
    }

    @Test
    void conditionalBuildersInFreestyleProjectMustBeResolvable(JenkinsRule j) throws Exception {
        final FreeStyleProject p = j.createFreeStyleProject();

        ConditionalBuilder cBuilder = new ConditionalBuilder(new BooleanCondition("true"), new BuildStepRunner.Run());
        Shell shell = new Shell("ls");
        Shell shell2 = new Shell("ls");
        cBuilder.getConditionalbuilders().add(shell);
        cBuilder.getConditionalbuilders().add(shell2);
        p.getBuildersList().add(cBuilder);

        final List<Shell> containedBuilders = ConditionalBuildStepHelper.getContainedBuilders(p, Shell.class);
        assertNotNull(containedBuilders, "no builders returned");
        assertEquals(2, containedBuilders.size(), "not correct number of builders returned");
    }
}
