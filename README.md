# helrepo-maven-plugin

Use maven repository to store helm charts.  This has three goals:
1. Package helm chart and add as a secondary artifact for build
2. Upgrade (install) helm chart release(s)
3. Uninstall helm chart release(s)

# Rationale


# Plugin
Plugin reports available at [plugin info](https://chonton.github.io/connection-maven-plugin/0.0.2/plugin-info.html).

There is a single goal: [connection](https://chonton.github.io/connection-maven-plugin/0.0.2/connection-mojo.html),
which binds by default to the *validate* phase.  

## Configuration
| Parameter    | Default  | Description                                                                         |
|--------------|----------|-------------------------------------------------------------------------------------|
| failBuild    | false    | Fail the build if the connection open failed                                        |
| property     |          | The property to receive the result of the query                                     |
| skip         | false    | Skip executing the plugin                                                           |
| userProperty | false    | If the property should be set as a user property, to be available in child projects |

# Examples

## Typical Use
```xml
  <build>
    <pluginManagement>
        <plugins>
          <plugin>
            <groupId>org.honton.chas</groupId>
            <artifactId>helmrepo-maven-plugin</artifactId>
            <version>0.0.1</version>
          </plugin>
        </plugins>
    </pluginManagement>

    <plugins>
      <plugin>
        <groupId>org.honton.chas</groupId>
        <artifactId>helmrepo-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>package</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
            
        </configuration>
      </plugin>
    </plugins>
  </build>
```
