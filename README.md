# [PRIVACY-CENTRIC-SERVICE]

Some resources are extra protected. We should supply a reason when reading or updating them.

A separate service will be used for updating and reading this data. It will only allow to update/read the data when a
reason is given.

The properties will not be returned from mu-cl-resources anymore, but rather by this service.

This has impact on the implementation for the screens which contain sensitive data.

```
  privacy:
    image: lblod/privacy-centric-service:0.5.0
    environment:
      DEFAULT_GRAPH: "http://mu.semte.ch/graphs/privacy-centric-graph"
      SESSION_GRAPH: "http://mu.semte.ch/graphs/sessions"
      APP_GRAPH: "http://mu.semte.ch/graphs/organisatieportaal"
    links:
      - db:database
```

## Example request

### Read (POST):

http://localhost/person-information-requests

`Accept: application/vnd.api+json`

`Content-Type: application/vnd.api+json`

```
          {
             "data":{
                "type":"person-information-requests",
                "relationships":{
                   "person":{
                      "data":{
                         "type":"people",
                         "id":"fd88cb5a70db029111aa23a086b86f37"
                      }
                   },
                   "reason":{
                      "data":{
                         "type":"request-reasons",
                         "id":"3aeec145-acf3-4b6e-9c00-5b8e285736e0"
                      }
                   }
                }
             }
          }
```
### Validate SSN (POST):
http://localhost/person-information-validate-ssn/{personId}?ssn=${ssn}

`Accept: application/vnd.api+json`

`Content-Type: application/vnd.api+json`

### Update (POST):

http://localhost/person-information-updates

`Accept: application/vnd.api+json`

`Content-Type: application/vnd.api+json`

```
 {
             "data":{
                "type":"person-information-updates",
                "attributes":{
                   "date-of-birth":"2021-02-03",
                   "registration":"84.04.17-319.90"
                },
                "relationships":{
                   "person":{
                      "data":{
                         "type":"people",
                         "id":"fd88cb5a70db029111aa23a086b86f37"
                      }
                   },
                   "nationalities":
                      {
                         "data":[{
                            "type":"nationalities",
                            "id":"b461dca9e69a540cd821559f0873fe46"
                         }]
                      }
                   ,
                   "gender":{
                      "data":{
                         "type":"genders",
                         "id":"5ab0e9b8a3b2ca7c5e000028"
                      }
                   },
                   "reason":{
                      "data":{
                         "type":"request-reasons",
                         "id":"3aeec145-acf3-4b6e-9c00-5b8e285736e0"
                      }
                   }
                }
             }
          }
```
