# lambdas-as-a-service

[![Build status](https://github.com/cake-lier/lambdas-as-a-service/actions/workflows/release.yml/badge.svg)](https://github.com/cake-lier/lambdas-as-a-service/actions/workflows/release.yml)
[![semantic-release: conventional-commits](https://img.shields.io/badge/semantic--release-conventional_commits-e10098?logo=semantic-release)](https://github.com/semantic-release/semantic-release)
[![Latest release](https://img.shields.io/github/v/release/cake-lier/lambdas-as-a-service)](https://github.com/cake-lier/lambdas-as-a-service/releases/latest/)
[![Scaladoc](https://img.shields.io/github/v/release/cake-lier/lambdas-as-a-service?label=scaladoc)](https://cake-lier.github.io/lambdas-as-a-service/io/github/cakelier)
[![Issues](https://img.shields.io/github/issues/cake-lier/lambdas-as-a-service)](https://github.com/cake-lier/lambdas-as-a-service/issues)
[![Pull requests](https://img.shields.io/github/issues-pr/cake-lier/lambdas-as-a-service)](https://github.com/cake-lier/lambdas-as-a-service/pulls)
[![Codecov](https://codecov.io/gh/cake-lier/lambdas-as-a-service/branch/main/graph/badge.svg?token=UX36N6CU78)](https://codecov.io/gh/cake-lier/lambdas-as-a-service)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=cake-lier_lambdas-as-a-service&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=cake-lier_lambdas-as-a-service)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=cake-lier_lambdas-as-a-service&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=cake-lier_lambdas-as-a-service)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=cake-lier_lambdas-as-a-service&metric=bugs)](https://sonarcloud.io/summary/new_code?id=cake-lier_lambdas-as-a-service)

## Why?

The project contained in this repository is my submission for the course "Distributed Systems,"
part of the course "Computer Science and Engineering - LM"
at "Alma Mater Studiorum — University of Bologna" for the academic year 2020 – 2021.

## What?

This project implements a prototype for a service of distributed computation.
This means that it can be configured to be deployed on different machines,
which can be part of the same cluster, but it is not mandatory.
The "main" component of the service will be able, under user command,
to ask the components dislocated in the other machines where to allocate a given executable.
After the negotiation, the "secondary"
component that has accepted the allocation job it will be responsible also for the execution of said executable,
which will be still requested by the user through the "main" component.
Some kind of role-based user authentication is used
in order to allow different users to use the service independently of one another.
The name of the project, "Lambdas as a Service" recalls exactly that:
the newer services of serverless computing called "Function as a Service"
which "AWS Lambda" by Amazon is one of the most prominent examples.

Being a project for the "Distributed Systems" course, it tried to apply most of the things discussed during the course.
In this project, agents where used for implementing the active part of the system,
while coordination and communication between actors in different machines happened thanks to tuple spaces.
Moreover, both the "main" component and the "tuple space" are web services, and as such, they were programmed.
This meant to leverage the "websocket"
technology to allow clients and servers to communicate between them in a reactive fashion.

## How (to run the system)?

All the components could be independently deployed on different machines.
The only requirement is that the components must be able to communicate via the HTTP protocol on a network,
whether it is the Internet or a LAN.
The deployment could be carried out
by running the Java JARs on the selected machines passing the correct environment variables.
Another way is
to use the docker images already containing the released JARs and passing the environment variables directly to them.
For demonstration purposes, a "docker compose" file is given,
which emulates a cluster with a main node and two secondary nodes.
The file leverages the docker images
previously quoted to create a local system which behaves exactly as it was distributed,
no configuration needed.
To start the service, only the command ```docker compose up``` must be used, in the main directory of the project.
Then, when all the components are up, signaled by three "up messages" in the docker log as below,
you can head to ```http://localhost:8080``` on your browser to access the service.
Other messages will appear in the log between these, this is normal,
but they are not relevant, so pay close attention to when all three appear.

```
lambdas-as-a-service-first_worker-1   | Worker bdd00fa6-db48-4b08-8b3c-488a1795f1c5 up
lambdas-as-a-service-second_worker-1  | Worker 17439dad-d9ed-4ee6-af05-7eca427ae025 up
lambdas-as-a-service-master-1         | Master up
```