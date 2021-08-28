package com.google.devtools.build.lib.vfs;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.regex.Pattern;

public class IgnoredEntrySet {
    private final ImmutableSet<PathFragment> ignoredPrefixes;
    private final ImmutableSet<String> wildcardPatterns;
    private final Map<String, Pattern> patternCache;

    public static IgnoredEntrySet empty() {
        return new IgnoredEntrySet(ImmutableSet.of());
    }

    public IgnoredEntrySet(ImmutableSet<String> patterns) {
        Preconditions.checkNotNull(patterns);

        ImmutableSet.Builder<PathFragment> ignoredPrefixesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<String> wildcardPatternsBuilder = ImmutableSet.builder();

        for (String pattern: patterns) {
            if (UnixGlob.isWildcardFree(pattern)) {
                ignoredPrefixesBuilder.add(PathFragment.create(pattern));
            } else {
                wildcardPatternsBuilder.add(pattern);
            }
        }

        ignoredPrefixes = ignoredPrefixesBuilder.build();
        wildcardPatterns = wildcardPatternsBuilder.build();
        patternCache = Maps.newHashMap();
    }

    public boolean isPathFragmentIgnored(PathFragment path) {
        for (PathFragment pathFragment: ignoredPrefixes) {
            if (path.startsWith(pathFragment)) {
                return true;
            }
        }
        for (String pattern: wildcardPatterns) {
            if (UnixGlob.matches(pattern + "/**", path.getPathString(), patternCache)) {
                return true;
            }
        }
        return false;
    }

    public IgnoredEntrySet combine(ImmutableSet<PathFragment> set) {
        ImmutableSet.Builder<String> allEntriesBuilder = ImmutableSet.builder();
        allEntriesBuilder.addAll(wildcardPatterns);
        addAllPathFragment(allEntriesBuilder, ignoredPrefixes);
        addAllPathFragment(allEntriesBuilder, set);
        return new IgnoredEntrySet(allEntriesBuilder.build());
    }

    private static void addAllPathFragment(
            ImmutableSet.Builder<String> builder,
            ImmutableSet<PathFragment> pathFragmentSet) {
        for (PathFragment pathFragment : pathFragmentSet) {
            builder.add(pathFragment.getPathString());
        }
    }

    public boolean isEmpty() {
        return ignoredPrefixes.isEmpty() && wildcardPatterns.isEmpty();
    }

    public int hashCode() {
        ImmutableSet.Builder<String> allEntriesBuilder = ImmutableSet.builder();
        allEntriesBuilder.addAll(wildcardPatterns);
        addAllPathFragment(allEntriesBuilder, ignoredPrefixes);
        return allEntriesBuilder.build().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof IgnoredEntrySet) {
            IgnoredEntrySet other = (IgnoredEntrySet) obj;
            return this.ignoredPrefixes.equals(other.ignoredPrefixes) && this.wildcardPatterns.equals(other.wildcardPatterns) ;
        }
        return false;
    }
}
