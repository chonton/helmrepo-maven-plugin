package org.honton.chas.helmrepo.maven.plugin;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CommandLineGenerator {

  @Getter
  private final List<String> command;

  public CommandLineGenerator(CommandOptions behavior) {
    command = new ArrayList<>();
    command.add("helm");
    behavior.addSubCommand(command);
  }

  public CommandLineGenerator appendRelease(ReleaseInfo release, CommandOptions behavior) throws IOException {
    command.add(release.getName());
    String chartName = behavior.chartReference(release);
    if (chartName != null) {
      command.add(release.getChart());
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

    if (release.getValueYaml() != null) {
      Path valuePath = behavior.releaseValues(release);
      appendValues(valuePath);
    }

    return this;
  }

  public CommandLineGenerator appendGlobalReleaseOptions(CommandOptions options) {
    if (options != null) {
      appendKubernetes(options.getKubernetes());
      appendValues(options.getGlobalValuePath());
    }
    return this;
  }

  private CommandLineGenerator appendKubernetes(KubernetesInfo kubernetes) {
    if (kubernetes != null) {
      String context = kubernetes.getContext();
      if (context != null) {
        command.add("--kube-context");
        command.add(context);
      }
    }
    return this;
  }

  private void appendValues(Path valuePath) {
    if (valuePath != null) {
      command.add("--values");
      command.add(valuePath.toString());
    }
  }
}
