package de.fu_berlin.inf.dpp.editor.annotations;

/**
 * Marks text contributions done by the driver.
 * 
 * @author rdjemili
 */
public class ContributionAnnotation extends AnnotationSaros {
	public static final String TYPE = "de.fu_berlin.inf.dpp.annotations.contribution";

	public ContributionAnnotation() {
		this("","");
	}

	public ContributionAnnotation(String label, String source) {
		super(TYPE, false, label, source);
		
	}
}
