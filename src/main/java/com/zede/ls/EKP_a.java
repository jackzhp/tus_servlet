package com.zede.ls;

/**
 *
 * KnowledgePoint
 *
 * 100 EKP's saved in one file. each file contains 100EKP. why? suppose every
 * EKP takes only 40 byes, I can not allow 40 bytes to take a block on the disk.
 * though blocksize+1 bytes is allowed to take 2 blocks. usually blocksize is
 * 4096.
 *
 *
 * seems too much not really relevant information(level, test, etc) put into
 * this class.
 * 
 * and seems some relevant information is missing. for example, for Japanese
 * no(English of), in this class, I need to state it is a relation "A of B".
 *
 * then for a SentenceAndBelow instance, when it is associated to this KP. its A
 * and B should take their corresponding value. for storage, this instantiate of
 * KP could go with the SentenceAndBelow.
 * 
 * To avoid much change, I use another class EKP0 for the KnowledgePoint and let
 * this EKP to be instantiate of EKP0.
 * 
 * another way around. EKP to be KnowledgePoint, and EKP_a to be its
 * application.
 *
 *
 * it should be an inner class of some thing such as ESentenceAndBelow. Inner
 * class is not good. though inner class could be derived from this class.
 * 
 */

public class EKP_a {
	EKP kp;

}
