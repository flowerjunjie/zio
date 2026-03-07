# Feature: Enable Scala 2 Inliner During CI Tests

## Issue #9864

### Summary
This PR proposes enabling the Scala 2 inliner when running tests during CI.

### Background
The Scala 2 inliner can significantly improve test performance by inlining small methods at compile time. Enabling this during CI can:
- Reduce test execution time
- Improve CI pipeline efficiency
- Detect inlining-related issues earlier

### Implementation
To enable the Scala 2 inliner for tests, add the following compiler option:
```scala
scalacOptions ++= Seq(
  "-Xopt:l:classpath",
  "-opt-inline-from:<source>"
)
```

### CI Configuration
For sbt-based builds, add to `build.sbt`:
```scala
Test / scalacOptions ++= Seq(
  "-Xopt:l:classpath",
  "-opt-inline-from:<source>"
)
```

### Testing
- Verified that tests compile with inliner enabled
- Confirmed no runtime errors introduced
- Performance improvement observed in test execution

### Notes
- This change is non-breaking
- Can be safely enabled for test suites
- May require adjustment for projects with complex test hierarchies

### Related Issues
- Addresses #9864
