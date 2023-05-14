package org.honton.chas.helmrepo.maven.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public class CommandLineGenerator {

  @Getter private final List<String> command;

  public CommandLineGenerator(CommandOptions behavior) {
    command = new ArrayList<>();
    command.add("helm");
    behavior.addSubCommand(command);
  }

  public CommandLineGenerator appendRelease(ReleaseInfo release, CommandOptions options)
      throws IOException {
    command.add(release.getName());
    String chartName = options.chartReference(release);
    if (chartName != null) {
      command.add(chartName);
    }

    if (release.getNamespace() != null) {
      command.add("--namespace");
      command.add(release.getNamespace());
    }

    command.add("--wait");
    if (release.getWait() != 0) {
      command.add("--timeout");
      command.add(release.getWait() + "s");
    }

    String yamlContent = release.getValueYaml();
    if (yamlContent != null) {
      Path yamlPath = options.releaseValues(release.getName() + ".yaml");
      if (yamlPath != null) {
        Files.writeString(yamlPath, yamlContent);
        appendValues(yamlPath);
      }
    }

    return this;
  }

  public CommandLineGenerator appendGlobalReleaseOptions(CommandOptions options) {
    if (options != null) {
      appendKubernetes(options.getKubernetes());
      Path globalValuePath = options.getGlobalValuePath();
      if (globalValuePath != null) {
        appendValues(globalValuePath);
      }
    }
    return this;
  }

  private void appendKubernetes(KubernetesInfo kubernetes) {
    if (kubernetes != null) {
      String context = kubernetes.getContext();
      if (context != null) {
        command.add("--kube-context");
        command.add(context);
      }
    }
  }

  private void appendValues(Path valuePath) {
    command.add("--values");
    command.add(valuePath.toString());
  }
}
