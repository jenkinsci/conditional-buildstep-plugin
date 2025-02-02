package org.jenkinsci.plugins.conditionalbuildstep;

import hudson.EnvVars;
import hudson.Functions;
import hudson.maven.MavenModuleSet;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.BooleanCondition;
import org.jenkinsci.plugins.conditionalbuildstep.elsecondition.SingleIfElseBlock;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;
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
        
        CommandInterpreter shell;
        if(!Functions.isWindows())
        	shell = new Shell("ls");
        else
        	shell = new BatchFile("dir");
        cBuilder.getConditionalbuilders().add(shell);
        p.getPrebuilders().add(cBuilder);

        final List<CommandInterpreter> containedBuilders = ConditionalBuildStepHelper.getContainedBuilders(p, CommandInterpreter.class);
        Assert.assertNotNull("no builders returned", containedBuilders);
        Assert.assertEquals("not correct nummber of builders returned", 1, containedBuilders.size());
    }

    @Test
    public void conditionalBuildersInFreestyleProjectMustBeResolvable() throws Exception {

        final FreeStyleProject p = j.createFreeStyleProject();

        ConditionalBuilder cBuilder = new ConditionalBuilder(new BooleanCondition("true"), new BuildStepRunner.Run());
        CommandInterpreter shell;
        CommandInterpreter shell2;
        
        if(!Functions.isWindows()) {
        	shell = new Shell("ls");
            shell2 = new Shell("ls");
        }
        else {
        	shell = new BatchFile("dir");
            shell2 = new BatchFile("dir");
        }
        cBuilder.getConditionalbuilders().add(shell);
        cBuilder.getConditionalbuilders().add(shell2);
        p.getBuildersList().add(cBuilder);

        final List<CommandInterpreter> containedBuilders = ConditionalBuildStepHelper.getContainedBuilders(p, CommandInterpreter.class);
        Assert.assertNotNull("no builders returned", containedBuilders);
        Assert.assertEquals("not correct nummber of builders returned", 2, containedBuilders.size());
    }
    
    @Test
    public void singleConditionalBuilderElseValidation() throws Exception {

        final FreeStyleProject p = j.createFreeStyleProject();
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        j.jenkins.getGlobalNodeProperties().add(prop);
        
        final String elseBlockString = "";
        envVars.put("TEST", "false");
        CommandInterpreter ifShell;
        CommandInterpreter elseShell;
        if(!Functions.isWindows()) {
        	ifShell = new Shell("echo \"Inside If Block\"");
        	elseShell = new Shell("echo \""+elseBlockString+"\"");
        }
        else {
        	ifShell = new BatchFile("echo \"Inside If Block\"");
        	elseShell = new BatchFile("echo \""+elseBlockString+"\"");
        }
        SingleConditionalBuilder scBuilder = new SingleConditionalBuilder(ifShell,new BooleanCondition("${TEST}"), new BuildStepRunner.Run(),null,true,elseShell);
        
        p.getBuildersList().add(scBuilder);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        String s = FileUtils.readFileToString(build.getLogFile());
        Assert.assertTrue("Else Block validation for singleConditionalBuilder failed.",
        		s.contains(elseBlockString));
    }
    
    @Test
    public void singleConditionalBuilderIfElseValidation() throws Exception {

        final FreeStyleProject p = j.createFreeStyleProject();
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        j.jenkins.getGlobalNodeProperties().add(prop);
        
        envVars.put("IFT", "false");
        envVars.put("IFELSET1", "true");
        envVars.put("IFELSET2", "true");
        
        CommandInterpreter ifShell;
        CommandInterpreter ifElseShell1;
        CommandInterpreter ifElseShell2;
        CommandInterpreter elseShell;
        if(!Functions.isWindows()) {
        	ifShell = new Shell("echo \"Inside If Block\"");
            ifElseShell1 = new Shell("echo \"Inside first If-Else Block\"");
            ifElseShell2 = new Shell("echo \"Inside second If-Else Block\"");
            elseShell = new Shell("echo \"Inside Else Block\"");
        }
        else {
        	ifShell = new BatchFile("echo \"Inside If Block\"");
        	ifElseShell1 = new BatchFile("echo \"Inside first If-Else Block\"");
            ifElseShell2 = new BatchFile("echo \"Inside second If-Else Block\"");
        	elseShell = new BatchFile("echo \"Inside Else Block\"");
        }
        
        List<SingleIfElseBlock> ifElseList = new ArrayList<SingleIfElseBlock>();
        ifElseList.add(new SingleIfElseBlock(ifElseShell1, new BooleanCondition("${IFELSET1}")));
        ifElseList.add(new SingleIfElseBlock(ifElseShell2, new BooleanCondition("${IFELSET2}")));
        SingleConditionalBuilder scBuilder = new SingleConditionalBuilder(ifShell,new BooleanCondition("${IFT}"), new BuildStepRunner.Run(),ifElseList,true,elseShell);
        
        p.getBuildersList().add(scBuilder);

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        String s = FileUtils.readFileToString(build.getLogFile());
        //Since the first if else block evaluates to true, it should be selected
        Assert.assertTrue("If Else Block validation for singleConditionalBuilder failed.",
        		s.contains("Inside first If-Else Block"));
    }

}
