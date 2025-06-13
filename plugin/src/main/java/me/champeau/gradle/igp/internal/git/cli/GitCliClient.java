package me.champeau.gradle.igp.internal.git.cli;

import java.io.File;
import java.util.List;
import java.util.Map;
import me.champeau.gradle.igp.internal.CheckoutMetadata;
import me.champeau.gradle.igp.internal.git.GitClientStrategy;
import me.champeau.gradle.igp.internal.git.cli.ExecOpsHelper.Result;
import me.champeau.gradle.igp.internal.git.jgit.DefaultAuthentication;
import org.gradle.api.GradleException;
import org.gradle.api.provider.ProviderFactory;
import org.slf4j.Logger;

public class GitCliClient implements GitClientStrategy {

  private final Logger logger;
  private final String git = "git";
  private final ExecOpsHelper ops;
  private final Map<String, CheckoutMetadata> checkoutMetadata;
  private final long refreshIntervalMillis;

  public GitCliClient(Logger logger, ProviderFactory providers, Map<String, CheckoutMetadata> checkoutMetadata,
      long refreshIntervalMillis) {

    this.logger = logger;
    this.ops = new ExecOpsHelper(providers);
    this.checkoutMetadata = checkoutMetadata;
    this.refreshIntervalMillis = refreshIntervalMillis;
  }

  public void cloneRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {

    checkAuth(auth);

    logger.info("Checking out {} ref {} in {}", uri, rev, repoDir);

    // Note that we need to do this in a more verbose way with `git init`, `git remote add...`, `git fetch`, and
    // `git checkout` due to intermittent issues with Gradle filesystem watching. `git clone` will fail when run in
    // a non-empty directory, which is often the case because Gradle will add a `.gradle/file-system.probe` file
    // very quickly. `git init` (etc) doesn't suffer this same limitation.
    try {
      repoDir.mkdirs();

      ops.exec(repoDir, List.of(git, "init"));
      ops.exec(repoDir, List.of(git, "remote", "add", "origin", uri));
      ops.exec(repoDir, List.of(git, "fetch"));
      ops.exec(repoDir, List.of(git, "checkout", getRev(rev, branchOrTag)));
    } catch (Exception e) {
      throw new GradleException("Unable to clone repository contents: " + e.getMessage(), e);
    } finally {
      checkoutMetadata.put(uri, current);
    }
  }

  public void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {

    checkAuth(auth);

    if (containsKey(checkoutMetadata, uri, current, refreshIntervalMillis)) {
      return;
    }

    try {
      Result result = ops.exec(repoDir, List.of(git, "symbolic-ref", "HEAD"));

      String fullBranch = result.stdOut.get();
      if (fullBranch.startsWith("refs/heads/")) {
        logger.info("Pulling from {}", uri);
        ops.exec(repoDir, List.of(git, "pull"));
      }

      logger.info("Checking out ref {} of {}", rev, uri);
      if (!rev.isEmpty()) {
        ops.exec(repoDir, List.of(git, "checkout", rev));
      } else {
        String resolve = branchOrTag;

        Result revParseResult = ops.exec(
            repoDir,
            List.of(git, "rev-parse", "--verify", resolve), spec -> spec.setIgnoreExitValue(true)
        );

        if (!revParseResult.isSuccess()) {
          Result showRefResult = ops.exec(repoDir, List.of(git, "show-ref", "--branches", "--tags"));
          // If we find `branchOrTag` in the list of all fully-qualified refs, use that, otherwise null and throw below
          resolve = showRefResult.stdOut.get().lines()
              .filter(line -> line.endsWith(branchOrTag))
              .findFirst()
              .orElse(null);
        }

        if (resolve != null) {
          ops.exec(repoDir, List.of(git, "checkout", resolve));
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

  /**
   * {@link GitCliClient} uses authentication provided by the user environment. Setting custom authentication via the
   * DSL is unsupported in this case, so we warn users.
   */
  private void checkAuth(DefaultAuthentication auth) {
    if (auth.isUserConfigured()) {
      logger.warn(
          "Custom authentication via the authentication DSL is incompatible with use of the git CLI client, and is therefore ignored.");
    }
  }

  private String getRev(String rev, String branchOrTag) {
    if (!rev.isEmpty()) {
      return rev;
    } else {
      return branchOrTag;
    }
  }
}
