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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import me.champeau.gradle.igp.Authentication;
import me.champeau.gradle.igp.GitIncludeExtension;
import me.champeau.gradle.igp.IncludedGitRepo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.initialization.Settings;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.champeau.gradle.igp.internal.ProviderUtils.forUseAtConfigurationTime;

public abstract class DefaultIncludeGitExtension implements GitIncludeExtension {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultIncludeGitExtension.class);

    public static final String LOCAL_GIT_PREFIX = "local.git.";
    public static final String AUTO_GIT_DIRS = "auto.include.git.dirs";

    private final Settings settings;

    private Map<String, CheckoutMetadata> checkoutMetadata;
    private Action<? super Authentication> defaultAuth = a -> {};

    @Inject
    protected abstract ObjectFactory getObjects();

    @Inject
    protected abstract ProviderFactory getProviders();

    @Inject
    public DefaultIncludeGitExtension(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void include(String name, Action<? super IncludedGitRepo> spec) {
        getCheckoutsDirectory().finalizeValue();
        ProviderFactory providers = getProviders();
        readCheckoutMetadata();
        DefaultIncludedGitRepo repo = getObjects().newInstance(DefaultIncludedGitRepo.class, name);
        repo.getCheckoutDirectory().convention(getCheckoutsDirectory().map(dir -> dir.dir(name)));
        spec.execute(repo);
        DefaultAuthentication auth = repo.getAuth().orElseGet(() -> {
            DefaultAuthentication result = getObjects().newInstance(DefaultAuthentication.class);
            defaultAuth.execute(result);
            return result;
        });
        File repoDir = repo.getCheckoutDirectory().get().getAsFile();
        String localRepoProperty = LOCAL_GIT_PREFIX + repo.getName();
        Provider<String> autoGitDirs;
        autoGitDirs = forUseAtConfigurationTime(providers.gradleProperty(AUTO_GIT_DIRS));
        Map<String, List<File>> autoDirs = Collections.emptyMap();
        if (autoGitDirs.isPresent()) {
            autoDirs = Arrays.stream(autoGitDirs.get().split("[,;](\\s)?"))
                    .map(File::new)
                    .flatMap(dir -> {
                                File[] dirEntries = dir.listFiles();
                                return dirEntries == null
                                       ? Stream.empty()
                                       : Arrays.stream(dirEntries)
                                               .filter(File::isDirectory);
                            }
                    )
                    .collect(Collectors.groupingBy(File::getName));
        }
        Map<String, List<File>> finalAutoDirs = autoDirs;
        Provider<String> localRepo = forUseAtConfigurationTime(providers.gradleProperty(localRepoProperty))
                .orElse(forUseAtConfigurationTime(providers.systemProperty(localRepoProperty)))
                .orElse(providers.provider(() -> {
                    List<File> files = finalAutoDirs.get(repo.getName());
                    if (files == null) {
                        return null;
                    }
                    if (files.size() == 1) {
                        return files.get(0).toString();
                    }
                    throw new GradleException("More than one directory named " + repo.getName() + " exists in auto Git repositories: " + files);
                }));
        if (localRepo.isPresent()) {
            LOGGER.info("Using local repository for {} instead of cloning", repo.getName());
            repoDir = new File(localRepo.get());
        } else {
            cloneOrUpdate(repoDir, repo, auth);
        }
        repo.configure(settings, repoDir);
    }

    @Override
    public void defaultAuthentication(Action<? super Authentication> config) {
        defaultAuth = config;
    }

    private void readCheckoutMetadata() {
        if (checkoutMetadata == null) {
            checkoutMetadata = new HashMap<>();
            File metadataFile = getCheckoutsDirectory().file("checkouts.bin").get().getAsFile();
            if (metadataFile.exists()) {
                try (DataInputStream dis = new DataInputStream(new FileInputStream(metadataFile))) {
                    int size = dis.readInt();
                    for (int i = 0; i < size; i++) {
                        String uri = dis.readUTF();
                        String ref = dis.readUTF();
                        String branch = dis.readUTF();
                        long lastUpdate = dis.readLong();
                        checkoutMetadata.put(uri, new CheckoutMetadata(uri, ref, branch, lastUpdate));
                    }
                } catch (IOException e) {
                    throw new GradleException("Unable to read checkout metadata", e);
                }
            } else {
                checkoutMetadata = new HashMap<>();
            }
        }
    }

    public void writeCheckoutMetadata() {
        if (checkoutMetadata == null) {
            return;
        }
        File metadataFile = getCheckoutsDirectory().file("checkouts.bin").get().getAsFile();
        File parentFile = metadataFile.getParentFile();
        parentFile.mkdirs();
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(metadataFile))) {
            int size = checkoutMetadata.size();
            dos.writeInt(size);
            checkoutMetadata.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(e -> {
                        CheckoutMetadata value = e.getValue();
                        try {
                            dos.writeUTF(value.getUri());
                            dos.writeUTF(value.getRef());
                            dos.writeUTF(value.getBranch());
                            dos.writeLong(value.getLastUpdate());
                        } catch (IOException ex) {
                            throw new GradleException("Unable to write checkout metadata", ex);
                        }
                    });
        } catch (IOException e) {
            throw new GradleException("Unable to write checkout metadata", e);
        }
    }

    private void cloneOrUpdate(File repoDir, IncludedGitRepo repo, DefaultAuthentication auth) {
        String uri = repo.getUri().get();
        String rev = repo.getCommit().getOrElse("");
        String branchOrTag = repo.getTag().orElse(repo.getBranch()).orElse("").get();
        CheckoutMetadata current = new CheckoutMetadata(uri, rev, branchOrTag, System.currentTimeMillis());
        if (repoDir.exists() && new File(repoDir, ".git").exists()) {
            updateRepository(repoDir, uri, rev, branchOrTag, current, auth);
        } else {
            cloneRepository(repoDir, uri, rev, branchOrTag, current, auth);
        }
    }

    <C extends GitCommand<?>, R, TC extends TransportCommand<C, R>> TC applyAuth(TC command, DefaultAuthentication authentication) {
        authentication.getBasicAuth().ifPresent(auth -> command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                auth.getUsername().get(),
                auth.getPassword().get()
        )));
        authentication.getSshWithPassword().ifPresent(auth -> {
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host host, Session session) {
                    session.setPassword(auth.getPassword().get());
                }
            };
            command.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);
            });
        });
        authentication.getSshWithPublicKey().ifPresent(keyConfig -> {
            SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch defaultJSch = super.createDefaultJSch( fs );
                    if (keyConfig.getPrivateKey().isPresent()) {
                        defaultJSch.addIdentity(keyConfig.getPrivateKey().get().getAsFile().getAbsolutePath());
                    }
                    return defaultJSch;
                }
            };
            command.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sshSessionFactory);
            });
        });
        return command;
    }

    private void cloneRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current, DefaultAuthentication auth) {
        LOGGER.info("Checking out {} ref {} in {}", uri, rev, repoDir);

        // Note that we need to do this in a more verbose way with `git init`, `git remote add...`, `git fetch`, and
        // `git checkout` due to intermittent issues with Gradle filesystem watching. `git clone` will fail when run in
        // a non-empty directory, which is often the case because Gradle will add a `.gradle/file-system.probe` file
        // very quickly. `git init` (etc) doesn't suffer this same limitation.
        try {
            //applyAuth(Git.cloneRepository() // TODO(tsr): figure out how to apply authentication
            // TODO(tsr): cleanup logging
            System.out.printf("...git clone %s %s --branch %s...%n", uri, repoDir, branchOrTag);
            // First we `git init`
            ExecOutput result = getProviders().exec(spec -> {
                spec.workingDir(repoDir);
                spec.commandLine("git", "init");
                spec.setIgnoreExitValue(true);
            });
            result.getResult().get().assertNormalExitValue();

            // Then we `git remote add origin <uri>` to set the remote
            result = getProviders().exec(spec -> {
                spec.workingDir(repoDir);
                spec.commandLine("git", "remote", "add", "origin", uri);
                spec.setIgnoreExitValue(true);
            });
            result.getResult().get().assertNormalExitValue();

            // Then `git fetch`
            result = getProviders().exec(spec -> {
                spec.workingDir(repoDir);
                spec.commandLine("git", "fetch");
                spec.setIgnoreExitValue(true);
            });
            result.getResult().get().assertNormalExitValue();

            //result = getProviders().exec(spec -> {
            //    spec.commandLine("git", "clone", uri, repoDir, "--branch", branchOrTag);
            //    spec.setIgnoreExitValue(true);
            //});
            //result.getResult().get().assertNormalExitValue();
            //System.out.printf("git clone result: %s%n", result.getResult().get());
            System.out.println("`git init` && `git remote add ...` && `git fetch` successful!");

            // And finally `git checkout <branchOrTag>`
            String checkout = branchOrTag;
            if (!rev.isEmpty()) {
                checkout = rev;
            }
            final String c = checkout;

            System.out.printf("...git checkout %s in %s... %n", c, repoDir);
            result = getProviders().exec(spec -> {
                spec.workingDir(repoDir);
                spec.commandLine("git", "checkout", c);
                spec.setIgnoreExitValue(true);
            });
            result.getResult().get().assertNormalExitValue();
            System.out.printf("git checkout result: %s%n", result.getResult().get());
        } catch (Exception e) {
            throw new GradleException("Unable to clone repository contents: " + e.getMessage(), e);
        } finally {
            checkoutMetadata.put(uri, current);
        }
        //try {
        //    applyAuth(Git.cloneRepository()
        //            .setURI(uri)
        //            .setBranch(branchOrTag)
        //            .setDirectory(repoDir), auth)
        //            .call();
        //    if (!rev.isEmpty()) {
        //        try (Git git = Git.open(repoDir)) {
        //            git.checkout()
        //                    .setName(rev)
        //                    .call();
        //        }
        //    }
        //} catch (GitAPIException | IOException e) {
        //    throw new GradleException("Unable to clone repository contents: " + e.getMessage(), e);
        //} finally {
        //    checkoutMetadata.put(uri, current);
        //}
    }

    private void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current, DefaultAuthentication auth) {
        if (checkoutMetadata.containsKey(uri)) {
            CheckoutMetadata old = checkoutMetadata.get(uri);
            boolean sameRef = Objects.equals(current.getRef(), old.getRef());
            boolean sameBranch = current.getBranch().equals(old.getBranch());
            boolean upToDate = current.getLastUpdate() - old.getLastUpdate() < getRefreshIntervalMillis().get();
            if (sameRef && sameBranch && upToDate) {
                return;
            }
        }

        try {
            // TODO(tsr): cleanup logging
            System.out.printf("git symbolic-ref HEAD%n");
            ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
            ExecOutput result = getProviders().exec(spec -> {
                spec.workingDir(repoDir);
                spec.commandLine("git", "symbolic-ref", "HEAD");
                spec.setStandardOutput(stdOut);
                spec.setIgnoreExitValue(true);
            });
            result.getResult().get().assertNormalExitValue();

            String fullBranch = stdOut.toString(Charset.defaultCharset());
            System.out.printf("git symbolic-ref HEAD out = %s%n", fullBranch);

            if (fullBranch.startsWith("refs/heads/")) {
                LOGGER.info("Pulling from {}", uri);
                //applyAuth(git.pull(), auth).call(); // TODO(tsr): figure out how to apply authentication
                result = getProviders().exec(spec -> {
                    spec.workingDir(repoDir);
                    spec.commandLine("git", "pull");
                    spec.setIgnoreExitValue(true);
                });
                result.getResult().get().assertNormalExitValue();

                if (!rev.isEmpty()) {
                    result = getProviders().exec(spec -> {
                        spec.workingDir(repoDir);
                        spec.commandLine("git", "checkout", rev);
                        spec.setIgnoreExitValue(true);
                    });
                    result.getResult().get().assertNormalExitValue();
                } else {
                    // TODO(tsr): ref.getName().endsWith(branchOrTag) etc.
                    result = getProviders().exec(spec -> {
                        spec.workingDir(repoDir);
                        spec.commandLine("git", "checkout", branchOrTag);
                        spec.setIgnoreExitValue(true);
                    });
                    result.getResult().get().assertNormalExitValue();
                }
            }
        } catch (Exception e) {
            throw new GradleException("Unable to update repository contents: " + e.getMessage(), e);
        } finally {
            checkoutMetadata.put(uri, current);
        }

        //try (Git git = Git.open(repoDir)) {
        //    String fullBranch = git.getRepository().getFullBranch();
        //    if (fullBranch.startsWith("refs/heads/")) {
        //        LOGGER.info("Pulling from {}", uri);
        //        applyAuth(git.pull(), auth).call();
        //    }
        //    LOGGER.info("Checking out ref {} of {}", rev, uri);
        //    if (!rev.isEmpty()) {
        //        git.checkout()
        //                .setName(rev)
        //                .call();
        //    } else {
        //        Ref resolve = git.getRepository().findRef(branchOrTag);
        //        if (resolve == null) {
        //            List<Ref> refs = git.getRepository().getRefDatabase().getRefs();
        //            for (Ref ref : refs) {
        //                if (ref.getName().endsWith(branchOrTag)) {
        //                    resolve = ref;
        //                    break;
        //                }
        //            }
        //        }
        //        if (resolve != null) {
        //            git.checkout()
        //                    .setName(resolve.getName())
        //                    .call();
        //        } else {
        //            throw new GradleException("Branch or tag " + branchOrTag + " not found");
        //        }
        //    }
        //} catch (GitAPIException | IOException e) {
        //    throw new GradleException("Unable to update repository contents: " + e.getMessage(), e);
        //} finally {
        //    checkoutMetadata.put(uri, current);
        //}
    }
}
