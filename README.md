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
[localhost:8086](http://localhost:8086)

### Configuration with Secrets

Sensitive configuration values (database credentials, tokens, keys) can be externalized to a separate file:

1. Copy `src/main/resources/application-secrets.properties.example` to `src/main/resources/application-secrets.properties`
2. Fill in your actual secret values
3. The secrets file is gitignored and will be automatically loaded by Spring Boot

### Downloading shared libraries

Koppeltaal 2.0 uses shared libraries as certain functionality (e.g. JWKS or SMART Backend Services)
are used in many components. These shared libraries are published
to [GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry).
In order to download these, you'll need a GitHub
[Personal Access Token](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token)
with at least the  `read:packages` scope.

After you have this token, you must add GitHub as a Maven `server` to your `~/.m2/settings.xml`.

The `<server>` tag should be added like this, replace the username and password:

```xml

<server>
  <id>github</id>
  <username>{{YOUR_GITHUB_USERNAME}}</username>
  <password>{{YOUR_GITHUB_PERSONAL_ACCESS_TOKEN}}</password>
</server>
```