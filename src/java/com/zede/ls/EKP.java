package com.zede.ls;

import java.util.Set;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 *
 * KnowledgePoint
 *
 */
public class EKP {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.INCREMENT)
    int id;

    /*
    TODO: this should be searchable with full text.
    kodo has this capability: 
    Example of full-text searching in JDO
The files for this sample are located in the samples/textindex directory of the Kodo installation. This sample demonstrates how full-text indexing might be implemented in JDO. Most relational databases cannot optimize contains queries for large text fields, meaning that any substring query will result in a full table scan (which can be extremely slow for tables with many rows).

The AbstractIndexable class implements javax.jdo.InstanceCallbacks which will cause the textual content of the implementing persistent class to be split into individual "word" tokens, and stored in a related table. Since this happens whenever an instance of the class is stored, the index is always up to date. The Indexer class is a utility that assists in building queries that act on the indexed field.

The TextIndexMain class is a driver to demonstrate a simple text indexing application.
     */
    @Persistent
    String desc;

    @Persistent
    ELevel level;

    @Persistent(mappedBy = "kps")
    Set<ETest> tests;

    void save() {
        System.out.println("save EKP not implemented yet");
    }
}
