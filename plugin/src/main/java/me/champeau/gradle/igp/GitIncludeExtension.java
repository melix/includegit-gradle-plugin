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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

/**
 * The main configuration of the Git include plugin.
 */
public interface GitIncludeExtension {
    /**
     * Determines, in milliseconds, how often the repository should be updated.
     * By default, 24 hours.
     * @return the refresh interval property
     */
    Property<Long> getRefreshIntervalMillis();

    /**
     * Determine where the Git repositories should be checked out.
     * @return the directory property
     */
    DirectoryProperty getCheckoutsDirectory();

    /**
     * Configures the default authentication mechanism, used when
     * no configuration is configured on a repository.
     * @param config the default configuration.
     */
    void defaultAuthentication(Action<? super Authentication> config);

    /**
     * Includes a Git repository as a Gradle included build.
     * @param name the name of the included build
     * @param spec the configuration of the Git repository
     */
    void include(String name, Action<? super IncludedGitRepo> spec);
}
