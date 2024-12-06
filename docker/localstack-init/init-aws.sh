#!/bin/bash

awslocal s3 mb s3://vca-target-bucket
awslocal s3 mb s3://bank-hols-bucket

awslocal sqs create-queue --queue-name send-application-to-print-queue
awslocal sqs create-queue --queue-name process-print-request-batch-queue
awslocal sqs create-queue --queue-name process-print-response-file-queue
awslocal sqs create-queue --queue-name process-print-response-queue
awslocal sqs create-queue --queue-name application-removed-queue
awslocal sqs create-queue --queue-name remove-certificate-queue
awslocal sqs create-queue --queue-name trigger-voter-card-statistics-update-queue
awslocal sqs create-queue --queue-name trigger-application-statistics-update-queue

awslocal s3api put-object --body /home/localstack/init-files/bank-holidays.json --bucket bank-hols-bucket --key bank-holidays.json
