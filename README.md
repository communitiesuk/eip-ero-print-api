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
* `API_ERO_MANAGEMENT_URL` - the base URL of the ERO Management REST API service.
* `DYNAMODB_ENDPOINT` - the localstack endpoint
* `DYNAMODB_PRINT_DETAILS_TABLE_NAME` - table name to persist print details
* `DYNAMODB_SCHEDULER_LOCKS_TABLE_NAME` - table name to persist scheduler locks
* `JOB_PRINT_REQUESTS_CRON` - cron expression for print request job
* `THREAD_POOL_ZIP_CORE_SIZE` - number of core threads for the Zip producer thread pool 
* `THREAD_POOL_ZIP_MAX_SIZE` - maximum number of threads for the Zip producer thread pool
* `SFTP_HOST` - Hostname of the SFTP server to send print requests
* `SFTP_USER` - Username to use when connecting to the SFTP server 
* `SFTP_PRIVATE_KEY` - SSH private key to use when securely connecting to the SFTP server
* `SFTP_PRINT_REQUEST_UPLOAD_DIRECTORY` - Directory on the remote host to write print request files


### Authentication and authorisation
Requests are authenticated by the presence of a signed cognito JWT as a bearer token in the HTTP request `authorization` header.  
EG: `Authorization: Bearer xxxxxyyyyyyzzzzz.....`  
Requests are authorised by their membership of groups and roles carried on the JWT token.  
The UI application is expected to handle the authentication with cognito and pass the JWT token in the `authorization` header.

