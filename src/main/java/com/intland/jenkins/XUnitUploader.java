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
import com.intland.codebeamer.api.client.Configuration;
import com.intland.codebeamer.api.client.rest.CodebeamerNotAccessibleException;
import com.intland.codebeamer.api.client.rest.RestAdapter;
import com.intland.codebeamer.api.client.rest.RestAdapterImpl;
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
import java.io.IOException;
import java.util.Collections;

public class XUnitUploader extends Notifier implements SimpleBuildStep {
    private String codebeamerUrl;
    private String credentialsId;

    @DataBoundConstructor
    public XUnitUploader(String codebeamerUrl, String credentialsId) {
        this.codebeamerUrl = codebeamerUrl;
        this.credentialsId = credentialsId;
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

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        Thread.sleep(1000);
        taskListener.getLogger().println("Hello World!");
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

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId, @QueryParameter String codebeamerUrl) {
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
            if (value.isEmpty()) {
                return FormValidation.error("CodeBeamer always requires credentials");
            }

            if (!userCanMakeSelection(item)) {
                return FormValidation.ok();
            }

            if (!codebeamerUrl.isEmpty()) {
                return testConnection(CredentialsHelper.getCurrentCredentials(item, value), codebeamerUrl);
            } else {
                return FormValidation.error("You must provide the URL of the CodeBeamer instance!");
            }
        }

        private FormValidation testConnection(StandardUsernamePasswordCredentials credentials, String codebeamerUrl) {
            Configuration config = new Configuration(codebeamerUrl, credentials.getUsername(), credentials.getPassword().getPlainText());
            RestAdapter restAdapter = new RestAdapterImpl(config, null);
            try {
                restAdapter.testConnection();
            } catch (CodebeamerNotAccessibleException e) {
                logger.warn(e);
                return FormValidation.error(String.format("CodeBeamer instance at %s cannot be reached with this credentials", codebeamerUrl));
            }
            return FormValidation.ok();
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
    }
}
