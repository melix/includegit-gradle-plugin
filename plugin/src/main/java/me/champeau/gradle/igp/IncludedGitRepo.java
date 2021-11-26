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
import org.gradle.api.file.DirectoryProperty;
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
     * A specific commit to checkout.
     * @return the commit property
     */
    Property<String> getCommit();

    /**
     * The directory to checkout the repository to.
     * @return the directory
     */
    DirectoryProperty getCheckoutDirectory();

    /**
     * Allows configuring the included build, in particular
     * dependency substitutions.
     * @param spec the configuration
     */
    void includeBuild(Action<? super ConfigurableIncludedBuild> spec);

    /**
     * If set to true, the checked out project will be automatically
     * included in the build.
     * @return the auto include property.
     */
    Property<Boolean> getAutoInclude();

    /**
     * If this method is called, then the auto-include property will
     * automatically be set to false.
     * @param relativePath the relative path from the checkout directory
     * to the project to include.
     */
    default void includeBuild(String relativePath) {
        includeBuild(relativePath, s -> {});
    }

    /**
     * If this method is called, then the auto-include property will
     * automatically be set to false.
     * @param relativePath the relative path from the checkout directory
     * to the project to include.
     * @param spec the spec of the included build
     */
    void includeBuild(String relativePath, Action<? super ConfigurableIncludedBuild> spec);

    /**
     * Configures authentication for this repository.
     * @param config the authentication configuration
     */
    void authentication(Action<? super Authentication> config);
}
