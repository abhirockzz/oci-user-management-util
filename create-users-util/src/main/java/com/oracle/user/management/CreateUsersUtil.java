package com.oracle.user.management;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
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

        String compartmentOCID = props.getProperty("tenancy_ocid");
        System.out.println("Compartment OCID " + compartmentOCID);

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
        identityClient.setRegion(Region.US_ASHBURN_1);

        for (int i = startUserSequence; i < (numOfUsers + startUserSequence); i++) {

            String userName = usernamePrefix + i;
            String description = DESCRIPTION_PREFIX + userName;

            try {
                CreateUserDetails userDetails = new CreateUserDetails(compartmentOCID, userName, description, null, null);

                CreateUserRequest createUserRequest = CreateUserRequest.builder().createUserDetails(userDetails).build();
                CreateUserResponse createUserResponse = identityClient.createUser(createUserRequest);
                User user = createUserResponse.getUser();
                System.out.println("Created user " + user.getName());

                System.out.println("generating password for user");
                CreateOrResetUIPasswordResponse createOrResetUIPasswordResponse = identityClient.createOrResetUIPassword(CreateOrResetUIPasswordRequest.builder().userId(user.getId()).build());
                System.out.println("one time password for " + user.getName() + " " + createOrResetUIPasswordResponse.getUIPassword().getPassword());

                identityClient.addUserToGroup(AddUserToGroupRequest.builder()
                        .addUserToGroupDetails(AddUserToGroupDetails.builder().groupId(groupOCID).userId(user.getId()).build())
                        .build());
                System.out.println("Added user " + user.getName() + " to group");

                String key = new String(Files.readAllBytes(Paths.get(publicKeyLocation)));

                identityClient.uploadApiKey(UploadApiKeyRequest.builder()
                        .userId(user.getId())
                        .createApiKeyDetails(CreateApiKeyDetails.builder().key(key).build())
                        .build());
                System.out.println("Uploaded API (public) key for user " + user.getName());
            } catch (Exception e) {
                System.out.println("could not create user " + userName + " due to " + e.getMessage());
            }
        }
    }
}
