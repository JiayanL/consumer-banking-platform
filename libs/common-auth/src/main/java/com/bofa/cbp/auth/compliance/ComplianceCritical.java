package com.bofa.cbp.auth.compliance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or class as compliance-critical. The coverage
 * dashboard parses these annotations out of the source tree to produce
 * per-category rollups.
 *
 * Apply sparingly: only on code paths that genuinely fall within the
 * specified OCC category. Over-application dilutes the signal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ComplianceCritical {
    ComplianceCategory category();

    /** Optional free-text note for reviewers. */
    String note() default "";
}
