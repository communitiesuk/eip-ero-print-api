# eip-ero-print-api
Spring Boot microservice that :
- Provides an API for sending applications to print.

## Developer Setup
### Kotlin API Developers

Configure your IDE with the code formatter (ktlint):
```
$ ./gradlew ktlintApplyToIdea
```
This only needs doing once to setup your IDE with the code styles.

#### Running Tests
```
$ ./gradlew check
```
This will run the tests and ktlint. (Be warned, ktlint will hurt your feelings!)

#### Building docker images
```
$ ./gradlew check bootBuildImage
```
This will build a docker image for the Spring Boot application.

## Running the application
Either `./gradlew bootRun` or run the class `PrintApiApplication`

### External Environment Variables
The following environment variables must be set in order to run the application:
* `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` - the uri of the cognito ERO user pool JWT issuer.
* `SQS_SEND_APPLICATION_TO_PRINT_QUEUE_NAME` - the queue name for sending application to print

### Authentication and authorisation
Requests are authenticated by the presence of a signed cognito JWT as a bearer token in the HTTP request `authorization` header.  
EG: `Authorization: Bearer xxxxxyyyyyyzzzzz.....`  
Requests are authorised by their membership of groups and roles carried on the JWT token.  
The UI application is expected to handle the authentication with cognito and pass the JWT token in the `authorization` header.

