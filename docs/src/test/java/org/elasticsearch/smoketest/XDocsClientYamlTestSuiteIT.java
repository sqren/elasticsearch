/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.smoketest;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ClientYamlTestResponse;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.junit.After;


import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken.basicAuthHeaderValue;

public class XDocsClientYamlTestSuiteIT extends ESClientYamlSuiteTestCase {
    private static final String USER_TOKEN = basicAuthHeaderValue("test_admin", new SecureString("changeme".toCharArray()));

    public XDocsClientYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return ESClientYamlSuiteTestCase.createParameters();
    }

    @Override
    protected void afterIfFailed(List<Throwable> errors) {
        super.afterIfFailed(errors);
        String name = getTestName().split("=")[1];
        name = name.substring(0, name.length() - 1);
        name = name.replaceAll("/([^/]+)$", ".asciidoc:$1");
        logger.error("This failing test was generated by documentation starting at {}. It may include many snippets. "
                + "See Elasticsearch's docs/README.asciidoc for an explanation of test generation.", name);
    }

    @Override
    protected boolean preserveTemplatesUponCompletion() {
        return true;
    }

    /**
     * All tests run as a an administrative user but use <code>es-shield-runas-user</code> to become a less privileged user.
     */
    @Override
    protected Settings restClientSettings() {
        return Settings.builder()
                .put(ThreadContext.PREFIX + ".Authorization", USER_TOKEN)
                .build();
    }

    /**
     * Re-enables watcher after every test just in case any test disables it. One does.
     */
    @After
    public void reenableWatcher() throws Exception {
        getAdminExecutionContext().callApi("xpack.watcher.start", emptyMap(), emptyList(), emptyMap());
    }

    /**
     * Deletes users after every test just in case any test adds any.
     */
    @After
    public void deleteUsers() throws Exception {
        ClientYamlTestResponse response = getAdminExecutionContext().callApi("xpack.security.get_user", emptyMap(), emptyList(),
                emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, Object> users = (Map<String, Object>) response.getBody();
        for (String user: users.keySet()) {
            Map<?, ?> metaDataMap = (Map<?, ?>) ((Map<?, ?>) users.get(user)).get("metadata");
            Boolean reserved = metaDataMap == null ? null : (Boolean) metaDataMap.get("_reserved");
            if (reserved == null || reserved == false) {
                logger.warn("Deleting leftover user {}", user);
                getAdminExecutionContext().callApi("xpack.security.delete_user", singletonMap("username", user), emptyList(), emptyMap());
            }
        }
    }
}