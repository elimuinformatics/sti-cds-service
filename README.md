# STI CDS Service

STI CDS Service is a clinical decision-support app based on Kogito, CQL, and BPMN processes. It supports FHIR and CDS-Hooks.

## Getting started


### Prerequisite


You will need to install the following:


1. Git [(Download)](https://git-scm.com/)

2. Docker [(Download)](https://www.docker.com/products/docker-desktop/)

3. Docker compose [(Download)](https://docs.docker.com/compose/install/)

  


### Installing

1. Clone the git repository

2. Compile the projects

```bash
docker build -t sti-cds-service .
```

3. Modify the docker-compose.yml to make sure you initialize your environment variables correctly:

    * *FHIR_TERMINOLOGY_SERVER_URL*: Points to the FHIR Server where the terminology services need to be created. The Bundle of data for said repo can be found at src/test/resources/terminology-bundle.json

    * *HIV_FHIR_SERVER_FILTER*: This property needs to be left blank. DRL rules in filter-cards.drl can be expanded to filter specific cards based on this value

4. Run *docker-compose build* 

To build the image with the new environment variables

5. Run *docker-compose up*

To start the instance

