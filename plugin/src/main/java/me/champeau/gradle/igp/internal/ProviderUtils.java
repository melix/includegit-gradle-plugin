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

import org.gradle.api.provider.Provider;
import org.gradle.util.GradleVersion;

public abstract class ProviderUtils {
    private static final GradleVersion GRADLE_65 = GradleVersion.version("6.5");
    private static final GradleVersion GRADLE_70 = GradleVersion.version("7.0");
    private static final GradleVersion CURRENT = GradleVersion.current();
    private static final boolean BETWEEN_65_AND_70 = CURRENT.compareTo(GRADLE_65)>=0 && CURRENT.compareTo(GRADLE_70)<=0;

    public static <T> Provider<T> forUseAtConfigurationTime(Provider<T> provider) {
        if (BETWEEN_65_AND_70) {
            return provider.forUseAtConfigurationTime();
        }
        return provider;
    }
}
