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
import org.gradle.api.Action;
import org.gradle.api.initialization.ConfigurableIncludedBuild;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.Optional;

public abstract class DefaultIncludedGitRepo implements IncludedGitRepo {
    private final String name;
    private final ObjectFactory objects;
    private Action<? super ConfigurableIncludedBuild> spec;
    private DefaultAuthentication auth;

    @Inject
    public DefaultIncludedGitRepo(String name, ObjectFactory objects) {
        this.name = name;
        this.objects = objects;
        this.spec = c -> c.setName(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void includedBuild(Action<? super ConfigurableIncludedBuild> spec) {
        Action<? super ConfigurableIncludedBuild> currentSpec = this.spec;
        this.spec = (Action<ConfigurableIncludedBuild>) c -> {
            currentSpec.execute(c);
            spec.execute(c);
        };
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

    void configure(ConfigurableIncludedBuild build) {
        spec.execute(build);
    }
}
