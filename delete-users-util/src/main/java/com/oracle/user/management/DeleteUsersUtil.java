package com.oracle.user.management;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.identity.IdentityClient;
import com.oracle.bmc.identity.model.User;
import com.oracle.bmc.identity.model.UserGroupMembership;
import com.oracle.bmc.identity.requests.DeleteUserRequest;
import com.oracle.bmc.identity.requests.ListUserGroupMembershipsRequest;
import com.oracle.bmc.identity.requests.ListUsersRequest;
import com.oracle.bmc.identity.requests.RemoveUserFromGroupRequest;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DeleteUsersUtil {

    public static void main(String[] args) throws Exception {

        if (args[0] == null) {
            System.out.println("Usage - java -jar delete-users-1.0.jar <path_to_properties_config_file>");
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

        ConfigFileReader.ConfigFile config = ConfigFileReader.parse(configFile, "DEFAULT");

        AuthenticationDetailsProvider provider
                = SimpleAuthenticationDetailsProvider.builder()
                        .tenantId(config.get("tenancy"))
                        .userId(config.get("user"))
                        .fingerprint(config.get("fingerprint"))
                        .passPhrase(config.get("pass_phrase"))
                        .privateKeySupplier(new SimplePrivateKeySupplier(config.get("key_file")))
                        .build();

        IdentityClient identityClient = new IdentityClient(provider);
        identityClient.setRegion(config.get("region"));

        /**
         * compartment ID required by the API is nothing but the root
         * compartment OCID (same as tenancy OCID) see -
         * https://docs.cloud.oracle.com/iaas/tools/java/latest/com/oracle/bmc/identity/model/CreateUserDetails.html#getCompartmentId--
         */
        String compartmentOCID = config.get("tenancy");
        System.out.println("Compartment OCID " + compartmentOCID);

        ListUsersRequest listUsersRequest = ListUsersRequest.builder().compartmentId(compartmentOCID).limit(10000).build();
        List<User> allUsers = identityClient.listUsers(listUsersRequest).getItems();
        //List<String> allUsernames = allUsers.stream().map((u) -> u.getName()).collect(Collectors.toList());

        List<User> usersToBeDeleted = new ArrayList<>();

        for (int i = startUserSequence; i < (numOfUsers + startUserSequence); i++) {
            String filter = usernamePrefix + i;
            usersToBeDeleted.addAll(allUsers.stream()
                    .filter((u) -> u.getName().equalsIgnoreCase(filter))
                    .collect(Collectors.toList()));
        }

        if (usersToBeDeleted.isEmpty()) {
            System.out.println("None of the users matched deletion criteria. Exiting....");
            return;
        }
        System.out.println("Users to be DELETED....");
        usersToBeDeleted.stream().forEach((u) -> System.out.println("User " + u.getName()));

        System.out.println("The users listed above will be DELETED. Please enter yes to confirm, else the process will terminate now");
        Scanner scanner = new Scanner(System.in);

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
                List<UserGroupMembership> userGroupMembership = identityClient.listUserGroupMemberships(ListUserGroupMembershipsRequest.builder()
                        .compartmentId(compartmentOCID)
                        .userId(userOCID)
                        .groupId(groupOCID)
                        .build())
                        .getItems();

                String userGroupMembershipId = null;
                if (!userGroupMembership.isEmpty()) {
                    userGroupMembershipId = userGroupMembership.get(0) //ASSUMING user is part of ONE group ONLY
                            .getId();
                    identityClient.removeUserFromGroup(RemoveUserFromGroupRequest.builder().userGroupMembershipId(userGroupMembershipId).build());
                    System.out.println("Deleted user from group");

                }
            } catch (Exception e) {
                System.out.println("could not delete user " + userOCID + " from group due to " + e.getMessage());
                //let delete user get triggered.. if group relationship exists, it is bound to fail...
            }

            try {
                DeleteUserRequest deleteUserRequest = DeleteUserRequest.builder().userId(userOCID).build();
                identityClient.deleteUser(deleteUserRequest);
                System.out.println("Deleted user " + userOCID);
            } catch (Exception e) {
                System.out.println("could not delete user " + userOCID + " due to " + e.getMessage());
            }
        }

    }
}
