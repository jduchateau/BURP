# BURP Extension Points

BURP can be extended by providing additional logical source providers and custom RML functions. 
Extensions are discovered at runtime using Java's ServiceLoader mechanism.

## What You Can Extend

### Logical Source Providers

Implement `burp.ls.LogicalSourceProvider` to add support for new reference formulations or custom source types (e.g.,
new data formats, protocols, or storage systems).

**Interface:** `src/main/java/burp/ls/LogicalSourceProvider.kt`

```kotlin
package burp.ls

import burp.model.LogicalSource
import org.apache.jena.rdf.model.Resource
import java.nio.file.Path

interface LogicalSourceProvider {
    fun supports(referenceFormulation: Resource): Boolean
    fun create(
        ls: Resource,
        mappingDirectory: Path,
        currentWorkingDirectory: Path
    ): LogicalSource
}
```

### RML-FNML Functions

Implement `burp.model.fnmlutil.RMLFunction` to provide custom function behavior for FNML mappings (e.g., string
manipulation, data transformation, external API calls).

**Interface:** `src/main/java/burp/model/fnmlutil/RMLFunction.kt`

```kotlin
package burp.model.fnmlutil

interface RMLFunction {
    val name: String
    fun apply(parameters: Map<String, Any?>): List<Return>
}
```

## How to Implement an Extension

### 1. Create a New Project

Create a new Maven or Gradle project and add a dependency on BURP.
For the examples below we will assume Kotlin and Maven; Gradle or Java can also be used.

### 2. Implement the Extension

#### Example: Custom Logical Source Provider

```kotlin
package com.example.extensions

import burp.ls.LogicalSourceProvider
import burp.model.LogicalSource
import com.google.auto.service.AutoService
import org.apache.jena.rdf.model.Resource
import java.nio.file.Path

@AutoService(LogicalSourceProvider::class)
class MyCustomSourceProvider : LogicalSourceProvider {
    override fun supports(referenceFormulation: Resource): Boolean {
        // Check if this provider supports the given reference formulation
        return referenceFormulation.uri == "http://example.org/my-format"
    }

    override fun create(
        ls: Resource,
        mappingDirectory: Path,
        currentWorkingDirectory: Path
    ): LogicalSource {
        // Create and return your custom LogicalSource implementation
        // ...
    }
}
```

#### Example: Custom RML-FNML  Function

```kotlin
package com.example.extensions

import burp.model.fnmlutil.RMLFunction
import burp.model.fnmlutil.Return
import com.google.auto.service.AutoService

@AutoService(RMLFunction::class)
class MyCustomFunction : RMLFunction {
    override val name: String = "https://example.org/functions#myFunction"

    override fun apply(parameters: Map<String, Any?>): List<Return> {
        // Implement your custom function logic
        val input = parameters["https://example.org/functions#input"] as? String ?: ""
        val result = input.uppercase() // Example transformation
        return listOf(Return(result))
    }
}
```

### 3. Register with ServiceLoader

BURP uses Java's ServiceLoader to discover extensions. 
You can register your implementations in two ways:

#### Option A: Using Google AutoService

Add the `@AutoService` annotation to your implementation class (as shown in the examples above). The annotation
processor will automatically generate the service descriptor files.

**Maven Configuration:**

```xml

<dependencies>
    <dependency>
        <groupId>com.google.auto.service</groupId>
        <artifactId>auto-service-annotations</artifactId>
        <version>1.1.1</version>
    </dependency>
</dependencies>

<build>
<plugins>
    <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
            <annotationProcessorPaths>
                <path>
                    <groupId>com.google.auto.service</groupId>
                    <artifactId>auto-service</artifactId>
                    <version>1.1.1</version>
                </path>
            </annotationProcessorPaths>
        </configuration>
    </plugin>
</plugins>
</build>
```

#### Option B: Manual META-INF/services Files

Create service descriptor files manually in your project's resources:

**For Logical Source Providers:**

File: `src/main/resources/META-INF/services/burp.ls.LogicalSourceProvider`

```
com.example.extensions.MyCustomSourceProvider
com.example.extensions.AnotherSourceProvider
```

**For RML Functions:**

File: `src/main/resources/META-INF/services/burp.model.fnmlutil.RMLFunction`

```
com.example.extensions.MyCustomFunction
com.example.extensions.AnotherCustomFunction
```

Each line should contain the fully qualified class name of an implementation. Multiple implementations can be registered
in the same file (one per line).

### 4. Build Your Extension

Package your extension as a JAR:

```bash
mvn package
```

This produces `your-extension.jar` in the `target/` directory.

## Running BURP with Your Extension

Add your extension JAR to the classpath when running BURP:

```bash
java -cp "burp.jar:target/your-extension.jar" burp.Main -m mapping.ttl -o output.ttl
```

On Windows, use semicolon (`;`) as the classpath separator.

## Testing Your Extension

1. Create test RML mappings that use your custom reference formulation or function
2. Run BURP with your extension on the classpath
3. Verify that your extension is loaded and functioning correctly
4. Add unit tests for your implementation classes

