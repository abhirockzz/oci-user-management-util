package com.oracle.user.management;

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

import java.io.FileInputStream;
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

        String compartmentOCID = props.getProperty("tenancy_ocid");
        System.out.println("Compartment OCID " + compartmentOCID);

        String groupOCID = props.getProperty("group_ocid");
        System.out.println("Group OCID " + groupOCID);

        String usernamePrefix = props.getProperty("username_prefix");
        System.out.println("Username prefix " + usernamePrefix);

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

        ListUsersRequest listUsersRequest = ListUsersRequest.builder().compartmentId(compartmentOCID).limit(10000).build();
        List<User> allUsers = identityClient.listUsers(listUsersRequest).getItems();

        List<User> usersToBeDeleted = allUsers.stream()
                .filter((u) -> u.getName().startsWith(usernamePrefix))
                .collect(Collectors.toList());

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

                String userGroupMembershipId = identityClient.listUserGroupMemberships(ListUserGroupMembershipsRequest.builder()
                        .compartmentId(compartmentOCID)
                        .userId(userOCID)
                        .groupId(groupOCID)
                        .build())
                        .getItems()
                        .get(0) //we just need the first one
                        .getId();

                identityClient.removeUserFromGroup(RemoveUserFromGroupRequest.builder().userGroupMembershipId(userGroupMembershipId).build());
                System.out.println("Deleted user from group");

                DeleteUserRequest deleteUserRequest = DeleteUserRequest.builder().userId(userOCID).build();
                identityClient.deleteUser(deleteUserRequest);
                System.out.println("Deleted user " + userOCID);
            } catch (Exception e) {
                System.out.println("could not delete user " + userOCID + " due to " + e.getMessage());
            }
        }

    }
}
