package org.honton.chas.helmrepo.maven.plugin;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class CommandLineGenerator {

  @Getter
  private List<String> command = new ArrayList<>();

  public CommandLineGenerator() {
    command.add("helm");
  }

  public CommandLineGenerator upgrade() {
    command.add("upgrade");
    command.add("--install");
    return this;
  }

  public CommandLineGenerator uninstall() {
    command.add("uninstall");
    return this;
  }

  public CommandLineGenerator appendRelease(ReleaseInfo release) {
    command.add(release.getName());
    command.add(release.getChart());
    return this;
  }

  public CommandLineGenerator appendGlobalReleaseOptions(GlobalReleaseOptions options) {
    if (options != null) {
      appendKubernetes(options.getKubernetes());
      appendValues(options.getValueYaml());
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

  private void appendValues(String valueYaml) {
    // TODO: !!!!!!!!
  }
}
