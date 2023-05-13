package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;

/**
 * Helm goal base functionality
 */
public abstract class HelmGoal extends AbstractMojo {
  /**
   * Skip upgrade
   */
  @Parameter(property = "helm.skip", defaultValue = "false")
  boolean skip;

  public final void execute() throws MojoFailureException, MojoExecutionException {
    if (skip) {
      getLog().info("skipping helm");
    } else {
      try {
        doExecute();
      } catch (IOException e) {
        throw new MojoFailureException(e.getMessage(), e);
      }
    }
  }

  protected abstract void doExecute() throws  MojoExecutionException, IOException;
}
