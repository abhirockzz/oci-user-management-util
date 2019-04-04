package com.oracle.user.management;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.AddUserToGroupDetails;
import com.oracle.bmc.identity.model.CreateApiKeyDetails;
import com.oracle.bmc.identity.model.CreateUserDetails;
import com.oracle.bmc.identity.model.User;
import com.oracle.bmc.identity.requests.AddUserToGroupRequest;
import com.oracle.bmc.identity.requests.CreateOrResetUIPasswordRequest;
import com.oracle.bmc.identity.requests.CreateUserRequest;
import com.oracle.bmc.identity.requests.UploadApiKeyRequest;
import com.oracle.bmc.identity.responses.CreateOrResetUIPasswordResponse;
import com.oracle.bmc.identity.responses.CreateUserResponse;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

public class CreateUsersUtil {

    static final String DESCRIPTION_PREFIX = "Workshop user ";

    public static void main(String[] args) throws Exception {

        if (args[0] == null) {
            System.out.println("Usage - java -jar create-users-1.0.jar <path_to_properties_config_file>");
        }

        String propertiesFileLocation = args[0];
        System.out.println("Configuration properties file location " + propertiesFileLocation);

        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFileLocation));

        String configFile = props.getProperty("config_file_path");
        System.out.println("Using Config file " + configFile);

        String groupOCID = props.getProperty("group_ocid");
        System.out.println("Group OCID " + groupOCID);

        String usernamePrefix = props.getProperty("username_prefix");
        System.out.println("Username prefix " + usernamePrefix);

        int startUserSequence = Integer.valueOf(props.getProperty("start_user_sequence"));
        System.out.println("Start user sequence " + startUserSequence);

        int numOfUsers = Integer.valueOf(props.getProperty("num_of_users"));
        System.out.println("Number of users " + numOfUsers);

        String publicKeyLocation = props.getProperty("user_public_key_path");
        System.out.println("User public API key " + publicKeyLocation);

        ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configFile, "DEFAULT");

        AuthenticationDetailsProvider provider
                = SimpleAuthenticationDetailsProvider.builder()
                        .tenantId(config.get("tenancy"))
                        .userId(config.get("user"))
                        .fingerprint(config.get("fingerprint"))
                        .passPhrase(config.get("pass_phrase"))
                        .privateKeySupplier(new SimplePrivateKeySupplier(config.get("key_file")))
                        .build();

        System.out.println("Creating " + numOfUsers + " users starting with sequence " + startUserSequence);

        for (int i = startUserSequence; i < (numOfUsers + startUserSequence); i++) {
            System.out.println(usernamePrefix + i);
        }

        System.out.println("The users listed above will be CREATED. Please enter yes to confirm, else the process will terminate now");
        Scanner scanner = new Scanner(System.in);
        String yesOrNo = scanner.nextLine();

        if (!yesOrNo.equalsIgnoreCase("yes")) {
            System.out.println("User creation process will NOT proceed further");
            System.exit(0);
        }

        IdentityClient identityClient = new IdentityClient(provider);
        identityClient.setRegion(config.get("region"));

        /**
         * compartment ID required by the API is nothing but the root
         * compartment OCID (same as tenancy OCID) see -
         * https://docs.cloud.oracle.com/iaas/tools/java/latest/com/oracle/bmc/identity/model/CreateUserDetails.html#getCompartmentId--
         */
        String compartmentOCID = config.get("tenancy");
        System.out.println("Compartment OCID " + compartmentOCID);

        for (int i = startUserSequence; i < (numOfUsers + startUserSequence); i++) {

            String userName = usernamePrefix + i;
            String description = DESCRIPTION_PREFIX + userName;

            CreateUserDetails userDetails = new CreateUserDetails(compartmentOCID, userName, description, null, null);

            CreateUserRequest createUserRequest = CreateUserRequest.builder().createUserDetails(userDetails).build();
            CreateUserResponse createUserResponse = null;
            try {
                createUserResponse = identityClient.createUser(createUserRequest);
            } catch (Exception e) {
                System.out.println("could not create user " + userName + " due to " + e.getMessage());
                continue; //continue creating other users - skip other steps
            }
            User user = createUserResponse.getUser();

            CreateOrResetUIPasswordResponse createOrResetUIPasswordResponse = null;
            try {
                createOrResetUIPasswordResponse = identityClient.createOrResetUIPassword(CreateOrResetUIPasswordRequest.builder().userId(user.getId()).build());
            } catch (Exception e) {
                System.out.println("Password generation for user " + userName + " failed due to " + e.getMessage() + FAILED_INSTRUCTION);
                continue; //continue creating other users - skip other steps
            }

            
            if (!groupOCID.equals("")) { // do not try to add to group if it has not been specified in properties file
                try {
                    identityClient.addUserToGroup(AddUserToGroupRequest.builder()
                            .addUserToGroupDetails(AddUserToGroupDetails.builder().groupId(groupOCID).userId(user.getId()).build())
                            .build());
                } catch (Exception e) {
                    System.out.println("Group addition for user " + userName + " failed due to " + e.getMessage() + FAILED_INSTRUCTION);
                    continue; //continue creating other users - skip other steps
                }
            }

            try {
                String key = new String(Files.readAllBytes(Paths.get(publicKeyLocation)));

                identityClient.uploadApiKey(UploadApiKeyRequest.builder()
                        .userId(user.getId())
                        .createApiKeyDetails(CreateApiKeyDetails.builder().key(key).build())
                        .build());
            } catch (Exception e) {
                System.out.println("Failed to upload public key for user " + userName + " due to " + e.getMessage() + FAILED_INSTRUCTION);
                continue; //continue creating other users - skip other steps

            }
            System.out.println("User:   " + user.getName() + "      Password:   " + createOrResetUIPasswordResponse.getUIPassword().getPassword());
        }
    }

    private static String FAILED_INSTRUCTION = ". Either execute this step manually or delete the user and re-run the utility";
}
