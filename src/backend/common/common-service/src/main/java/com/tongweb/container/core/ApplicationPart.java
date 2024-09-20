package com.tongweb.container.core;

import com.tongweb.web.util.http.fileupload.FileItem;
import com.tongweb.web.util.http.fileupload.ParameterParser;
import com.tongweb.web.util.http.fileupload.disk.DiskFileItem;
import com.tongweb.web.util.http.parser.HttpParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.Part;

public class ApplicationPart implements Part {
    private final FileItem fileItem;
    private final File location;

    public ApplicationPart(FileItem fileItem, File location) {
        this.fileItem = fileItem;
        this.location = location;
    }

    public void delete() throws IOException {
        this.fileItem.delete();
    }

    public String getContentType() {
        return this.fileItem.getContentType();
    }

    public String getHeader(String name) {
        return this.fileItem instanceof DiskFileItem ? this.fileItem.getHeaders().getHeader(name) : null;
    }

    public Collection<String> getHeaderNames() {
        if (!(this.fileItem instanceof DiskFileItem)) {
            return Collections.emptyList();
        } else {
            LinkedHashSet<String> headerNames = new LinkedHashSet();
            Iterator<String> iter = this.fileItem.getHeaders().getHeaderNames();

            while(iter.hasNext()) {
                headerNames.add(iter.next());
            }

            return headerNames;
        }
    }

    public Collection<String> getHeaders(String name) {
        if (!(this.fileItem instanceof DiskFileItem)) {
            return Collections.emptyList();
        } else {
            LinkedHashSet<String> headers = new LinkedHashSet();
            Iterator<String> iter = this.fileItem.getHeaders().getHeaders(name);

            while(iter.hasNext()) {
                headers.add(iter.next());
            }

            return headers;
        }
    }

    public InputStream getInputStream() throws IOException {
        return this.fileItem.getInputStream();
    }

    public String getName() {
        return this.fileItem.getFieldName();
    }

    public long getSize() {
        return this.fileItem.getSize();
    }

    public void write(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.isAbsolute()) {
            file = new File(this.location, fileName);
        }

        try {
            this.fileItem.write(file);
        } catch (Exception var4) {
            Exception e = var4;
            throw new IOException(e);
        }
    }

    public String getString(String encoding) throws UnsupportedEncodingException, IOException {
        return this.getSize() < 1 ? "" : this.fileItem.getString(encoding);
    }

    public String getSubmittedFileName() {
        String fileName = null;
        String cd = this.getHeader("Content-Disposition");
        if (cd != null) {
            String cdl = cd.toLowerCase(Locale.ENGLISH);
            if (cdl.startsWith("form-data") || cdl.startsWith("attachment")) {
                ParameterParser paramParser = new ParameterParser();
                paramParser.setLowerCaseNames(true);
                Map<String, String> params = paramParser.parse(cd, ';');
                if (params.containsKey("filename")) {
                    fileName = (String)params.get("filename");
                    if (fileName != null) {
                        if (fileName.indexOf(92) > -1) {
                            fileName = HttpParser.unquote(fileName.trim());
                        } else {
                            fileName = fileName.trim();
                        }
                    } else {
                        fileName = "";
                    }
                }
            }
        }

        return fileName;
    }
}
