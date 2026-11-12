# Auxio-AI-Slop contribution guidelines

Auxio-AI-Slop is an unofficial Linux port of [Auxio](https://github.com/OxygenCobalt/Auxio).

## Crashes & Bugs
Log them in the [Issues](https://github.com/kvprcxelo/Auxio-AI-Slop/issues) tab.

Please keep in mind when reporting an issue:
- **Has it been reported?** Make sure an issue for the issue is not already there.
- **Has it been already fixed?** Make sure a fix wasn't already added.
- **Is it still relevant in the latest build?** Make sure to test it in the latest build.

If you do make an issue, Make sure to provide:
- A description of the bug/crash
- A summary of the steps to create the bug/crash
- The log output if possible, the longer the better.

If you have knowledge of Kotlin or Linux desktop development in general, you could also go about fixing the bug yourself and opening a [Pull Request](https://github.com/kvprcxelo/Auxio-AI-Slop/pulls).

## Feature Requests
These should also be logged in the [Issues](https://github.com/kvprcxelo/Auxio-AI-Slop/issues) tab.

Please keep in mind when requesting a feature:
- **Has it already been requested?** Make sure request for this feature is not already here.
- **Has it been already added?** Make sure this feature has not already been added in the most recent build.

If you do make a request, provide the following:
- What is it that you want?
- Is it related to some problem? If so, describe why.
- Why do you think it will benefit everyone's usage of the app?

If you have the knowledge, you can also implement the feature yourself and create a [Pull Request](https://github.com/kvprcxelo/Auxio-AI-Slop/pulls), but its recommended that **you create an issue beforehand to give the maintainer a heads up.**

## Code Contributions
If you have knowledge of Kotlin, feel free to to contribute to the project.
- If you want to help out with an existing bug report, comment on the issue that you want to fix saying that you are going to try your hand at it.
- If you want to add something, its recommended to open up an issue for what you want to change before you start working on it. That way it can be determined if the addition will be merged in the first place, and generally gives a heads-up overall.
- Do not bring non-free software into the project, such as Binary Blobs.
- Make sure you stick to the project's styling, which should be auto-formatted on every build.
- Please ***FULLY TEST*** your changes before creating a PR. Untested code will not be merged.
- Only **Kotlin** will be accepted, except for the case that a UI component must be vendored in the project.
- Keep your branch up to date with this repository's `dev` branch and continue to maintain it after you create the PR. This makes it less of a hassle to merge.
