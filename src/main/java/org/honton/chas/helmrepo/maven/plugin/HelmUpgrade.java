package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Upgrade helm release(s)
 */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class HelmUpgrade extends HelmRelease {

  @Override
  public void addSubCommand(List<String> command) {
    command.add("upgrade");
    command.add("--install");
  }

  @Override
  public String chartReference(ReleaseInfo info) {
    return info.getChart();
  }

  @Override
  public Path releaseValues(ReleaseInfo info) throws IOException {
    Path releaseValuesPath = targetValuesPath.resolve(info.getName());
    Files.writeString(releaseValuesPath, info.getValueYaml());
    return releaseValuesPath;
  }

  @Override
  public Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder;
  }

}
