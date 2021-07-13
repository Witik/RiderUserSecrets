# .NET Core User Secrets
This plugin adds the ability to create and open User Secrets, for more information see the [User Secrets documentation](https://docs.microsoft.com/en-us/aspnet/core/security/app-secrets).

## How to use

1. Right-click on the .NET project file and navigate to `Tools` section
1.
    Click **Initialize User Secrets** to add support for User Secrets if project doesn't have it yet
    
    or
    
    Click **Open Project User Secrets** to open existing project secrets file for editing 

Both context menu actions are also supported for `Directory.Build.props` and `Directory.Build.targets` files

![Usage Example](usage.png)

## Changelog

### 1.2.0
- Added "Initialize User Secrets" menu item

### 1.1.0
- Fixed Rider 2021.1 compatibility
- Support secrets file in Directory.Build.props

### 1.0.1
 - Fixed Rider 2020.3 compatibility

### 1.0.0
 - Switched to using Gradle and the IntelliJ SDK
 - Added MsBuild support

### 0.2.3
 - Fixed Rider 2020.2 compatibility

### 0.2.2
 - Fixed Rider 2018.3 compatibility

### 0.2.1
 - Fixed user secrets path

### 0.2.0
 - Initial cross platform support
 
### 0.1.0
 - Default file contents

## Local development

Import the project in IntelliJ as a Gradle project.

* To run/debug, use the `Run/debug plugin` run configuration.
* To create a plugin distribution, use the `Build plugin` run configuration. Once completed, the plugin ZIP file will be created in the `build/distributions` folder.
