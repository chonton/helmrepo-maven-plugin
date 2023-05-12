package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.LinkedList;

/**
 * Upgrade helm release(s)
 */
@Mojo(name = "uninstall", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public class HelmUninstall extends HelmRelease {

  @Override
  protected CommandLineGenerator getCommandLineGenerator(ReleaseInfo release) {
    return new CommandLineGenerator().uninstall();
  }

  @Override
  protected Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder::descendingIterator;
  }
}
