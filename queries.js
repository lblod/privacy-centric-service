import {
  sparqlEscapeUri,
  uuid,
  sparqlEscapeDate,
  sparqlEscapeString,
} from "mu";

const PREFIXES = `
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX mu: <http://mu.semte.ch/vocabularies/core/>
PREFIX persoon: <https://data.vlaanderen.be/ns/persoon#>
PREFIX ext: <http://mu.semte.ch/vocabularies/ext/>
PREFIX person: <http://www.w3.org/ns/person#>
PREFIX session: <http://mu.semte.ch/vocabularies/session/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX besluit: <http://data.vlaanderen.be/ns/besluit#>
`;

export function askSsn(graph, ssn, personId) {
  return `
    ${PREFIXES}
    ASK WHERE {
        GRAPH ${sparqlEscapeUri(graph)} {
          ?gsIdentificator <https://data.vlaanderen.be/ns/generiek#lokaleIdentificator> ?lokaleIdentificator.
          ?registrationId <https://data.vlaanderen.be/ns/generiek#gestructureerdeIdentificator> ?gsIdentificator.
          ?person persoon:registratie ?registrationId.
        }
        graph ?g {
          ?person a ?type;
            mu:uuid ?id.
        }
        FILTER(?lokaleIdentificator = ${sparqlEscapeString(
          ssn
        )} && ?type = person:Person && ?id != ${sparqlEscapeString(
    personId
  )}) .
      
      }
    `;
}

export function deleteDataPerson(graph, personId) {
  return `
    ${PREFIXES}
      
    DELETE {
      GRAPH ${sparqlEscapeUri(graph)} {
        ?person persoon:heeftGeboorte ?heeftGeboorte.
        ?heeftGeboorte ?p ?o.

        ?person persoon:registratie ?registration.
        ?registration <https://data.vlaanderen.be/ns/generiek#gestructureerdeIdentificator> ?identificator.
        ?identificator ?x ?y.
        ?registration ?t ?j.

        ?person persoon:geslacht ?geslacht.

        ?person persoon:heeftNationaliteit ?nationaliteit.
      }
    }
    WHERE {
      ?person mu:uuid ${sparqlEscapeString(personId)} .
      GRAPH ${sparqlEscapeUri(graph)} {
        optional {
          ?person persoon:heeftGeboorte ?heeftGeboorte.
          ?heeftGeboorte ?p ?o.
        }

        optional {
          ?person persoon:registratie ?registration.
          ?registration <https://data.vlaanderen.be/ns/generiek#gestructureerdeIdentificator> ?identificator.
          ?identificator ?x ?y.
          ?registration ?t ?j.
        }

        optional {
          ?person persoon:geslacht ?geslacht.
        }

        optional {
          ?person  persoon:heeftNationaliteit ?nationaliteit.
        }
      }
    }
  `;
}

export function getAccount(sessionGraphUri, sessionId) {
  return `
    ${PREFIXES}
    SELECT ?account
    WHERE {
      GRAPH ${sparqlEscapeUri(sessionGraphUri)} {
          ${sparqlEscapeUri(sessionId)} session:account ?account.
      }
    }

  `;
}

export function getGenderById(genderId) {
  return `
    ${PREFIXES}
    select ?genderUri where {
      ?genderUri mu:uuid ${sparqlEscapeString(genderId)}.
    }
  `;
}

export function getNationalityById(nationalityId) {
  return `
    ${PREFIXES}
    select ?nationality where {
      ?nationality mu:uuid ${sparqlEscapeString(nationalityId)}.
    }
    
  `;
}

export function getPersonBirth(graph, appGraph, personId) {
  return `
    ${PREFIXES}
    select distinct ?dateOfBirth ?registrationNumber ?genderId where {
      ?person mu:uuid ${sparqlEscapeString(personId)} .
      graph ${sparqlEscapeUri(graph)} {
        optional {
          ?person persoon:heeftGeboorte ?heeftGeboorte.
          ?heeftGeboorte persoon:datum ?dateOfBirth.
        }
      }
    }
  `;
}

export function getPersonRegistrationNumber(graph, appGraph, personId) {
  return `
    ${PREFIXES}
    select distinct ?dateOfBirth ?registrationNumber ?genderId where {
      ?person mu:uuid ${sparqlEscapeString(personId)} .
      graph ${sparqlEscapeUri(graph)} {
        optional {
          ?person persoon:registratie ?registratie.
          ?registratie <https://data.vlaanderen.be/ns/generiek#gestructureerdeIdentificator> ?identificator.
          ?identificator<https://data.vlaanderen.be/ns/generiek#lokaleIdentificator> ?registrationNumber.
        }
      }
    }
  `;
}

export function getPersonGender(graph, appGraph, personId) {
  return `
    ${PREFIXES}
    select distinct ?genderId where {
      ?person mu:uuid ${sparqlEscapeString(personId)} .
      graph ${sparqlEscapeUri(graph)} {
        optional {
          ?person persoon:geslacht ?geslacht.
          graph ${sparqlEscapeUri(appGraph)} {
            ?geslacht mu:uuid ?genderId.
          }
        }
      }
    }
  `;
}

