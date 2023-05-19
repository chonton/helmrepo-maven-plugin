package org.honton.chas.helmrepo.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Template helm release(s) */
@Mojo(name = "template", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class HelmTemplate extends HelmRelease {

  /** Location for expanded charts */
  @Parameter(defaultValue = "${project.build.directory}/helm")
  File templateDir;

  @Override
  public void addSubCommand(List<String> commandLine) {
    commandLine.add("template");
  }

  @Override
  public void releaseOptions(ReleaseInfo release, List<String> command) throws IOException {
    command.add("--output-dir");
    Path templatePath = Files.createDirectories(templateDir.toPath());
    command.add(pwd.relativize(templatePath).toString());
  }
}
