# SMART Backend Service Registration
This application allows clients to register a JWKS endpoint for their SMART backend service.

The maintainer of the FHIR  server  can approve or deny requests. After approval, the `client_id` 
will be shared with the SMART backend service.

## Integration with Koppeltaal Authentication Service
In order for the [Koppeltaal Authentication Service](https://github.com/Koppeltaal/Koppeltaal-2.0-Authentication-Service) to know which SMART backend services
are approved, we currently simply share the same database.

## Design shortcuts
* Currently, the application does not contain roles. So after logging in, the user can both add a 
request and approve requests. 

*  The IRMA integration should be extracted into a custom spring-boot-starter project and
   integrate with Spring Security. This is more  secure and allows for 
   `org.springframework.data.annotation` annotations.

## Run locally
In order to run locally, you'll need:

* A running MySQL environment. 
* The [IRMA app](https://irma.app/) installed with a valid email property configured  

For MySQL, configure the `spring.datasource` properties in the `application.properties` and start 
the application. 

By default, the application should be available at 
[localhost:8080](http://localhost:8080)