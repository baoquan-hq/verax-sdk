package com.baoquan.verax.sdk.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Collection;

import static java.lang.String.format;


public class Utils {



  /**
   * convert object to json string
   * @param object object
   * @return json string
   * @throws JsonProcessingException JsonProcessingException
   */
  public static String objectToJson(Object object) throws JsonProcessingException{
    if (object == null) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(object);
  }

  /**
   * convert json string to target class object
   * @param json json string
   * @param t t
   * @param <T> T
   * @return target object
   * @throws IOException IOException
   */
  public static <T>T jsonToObject(String json, Class<T> t) throws IOException {
    if (json == null) {
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.readValue(json, t);
  }

  public static InputStream generateTarGzInputStream(File src, String pathPrefix) throws IOException {
    File sourceDirectory = src;

    ByteArrayOutputStream bos = new ByteArrayOutputStream(500000);

    String sourcePath = sourceDirectory.getAbsolutePath();

    TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(bos)));
    archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

    try {
      Collection<File> childrenFiles = org.apache.commons.io.FileUtils.listFiles(sourceDirectory, null, true);

      ArchiveEntry archiveEntry;
      FileInputStream fileInputStream;
      for (File childFile : childrenFiles) {
        String childPath = childFile.getAbsolutePath();
        String relativePath = childPath.substring((sourcePath.length() + 1), childPath.length());

        if (pathPrefix != null) {
          relativePath = org.hyperledger.fabric.sdk.helper.Utils.combinePaths(pathPrefix, relativePath);
        }

        relativePath = FilenameUtils.separatorsToUnix(relativePath);

        archiveEntry = new TarArchiveEntry(childFile, relativePath);
        fileInputStream = new FileInputStream(childFile);
        archiveOutputStream.putArchiveEntry(archiveEntry);

        try {
          IOUtils.copy(fileInputStream, archiveOutputStream);
        } finally {
          IOUtils.closeQuietly(fileInputStream);
          archiveOutputStream.closeArchiveEntry();
        }
      }
    } finally {
      IOUtils.closeQuietly(archiveOutputStream);
    }

    return new ByteArrayInputStream(bos.toByteArray());
  }


}
