package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/** Upgrade helm release(s) */
@Mojo(name = "upgrade", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, threadSafe = true)
public class UpgradeMojo extends ReleaseMojo {

  @Override
  public CommandLineGenerator getCommandLineGenerator(Release release) {
    return new CommandLineGenerator().upgrade();
  }

}
