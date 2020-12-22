package com.zede.ls;

import java.util.ArrayList;

/*
 * 
 * this is an example of an entity which is more than a sentence which is SentenceAndBelow.
 * 
 * For example, subtitle from a movie.
 * 
 * 
 * 
 * 
 */

public class ESABFromAV { // SAB for SentenceAndBelow

	ArrayList<ESentenceAndBelowHere> s;

	class ESentenceAndBelowHere extends ESentenceAndBelow {
		// location info(absolute location, relative location)
		// special meaning and emotion conveyed.
	}

}
