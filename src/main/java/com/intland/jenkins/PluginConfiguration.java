/*
 * Copyright (c) 2017 Intland Software (support@intland.com)
 *
 */
package com.intland.jenkins;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.intland.codebeamer.api.client.CodebeamerApiConfiguration;
import com.intland.codebeamer.api.client.rest.RestAdapter;
import com.intland.codebeamer.api.client.rest.RestAdapterImpl;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;

import java.util.Collections;

public final class PluginConfiguration {
    private static PluginConfiguration instance = null;

    private String codebeamerUrl;

    private Item item;
    private String credentialsId;
    private RestAdapter restAdapter;

    private Integer testConfigurationId;
    private Integer testCaseTrackerId;
    private Integer testCaseId;
    private Integer releaseId;
    private Integer testRunTrackerId;
    private String defaultPackagePrefix;


    private PluginConfiguration() {

    }

    public static PluginConfiguration getInstance() {
        if (instance == null) {
            instance = new PluginConfiguration();
        }
        return instance;
    }

    public Integer getTestConfigurationId() {
        return testConfigurationId;
    }

    public Integer getTestCaseTrackerId() {
        return testCaseTrackerId;
    }

    public Integer getReleaseId() {
        return releaseId;
    }

    public Integer getTestRunTrackerId() {
        return testRunTrackerId;
    }

    public String getCodebeamerUrl() {
        return codebeamerUrl;
    }

    public Integer getTestCaseId() {
		return testCaseId;
	}

	public String getDefaultPackagePrefix() {
		return defaultPackagePrefix;
	}

	public StandardUsernamePasswordCredentials getCurrentCredentials() {
        if (item == null || credentialsId == null) {
            return null;
        }
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        item,
                        item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()
                ),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    public PluginConfiguration updateItem(Item item) {
        resetState();
        this.item = item;
        return this;
    }

    public PluginConfiguration updateCredentialsId(String credentialsId) {
        resetState();
        this.credentialsId = credentialsId;
        return this;
    }

    public PluginConfiguration updateCodebeamerUrl(String codebeamerUrl) {
        resetState();
        this.codebeamerUrl = codebeamerUrl;
        return this;
    }

    public PluginConfiguration updateTestConfigurationId(Integer id) {
        this.testConfigurationId = id;
        return this;
    }

    public PluginConfiguration updateTestCaseTrackerId(Integer id) {
        this.testCaseTrackerId = id;
        return this;
    }
    
    public PluginConfiguration updateTestCaseId(Integer id) {
        this.testCaseId = id;
        return this;
    }
    
    public PluginConfiguration updateDefaultPackagePrefix(String defaultPackagePrefix) {
        this.defaultPackagePrefix = defaultPackagePrefix;
        return this;
    }

    public PluginConfiguration updateReleaseId(Integer id) {
        this.releaseId = id;
        return this;
    }

    public PluginConfiguration updateTestRunTrackerId(Integer id) {
        this.testRunTrackerId = id;
        return this;
    }

    public RestAdapter getRestAdapter() {
        if (restAdapter == null) {
            setRestAdapterConfiguration();
            restAdapter = new RestAdapterImpl(null);
        }
        return restAdapter;
    }

    private void setRestAdapterConfiguration() {
        if (codebeamerUrl != null) {
            StandardUsernamePasswordCredentials credentials = getCurrentCredentials();
            CodebeamerApiConfiguration.getInstance()
                    .withUri(codebeamerUrl)
                    .withUsername(credentials.getUsername())
                    .withPassword(credentials.getPassword().getPlainText());
        }
    }

    private void resetState() {
        restAdapter = null;
    }
}
