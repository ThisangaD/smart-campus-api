package com.smartcampus.model;

import java.util.UUID;

/**
 * Represents a single historical reading event captured by a sensor.
 */
public class SensorReading {

    private String id;          // UUID
    private long   timestamp;   // Epoch ms
    private double value;       // Measured value

    // ── Constructors ──────────────────────────────────────────────────────────

    public SensorReading() {}

    public SensorReading(double value) {
        this.id        = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.value     = value;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                      { return id; }
    public void   setId(String id)             { this.id = id; }

    public long   getTimestamp()               { return timestamp; }
    public void   setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getValue()                   { return value; }
    public void   setValue(double value)       { this.value = value; }
}
