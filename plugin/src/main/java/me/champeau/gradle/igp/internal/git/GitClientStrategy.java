package me.champeau.gradle.igp.internal.git;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import me.champeau.gradle.igp.internal.CheckoutMetadata;
import me.champeau.gradle.igp.internal.git.jgit.DefaultAuthentication;

public interface GitClientStrategy {
  void cloneRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current, DefaultAuthentication auth);
  void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current, DefaultAuthentication auth);

  default boolean containsKey(
      Map<String, CheckoutMetadata> checkoutMetadata,
      String uri,
      CheckoutMetadata current,
      long refreshIntervalMillis
  ) {
    CheckoutMetadata old = checkoutMetadata.get(uri);

    // Can happen when checking out forks.
    if (old == null) {
      return false;
    }

    boolean sameRef = Objects.equals(current.getRef(), old.getRef());
    boolean sameBranch = current.getBranch().equals(old.getBranch());
    boolean upToDate = current.getLastUpdate() - old.getLastUpdate() < refreshIntervalMillis;

    return sameRef && sameBranch && upToDate;
  }
}
