---
name: "\U0001F680 New Release Tracker"
about: 'Create an issue to track the progress of a new Mandrel release'
title: '<version> Release Tracker'
labels: release
assignees: ''
---

# Prerequisites

- [x] All issues in the version's milestone are closed
- [x] Suite files marked for release in source tree
- [x] Version tag created
- [ ] Corresponding GraalVM CE builds released
- [ ] OpenJDK Temurin builds required as the base-JDK is released 

# Builds/Test Matrix

## Builds

|          | amd64    | aarch64 |
|----------|----------|----------|
| Linux    | :pause_button: | :pause_button: |
| Windows  | :pause_button: | :no_entry: |

## Quarkus Native Integration Tests

|          | amd64    | aarch64 |
|----------|----------|----------|
| Linux    | :pause_button: | :pause_button: |
| Windows  | :pause_button: | :no_entry: |

## [Mandrel Integration Tests](https://github.com/Karm/mandrel-integration-tests/)

|          | amd64    | aarch64 |
|----------|----------|----------|
| Linux    | :pause_button: | :pause_button: |
| Windows  | :pause_button: | :no_entry: |

## Legend

:white_check_mark: means the activity has successfully completed.

:hourglass_flowing_sand: means tests are still in progress.

:no_entry: means there is no build planned for that combination.

:pause_button: means the activity has not started yet.

:x: means there is an issue with the build or tests of that combination.

**Note:** When updating the change please use a hyperlink to make the emoji link to the corresponding CI run or GitHub issue.

# Next steps

- [ ] [Quarkus builder image](https://github.com/quarkusio/quarkus-images) generation
- [ ] Notify the appropriate channels
  - [ ] Quarkus-Dev mailing list
  - [ ] Mandrel slack channel
- [ ] Update sdkman's [update-mandrel.yml](https://github.com/sdkman/sdkman-disco-integration/blob/main/.github/workflows/update-mandrel.yml) on new major Mandrel or JDK release.