package me.champeau.gradle.igp.internal.git;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import me.champeau.gradle.igp.internal.CheckoutMetadata;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOutput;
import org.slf4j.Logger;

public class GitCliClient implements GitClientStrategy {

  private final Logger logger;
  private final ProviderFactory providers;
  private final Map<String, CheckoutMetadata> checkoutMetadata;
  private final long refreshIntervalMillis;

  public GitCliClient(Logger logger, ProviderFactory providers, Map<String, CheckoutMetadata> checkoutMetadata,
      long refreshIntervalMillis) {

    this.logger = logger;
    this.providers = providers;
    this.checkoutMetadata = checkoutMetadata;
    this.refreshIntervalMillis = refreshIntervalMillis;
  }

  public void cloneRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {
    logger.info("Checking out {} ref {} in {}", uri, rev, repoDir);

    // Note that we need to do this in a more verbose way with `git init`, `git remote add...`, `git fetch`, and
    // `git checkout` due to intermittent issues with Gradle filesystem watching. `git clone` will fail when run in
    // a non-empty directory, which is often the case because Gradle will add a `.gradle/file-system.probe` file
    // very quickly. `git init` (etc) doesn't suffer this same limitation.
    try {
      //applyAuth(Git.cloneRepository() // TODO(tsr): figure out how to apply authentication
      // TODO(tsr): cleanup logging
      System.out.printf("...git clone %s %s --branch %s...%n", uri, repoDir, branchOrTag);
      // First we `git init`
      ExecOutput result = providers.exec(spec -> {
        spec.workingDir(repoDir);
        spec.commandLine("git", "init");
        spec.setIgnoreExitValue(true);
      });
      result.getResult().get().assertNormalExitValue();

      // Then we `git remote add origin <uri>` to set the remote
      result = providers.exec(spec -> {
        spec.workingDir(repoDir);
        spec.commandLine("git", "remote", "add", "origin", uri);
        spec.setIgnoreExitValue(true);
      });
      result.getResult().get().assertNormalExitValue();

      // Then `git fetch`
      result = providers.exec(spec -> {
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
      result = providers.exec(spec -> {
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
  }

  public void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {

    if (checkoutMetadata.containsKey(uri)) {
      CheckoutMetadata old = checkoutMetadata.get(uri);
      boolean sameRef = Objects.equals(current.getRef(), old.getRef());
      boolean sameBranch = current.getBranch().equals(old.getBranch());
      boolean upToDate = current.getLastUpdate() - old.getLastUpdate() < refreshIntervalMillis;
      if (sameRef && sameBranch && upToDate) {
        return;
      }
    }

    try {
      // TODO(tsr): cleanup logging
      System.out.printf("git symbolic-ref HEAD%n");
      ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
      ExecOutput result = providers.exec(spec -> {
        spec.workingDir(repoDir);
        spec.commandLine("git", "symbolic-ref", "HEAD");
        spec.setStandardOutput(stdOut);
        spec.setIgnoreExitValue(true);
      });
      result.getResult().get().assertNormalExitValue();

      String fullBranch = stdOut.toString(Charset.defaultCharset());
      System.out.printf("git symbolic-ref HEAD out = %s%n", fullBranch);

      if (fullBranch.startsWith("refs/heads/")) {
        logger.info("Pulling from {}", uri);
        //applyAuth(git.pull(), auth).call(); // TODO(tsr): figure out how to apply authentication
        result = providers.exec(spec -> {
          spec.workingDir(repoDir);
          spec.commandLine("git", "pull");
          spec.setIgnoreExitValue(true);
        });
        result.getResult().get().assertNormalExitValue();

        if (!rev.isEmpty()) {
          result = providers.exec(spec -> {
            spec.workingDir(repoDir);
            spec.commandLine("git", "checkout", rev);
            spec.setIgnoreExitValue(true);
          });
          result.getResult().get().assertNormalExitValue();
        } else {
          // TODO(tsr): ref.getName().endsWith(branchOrTag) etc.
          result = providers.exec(spec -> {
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
  }
}
