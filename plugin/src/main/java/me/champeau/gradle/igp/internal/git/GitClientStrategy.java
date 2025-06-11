package me.champeau.gradle.igp.internal.git;

import java.io.File;
import me.champeau.gradle.igp.internal.CheckoutMetadata;

public interface GitClientStrategy {
  void cloneRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current, DefaultAuthentication auth);
  void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current, DefaultAuthentication auth);
}
