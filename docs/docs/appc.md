---
title: Running AppC Containers on Marathon
---

# Running AppC Containers on Marathon

This document describes how to run [AppC](https://github.com/appc/spec) containers on
Marathon using the native AppC support added in Apache Mesos version 1.0 (released July 2016).

## Configuration

Note that DCOS clusters are already configured to run AppC containers, so
DCOS users do not need to follow the steps below.

#### Configure mesos-agent

  <div class="alert alert-info">
    <strong>Note:</strong> All commands below assume `mesos-agent` is being run
    as a service using the package provided by 
    <a href="http://mesosphere.com/2014/07/17/mesosphere-package-repositories/">Mesosphere</a>
  </div>

1. Increase the executor timeout to account for the potential delay in pulling an AppC image to the agent.


    ```bash
    $ echo '5mins' > /etc/mesos-agent/executor_registration_timeout
    ```

2. Restart `mesos-agent` process to load the new configuration

#### Configure Marathon

1. Increase the Marathon [command line option]({{ site.baseurl }}/docs/command-line-flags.html)
`--task_launch_timeout` to at least the executor timeout, in milliseconds, 
you set on your slaves in the previous step.


## Overview

To use the native AppC container support, add a `container` field containing an "appc" field
to your app definition JSON:

```json
{
  "container": {
    "type": "MESOS",
    "appc": {
      "image": "group/image"
    },
    "volumes": [
      {
        "containerPath": "/etc/a",
        "hostPath": "/var/data/a",
        "mode": "RO"
      },
      {
        "containerPath": "/etc/b",
        "hostPath": "/var/data/b",
        "mode": "RW"
      }
    ]
  }
}
```

where `type` must be "MESOS" and `volumes` is optional.

For convenience, the mount point of the Mesos sandbox is available in the
environment as `$MESOS_SANDBOX`.

### Advanced Usage

#### Forcing an Image Pull

Marathon supports an option to pull the image anew before launching each task.

```json
{
  "type": "MESOS",
  "appc": {
    "image": "group/image",
    "forcePullImage": true
  }
}
```

#### Specifying an Image ID

An AppC image can be addressed and verified against a hash value derived from its uncompressed form.
Such an "image ID" uniquely and globally references the image. It comprises of a hash algorithm name and a hash value,
separated by a dash ("-"). Example:
```json
{
  "type": "MESOS",
  "appc": {
    "image": "group/image",
    "id": "sha512-f7eb89d44f44d416f2872e43bc5a4c6c3e12c460e845753e0a7b28cdce0e89d2"
  }
}
```
Currently, the only hash algorithm supported by Mesos and thus Marathon is sha512.

#### Specifying additional labels

Additional labels can be specified for an AppC image to facilitate image discovery and dependency resolution.
Such labels must be fields representing key-value pairs in a "labels" object. Every key is restricted to the
[AC Identifier formatting rules](https://github.com/appc/spec/blob/master/spec/types.md#ac-identifier-type)
and every value can be an arbitrary string. Furthermore, keys must be unique, and cannot be "name".
These label keys are suggested:

  - "version", which should be unique for every build of an app (on a given "os"/"arch" combination).
  - "os"
  - "arch"

Example:
```json
{
  "type": "MESOS",
  "appc": {
    "image": "group/image",
    "labels": {
      "version": "1.2.0",
      "arch": "amd64",
      "os": "linux"
    }
  }
}
```

Please refer to [AppC documentation](https://github.com/appc/spec/blob/master/spec/aci.md) for more details.

#### Command vs Args

As of version 0.7.0, Marathon supports an `args` field in the app JSON.  It is
invalid to supply both `cmd` and `args` for the same app.  The behavior of `cmd`
is as in previous releases (the value is wrapped by Mesos via
`/bin/sh -c '${app.cmd}`). Please refer to the [documentation for Docker container support](native-docker.md)
regarding specifics on this topic.
