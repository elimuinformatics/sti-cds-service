# STI CDS Service

STI CDS Service is a clinical decision-support app based on Kogito, CQL, and BPMN processes. It supports FHIR and CDS-Hooks.

## Getting started


### Prerequisite


You will need to install the following:


1. Git [(Download)](https://git-scm.com/)

2. Docker [(Download)](https://www.docker.com/products/docker-desktop/)

3. Docker compose [(Download)](https://docs.docker.com/compose/install/)

  


### Installing and building

1. Clone the git repository

2. Build the Docker image

```bash
docker build -t sti-cds-service .
```

### To run the service with an independent FHIR server for data and terminology server

1. Modify the docker-compose.yaml to make sure you initialize your environment variables correctly:

    * *FHIR_TERMINOLOGY_SERVER_URL*: Points to the FHIR Server where the terminology services need to be created. The Bundle of data for said repo can be found at src/test/resources/terminology-bundle.json.

    * *HIV_FHIR_SERVER_FILTER*: This property needs to be left blank. DRL rules in filter-cards.drl can be expanded to filter specific cards based on this value

2. Run *docker-compose build* 

To build the image with the new environment variables

3. Run *docker-compose up*

4. Follow instructions in `ehr-config-details.md` to integrate the service with an EHR.

### To test the service with a local HAPI-FHIR container
The local HAPI FHIR server started via `docker-compose-localtest.yaml` will provide patient data and serve as the terminology server. This is useful for testing the service locally.
The steps below will populate all the knowledge artifacts and valuesets in the FHIR server. They will also load test patient data into the FHIR server.
Two requests are included to demonstrate the service.

After running the "Installing and building" steps 1 and 2, run these steps:

1. Build the docker containers
```bash
docker compose -f docker-compose-localtest.yaml build
```

2. Start the containers
```bash
docker compose -f docker-compose-localtest.yaml up
```
3. Test the service.
From `src/test/resources`:
- In postman open the collection `GC Knowledge Artifacts.postman_collection.json`
Run the collection. 

Now open the postman collection `GC Testing.postman_collection.json`
Run the collection. Inspect the output of the two requests "Call presumptive GC..." and "Call confirmed GC..." to see the CDS Hooks cards.
