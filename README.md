# eip-ero-print-api
Spring Boot microservice that :
- Provides an API for sending applications to print.

## Developer Setup
### Kotlin API Developers

Configure your IDE with the code formatter (ktlint):
```
$ ./gradlew ktlintApplyToIdea
```
This only needs doing once to set up your IDE with the code styles.
If you prefer, you can run `./gradlew addKtlintFormatPreCommitHook` to add a pre-commit hook to format your code on commit.

#### AWS CodeArtifact Access Set Up

To access libraries stored in the AWS CodeArtifact repository an access token is required that the build script fetches
in the background using the credentials for the `code-artifact` profile. To create this profile on your developer
machine follow these instructions:

```shell
aws configure --profile code-artifact
```

At the prompts configure the `code-artifact` profile as follows:
* Your AWS Access Key ID
* Your AWS Secret Access Key
* Default region name, `eu-west-2`
* Default output format, `json`

Note: AWS CLI must be installed on the developer workstation as a pre-requisite.

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
* `SQS_PROCESS_PRINT_REQUEST_BATCH_QUEUE_NAME` - the queue name for processing print request batches
* `SQS_PROCESS_PRINT_RESPONSE_FILE_QUEUE_NAME` - the queue name for processing print response files
* `SQS_PROCESS_PRINT_RESPONSE_QUEUE_NAME` - the queue name for processing individual print responses
* `SQS_APPLICATION_REMOVED_QUEUE_NAME` - the queue name to notify this api that a source application has been removed
* `SQS_REMOVE_CERTIFICATE_QUEUE_NAME` - the queue name for removing certificates after the final data retention period
* `API_PRINT_API_BASE_URL` - the base URL to this print-api REST API service or its API Gateway.
* `API_ERO_MANAGEMENT_URL` - the base URL of the ERO Management REST API service.
* `THREAD_POOL_ZIP_CORE_SIZE` - number of core threads for the Zip producer thread pool 
* `THREAD_POOL_ZIP_MAX_SIZE` - maximum number of threads for the Zip producer thread pool
* `SFTP_HOST` - Hostname of the SFTP server to send print requests
* `SFTP_USER` - Username to use when connecting to the SFTP server 
* `SFTP_PRIVATE_KEY` - SSH private key to use when securely connecting to the SFTP server
* `SFTP_PRINT_REQUEST_UPLOAD_DIRECTORY` - Directory on the remote host to write print request files
* `SFTP_PRINT_RESPONSE_DOWNLOAD_DIRECTORY` - Directory on the remote host to read print response files
* `JOBS_BATCH_PRINT_REQUESTS_CRON` - Optional. Overrides the cron schedule for when print requests are batched and sent to the Print Provider
* `JOBS_PROCESS_PRINT_RESPONSES_CRON` - Optional. Overrides the cron schedule for when the Print Provider's OutBound folder is polled to find and process print responses
* `S3_CERTIFICATE_PHOTOS_TARGET_BUCKET_NAME` - the S3 bucket name where certificate photos are stored
* `S3_CERTIFICATE_PHOTOS_TARGET_BUCKET_PROXY_ENDPOINT` - the URL of the custom domain file proxy for certificate photos
* `S3_BANK_HOLIDAYS_BUCKET_NAME` - the bucket name where bank holidays file will be present
* `S3_BANK_HOLIDAYS_BUCKET_OBJECT_KEY` - the name(object key) of bank holidays json file on bank holidays bucket

#### MYSQL Configuration
For local setup refer to src/main/resources/db/readme.
* `MYSQL_HOST`
* `MYSQL_PORT`
* `MYSQL_USER`
* `MYSQL_PASSWORD` - only used locally or when running tests

#### Infrastructure overrides
The following are overridden by the task definition in AWS:
* `SPRING_DATASOURCE_URL` - This is set to the deployed RDS' URL.
* `SPRING_DATASOURCE_DRIVERCLASSNAME` - This is overridden to use the AWS Aurora MySQL JDBC Driver.
* `SPRING_LIQUIBASE_DRIVERCLASSNAME` - This is overridden to use the AWS Aurora MySQL JDBC Driver.
*
#### Liquibase Configuration
* `LIQUIBASE_CONTEXT` Contexts for liquibase scripts.
  For local setup use ddl.

### Authentication and authorisation
Requests are authenticated by the presence of a signed cognito JWT as a bearer token in the HTTP request `authorization` header.  
EG: `Authorization: Bearer xxxxxyyyyyyzzzzz.....`  
Requests are authorised by their membership of groups and roles carried on the JWT token.  
The UI application is expected to handle the authentication with cognito and pass the JWT token in the `authorization` header.

## Connecting to the local Print Provider SFTP server
When running integration tests, an SFTP server will be started.
The port that it will be listening on will be written to the logs as shown below:
```text
17:09:50.314 [Test worker] INFO  uk.gov.dluhc.printapi.config.SftpContainerConfiguration - sftp mapped port: 58272
```
To connect to the SFTP server so that you can inspect the state of the server while the integration test is suspended
at a break point during debugging, you can `cd` to the directory containing the user's private key (printer_rsa) 
and then run the sftp command as shown below.
```shell
cd src/test/resources/ssh
export PORT=<find port number from logs>
sftp -v -P $PORT -oStrictHostKeyChecking=no -oKexAlgorithms=+diffie-hellman-group1-sha1 -o "IdentityFile=./printer_rsa" -o User=user localhost
```

### Scheduled Beans

#### ProcessPrintResponsesBatchJob
Polls the print provider's SFTP server for response files. Each response file is processed and a number of SQS messages
are queued for updating the affected batches and specific print requests.

#### BatchPrintRequestsJob
Assigns print requests with status `PENDING_ASSIGNMENT_TO_BATCH` to a batch for processing.
Each run processes a maximum of 1,500 print requests and these are divided into batches containing a maximum of 50 requests.
Remaining requests must wait for the next run of the job to be assigned to a batch. Once the daily threshold of 150,000 requests
is reached, any remaining requests must wait for the next day to be assigned to a batch.

#### InitialRetentionPeriodDataRemovalJob
This job removes specific certificate data that needs to be removed after the first retention period (which the legislation
defines as 28 working days after the certificate is printed).
