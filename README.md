## Gatling protocol manager for Git

gatling-git is a plug-in protocol manager for [Gatling](https://gatling.io), the popular Scala-based
performance testing tool.

### Why yet another Gatling plugin?

[Gatling](https://gatling.io) allows creating simple end-to-end test scenarios for web applications
or REST-API services by leveraging the Akka/Netty client protocol stack. However, when performing
testing against services that have also a Git server component, a more specific protocol handler
built on top of the Git protocol itself is needed.

Gatling-git is a JGit-based implementation of the Gatling protocol manager. You can build higher
level test scenarios using directly the Git verbs in your tests.

### Prerequisites

* [Scala 2.13][scala]
* [Gatling 3.9.0][gatling-3.9.0]

[gatling-3.9.0]: https://mvnrepository.com/artifact/io.gatling/gatling-core/3.9.0
[scala]: https://www.scala-lang.org/download/

### Using the Gatling Git test scenarios and protocol

Add the gatling-git plugin, in addition to the standard Gatling imports.

```scala
import com.github.barbasa.gatling.git._
import com.github.barbasa.gatling.git.protocol._
import com.github.barbasa.gatling.git.request.builder._
```

Add the following dependencies to your Gatling SBT project:

```scala
val GatlingVersion = "3.9.0"

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % GatlingVersion % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-test-framework" % GatlingVersion % Test
libraryDependencies += "com.gerritforge" %% "gatling-git" % "1.0.4" % Test
```

The gatling-git plugin relies on a global configuration defined in the `application.conf` of
your tests and read through a global implicit configuration object that needs to be defined
in your test class.

```scala
implicit val gatlingGitConfiguration = GatlingGitConfiguration()
```

The gatling-git plugin provides the following Git-specific actions for your test scenarios:
* clone
* fetch
* pull
* push

Use any of the above inside your Gatling test scenario, like in the following example:

```scala
  val gitCloneScenario = scenario("Git clone from Gerrit")
                           .exec(new GitRequestBuilder(
                                     GitRequestSession(
                                       "clone", "ssh://github.com/GerritForge/gatling-git", "master")))
```

Inject a number of concurrent users into the scenario using the usual Gatling DSL syntax,
and then setup the Git protocol associated to the simulation.

For instance, for a constant traffic from 2 concurrent users for a 1 minute test:

```scala
val gitProtocol = GitProtocol()

  setUp(
    gitPush.inject(constantConcurrentUsers(2) during (1.minute))
  ).protocols(gitProtocol)
```

### Build from source

See below the instructions on how to build the gatling-git plugin from source.
It requires [Scala 2.13][scala] and [SBT 1.8.2][sbt-1.8.2]

```bash
sbt compile
```

[sbt-1.8.2]: https://www.scala-sbt.org/download.html

### Setup

If you are running SSH commands the private keys of the users used for testing need to go in `/tmp/ssh-keys`.
The keys need to be generated this way (JSch won't validate them [otherwise](https://stackoverflow.com/questions/53134212/invalid-privatekey-when-using-jsch)):

```bash
ssh-keygen -m PEM -t rsa -C "test@mail.com" -f /tmp/ssh-keys/id_rsa
```

NOTE: Don't forget to add the public keys for the testing user(s) to your git server.

#### Using Gatling's feeder input

The ReplayRecordsScenario is expecting the [src/test/resources/data/requests.json](/gatling-extension/src/test/resources/data/requests.json) file.
Here below an example:

```json
[
  {
    "url": "ssh://admin@localhost:29418/loadtest-repo.git",
    "cmd": "clone"
  },
  {
    "url": "http://localhost:8080/loadtest-repo.git",
    "cmd": "fetch"
  },
  {
    "url": "http://localhost:8080/loadtest-repo.git",
    "cmd": "push",
    "ref-spec": "HEAD:refs/for/master",
    "compute-change-id": true
  }
]
```

Valid commands that can be specified in the `cmd` parameter are:

* `clone`: clone the remote repository
* `fetch`: run a git-upload-pack
* `pull`: run a git-upload-pack and then merge the remote fetched head to the local branch
* `push`: push the local ref to the remote Git server
* `cleanup-repo`: clean up all the content of the local repository

The common parameters are:

* `url`: The HTTP or SSH Git URL of the remote repository.
* `ref-spec`: ref-spec of the `push` operation. Can be specified with a simple branch name or have
  the more general form of `local:remote` refs.
* `ignoreFailureRegexps`:
  In some scenarios some failures are expected due to the nature of the tests
  rather than actual git protocol errors.

  As an example, WantNotValid exceptions may be thrown by jgit during
  reachability checks, in a scenario where tests perform force pushes with a
  high concurrency and high frequency.

  In this case, consumers of gatling-git might explicitly list failure messages
  that are expected and thus are to be ignored.
  default: `empty list`

  To ignore `want <sha1> not valid` exceptions during clones for example, the
  following request can be built:

  ```
    new GitRequestBuilder(
      GitRequestSession(
        "clone",
        s"$url/${testConfig.project}",
        "${refSpec}",
        ignoreFailureRegexps = List(".*want.+not valid.*")
      )
  ```
* `repoDirOverride` allows to specify a directory that will be used as the request's git repository.
  This can be useful if we want to test how different commands interact on the same repository.

  *Note*: When used with a Clone command, the repoOverride specifies the target worktree for the
  clone operation. It is the caller’s responsibility to ensure that the chosen worktree name is
  available. This can be achieved by selecting a unique, non-existing name, calling cleanup-repo to
  clear previous worktrees, or setting the deleteOnExit flag to automatically remove the worktree
  when the process completes.

The push operation have optional extra parameters:

* `force`: set to `true` performs a forced push
* `compute-change-id`: set to `true` for generating the standard `Change-Id` when pushing commits
  for review.

### How to run the tests

All tests:
```
sbt "Gatling/test"
```

Single test:
```
sbt "Gatling/testOnly com.github.barbasa.gatling.git.ReplayRecordsScenario"
```

Generate report:
```
sbt "Gatling/lastReport"
```

### How to use with docker

For an example of how to use this in Docker, see [gatling-sbt-gerrit-test](https://github.com/GerritForge/gatling-sbt-gerrit-test/blob/5b30c98438411307e8a4da01cb93cfac8e7a0ecb/build.sbt#L49)