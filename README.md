# connection-maven-plugin

Set a property based upon whether a URL can be opened. 

# Rationale
You may wish to disable a plugin based upon whether a resource is available or not.

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
            <artifactId>connection-maven-plugin</artifactId>
            <version>0.0.1</version>
          </plugin>
        </plugins>
    </pluginManagement>

    <plugins>
      <plugin>
        <groupId>org.honton.chas</groupId>
        <artifactId>connection-maven-plugin</artifactId>
        <executions>
          <execution>
            <goals>
              <goal>connection</goal>
            </goals>
            <configuration>
              <url>https://example.com/sq:9000</url>
              <property>sonar.skip</property>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
