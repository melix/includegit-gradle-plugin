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
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.Optional;

public class DefaultAuthentication implements Authentication {

    private final ObjectFactory objects;

    private KeyConfiguration keyConfiguration;
    private BasicAuth basicAuth;
    private WithPassword sshWithPassword;

    @Inject
    public DefaultAuthentication(ObjectFactory objects) {
        this.objects = objects;
    }

    @Override
    public void none() {
        keyConfiguration = null;
        basicAuth = null;
        sshWithPassword = null;
    }

    @Override
    public void basic(Action<? super BasicAuth> spec) {
        none();
        basicAuth = objects.newInstance(BasicAuth.class);
        spec.execute(basicAuth);
    }

    @Override
    public void sshWithPublicKey(Action<? super KeyConfiguration> spec) {
        none();
        keyConfiguration = objects.newInstance(KeyConfiguration.class);
        spec.execute(keyConfiguration);
    }

    @Override
    public void sshWithPassword(Action<? super WithPassword> spec) {
        none();
        sshWithPassword = objects.newInstance(WithPassword.class);
        spec.execute(sshWithPassword);
    }

    Optional<KeyConfiguration> getSshWithPublicKey() {
        return Optional.ofNullable(keyConfiguration);
    }

    Optional<BasicAuth> getBasicAuth() {
        return Optional.ofNullable(basicAuth);
    }

    Optional<WithPassword> getSshWithPassword() {
        return Optional.ofNullable(sshWithPassword);
    }
}
