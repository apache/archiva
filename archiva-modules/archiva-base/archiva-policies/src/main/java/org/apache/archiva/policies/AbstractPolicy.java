package org.apache.archiva.policies;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Abstract policy class that handles the name and description loading with message bundles.
 *
 * The prefix for the keys is normally:
 * <ul>
 *     <li>Policies: POLICY-ID.policy.</li>
 *     <li>Options: POLICY-ID.option.</li>
 * </ul>
 *
 * This prefix can be changed by subclasses.
 *
 * For each policy and each option there must exist a name and description entry in the message bundle.
 *
 */
public abstract class AbstractPolicy implements Policy {

    private String policyPrefix;
    private String optionPrefix;

    public AbstractPolicy() {
        policyPrefix = getId() + ".policy.";
        optionPrefix = getId() + ".option.";
    }

    protected String getPolicyPrefix() {
        return policyPrefix;
    }

    protected String getOptionPrefix() {
        return optionPrefix;
    }

    protected void setPolicyPrefix(String policyPrefix) {
        this.policyPrefix = policyPrefix;
    }

    public void setOptionPrefix(String optionPrefix) {
        this.optionPrefix = optionPrefix;
    }

    private static final ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE, locale);
    }


    @Override
    public String getName() {
        return getName(Locale.getDefault());
    }

    @Override
    public String getName(Locale locale) {
        return getBundle(locale).getString(getPolicyPrefix() + "name");
    }

    @Override
    public String getDescription(Locale locale) {
        return MessageFormat.format(getBundle(locale).getString(getPolicyPrefix() + "description")
                , getOptions().stream().map(o -> o.getId()).collect(Collectors.joining(",")));
    }

    @Override
    public String getOptionDescription(Locale locale, PolicyOption option) {
        return getBundle(locale).getString(getOptionPrefix()+option.getId()+".description");
    }

    @Override
    public String getOptionName(Locale locale, PolicyOption option) {
        return getBundle(locale).getString(getOptionPrefix()+option.getId()+".name");
    }

}
