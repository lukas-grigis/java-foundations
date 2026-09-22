package dev.lukasgrigis.foundations.scopedvalues.proof;

import dev.lukasgrigis.foundations.scopedvalues.example.ScopedTenant;
import dev.lukasgrigis.foundations.scopedvalues.example.ThreadLocalTenant;

/**
 * Question: a helper deep in the call stack switches the tenant to look something up for another tenant.
 * Which tenant does its caller see once the helper has returned?
 */
public final class WriteBack {

    private WriteBack() {
    }

    static void main() {
        runWithThreadLocal();
        runWithScopedValue();
    }

    private static void runWithThreadLocal() {
        ThreadLocalTenant.runAs("tenant-a", () -> {
            lookUpForOtherTenantWithThreadLocal();
            System.out.println("ThreadLocal  caller sees " + ThreadLocalTenant.current() + " after the call");
        });
    }

    private static void runWithScopedValue() {
        ScopedTenant.runAs("tenant-a", () -> {
            lookUpForOtherTenantWithScopedValue();
            System.out.println("ScopedValue  caller sees " + ScopedTenant.current() + " after the call");
        });
    }

    // whatever can read the tenant can set it, and nothing makes the helper put the old one back
    private static void lookUpForOtherTenantWithThreadLocal() {
        ThreadLocalTenant.set("tenant-b");
        lookUp(ThreadLocalTenant.current());
    }

    // the only way to switch is a nested scope, which ends when the helper's lookup returns
    private static void lookUpForOtherTenantWithScopedValue() {
        ScopedTenant.runAs("tenant-b", () -> lookUp(ScopedTenant.current()));
    }

    private static void lookUp(String tenant) {
        if (!"tenant-b".equals(tenant)) {
            throw new IllegalStateException("the helper did not run as tenant-b but as " + tenant);
        }
    }

}
