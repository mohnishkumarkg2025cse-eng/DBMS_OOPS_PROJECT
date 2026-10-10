package com.ruleweaver.service;

/** Thrown when no policy exists with the requested id. Unchecked, so callers are not forced to catch it. */
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policyId) {
        super("No policy found with id: " + policyId);
    }
}
