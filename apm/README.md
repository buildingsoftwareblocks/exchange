# APM image

Create a container image of the APM
server [JavaMelody Server](https://github.com/javamelody/javamelody/wiki/UserGuideAdvanced).

## Build

Use the following command to add it to our local repository.

``docker build -t "ghcr.io/buildingsoftwareblocks/apm" .``

## Parameters

The following environment parameter can be set:

| Parameter        | Description                                   | Default |
|------------------|-----------------------------------------------|---------|
| APM_RESOLUTION   | period in seconds the server request the data | 120     |
| APM_SERVICE_PORT | port the server listens on                    | 8080    |
| APM_PARAMS       | additional parameters for the server          |         |

