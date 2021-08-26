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

    public IgnoredEntrySet(ImmutableSet<String> patterns) {
        Preconditions.checkNotNull(patterns);

        ImmutableSet.Builder<PathFragment> ignoredPrefixesBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<String> wildcardPatternsBuilder = ImmutableSet.builder();

        for (String pattern: patterns) {
            if (UnixGlob.isWildcardFree(pattern))) {
                ignoredPrefixesBuilder.add(PathFragment.create(pattern));
            } else {
                wildcardPatternsBuilder.add(path);
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

    public boolean isEmpty() {
        return ignoredPrefixes.isEmpty() && wildcardPatterns.isEmpty();
    }

    public IgnoredEntrySet filterOnlySubdirectories(PathFragment root) {
        ImmutableSet.Builder<PathFragment> filteredPathsBuilder = ImmutableSet.builder();
        for (PathFragment prefix : ignoredPrefixes) {
            if (!prefix.equals(root) && prefix.startsWith(root)) {
                filteredPathsBuilder.add(prefix);
            }
        }
        for (String pattern: wildcardPatterns) {
            filteredPathsBuilder.add(PathFragment.create(pattern));
        }
        return new IgnoredEntrySet(filteredPathsBuilder.build());
    }

    public IgnoredEntrySet filterSubdirectories(PathFragment root) {
        ImmutableSet.Builder<PathFragment> filteredPathsBuilder = ImmutableSet.builder();
        for (PathFragment prefix : ignoredPrefixes) {
            if (prefix.startsWith(root)) {
                filteredPathsBuilder.add(prefix);
            }
        }
        for (String pattern: wildcardPatterns) {
            filteredPathsBuilder.add(PathFragment.create(pattern));
        }
        return new IgnoredEntrySet(filteredPathsBuilder.build());
    }

    public void checkAllPathsAreUnder(PathFragment root) {
        for (PathFragment path : ignoredPrefixes) {
            Preconditions.checkArgument(
                    !path.equals(root) && path.startsWith(root),
                    "%s is not beneath %s",
                    path,
                    root);
        }
    }

    public int hashCode() {
        ImmutableSet.Builder<String> allEntriesBuilder = ImmutableSet.builder();
        allEntriesBuilder.addAll(wildcardPatterns);
        for (PathFragment prefix : ignoredPrefixes) {
            allEntriesBuilder.add(prefix.getPathString());
        }
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
