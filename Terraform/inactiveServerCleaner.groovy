import org.artifactory.state.ArtifactoryServerState
import org.artifactory.storage.db.servers.service.ArtifactoryServersCommonService
import org.artifactory.common.ConstantValues
import org.slf4j.Logger

import java.util.concurrent.TimeUnit

jobs {
    clean(interval: 60000, delay: 600000) {
        def artifactoryServersCommonService = ctx.beanForType(ArtifactoryServersCommonService)
        def artifactoryInactiveServerCleaner = new ArtifactoryInactiveServersCleaner(artifactoryServersCommonService, log)
        artifactoryInactiveServerCleaner.cleanInactiveArtifactoryServers()
    }
}

public class ArtifactoryInactiveServersCleaner {

    private ArtifactoryServersCommonService artifactoryServersCommonService
    private Logger log

    ArtifactoryInactiveServersCleaner(ArtifactoryServersCommonService artifactoryServersCommonService, Logger log) {
        this.artifactoryServersCommonService = artifactoryServersCommonService
        this.log = log
    }

    def cleanInactiveArtifactoryServers() {
        log.info "Executing inactive artifactory servers cleaner plugin"
        List<String> allMembers = artifactoryServersCommonService.getAllArtifactoryServers()
        for (member in allMembers) {
            def heartbeat = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - member.getLastHeartbeat())
            def noHeartbeat = heartbeat > ConstantValues.haHeartbeatStaleIntervalSecs.getInt()
            if (member.getServerState() == ArtifactoryServerState.UNAVAILABLE || noHeartbeat) {
                try {
                    log.info "Running inactive artifactory servers cleaning task, found ${member.serverId} inactive servers to " +
                            "remove"
                    artifactoryServersCommonService.removeServer(member.serverId)

                }catch (Exception e){
                    log.error "Error: Not able to remove ${member.serverId}, ${e.message}"
                }
            }
        }
        log.info "No inactive servers found"
    }
}