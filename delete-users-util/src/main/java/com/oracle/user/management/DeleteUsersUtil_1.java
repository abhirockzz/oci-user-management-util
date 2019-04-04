package com.oracle.user.management;

import com.google.common.base.Supplier;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.User;
import com.oracle.bmc.identity.requests.DeleteUserRequest;
import com.oracle.bmc.identity.requests.ListUserGroupMembershipsRequest;
import com.oracle.bmc.identity.requests.ListUsersRequest;
import com.oracle.bmc.identity.requests.RemoveUserFromGroupRequest;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DeleteUsersUtil_1 {

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

        System.out.println("Enter compartment OCID from which to delete users");
        String compartmentOCID = scanner.nextLine();
        //String compartmentOCID = "ocid1.tenancy.oc1..aaaaaaaapsj3hr6pl4abnz52jm3wkgf2gfxymbeofzswhcp5jdem3fhjmkeq";
        System.out.println("Compartment OCID " + compartmentOCID);

        System.out.println("Enter the username prefix of the users you want to delete");
        String usernamePrefix = scanner.nextLine();
        //String usernamePrefix = "training";
        System.out.println("Username prefix " + usernamePrefix);
        
        System.out.println("Enter the Group OCID (functions-developers) from which you want to delete the user");
        String groupOCID = scanner.nextLine();
        //String usernamePrefix = "training";
        System.out.println("Group OCID " + groupOCID);

//        System.out.println("Enter starting user sequence e.g. 101");
//        int startUserSequence = scanner.nextInt();
//        System.out.println("Start user sequence " + startUserSequence);
//
//        System.out.println("Enter end user sequence e.g. 101");
//        int endUserSequence = scanner.nextInt();
//        System.out.println("End user sequence " + endUserSequence);

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

        //System.out.println("Deleting users starting from " + startUserSequence + " to " + endUserSequence);

        IdentityClient identityClient = new IdentityClient(provider);
        identityClient.setRegion(Region.US_ASHBURN_1);

        ListUsersRequest listUsersRequest = ListUsersRequest.builder().compartmentId(compartmentOCID).limit(10000).build();
        List<User> allUsers = identityClient.listUsers(listUsersRequest).getItems();

        List<User> usersToBeDeleted = allUsers.stream()
                .filter((u) -> u.getName().startsWith(usernamePrefix))
                .collect(Collectors.toList());

        System.out.println("Users to be DELETED....");
        usersToBeDeleted.stream().forEach((u) -> System.out.println("User " + u.getName()));

        System.out.println("The users listed above will be DELETED. Please enter yes to confirm, else the process will terminate now");
        String yesOrNo = scanner.nextLine();

        if (!yesOrNo.equalsIgnoreCase("yes")) {
            System.out.println("User deletion process will NOT proceed further");
            System.exit(0);
        }

        List<String> usersOCIDsToBeDeleted = usersToBeDeleted.stream()
                .map((u) -> u.getId())
                .collect(Collectors.toList());

        for (String userOCID : usersOCIDsToBeDeleted) {

            try {
                
                String userGroupMembershipId = identityClient.listUserGroupMemberships(ListUserGroupMembershipsRequest.builder()
                                                                                                                      .compartmentId(compartmentOCID)
                                                                                                                      .userId(userOCID)
                                                                                                                      .groupId(groupOCID)
                                                                                                                      .build())
                                                             .getItems()
                                                             .get(0) //we just need the first one
                                                             .getId();
                
                System.out.println("Group membership OCID "+ userGroupMembershipId);
                identityClient.removeUserFromGroup(RemoveUserFromGroupRequest.builder().userGroupMembershipId(userGroupMembershipId).build());
                System.out.println("Deleted user from functions-developers group");
                
                DeleteUserRequest deleteUserRequest = DeleteUserRequest.builder().userId(userOCID).build();
                identityClient.deleteUser(deleteUserRequest);
                System.out.println("Deleted user " + userOCID);
            } catch (Exception e) {
                System.out.println("could not delete user " + userOCID + " due to " + e.getMessage());
            }
        }

    }
}
