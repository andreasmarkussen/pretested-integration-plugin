package org.jenkinsci.plugins.pretestedintegration.integration.scm.git;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.CleanCheckout;
import hudson.plugins.git.extensions.impl.PruneStaleBranch;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;


import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import org.jenkinsci.plugins.pretestedintegration.PretestedIntegrationBuildWrapper;
import org.jenkinsci.plugins.pretestedintegration.PretestedIntegrationPostCheckout;
import static org.jenkinsci.plugins.pretestedintegration.integration.scm.git.TestUtilsFactory.STRATEGY_TYPE;
import org.jenkinsci.plugins.pretestedintegration.scm.git.AccumulatedCommitStrategy;
import org.jenkinsci.plugins.pretestedintegration.scm.git.GitBridge;
import org.jenkinsci.plugins.pretestedintegration.scm.git.SquashCommitStrategy;

/**
 * Basically checks that you are not allowed to try to use 'master'-branch
 * as the one you will integrate, as this would delete the 'master'-branch.
 * We do not allow this configuration, even though it could be valid, just to
 * protect most people from doing wrong configuration.
 */
public class DoNotAllowMasterBranchAsReadyBranchIT {
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void integrateMasterBranch() throws Exception {
        
        Repository repo = TestUtilsFactory.createRepoWithoutBranches("master");

        List<GitSCMExtension> gitSCMExtensions = new ArrayList<GitSCMExtension>();
        gitSCMExtensions.add(new PruneStaleBranch());
        gitSCMExtensions.add(new CleanCheckout());
        
        SCM gitSCM1 = new GitSCM(Collections.singletonList(new UserRemoteConfig("file://" + repo.getDirectory().getAbsolutePath(), null, null, null)),
                Collections.singletonList(new BranchSpec("master")),
                false, Collections.<SubmoduleConfig>emptyList(),
                null, null, gitSCMExtensions);

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.setScm(gitSCM1);
        GitBridge gitBridge = new GitBridge(new SquashCommitStrategy(), "master", "origin");

        project.getBuildWrappersList().add(new PretestedIntegrationBuildWrapper(gitBridge));
        project.getPublishersList().add(new PretestedIntegrationPostCheckout());

        TestUtilsFactory.triggerProject(project);

        jenkinsRule.waitUntilNoActivityUpTo(60000);
        
        FreeStyleBuild build1 = project.getLastBuild();
        
        TestUtilsFactory.destroyRepo(repo); // clean repo here before assert, don't need it any more and ensure we remember to clean
        
        System.out.println("==build1 actions==");
        for(BuildData action : build1.getActions(BuildData.class)) {            
            System.out.println(action.lastBuild.revision.getBranches().iterator().next().getName());
        }
        String console = jenkinsRule.createWebClient().getPage(build1, "console").asText();
        System.out.println("===CONSOLE===");
        System.out.println(console);
        System.out.println("===CONSOLE===");  
        
        System.out.println("===Result check 1==="); 
        assertTrue(console.contains("Using the master branch for polling and development is not" +
                    " allowed since it will attempt to merge it to other branches and delete it after."));
        System.out.println("===Result check 2==="); 
        System.out.println("===Result check 2===");
        assertTrue(build1.getResult().equals(Result.NOT_BUILT));
        System.out.println("===Result check done==="); 


        

    }
    
    
}