package sk.db.relational.spring.services.impl;

import jakarta.persistence.*;

@Entity
@Table(name = "retry_record")
public class RetryRecord {
    @Id public String id;
    @Column(name = "natural_key", nullable = false, unique = true) public String key;
    @Version public Long version;
    public int value;

    public RetryRecord() {}
    public RetryRecord(String id, String key) { this.id = id; this.key = key; }
}
