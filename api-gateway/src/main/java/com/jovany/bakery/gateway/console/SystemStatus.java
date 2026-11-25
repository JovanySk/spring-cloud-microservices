package com.jovany.bakery.gateway.console;

import java.util.List;

/** Snapshot returned by GET /console/api/status. Read-only, built on every request; checkedAt is ISO-8601 UTC. */
public record SystemStatus(
        String checkedAt,
        String overall,
        Node gateway,
        List<Node> infrastructure,
        List<Service> services) {

    /** A single process probed directly (gateway, Eureka, Config Server). */
    public record Node(String name, String url, String status) {
    }

    /** A business service as seen through Eureka: all its instances and their health. */
    public record Service(String name, String status, List<Instance> instances) {
    }

    public record Instance(String instanceId, String uri, String status) {
    }
}
