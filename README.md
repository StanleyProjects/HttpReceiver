# HttpReceiver
A tool for receiving HTTP messages and responding to them.

---

## Snapshot

![version](https://img.shields.io/static/v1?label=version&message=0.0.1-SNAPSHOT&labelColor=212121&color=2962ff&style=flat)

- [Maven](https://s01.oss.sonatype.org/content/repositories/snapshots/com/github/kepocnhh/HttpReceiver/0.0.1-SNAPSHOT)
- [Documentation](https://StanleyProjects.github.io/HttpReceiver/doc/0.0.1-SNAPSHOT)

### Build
```
$ gradle lib:assembleSnapshotJar
```

### Import
```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("com.github.kepocnhh:HttpReceiver:0.0.1-SNAPSHOT")
}
```

---

## Unstable

![version](https://img.shields.io/static/v1?label=version&message=0.1.0u-SNAPSHOT&labelColor=212121&color=2962ff&style=flat)

- [Maven](https://s01.oss.sonatype.org/content/repositories/snapshots/com/github/kepocnhh/HttpReceiver/0.1.0u-SNAPSHOT)

### Build
```
$ gradle lib:assembleUnstableJar
```

### Import
```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("com.github.kepocnhh:HttpReceiver:0.1.0u-SNAPSHOT")
}
```

---
