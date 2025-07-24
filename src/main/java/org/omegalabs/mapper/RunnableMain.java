package org.omegalabs.mapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class RunnableMain {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: <run-me> <target jar/class path>");
      return;
    }

    String filePath = args[0];
    NullReportRemapper generator = new NullReportRemapper();
    Set<String> generatedMappings = new LinkedHashSet<>(); // 使用Set来存储生成的SRG行
    System.out.println("Enter the original package prefix");
    Scanner scanner = new Scanner(System.in);
    // starts with
    String targetPrefix = "^" + scanner.nextLine();
    NullReportRemapper.SUSPICIOUS_PACKAGE_REGEX =
        targetPrefix + NullReportRemapper.SUSPICIOUS_PACKAGE_REGEX;

    try (InputStream is = new FileInputStream(filePath)) {
      if (filePath.toLowerCase().endsWith(".jar")) {
        System.out.println("Processing jar -> " + filePath);
        generatedMappings = generator.generateMappingsFromJar(is);
      } else if (filePath.toLowerCase().endsWith(".class")) {
        System.out.println("Processing class ->" + filePath);
        generatedMappings = generator.generateMappings(is);
      } else {
        System.out.println("Unsupported file type!");
        return;
      }

      if (generatedMappings.isEmpty()) {
          System.out.println("Nothing?");
      } else {
          System.out.println("\n--- Generated SRG Mappings ---");
          generatedMappings.forEach(System.out::println);
      }

    } catch (IOException e) {
      System.err.println("Error occurred: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
