package burp.model;

import burp.reporting.BurpException;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractLogicalSource extends Iterable implements FieldParent {

    @NotNull
    public Set<Object> nulls = new HashSet<>();
    public abstract Iterator<Iteration> iterator() throws BurpException;

    @Override
    public String getAbsoluteFieldName() {
        return "<i>";
    }
}