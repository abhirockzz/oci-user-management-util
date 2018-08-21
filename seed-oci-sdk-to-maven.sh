SDK_VERSION=1.2.43
GROUP_ID=com.oracle.oci.sdk
ARTIFACT_ID=oci-java-sdk
ZIP_FILE_NAME=oci-java-sdk.zip
curl -L https://github.com/oracle/oci-java-sdk/releases/download/v${SDK_VERSION}/oci-java-sdk.zip > ${ZIP_FILE_NAME}
unzip ${ZIP_FILE_NAME}
cwd=`pwd`
mvn install:install-file -Dfile=${cwd}/lib/oci-java-sdk-full-${SDK_VERSION}.jar -DgroupId=${GROUP_ID} -DartifactId=${ARTIFACT_ID} -Dversion=${SDK_VERSION} -Dpackaging=jar