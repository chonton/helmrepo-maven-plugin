package org.honton.chas.helmrepo.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 */
public abstract class HelmGoal extends AbstractMojo {
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  MavenSession session;
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  MavenProject project;

  /**
   * Skip upgrade
   */
  @Parameter(property = "helm.skip", defaultValue = "false")
  boolean skip;


  public final void execute() throws MojoFailureException, MojoExecutionException {
    if (skip) {
      getLog().info("skipping helm");
    } else {
    doExecute();
    }
  }

  protected abstract void doExecute() throws MojoFailureException, MojoExecutionException;
}
