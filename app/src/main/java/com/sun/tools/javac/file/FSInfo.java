package com.sun.tools.javac.file;

import com.sun.tools.javac.util.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Get meta-info about files. Default direct (non-caching) implementation.
 *
 * @see CacheFSInfo
 * <p>
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class FSInfo {

    protected FSInfo() {
    }

    protected FSInfo(Context context) {
        context.put(FSInfo.class, this);
    }

    /**
     * Get the FSInfo instance for this context.
     *
     * @param context the context
     * @return the Paths instance for this context
     */
    public static FSInfo instance(Context context) {
        FSInfo instance = context.get(FSInfo.class);
        if (instance == null)
            instance = new FSInfo();
        return instance;
    }

    public File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }

    public boolean exists(File file) {
        return file.exists();
    }

    public boolean isDirectory(File file) {
        return file.isDirectory();
    }

    public boolean isFile(File file) {
        return file.isFile();
    }

    public List<File> getJarClassPath(File file) throws IOException {
        String parent = file.getParent();
        JarFile jarFile = new JarFile(file);
        try {
            Manifest man = jarFile.getManifest();
            if (man == null)
                return Collections.emptyList();

            Attributes attr = man.getMainAttributes();
            if (attr == null)
                return Collections.emptyList();

            String path = attr.getValue(Attributes.Name.CLASS_PATH);
            if (path == null)
                return Collections.emptyList();

            List<File> list = new ArrayList<File>();

            for (StringTokenizer st = new StringTokenizer(path); st.hasMoreTokens(); ) {
                String elt = st.nextToken();
                File f = (parent == null ? new File(elt) : new File(parent, elt));
                list.add(f);
            }

            return list;
        } finally {
            jarFile.close();
        }
    }
}