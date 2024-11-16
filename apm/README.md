# APM image

Create a container image of the APM (Application Performance Monitoring)
server [JavaMelody Server](https://github.com/javamelody/javamelody/wiki/UserGuideAdvanced).

## Build

Use the following command to add it to our local repository.

``docker build -t "ghcr.io/buildingsoftwareblocks/apm" .``

## Parameters

The following environment parameter can be set:

| Parameter      | Description                                   | Default |
|----------------|-----------------------------------------------|---------|
| APM_RESOLUTION | period in seconds the server request the data | 120     |


