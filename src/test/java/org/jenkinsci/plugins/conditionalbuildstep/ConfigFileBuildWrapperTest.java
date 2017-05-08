package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;

import java.util.List;

import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.BooleanCondition;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ConfigFileBuildWrapperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void conditionalBuildersInMavenProjectMustBeResolvable() throws Exception {

        final MavenModuleSet p = j.createProject(MavenModuleSet.class, "mvn");
        p.setRunHeadless(true);

        ConditionalBuilder cBuilder = new ConditionalBuilder(new BooleanCondition("true"), new BuildStepRunner.Run());
        Shell shell = new Shell("ls");
        cBuilder.getConditionalbuilders().add(shell);
        p.getPrebuilders().add(cBuilder);

        final List<Shell> containedBuilders = ConditionalBuildStepHelper.getContainedBuilders(p, Shell.class);
        Assert.assertNotNull("no builders returned", containedBuilders);
        Assert.assertEquals("not correct nummber of builders returned", 1, containedBuilders.size());
    }

    @Test
    public void conditionalBuildersInFreestyleProjectMustBeResolvable() throws Exception {

        final FreeStyleProject p = j.createFreeStyleProject();

        ConditionalBuilder cBuilder = new ConditionalBuilder(new BooleanCondition("true"), new BuildStepRunner.Run());
        Shell shell = new Shell("ls");
        Shell shell2 = new Shell("ls");
        cBuilder.getConditionalbuilders().add(shell);
        cBuilder.getConditionalbuilders().add(shell2);
        p.getBuildersList().add(cBuilder);

        final List<Shell> containedBuilders = ConditionalBuildStepHelper.getContainedBuilders(p, Shell.class);
        Assert.assertNotNull("no builders returned", containedBuilders);
        Assert.assertEquals("not correct nummber of builders returned", 2, containedBuilders.size());
    }

}
