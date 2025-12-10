package burp.model;

import burp.reporting.Origin;
import burp.reporting.PlanNode;
import org.apache.jena.atlas.lib.NotImplemented;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Iteration {

    public Set<Object> nulls = new HashSet<>();

    public Iteration(Set<Object> nulls) {
        this.nulls = nulls;
    }

    public abstract List<Object> getValuesFor(@NotNull String reference, Origin origin);

    public abstract List<String> getStringsFor(@NotNull String reference, Origin origin);

    public abstract String asString();
}