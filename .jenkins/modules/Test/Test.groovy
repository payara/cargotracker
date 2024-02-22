MPLPostStep('always') {
    cleanWs()
}

MPLPostStep('failure') {
    echo "There are test failures, archiving server logs to ${CFG.suite.suite_name}-Logs.zip"
    sh "cp -R ./${getPayaraDirectoryName(CFG.'build.version')}/glassfish/domains/${CFG.domain_name}/logs ./${CFG.suite.suite_name}-Logs"
    archiveArtifacts artifacts: "${CFG.suite.suite_name}-Logs/**/*.*"
}

withMaven(jdk: CFG.jdk, options: [artifactsPublisher(disabled: true)]) {
    sh "rm -rf /tmp/cargo*"

    sh """mvn -B -V -ff -e clean install --strict-checksums -Dsurefire.useFile=false \
    -Djavax.net.ssl.trustStore=${env.JAVA_HOME}/jre/lib/security/cacerts \
    -Djavax.xml.accessExternalSchema=all -Dpayara.version=${CFG.'build.version'} \
    -Ppayara-server-remote"""
}