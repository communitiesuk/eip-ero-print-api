# Print Provider Schema
# v0.0.3
This folder contains the Print Provider schema files.

The schema is defined in `yamlschema`. Each type is defined in its own file.

## Audience
The intended audience / users of these schema files are the Print Provider, and EROP.
The schema has been designed as a joint effort by both parties, and is the API contract between EROP and the Print Provider.

## Schema versioning
The schema is versioned, though `yamlschema` does not have a formal version tag or attribute.  
The current version is as defined on line 2 of this `README.md` and each `yaml` file has the same version number as a comment
at the top of the file.  

Changes to the schema are not related to individual files. Individual files do not have a version number that is independent
of the other schema files. We maintain 1 version for the schema as a whole.

**Any changes to the schema must increment the version in this `README.md` and all `yaml` files, regardless of which file
the change impacted.**

## Changes to the schema
Any changes to the schema are the result of a collaboration between EROP and the Print Provider as it forms the API contract
between both parties. All changes are negotiated and agreed by both parties.

## Schema overview
This schema contains definitions for the Print Request, Print Batch and Print Request Response models.

* The Print Request represents the PSV (Pipe Seperated Values) message sent from EROP to the Print Provider for a batch of print requests. 
* The Print Responses type represents a response from the Print Provider to EROP containing:
  * A collection of Batch Response. Can be an empty collection. 
  * A collection of Print Response. Can be an empty collection.
* A Print Response type represents a status update for a single print request
* A Batch Response type represents a status update for a print batch.

