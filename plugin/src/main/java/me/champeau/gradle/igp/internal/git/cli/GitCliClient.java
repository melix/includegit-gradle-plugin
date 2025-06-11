package me.champeau.gradle.igp.internal.git.cli;

import java.io.File;
import java.util.List;
import java.util.Map;
import me.champeau.gradle.igp.internal.CheckoutMetadata;
import me.champeau.gradle.igp.internal.git.GitClientStrategy;
import me.champeau.gradle.igp.internal.git.jgit.DefaultAuthentication;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ProviderFactory;
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
      ExecOpsHelper ops = new ExecOpsHelper(providers);

      ops.exec(List.of("git", "init"));
      ops.exec(List.of("git", "remote", "add", "origin", uri));
      ops.exec(List.of("git", "fetch"));
      ops.exec(List.of("git", "checkout", getRev(rev, branchOrTag)));
    } catch (Exception e) {
      throw new GradleException("Unable to clone repository contents: " + e.getMessage(), e);
    } finally {
      checkoutMetadata.put(uri, current);
    }
  }

  private String getRev(String rev, String branchOrTag) {
    if (!rev.isEmpty()) {
      return rev;
    } else {
      return branchOrTag;
    }
  }

  public void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {

    if (containsKey(checkoutMetadata, uri, current, refreshIntervalMillis)) {
      return;
    }

    try {
      ExecOpsHelper ops = new ExecOpsHelper(providers);

      ExecOpsHelper.Result result = ops.exec(List.of("git", "symbolic-ref", "HEAD"));

      String fullBranch = result.stdOut;
      if (fullBranch.startsWith("refs/heads/")) {
        logger.info("Pulling from {}", uri);
        ops.exec(List.of("git", "pull"));
      }

      logger.info("Checking out ref {} of {}", rev, uri);
      if (!rev.isEmpty()) {
        ops.exec(List.of("git", "checkout", rev));
      } else {
        // TODO(tsr): try to validate that branchOrTag is a real reference
        //  ref.getName().endsWith(branchOrTag) etc.
        String resolve = branchOrTag;
        if (resolve != null) {
          ops.exec(List.of("git", "checkout", branchOrTag));
        } else {
          throw new GradleException("Branch or tag " + branchOrTag + " not found");
        }
      }
    } catch (Exception e) {
      throw new GradleException("Unable to update repository contents: " + e.getMessage(), e);
    } finally {
      checkoutMetadata.put(uri, current);
    }
  }
}
