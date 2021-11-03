/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.champeau.gradle.igp;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.initialization.ConfigurableIncludedBuild;
import org.gradle.api.provider.Property;

/**
 * Configures an included Git repository.
 */
public interface IncludedGitRepo extends Named {
    /**
     * The URI to the repository, using any URI supported
     * by JGit.
     * @return the URI property
     */
    Property<String> getUri();

    /**
     * The branch to checkout.
     * @return the branch property
     */
    Property<String> getBranch();

    /**
     * A tag to checkout.
     * @return the tag property
     */
    Property<String> getTag();

    /**
     * Allows configuring the included build, in particular
     * dependency substitutions.
     * @param spec the configuration
     */
    void includedBuild(Action<? super ConfigurableIncludedBuild> spec);

    /**
     * Configures authentication for this repository.
     * @param config the authentication configuration
     */
    void authentication(Action<? super Authentication> config);
}
