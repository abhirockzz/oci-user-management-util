package com.oracle.user.management;

import com.google.common.base.Supplier;
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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class CreateUsersUtil_bkp {

    //static final String USERNAME_PREFIX = "training";
    static final String DESCRIPTION_PREFIX = "Workshop user ";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter private key location");
        String privateKey = scanner.nextLine();
        //String privateKey = "D:\\Abhishek\\work\\oracle_functions\\oci-keys\\oci_api_key_2.pem";
        System.out.println("Using Private Key " + privateKey);

        System.out.println("Enter config file location");
        String configFile = scanner.nextLine();
        //String configFile = "C:\\Users\\agupgupt\\.oci\\config";
        System.out.println("Using Config file " + configFile);
        
        System.out.println("Enter compartment OCID in which to create users");
        String compartmentOCID = scanner.nextLine();
        //String compartmentOCID = "ocid1.tenancy.oc1..aaaaaaaapsj3hr6pl4abnz52jm3wkgf2gfxymbeofzswhcp5jdem3fhjmkeq";
        System.out.println("Compartment OCID " + compartmentOCID);

        System.out.println("Enter the username prefix of the users you want to create");
        String usernamePrefix = scanner.nextLine();
        //String usernamePrefix = "training";
        System.out.println("Username prefix " + usernamePrefix);

        System.out.println("Enter starting user sequence e.g. 101");
        int startUserSequence = scanner.nextInt();
        //int startUserSequence = 10;
        System.out.println("Start user sequence " + startUserSequence);

        System.out.println("Enter number of users");
        int numOfUsers = scanner.nextInt();
        //int numOfUsers = 2;
        System.out.println("Number of users " + numOfUsers);

        scanner.nextLine(); //caveat!

        System.out.println("Enter the Group OCID (functions-developers) to which you want to add the user");
        String groupOCID = scanner.nextLine();
        //String usernamePrefix = "training";
        System.out.println("Group OCID " + groupOCID);
        
        
        System.out.println("Enter the public key location (should be in PEM format) which you want to upload as the user's API key");
        String publicKeyLocation = scanner.nextLine();
        //String usernamePrefix = "training";
        System.out.println("User public API key " + publicKeyLocation);

        ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configFile, "DEFAULT");
        Supplier<InputStream> privateKeySupplier = new SimplePrivateKeySupplier(privateKey);

        //AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(config);
        AuthenticationDetailsProvider provider
                = SimpleAuthenticationDetailsProvider.builder()
                        .tenantId(config.get("tenancy"))
                        .userId(config.get("user"))
                        .fingerprint(config.get("fingerprint"))
                        .passPhrase(config.get("pass_phrase"))
                        .privateKeySupplier(privateKeySupplier)
                        .build();

        System.out.println("Creating " + numOfUsers + " users starting with sequence " + startUserSequence);

        for (int i = startUserSequence; i < (numOfUsers + startUserSequence); i++) {
            System.out.println(usernamePrefix + i);
        }

        System.out.println("The users listed above will be CREATED. Please enter yes to confirm, else the process will terminate now");
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
                //System.out.println("Creating user no. " + userName);

                CreateUserRequest createUserRequest = CreateUserRequest.builder().createUserDetails(userDetails).build();
                CreateUserResponse createUserResponse = identityClient.createUser(createUserRequest);
                User user = createUserResponse.getUser();
                System.out.println("Created user " + user.getName());
                
                System.out.println("generating password for user");
                CreateOrResetUIPasswordResponse createOrResetUIPasswordResponse = identityClient.createOrResetUIPassword(CreateOrResetUIPasswordRequest.builder().userId(user.getId()).build());
                System.out.println("one time password for "+ user.getName() + " " + createOrResetUIPasswordResponse.getUIPassword().getPassword());
                
                identityClient.addUserToGroup(AddUserToGroupRequest.builder()
                                             .addUserToGroupDetails(AddUserToGroupDetails.builder().groupId(groupOCID).userId(user.getId()).build())
                                             .build());
                System.out.println("Added user "+ user.getName() + " to group");
                
                String key = new String(Files.readAllBytes(Paths.get(publicKeyLocation)));
                //System.out.println("Uploading public key "+ key);
                
                identityClient.uploadApiKey(UploadApiKeyRequest.builder()
                                                               .userId(user.getId())
                                                               .createApiKeyDetails(CreateApiKeyDetails.builder().key(key).build())
                                                               .build());
                System.out.println("Uploaded API (public) key for user "+ user.getName());
            } catch (Exception e) {
                System.out.println("could not create user " + userName + " due to " + e.getMessage());
            }
        }
    }
}
