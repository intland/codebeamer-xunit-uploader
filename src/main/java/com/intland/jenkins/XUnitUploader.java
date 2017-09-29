/*
 * Copyright (c) 2017 Intland Software (support@intland.com)
 *
 */
package com.intland.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.intland.codebeamer.api.client.CodebeamerApiConfiguration;
import com.intland.codebeamer.api.client.XUnitFileCollector;
import com.intland.codebeamer.api.client.dto.TrackerDto;
import com.intland.codebeamer.api.client.dto.TrackerItemDto;
import com.intland.codebeamer.api.client.dto.TrackerTypeDto;
import com.intland.codebeamer.api.client.rest.RequestFailed;
import com.intland.codebeamer.api.client.rest.RestAdapter;
import com.intland.codebeamer.api.client.trackertypes.*;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

public class XUnitUploader extends Notifier implements SimpleBuildStep {
    private String codebeamerUrl;
    private String credentialsId;
    private Integer testConfigurationId;
    private Integer testCaseTrackerId;
    private Integer testSetTrackerId;
    private Integer testRunTrackerId;
    private String testResultsDir;

    @DataBoundConstructor
    public XUnitUploader(String codebeamerUrl, String credentialsId, Integer testConfigurationId, Integer testCaseTrackerId, Integer testSetTrackerId, Integer testRunTrackerId, String testResultsDir) {
        this.codebeamerUrl = codebeamerUrl;
        this.credentialsId = credentialsId;
        this.testConfigurationId = testConfigurationId;
        this.testCaseTrackerId = testCaseTrackerId;
        this.testSetTrackerId = testSetTrackerId;
        this.testRunTrackerId = testRunTrackerId;
        this.testResultsDir = testResultsDir;
    }

    public Integer getTestConfigurationId() {
        return testConfigurationId;
    }

    @DataBoundSetter
    public void setTestConfigurationId(Integer testConfigurationId) {
        this.testConfigurationId = testConfigurationId;
    }

    public Integer getTestSetTrackerId() {
        return testSetTrackerId;
    }

    @DataBoundSetter
    public void setTestSetTrackerId(Integer testSetTrackerId) {
        this.testSetTrackerId = testSetTrackerId;
    }

    public Integer getTestCaseTrackerId() {
        return testCaseTrackerId;
    }

    @DataBoundSetter
    public void setTestCaseTrackerId(Integer testCaseTrackerId) {
        this.testCaseTrackerId = testCaseTrackerId;
    }

    public Integer getTestRunTrackerId() {
        return testRunTrackerId;
    }

    @DataBoundSetter
    public void setTestRunTrackerId(Integer testRunTrackerId) {
        this.testRunTrackerId = testRunTrackerId;
    }

    public String getCodebeamerUrl() {
        return this.codebeamerUrl;
    }

    @DataBoundSetter
    public void setCodebeamerUrl(String codebeamerUrl) {
        this.codebeamerUrl = codebeamerUrl;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getTestResultsDir() {
        return testResultsDir;
    }

    @DataBoundSetter
    public void setTestResultsDir(String testResultsDir) {
        this.testResultsDir = testResultsDir;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        PluginConfiguration.getInstance()
                .updateItem(run.getParent())
                .updateCodebeamerUrl(this.codebeamerUrl)
                .updateCredentialsId(this.credentialsId);

        CodebeamerApiConfiguration.getInstance()
                .withUri(this.codebeamerUrl)
                .withUsername(PluginConfiguration.getInstance().getCurrentCredentials().getUsername())
                .withPassword(PluginConfiguration.getInstance().getCurrentCredentials().getPassword().getPlainText())
                .withTestConfigurationId(testConfigurationId)
                .withTestCaseTrackerId(testCaseTrackerId)
                .withTestSetTrackerId(testSetTrackerId)
                .withTestRunTrackerId(testRunTrackerId)
                .withBuildIdentifier(String.format("%s #%s", run.getParent().getName(), -1));

        PrintStream logger = taskListener.getLogger();
        logger.println("Hello XUnitUploader Plugin!");

        final String testResults = run.getEnvironment(taskListener).expand(this.testResultsDir);

        logger.println("test results dir:" + testResults);
        logger.println("file path:" + filePath);

        XUnitFileCollector collector = new XUnitFileCollector();

        String projectRoot = filePath.toString();
        String testResultsAbsolutePath = projectRoot + "/" + testResults;

        File path = new File(testResultsAbsolutePath);

        File[] files = collector.getFiles(path);
        logger.println("List of files:\n===\n" + collector.getFileList(files) + "\n===");



        RestAdapter rest = PluginConfiguration.getInstance().getRestAdapter();
        try {
            rest.uploadXUnitResults(files);
        } catch (RequestFailed e) {
            logger.println(e.getMessage());
            throw e;
        }

        logger.println("Goodbye XUnitUploader Plugin!");
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol("xUnitUploader")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        private static final String FIELD_IS_MANDATORY = "This field is mandatory";

        private static final Logger logger = Logger.getLogger("xunit-uploader-configuration");

        public DescriptorImpl() {
            load();
        }

        protected DescriptorImpl(Class<? extends XUnitUploader> clazz) {
            super(clazz);
        }

        @Override
        public String getDisplayName() {
            return "CodeBeamer XUnit Uploader";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();

            if (!userCanMakeSelection(item)) {
                return result.includeCurrentValue(credentialsId);
            }

            return result
                    .includeEmptyValue()
                    .includeMatchingAs(
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
                            item,
                            StandardUsernamePasswordCredentials.class,
                            Collections.<DomainRequirement>emptyList(),
                            CredentialsMatchers.always()
                    )
                    .includeCurrentValue(credentialsId);
        }

        //TODO: implement doCheckCodebeamerUrl

        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl) {
            setupConfiguration(item, codebeamerUrl, value);

            if (value.isEmpty()) {
                return FormValidation.error("CodeBeamer always requires credentials");
            }

            if (!userCanMakeSelection(item)) {
                return FormValidation.ok();
            }

            if (!codebeamerUrl.isEmpty()) {
                return testCredentials();
            } else {
                return FormValidation.error("You must provide the URL of the CodeBeamer instance!");
            }
        }

        public FormValidation doCheckTestConfigurationId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
            if (value.isEmpty()) {
                return FormValidation.error(FIELD_IS_MANDATORY);
            }
            setupConfiguration(item, codebeamerUrl, credentialsId);
            return checkTrackerItemMatchesTrackerType(Integer.valueOf(value), new TestConfiguration());
        }

