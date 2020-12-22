package com.zede.ls;

import java.util.ArrayList;
import java.util.HashSet;


/*
 * if I use protobuf, then all this file can be generated automatically.
 * 
 * a meaningful object can be used in a specific context, but it is complete in that context.
 * 
 */
public class ESentenceAndBelow {
	ArrayList<ESource_SAB> sources;
	int id; //int is more than enough. each of them is identified by sha256. we take the head part of its big endian representation.
	String sab; //this is just the sentence it self.
	//the pronunciation of its elements, should be in the kps.

	HashSet<EKPhere> kps;

	class EKPhere extends EKP_a {

	}

}
