# Contributing to Mandrel

Mandrel is a downstream distribution of [GraalVM CE](www.github.com/oracle/graal).
As a result contributions to Mandrel should be limited to:

1. Backports from the upstream repository of GraalVM (e.g. bringing a feature/bug-fix from GraalVM CE 20.2 to Mandrel 20.1)
2. Mandrel specific bug fixes (e.g. patches to make it work with upstream OpenJDK instead of [LabsJDK CE](https://github.com/graalvm/labs-openjdk-11))
3. Mandrel specific documentation

Anything not fitting the above list should probably be contributed upstream instead.

## Workflow

Mandrel follows the github workflow.
To contribute please fork this repository and open a pull request (PR).

## Repository Structure

Mandrel, being a downstream distribution of GraalVM CE, is not developed on the master branch.
Instead version specific branches, based on the corresponding GraalVM CE release, are used for development and maintenance. 
For instance, Mandrel 20.1.x.x releases are developed in the `mandrel/20.1` branch.
Similarly, Mandrel 20.2.x.x releases are developed in the `mandrel/20.2` branch and so on.

The `master` branch is used only as a landing page and for hosting some housekeeping github actions workflows and templates.

In addition to the `mandrel/XX.Y` branches Mandrel also includes `graal/X` branches like `graal/master`.
These branches are mirrors of the corresponding upstream branch and are being synchronized and tested nightly, in an effort to detect breaking changes upstream as soon as possible. 

## Oracle Contributor Agreement (OCA)

As part of the GraalVM community and similarly to OpenJDK, Mandrel requires contributors to sign the [Oracle Contributor Agreement (OCA)](https://www.oracle.com/technical-resources/oracle-contributor-agreement.html).
