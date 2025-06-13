package me.champeau.gradle.igp.internal.git.cli;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.gradle.api.Action;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOutput;
import org.gradle.process.ExecSpec;

class ExecOpsHelper {

  static class Result {
    Provider<String> stdOut;
    Provider<String> stdErr;
    File workingDir;
    int exitCode;

    Result(Provider<String> stdOut, Provider<String> stdErr, File workingDir, int exitCode) {
      this.stdOut = stdOut;
      this.stdErr = stdErr;
      this.workingDir = workingDir;
      this.exitCode = exitCode;
    }

    boolean isSuccess() {
      return exitCode == 0;
    }

    void assertNormalExitValue() {
      if (exitCode != 0) {
        throw new IllegalStateException(String.format("Command failed with exit code %d: %s", exitCode, stdErr));
      }
    }
  }

  private final ProviderFactory providers;

  ExecOpsHelper(ProviderFactory providers) {
    this.providers = providers;
  }

  Result exec(@Nonnull Iterable<String> command) {
    return exec(null, command, null);
  }

  Result exec(@Nonnull File workingDir, @Nonnull Iterable<String> command) {
    return exec(workingDir, command, null);
  }

  Result exec(@Nonnull Iterable<String> command, @Nonnull Action<ExecSpec> action) {
    return exec(null, command, action);
  }

  Result exec(@Nullable File workingDir, @Nonnull Iterable<String> command, @Nullable Action<ExecSpec> action) {
    AtomicReference<File> theWorkingDir = new AtomicReference<>();

    ExecOutput result = providers.exec(spec -> {
      spec.commandLine(command);

      if (workingDir != null) {
        spec.workingDir(workingDir);
      }
      if (action != null) {
        action.execute(spec);
      }

      theWorkingDir.set(spec.getWorkingDir());
    });

    return new Result(
        result.getStandardOutput().getAsText(),
        result.getStandardError().getAsText(),
        theWorkingDir.get(),
        result.getResult().get().getExitValue()
    );
  }
}
