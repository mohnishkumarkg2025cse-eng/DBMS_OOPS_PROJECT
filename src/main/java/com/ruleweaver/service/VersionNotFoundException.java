package com.ruleweaver.service;

/** Thrown when a policy exists but does not have the requested version number (or has no versions at all). */
public class VersionNotFoundException extends RuntimeException {

    public VersionNotFoundException(String policyId, int versionNumber) {
        super("Policy " + policyId + " has no version " + versionNumber);
    }

    public VersionNotFoundException(String policyId) {
        super("Policy " + policyId + " has no versions yet");
    }
}
