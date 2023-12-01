# Changelists Automated

![Build](https://github.com/sblundy/changelist-protocol/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/23204-changelists-automated.svg)](https://plugins.jetbrains.com/plugin/23204-changelists-automated)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23204-changelists-automated.svg)](https://plugins.jetbrains.com/plugin/23204-changelists-automated)

[Documentation](https://sblundy.github.io/changelist-protocol/) — [Installation](#installation)

<!-- Plugin description -->
Manipulate IDE changelists externally via the [Built-In Server](https://blog.jetbrains.com/webide/2013/03/built-in-server-in-webstorm-6/) and [`jetbrains://` custom URL scheme](https://youtrack.jetbrains.com/issue/TBX-3965/Documentation-for-Toolbox-Reference-URL-Scheme)<sup>*</sup>.
With it you can integrate with local task trackers, scripts, and processes.

<sup>*</sup> `jetbrains://` custom URL scheme requires Jetbrains Toolbox. Also, it may not be available on all platforms

### Features

1. List current Changelists: 
   ```shell
   curl http://localhost:63342/api/changelist/changelist-protocol/
   ```
2. Add Changelist: 
   ```shell
   open jetbrains://idea/changelist/add?project=changelist-protocol&name=New%20Changelist
   curl --json '{"name":"New Changelist","comment":"New comment"}' http://localhost:63342/api/changelist/changelist-protocol
   ```
3. Update a Changelist: 
   ```shell
   open jetbrains://idea/changelist/update?project=changelist-protocol&name=New%20Changelist&comment=New%20comment
   curl -X PUT --json '{"comment":"New comment"}' http://localhost:63342/api/changelist/changelist-protocol/New%20Changelist
   ```
4. Delete a Changelist: 
   ```shell
   open jetbrains://idea/changelist/remove?project=changelist-protocol&name=New%20Changelist
   curl -X DELETE http://localhost:63342/api/changelist/changelist-protocol/Actual%20Changelist
   ```
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "changelist-protocol"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/sblundy/changelist-protocol/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
