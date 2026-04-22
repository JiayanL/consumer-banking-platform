package com.bofa.cbp.audit.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 128)
    private String resource;

    @Column(length = 4096)
    private String payload;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(length = 64)
    private String sourceService;

    public AuditEvent() {}

    public AuditEvent(String actor, String action, String resource, String payload,
                      Instant occurredAt, String sourceService) {
        this.actor = actor;
        this.action = action;
        this.resource = resource;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.sourceService = sourceService;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
}
