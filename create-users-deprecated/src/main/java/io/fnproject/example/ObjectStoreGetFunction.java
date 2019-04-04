package io.fnproject.example;

import com.google.common.base.Supplier;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ObjectStoreGetFunction {

    private ObjectStorage objStoreClient = null;

    public ObjectStoreGetFunction() {
        System.err.println("Inside ObjectStoreGetFunction ctor");
        InputStream in = ObjectStoreGetFunction.class.getResourceAsStream("/config");
        ConfigFileReader.ConfigFile config = null;
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

            objStoreClient = new ObjectStorageClient(provider);
            objStoreClient.setRegion(config.get("region"));

            System.err.println("ObjectStorage client setup");
        } catch (Exception ex) {
            System.err.println("Error occurred in ObjectStoreGetFunction ctor " + ex.getMessage());
        }
    }

    public static class GetObjectInfo {

        private String bucketName;
        private String name;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    /*
    works for String content
    */
    public String handle(GetObjectInfo objectInfo) {
        System.err.println("Inside ObjectStoreGetFunction/handle");
        String result = "FAILED";

        if (objStoreClient == null) {
            return result;
        }
        try {

            String nameSpace = System.getenv().getOrDefault("NAMESPACE", "test-namespace");
            
            GetObjectRequest gor = GetObjectRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(objectInfo.getBucketName())
                    .objectName(objectInfo.getName())
                    .build();

            GetObjectResponse response = objStoreClient.getObject(gor);
            result = new BufferedReader(new InputStreamReader(response.getInputStream()))
                                    .lines().collect(Collectors.joining("\n"));

        } catch (Exception e) {
            System.err.println("Error fetching object " + e.getMessage());
            result = "Error fetching object " + e.getMessage();
        }

        return result;
    }
}
