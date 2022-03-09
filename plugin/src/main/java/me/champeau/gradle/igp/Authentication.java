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
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Allows configuring authentication to Git repositories.
 */
public interface Authentication {
    /**
     * No authentication.
     */
    void none();

    /**
     * Configures basic authentication, with a username and a password.
     * @param spec the configuration
     */
    void basic(Action<? super BasicAuth> spec);

    /**
     * Configures ssh with a public key.
     */
    default void sshWithPublicKey() {
        sshWithPublicKey(k -> {});
    }

    /**
     * Configures SSH with a public key, allowing configuration
     * of the private key location, as well as an encryption
     * passphrase if the key is encrypted.
     * @param spec the key configuration
     */
    void sshWithPublicKey(Action<? super KeyConfiguration> spec);

    /**
     * Configures SSH with password authentication.
     * @param spec the configuration
     */
    void sshWithPassword(Action<? super WithPassword> spec);

    /**
     * Represents a username.
     */
    interface WithUserName {
        Property<String> getUsername();
    }

    /**
     * Represents a password.
     */
    interface WithPassword {
        Property<String> getPassword();
    }

    /**
     * Represents a username + password.
     */
    interface BasicAuth extends WithUserName, WithPassword {

    }

    /**
     * Represents a key configuration.
     */
    interface KeyConfiguration {
        RegularFileProperty getPrivateKey();
        Property<String> getPassphrase();
    }
}
