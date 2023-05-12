package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.LinkedList;

/** Upgrade helm release(s) */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class HelmUpgrade extends HelmRelease {

  @Override
  protected CommandLineGenerator getCommandLineGenerator(ReleaseInfo release) {
    return new CommandLineGenerator().upgrade();
  }

  @Override
  protected Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder) {
    return inOrder;
  }
}
