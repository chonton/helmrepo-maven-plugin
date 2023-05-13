package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Uninstall helm release(s)
 */
@Mojo(name = "uninstall", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class HelmUninstall extends HelmRelease {

  @Override
  public void addSubCommand(List<String> commandLine) {
    commandLine.add("uninstall");
  }

  @Override
  public String chartReference(ReleaseInfo info) {
    return null;
  }

  @Override
  public Path releaseValues(ReleaseInfo info) {
    return null;
  }

  @Override
  public Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder::descendingIterator;
  }

}
