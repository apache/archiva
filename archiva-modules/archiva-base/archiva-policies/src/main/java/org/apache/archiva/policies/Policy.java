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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;


/**
 * This is a generic interface for policies. Policies define different actions to apply to artifacts during the
 * repository lifecycle, e.g. download, upload, errors.
 */
public interface Policy
{

    String RESOURCE_BUNDLE = "archiva_policies";

    /**
     * Get the list of options for this policy.
     *
     * @return the list of options for this policy.
     */
    List<PolicyOption> getOptions();

    /**
     * Get the default option for this policy.
     *
     * @return the default policy for this policy.
     */
    PolicyOption getDefaultOption();

    /**
     * Get the id for this policy.
     *
     * @return the id for this policy.
     */
    String getId();

    /**
     * Get the display name for this policy.
     *
     * @return the name for this policy
     */
    String getName();

    /**
     * Get the policy name in the language of the given locale.
     * @param locale The locale
     * @return The policy name
     */
    String getName(Locale locale);

    /**
     * Return a description of the policy.
     * @param locale The language
     * @return The description
     */
    String getDescription(Locale locale);

    /**
     * Returns a description for the given option.
     * @param locale The locale for the description.
     * @param option The option to ask the description for.
     * @return A description of the option in the requested language.
     * @throws MissingResourceException if the option is not known by this policy.
     */
    String getOptionDescription(Locale locale, PolicyOption option) throws MissingResourceException;

    /**
     * Returns a name for the given option.
     * @param locale The locale for the name
     * @param option  The option identifier
     * @return  A name in the requested language.
     * @throws MissingResourceException if the option is not known by this policy.
     */
    String getOptionName(Locale locale, PolicyOption option) throws MissingResourceException;
}
