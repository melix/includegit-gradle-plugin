package me.champeau.gradle.igp.internal.git.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.gradle.api.Action;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.process.ExecOutput;
import org.gradle.process.ExecSpec;

class ExecOpsHelper {

  static class Result {
    String stdOut;
    String stdErr;
    File workingDir;
    int exitCode;

    Result(String stdOut, String stdErr, File workingDir, int exitCode) {
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
    return exec(command, null, null);
  }

  Result exec(@Nonnull Iterable<String> command, @Nonnull File workingDir) {
    return exec(command, workingDir, null);
  }

  Result exec(@Nonnull Iterable<String> command, @Nonnull Action<ExecSpec> action) {
    return exec(command, null, action);
  }

  Result exec(@Nonnull Iterable<String> command, @Nullable File workingDir, @Nullable Action<ExecSpec> action) {
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    AtomicReference<File> theWorkingDir = new AtomicReference<>();

    ExecOutput result = providers.exec(spec -> {
      spec.workingDir(workingDir);
      spec.commandLine(command);
      spec.setStandardOutput(stdOut);
      spec.setErrorOutput(stdErr);

      if (action != null) {
        action.execute(spec);
      }

      theWorkingDir.set(spec.getWorkingDir());
    });

    return new Result(
        stdOut.toString(Charset.defaultCharset()),
        stdErr.toString(Charset.defaultCharset()),
        theWorkingDir.get(),
        result.getResult().get().getExitValue()
    );
  }
}
