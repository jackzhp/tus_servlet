package com.zede.ls;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 *
 */
@PersistenceCapable
public class EUser {

    @PrimaryKey
    int id;

    @Persistent
    ELevel target; //like grade 5
    //but could be 5.1 to 5.20.  the 20 classes for grade 5, and each class's EKP's.
    @Persistent
    ELevel actual;

    EUser(int i) {
        id = i;
    }

}
