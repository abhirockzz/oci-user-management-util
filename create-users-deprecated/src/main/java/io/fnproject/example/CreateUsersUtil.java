package io.fnproject.example;

import com.google.common.base.Supplier;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.CreateUserDetails;
import com.oracle.bmc.identity.model.User;
import com.oracle.bmc.identity.requests.CreateUserRequest;
import com.oracle.bmc.identity.responses.CreateUserResponse;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import java.io.InputStream;
import java.util.Collections;
import java.util.Scanner;

public class CreateUsersUtil {

    static final String USERNAME_PREFIX = "training";
    //IdentityClient identityClient = null;


    public static void main(String[] args) throws Exception {
        
        System.err.println("Inside CreateUsersUtil ctor");
        InputStream in = ObjectStoreGetFunction.class.getResourceAsStream("/config");
        ConfigFileReader.ConfigFile config = null;
        
        IdentityClient identityClient = null;
        try {
            config = ConfigFileReader.parse(in, "DEFAULT");
            String privateKey = System.getenv().getOrDefault("OCI_PRIVATE_KEY_FILE_NAME", "oci_api_key.pem");
            Supplier<InputStream> privateKeySupplierFromJAR = () -> {
                return ObjectStoreGetFunction.class.getResourceAsStream("/" + privateKey);
            };

            AuthenticationDetailsProvider provider
                    = SimpleAuthenticationDetailsProvider.builder()
                            .tenantId(config.get("tenancy"))
                            .userId(config.get("user"))
                            .fingerprint(config.get("fingerprint"))
                            .passPhrase(config.get("pass_phrase"))
                            .privateKeySupplier(privateKeySupplierFromJAR)
                            .build();

            System.err.println("AuthenticationDetailsProvider setup");

            identityClient = new IdentityClient(provider);
            identityClient.setRegion("us-ashburn-1");

            System.err.println("IdentityClient client setup");
        } catch (Exception ex) {
            System.err.println("Error occurred in CreateUsersUtil ctor " + ex.getMessage());
        } 
        String startUserSequence = "10";
        String numOfUsers = "2";
        String compartmentOCID = "ocid1.tenancy.oc1..aaaaaaaapsj3hr6pl4abnz52jm3wkgf2gfxymbeofzswhcp5jdem3fhjmkeq";
        
        for (int i = Integer.valueOf(startUserSequence); i < Integer.valueOf(numOfUsers + startUserSequence) - 1; i++) {
            CreateUserDetails userDetails = new CreateUserDetails(compartmentOCID, USERNAME_PREFIX + i, "workshop user " + USERNAME_PREFIX + i, 
                    Collections.EMPTY_MAP, Collections.EMPTY_MAP);
            System.out.println("Creating user no. " + USERNAME_PREFIX + i);

            CreateUserRequest createUserRequest = CreateUserRequest.builder().createUserDetails(userDetails).build();
            CreateUserResponse createUserResponse = identityClient.createUser(createUserRequest);
            User user = createUserResponse.getUser();
            System.out.println("Created user " + user.getName());
        }

    }
}
