package me.champeau.gradle.igp.internal.git.jgit;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import me.champeau.gradle.igp.internal.CheckoutMetadata;
import me.champeau.gradle.igp.internal.git.GitClientStrategy;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.gradle.api.GradleException;
import org.slf4j.Logger;

public class JGitClient implements GitClientStrategy {

  private final Logger logger;
  private final Map<String, CheckoutMetadata> checkoutMetadata;
  private final long refreshIntervalMillis;

  public JGitClient(Logger logger, Map<String, CheckoutMetadata> checkoutMetadata,
      long refreshIntervalMillis) {
    this.logger = logger;
    this.checkoutMetadata = checkoutMetadata;
    this.refreshIntervalMillis = refreshIntervalMillis;
  }

  public void cloneRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {

    logger.info("Checking out {} ref {} in {}", uri, rev, repoDir);

    try {
      applyAuth(Git.cloneRepository()
          .setURI(uri)
          .setBranch(branchOrTag)
          .setDirectory(repoDir), auth)
          .call();
      if (!rev.isEmpty()) {
        try (Git git = Git.open(repoDir)) {
          git.checkout()
              .setName(rev)
              .call();
        }
      }
    } catch (GitAPIException | IOException e) {
      throw new GradleException("Unable to clone repository contents: " + e.getMessage(), e);
    } finally {
      checkoutMetadata.put(uri, current);
    }
  }

  public void updateRepository(File repoDir, String uri, String rev, String branchOrTag, CheckoutMetadata current,
      DefaultAuthentication auth) {

    if (containsKey(checkoutMetadata, uri, current, refreshIntervalMillis)) {
      return;
    }

    try (Git git = Git.open(repoDir)) {
      String fullBranch = git.getRepository().getFullBranch();
      if (fullBranch.startsWith("refs/heads/")) {
        logger.info("Pulling from {}", uri);
        applyAuth(git.pull(), auth).call();
      }
      logger.info("Checking out ref {} of {}", rev, uri);
      if (!rev.isEmpty()) {
        git.checkout()
            .setName(rev)
            .call();
      } else {
        Ref resolve = git.getRepository().findRef(branchOrTag);
        if (resolve == null) {
          List<Ref> refs = git.getRepository().getRefDatabase().getRefs();
          for (Ref ref : refs) {
            if (ref.getName().endsWith(branchOrTag)) {
              resolve = ref;
              break;
            }
          }
        }
        if (resolve != null) {
          git.checkout()
              .setName(resolve.getName())
              .call();
        } else {
          throw new GradleException("Branch or tag " + branchOrTag + " not found");
        }
      }
    } catch (GitAPIException | IOException e) {
      throw new GradleException("Unable to update repository contents: " + e.getMessage(), e);
    } finally {
      checkoutMetadata.put(uri, current);
    }
  }

  <C extends GitCommand<?>, R, TC extends TransportCommand<C, R>> TC applyAuth(TC command,
      DefaultAuthentication authentication) {
    authentication.getBasicAuth()
        .ifPresent(auth -> command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
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
          JSch defaultJSch = super.createDefaultJSch(fs);
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
}
