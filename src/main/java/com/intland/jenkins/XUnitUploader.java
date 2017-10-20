/*
 * Copyright (c) 2017 Intland Software (support@intland.com)
 *
 */
package com.intland.jenkins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

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
import com.intland.codebeamer.api.client.trackertypes.Release;
import com.intland.codebeamer.api.client.trackertypes.TestCase;
import com.intland.codebeamer.api.client.trackertypes.TestConfiguration;
import com.intland.codebeamer.api.client.trackertypes.TestRun;
import com.intland.codebeamer.api.client.trackertypes.TrackerType;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
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

public class XUnitUploader extends Notifier implements SimpleBuildStep {
    private String codebeamerUrl;
    private String credentialsId;
    private Integer testConfigurationId;
    private Integer testCaseTrackerId;
    private Integer testCaseId;
    private Integer releaseId;
    private Integer testRunTrackerId;
    private String testResultsDir;
    private String defaultPackagePrefix;

    @DataBoundConstructor
    public XUnitUploader(String codebeamerUrl, String credentialsId, Integer testConfigurationId, Integer testCaseTrackerId, Integer testCaseId, Integer releaseId, Integer testRunTrackerId, String testResultsDir, String defaultPackagePrefix) {
        this.codebeamerUrl = codebeamerUrl;
        this.credentialsId = credentialsId;
        this.testConfigurationId = testConfigurationId;
        this.testCaseTrackerId = testCaseTrackerId;
        this.testCaseId = testCaseId;
        this.releaseId = releaseId;
        this.testRunTrackerId = testRunTrackerId;
        this.testResultsDir = testResultsDir;
        this.defaultPackagePrefix = defaultPackagePrefix;
    }

    public Integer getTestConfigurationId() {
        return testConfigurationId;
    }

    @DataBoundSetter
    public void setTestConfigurationId(Integer testConfigurationId) {
        this.testConfigurationId = testConfigurationId;
    }

    public Integer getReleaseId() {
        return releaseId;
    }

    @DataBoundSetter
    public void setReleaseId(Integer releaseId) {
        this.releaseId = releaseId;
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

    public Integer getTestCaseId() {
		return testCaseId;
	}

    @DataBoundSetter
	public void setTestCaseId(Integer testCaseId) {
		this.testCaseId = testCaseId;
	}

	public String getDefaultPackagePrefix() {
		return defaultPackagePrefix;
	}

    @DataBoundSetter
	public void setDefaultPackagePrefix(String defaultPackagePrefix) {
		this.defaultPackagePrefix = defaultPackagePrefix;
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
                .withTestCaseId(testCaseId)
                .withReleaseId(releaseId)
                .withTestRunTrackerId(testRunTrackerId)
                .withDefaultPackagePrefix(defaultPackagePrefix)
                .withBuildIdentifier(String.format("%s #%s", run.getParent().getName(), run.getNumber()));

        PrintStream logger = taskListener.getLogger();
        logger.println("Hello XUnitUploader Plugin!");

        final String testResults = run.getEnvironment(taskListener).expand(this.testResultsDir);

        logger.println("test results dir:" + testResults);
        logger.println("file path:" + filePath);

        XUnitFileCollector collector = new XUnitFileCollector();

        String projectRoot = filePath.getRemote();
        String testResultsAbsolutePath = projectRoot + "/" + testResults;

        File path = new File(testResultsAbsolutePath);

        File[] files = collector.getFiles(path);
        logger.println("List of files:\n===\n" + collector.getFileList(files) + "\n===");

        logger.println("Zip files.");
        File zipFile = null;
        try {
        	zipFile = zipFiles(files);
        
	        logger.println("Upload test results.");
	        RestAdapter rest = PluginConfiguration.getInstance().getRestAdapter();
	        try {
	            rest.uploadXUnitResults(new File[] { zipFile });
	        } catch (RequestFailed e) {
	            logger.println(e.getMessage());
	            throw e;
	        }
        } finally {
        	if (zipFile != null && zipFile.exists()) {
        		try {
        			zipFile.delete();
        		} catch(Exception ex) {
        			logger.println("Zip file cannot be deleted.");
        		}
        	}
        }

        logger.println("Goodbye XUnitUploader Plugin!");
    }

    private File zipFiles(File[] files) throws IOException {
    	File zipFile = File.createTempFile("xunituploader", ".zip");
            
        ZipOutputStream zos = null;
        try {
        	zos = new ZipOutputStream(new FileOutputStream(zipFile));

            for (File file : files) {
               FileInputStream fis = null;
               try {
	               fis = new FileInputStream(file);
	               ZipEntry entry = new ZipEntry(file.getName());
	               zos.putNextEntry(entry);
	               IOUtils.copyLarge(fis, zos);
               } finally {
            	   IOUtils.closeQuietly(fis);
               }
            }
        } finally {
        	IOUtils.closeQuietly(zos);
        }
        
    	return zipFile;
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
        private static final String FIELD_TESTCASE_TRACKER_IS_MANDATORY = "This field is mandatory if test case ID is not set";

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

        @SuppressWarnings("rawtypes")
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

        public FormValidation doCheckReleaseId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
        	FormValidation result = FormValidation.ok();
            if (!value.isEmpty()) {
            	setupConfiguration(item, codebeamerUrl, credentialsId);
            	result = checkTrackerItemMatchesTrackerType(Integer.valueOf(value), new Release());
            }
            return result;
        }

        public FormValidation doCheckTestCaseTrackerId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String testCaseId, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
            if (value.isEmpty()) {
            	if (StringUtils.isEmpty(testCaseId)) {
            		return FormValidation.error(FIELD_TESTCASE_TRACKER_IS_MANDATORY);
            	} else {
            		return FormValidation.ok();
            	}
            }
            setupConfiguration(item, codebeamerUrl, credentialsId);
            return checkTrackerExistsAndType(Integer.valueOf(value), new TestCase());
        }
        
        public FormValidation doCheckTestCaseId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
        	FormValidation result = FormValidation.ok();
            if (!value.isEmpty()) {
            	setupConfiguration(item, codebeamerUrl, credentialsId);
            	result = checkTrackerItemMatchesTrackerType(Integer.valueOf(value), new TestCase());
            }
            return result;
        }

        public FormValidation doCheckTestRunTrackerId(@AncestorInPath Item item, @QueryParameter String value, @QueryParameter String codebeamerUrl, @QueryParameter String credentialsId) {
            if (value.isEmpty()) {
                return FormValidation.error(FIELD_IS_MANDATORY);
            }
            setupConfiguration(item, codebeamerUrl, credentialsId);
            return checkTrackerExistsAndType(Integer.valueOf(value), new TestRun());
        }

        @SuppressWarnings("rawtypes")
		public FormValidation doCheckTestResultsDir(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            if (project == null) {
                return FormValidation.ok();
            }
            return FilePath.validateFileMask(project.getSomeWorkspace(), value);
        }
        
        @SuppressWarnings("rawtypes")
		public FormValidation doCheckDefaultPackagePrefix(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException {
            return FormValidation.ok();
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
