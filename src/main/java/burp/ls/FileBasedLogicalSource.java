package burp.ls;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.jena.rdf.model.Resource;

import burp.model.Iteration;
import burp.model.LogicalSource;
import burp.util.Util;
import burp.vocabularies.RML;

abstract class FileBasedLogicalSource extends LogicalSource {

    protected List<Iteration> iterations = null;
    public SourceFile file;
    public Charset encoding = StandardCharsets.UTF_8;
    public Resource compression = RML.none;

    public String getDecompressedFile() {
        String absolutePath = Objects.requireNonNull(file.getFile(), "Cannot obtain file " + file).getAbsolutePath();
        return Util.getDecompressedFile(absolutePath, compression);
    }

}