export function getPersonNationalities(graph, appGraph, personId) {
  return `
    ${PREFIXES}
    select distinct ?nationaliteitId where {
      ?person mu:uuid ${sparqlEscapeString(personId)} .
      graph ${sparqlEscapeUri(graph)} {
        ?person persoon:heeftNationaliteit ?nationaliteit.
      }
      graph ${sparqlEscapeUri(appGraph)}  {
        ?nationaliteit mu:uuid ?nationaliteitId.
      }
    }
  `;
}

export function getReasonById(reasonId) {
  return `
    ${PREFIXES}
    select ?reasonUri where {
      ?reasonUri mu:uuid ${sparqlEscapeString(reasonId)}.
    }
    
  `;
}
export function requestReadReason(graph, accountUri, personId, reasonCodeUri) {
  let now = new Date().toISOString();
  let reasonId = uuid();
  return `
    ${PREFIXES}
    INSERT {
      graph ${sparqlEscapeUri(graph)} {
        <http://data.lblod.info/id/person-information-reads/${reasonId}> a ext:PersonInformationRead;
          mu:uuid "${reasonId}";
          ext:date "${now}"^^xsd:dateTime;
          ext:requester  ${sparqlEscapeUri(
            accountUri
          )};
          ext:person ?person ;
          ext:code  ${sparqlEscapeUri(
            reasonCodeUri
          )}.
      }
    } WHERE {
      ?person mu:uuid ${sparqlEscapeString(personId)} .
    }
  `;
}

export function insertDataPerson(
  graph,
  accountUri,
  personId,
  reasonCodeUri,
  dateOfBirth,
  registration,
  gender,
  nationalities
) {
  let now = new Date().toISOString();
  let reasonId = uuid();

  let query = "";

  if (dateOfBirth) {
    let dateOfBirthId = uuid();

    query = `
    ${query}
    <http://data.lblod.info/id/geboortes/${dateOfBirthId}>  a persoon:Geboorte;
      mu:uuid "${dateOfBirthId}";
      persoon:datum ${sparqlEscapeDate(dateOfBirth)}.

    ?person persoon:heeftGeboorte <http://data.lblod.info/id/geboortes/${dateOfBirthId}>.

    <http://data.lblod.info/id/person-information-updates/${reasonId}>  persoon:heeftGeboorte <http://data.lblod.info/id/geboortes/${dateOfBirthId}>.

  `;
  }

  if (registration) {
    let registrationId = uuid();
    let gestId = uuid();

    query = `
      ${query}
      <http://data.lblod.info/id/gestructureerdeIdentificatoren/${gestId}> a <https://data.vlaanderen.be/ns/generiek#GestructureerdeIdentificator>;
      mu:uuid "${gestId}";
      <https://data.vlaanderen.be/ns/generiek#lokaleIdentificator> ${sparqlEscapeString(
        registration
      )}.

      <http://data.lblod.info/id/identificatoren/${registrationId}> a <http://www.w3.org/ns/adms#Identifier>;
        mu:uuid "${registrationId}";
        <http://www.w3.org/2004/02/skos/core#notation> "Rijksregisternummer";
        <https://data.vlaanderen.be/ns/generiek#gestructureerdeIdentificator> <http://data.lblod.info/id/gestructureerdeIdentificatoren/${gestId}>.

      ?person persoon:registratie <http://data.lblod.info/id/identificatoren/${registrationId}>.

      <http://data.lblod.info/id/person-information-updates/${reasonId}>  persoon:registratie <http://data.lblod.info/id/identificatoren/${registrationId}>.
    `;
  }

  if (gender) {
    query = `
      ${query}
      ?person persoon:geslacht ${sparqlEscapeUri(gender)}.
      <http://data.lblod.info/id/person-information-updates/${reasonId}> persoon:geslacht ${sparqlEscapeUri(
      gender
    )}.
    `;
  }

  if (nationalities?.length) {
    let nationalitiesSubQuery = nationalities
      .map((nationality) => {
        return `
      ?person persoon:heeftNationaliteit ${sparqlEscapeUri(nationality)}.
      <http://data.lblod.info/id/person-information-updates/${reasonId}> persoon:heeftNationaliteit ${sparqlEscapeUri(
          nationality
        )}.
      `;
      })
      .join("");

    query = `
      ${query}

      ${nationalitiesSubQuery}
    `;
  }

  return `
  ${PREFIXES}
  INSERT {
    GRAPH ${sparqlEscapeUri(graph)} {
      <http://data.lblod.info/id/person-information-updates/${reasonId}>  a ext:PersonInformationUpdate;
      mu:uuid "${reasonId}";
      ext:date "${now}"^^xsd:dateTime;
      ext:requester  ${sparqlEscapeUri(accountUri)};
      ext:person ?person ;
      ext:code ${sparqlEscapeUri(reasonCodeUri)} .

      ${query}
    }
  } WHERE {
    ?person mu:uuid ${sparqlEscapeString(personId)} .
  }
  `;
}
