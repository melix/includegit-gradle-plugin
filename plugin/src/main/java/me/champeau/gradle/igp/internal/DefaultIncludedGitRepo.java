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
package me.champeau.gradle.igp.internal;

import me.champeau.gradle.igp.Authentication;
import me.champeau.gradle.igp.IncludedGitRepo;
import me.champeau.gradle.igp.internal.git.jgit.DefaultAuthentication;
import org.gradle.api.Action;
import org.gradle.api.initialization.ConfigurableIncludedBuild;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DefaultIncludedGitRepo implements IncludedGitRepo {
    private final String name;
    private final ObjectFactory objects;
    private final Action<ConfigurableIncludedBuild> rootSpec;
    private final List<IncludedBuild> includes = new ArrayList<>();
    private final List<Action<? super CodeReadyEvent>> codeReadyEvents = new ArrayList<>();
    private DefaultAuthentication auth;

    @Inject
    public DefaultIncludedGitRepo(String name, ObjectFactory objects) {
        this.name = name;
        this.objects = objects;
        this.rootSpec = c -> c.setName(name);
        getAutoInclude().convention(true);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void includeBuild(Action<? super ConfigurableIncludedBuild> spec) {
        includeBuild(".", c -> {
            rootSpec.execute(c);
            spec.execute(c);
        });
    }

    @Override
    public void codeReady(Action<? super CodeReadyEvent> configuration) {
        codeReadyEvents.add(configuration);
    }

    /**
     * If this method is called, then the auto-include property will
     * automatically be set to false.
     *
     * @param relativePath the relative path from the checkout directory
     * to the project to include.
     * @param spec the spec of the included build
     */
    @Override
    public void includeBuild(String relativePath, Action<? super ConfigurableIncludedBuild> spec) {
        if (getAutoInclude().get()) {
            getAutoInclude().set(false);
            getAutoInclude().finalizeValue();
        }
        includes.add(new IncludedBuild(relativePath, spec));
    }

    @Override
    public void authentication(Action<? super Authentication> config) {
        if (auth == null) {
            auth = objects.newInstance(DefaultAuthentication.class);
        }
        config.execute(auth);
    }

    Optional<DefaultAuthentication> getAuth() {
        return Optional.ofNullable(auth);
    }

    void configure(Settings settings, File checkoutDirectory) {
        if (!codeReadyEvents.isEmpty()) {
            fireCodeReadyEvents(checkoutDirectory);
        }
        if (getAutoInclude().get()) {
            settings.includeBuild(checkoutDirectory, rootSpec);
        } else {
            for (IncludedBuild include : includes) {
                // unchecked cast because of inconsistency in Gradle API
                //noinspection unchecked
                settings.includeBuild(new File(checkoutDirectory, include.directory), (Action<ConfigurableIncludedBuild>) include.spec);
            }
        }
    }

    private void fireCodeReadyEvents(File checkoutDirectory) {
        CodeReadyEvent codeReadyEvent = new CodeReadyEvent() {
            @Override
            public IncludedGitRepo getIncludedGitRepo() {
                return DefaultIncludedGitRepo.this;
            }

            @Override
            public File getCheckoutDirectory() {
                return checkoutDirectory;
            }
        };
        for (Action<? super CodeReadyEvent> action : codeReadyEvents) {
            action.execute(codeReadyEvent);
        }
    }

    private static class IncludedBuild {
        private final String directory;
        private final Action<? super ConfigurableIncludedBuild> spec;

        private IncludedBuild(String directory, Action<? super ConfigurableIncludedBuild> spec) {
            this.directory = directory;
            this.spec = spec;
        }
    }
}