        public FormValidation doCheckTestSetTrackerId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
            if (value.isEmpty()) {
                return FormValidation.error(FIELD_IS_MANDATORY);
            }
            setupConfiguration(item, codebeamerUrl, credentialsId);
            return checkTrackerExistsAndType(Integer.valueOf(value), new TestSet());
        }

        public FormValidation doCheckTestCaseTrackerId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
            if (value.isEmpty()) {
                return FormValidation.error(FIELD_IS_MANDATORY);
            }
            setupConfiguration(item, codebeamerUrl, credentialsId);
            return checkTrackerExistsAndType(Integer.valueOf(value), new TestCase());
        }

        public FormValidation doCheckTestRunTrackerId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
            if (value.isEmpty()) {
                return FormValidation.error(FIELD_IS_MANDATORY);
            }
            setupConfiguration(item, codebeamerUrl, credentialsId);
            return checkTrackerExistsAndType(Integer.valueOf(value), new TestRun());
        }

        public FormValidation doCheckTestResultsDir(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (project == null) {
                return FormValidation.ok();
            }
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }

        private FormValidation testCredentials() {
            RestAdapter restAdapter = PluginConfiguration.getInstance().getRestAdapter();
            return restAdapter.testCredentials() ? FormValidation.ok() : FormValidation.error(String.format("CodeBeamer instance at %s cannot be reached with this credentials", PluginConfiguration.getInstance().getCodebeamerUrl()));
        }

        private boolean userCanMakeSelection(Item item) {
            if (item == null) {
                if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return false;
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return false;
                }
            }
            return true;
        }

        private FormValidation checkTrackerItemMatchesTrackerType(Integer value, TrackerType... wantedTypes) {
            RestAdapter restAdapter = PluginConfiguration.getInstance().getRestAdapter();
            try {
                TrackerItemDto trackerItemDto = restAdapter.getTrackerItem(value);
                if (trackerItemDto == null) {
                    return FormValidation.error(String.format("Item with ID %s does not exist", value));
                }
                return checkTrackerExistsAndType(trackerItemDto.getTracker().getId(), wantedTypes);
            } catch (RequestFailed ex) {
                logger.warn(ex);
                return FormValidation.error(ex.getMessage());
            }
        }

        private FormValidation checkTrackerExistsAndType(Integer value, TrackerType... wantedTypes) {
            RestAdapter restAdapter = PluginConfiguration.getInstance().getRestAdapter();
            try {
                TrackerDto trackerDto = restAdapter.getTracker(value);
                if (trackerDto == null) {
                    return FormValidation.error(String.format("Tracker with ID %s does not exist", value));
                }
                for (TrackerType wantedType : wantedTypes) {
                    if (validateTrackerType(trackerDto, wantedType)) {
                        return FormValidation.ok();
                    }
                }
                return FormValidation.error("Tracker Type does not match");
            } catch (RequestFailed ex) {
                logger.warn(ex);
                return FormValidation.error(ex.getMessage());
            }
        }

        private boolean validateTrackerType(TrackerDto tracker, TrackerType wantedType) throws RequestFailed {
            TrackerTypeDto trackerType = tracker.getType();
            return trackerType.getTypeId() == wantedType.getId();
        }

        private void setupConfiguration(Item item, String codebeamerUrl, String credentialsId) {
            PluginConfiguration.getInstance()
                    .updateItem(item)
                    .updateCodebeamerUrl(codebeamerUrl)
                    .updateCredentialsId(credentialsId);
        }
    }
}
