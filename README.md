# OCI User management utility

Java utility to automate user creation and deletion in Oracle Cloud infrastructure

## Pre-requisite and setup

- You should have `Maven` installed
- `git clone https://github.com/abhirockzz/oci-user-management-util`
- Seed OCI SDK to your local Maven
	- `chmod +x seed-oci-sdk-to-maven.sh`
	- run the script `seed-oci-sdk-to-maven.sh`
	- it pulls down the OCI SDK from https://github.com/oracle/oci-java-sdk/releases (change `SDK_VERSION` in the script if needed)
	- seeds it to local Maven using `mvn install`

## Common

- create a properties file with the required info for creation or deletion (you can use `util.properties` as a template)

		#common parameters
		
		config_file_path=/test/oci/config
		tenancy_ocid=ocid1.tenancy.oc1..aaaaaaaapsj3hr6pl4abnz52jm3wkgf2gfxymbeofzswhcp5jdem3fhjmkeq
		group_ocid=ocid1.group.oc1..aaaaaaaa46umbo4kgln6jc3q2mgx5o76hycj2g55hjykwnaxz4l6epy72tnq
		username_prefix=workshop
		
		#creation
		
		user_public_key_path=/test/oci_api_key_public.pem
		start_user_sequence=101
		num_of_users=2

- have the private key ready - private key for admin privileged user which can execute user management operations
- have `config` file ready - this should contain info for admin privileged user which can execute user management operations
	- make sure you have the `DEFAULT` profile specified

		    [DEFAULT]
		    user=ocid1.user.oc1..aaaaaaaa7grmsqmsx27zuhcqesvb5dvhrwppxtpoxhlvfxvlukuwdypzeg2q
		    fingerprint=42:82:5f:44:ca:a1:2e:58:d2:63:6a:af:52:d5:3d:42
		    key_file=/test/private_key.pem
		    pass_phrase=4242
			tenancy=ocid1.tenancy.oc1..aaaaaaaapsj3hr6pl4abnz52jm3wkgf2gfxymbeofzswhcp5jdem3fhjmkeq
		    region=us-phoenix-1
	- make sure that you enter the full path for your private key in the `key_file` attribute in your `config` file

- for the end user, keep the following ready
	- public key (PEM format as per OCI doc) - this needs to be specified in the properties file for the user creation process
	- private key (PEM format as per OCI doc) - this needs to be provided to end users (by sharing via OCI object store)
- Both user and deletion utilities lists down the affected users (to be created or deleted) and prompts you for confirmation before triggering the actual process (just to be safe) 

## User creation

### What it does

- creates user
- generates one time (temporary) password (just like UI) - this needs to be copied and saved for distributing it to users
- adds to a group
- uploads public key

### To run

- `cd oci-user-management-util/create-users-util`
- `mvn clean install` - check `target` directory for presence of `create-users-1.0.jar` 
- enter configuration in `util.properties`
- `java -jar target/create-users-1.0.jar ../util.properties` (you can point to a different properties file as well)


## User deletion

### What it does

- removes user from group
- deletes the user

### To run

- `cd oci-user-management-util/delete-users-util`
- `mvn clean install` - check `target` directory for presence of `delete-users-1.0.jar` 
- enter configuration in `util.properties`
- `java -jar target/delete-users-1.0.jar ../util.properties` (you can point to a different properties file as well)
