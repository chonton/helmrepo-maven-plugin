package org.honton.chas.helmrepo.maven.plugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Behavior variations
 */
public interface CommandOptions extends GlobalReleaseOptions {

  /**
   * What is the chart reference for the helm command?  May return null.
   */
  String chartReference(ReleaseInfo info);

  /**
   * Given that info.valueYaml is not empty, write the content into a file and return the location of the file.  If no values option should be generated, return null.
   */
  Path releaseValues(ReleaseInfo info) throws IOException;

  /**
   * Given a list of releases, return an iterator that determines the traversal order
   */
  Iterable<ReleaseInfo> getIterable(LinkedList<ReleaseInfo> inOrder);

  /**
   * Add the helm subcommand to command line
   */
  void addSubCommand(List<String> commandLine);
}
