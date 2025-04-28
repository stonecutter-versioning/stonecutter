> [!IMPORTANT]
> Stonecutter has moved to [Codeberg](https://codeberg.org/stonecutter/stonecutter).
> This repository will not be updated.

# Stonecutter
Modern Gradle plugin for multi-version management.

## What is it?
Stonecutter is a Gradle plugin that allows working on a project that targets multiple compatible release versions of a dependency.  
It's mostly meant for mods Minecraft mods, but on itself the plugin is platform-independent.

This project is inspired by [Preprocessor](https://github.com/ReplayMod/preprocessor), [JCP](https://github.com/raydac/java-comment-preprocessor) and [The original Stonecutter](https://github.com/SHsuperCM/Stonecutter), 
expanding on their features and providing new ones.

This page briefly covers the functionality of Stonecutter. For more information visit the [Stonecutter website](https://stonecutter.kikugie.dev/).

## Features
### Comment syntax
Like other code processors, Stonecutter uses comments to specify how the code fragment should be modified. 
To accomplish that, Stonecutter uses the custom language and parser - Stitcher.  

Supported functionality:
```kotlin
// Simple semver condition
//? if >1.2.0
func()

// Targeting multiple lines
//? if >1.2.0 {
func1()
func2()
//?}

// Boolean operations
//? if >1.2.0 || <0.3.0
func()

// Comparing provided variables
//? if id: >1.2.0
func()
```

And much more! See the [Documentation website](https://stonecutter.kikugie.dev/stonecutter/introduction.html) for a detailed guide.

### Performance
Even though the Stitcher parser is optimised on itself, Stonecutter caches both the produced ASTs and transformed files to speed up version switching.

### Clean code
One of the first reasons to work on Stonecutter was the clutter of Preprocessor and JCP.  
For example, here's a block of code from the Preprocessor repository:
```java
//#if MC>=11200
category.addDetail(name, callable::call);
//#else
//$$ category.setDetail(name, callable::call);
//#endif
```
The same functionality in Stonecutter is accomplished using:
```java
//? >=1.12 {
category.addDetail(name, callable::call);
//?} else 
/*category.setDetail(name, callable::call);*/
```
The difference is not too apparent on a small example, but when it comes to disabling entire methods, 
a single multiline comment looks much better than a dozen `//$$ `.

Stonecutter also has inline comment support, which allows doing this:
```java
target.call(arg1, arg2, /*? if dependency: >=0.2 >>*//*arg3,*/ arg4)
```

There are even more features, described as usual on the [Documentation website](https://stonecutter.kikugie.dev/stonecutter/introduction.html).

## Roadmap
Stonecutter already went through countless hours of development and research, but there is still more to go.  
Planned features include:
- Resource file support
- Intellij IDEA plugin
- Multi-file processing scripts
- Project tree management (for Architectury support)
- File transformation plugins

If you have any questions or suggestions, join my [Discord server](https://discord.gg/TBgNUCfryS) or open an issue/start a discussion.
