withMaven(jdk: CFG.jdk, options: [artifactsPublisher(disabled: true)]) {
    sh "rm -rf /tmp/cargo*"
    
    sh """mvn -B -V -ff -e clean install --strict-checksums -Dsurefire.useFile=false \
    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
    -Djavax.xml.accessExternalSchema=all -Dpayara.version=${CFG.'build.version'} \
    -Ppayara-server-remote,payara5"""
}