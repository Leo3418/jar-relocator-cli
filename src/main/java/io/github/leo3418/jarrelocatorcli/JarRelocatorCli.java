/*
 * Copyright (C) 2022 Yuan Liao
 * Copyright (C) 2022 jar-relocator-cli Contributors
 *
 * This file is part of jar-relocator-cli.
 *
 * jar-relocator-cli is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * jar-relocator-cli is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jar-relocator-cli.  If not, see
 * <https://www.gnu.org/licenses/>.
 */

package io.github.leo3418.jarrelocatorcli;

import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

/**
 * The main class for this program.
 */
@Command(
        name = "jar-relocator-cli",
        version = "0.0.0",
        description = "Relocate Java packages matching PATTERN1 in JAR(s) " +
                "to PATTERN2.",
        mixinStandardHelpOptions = true,
        exitCodeListHeading = "%nExit status:%n",
        exitCodeList = {
                "0:if OK",
                "1:if minor problems (e.g. cannot relocate JARs)",
                "2:if serious trouble (e.g. cannot parse command-line argument)"
        }
)
public final class JarRelocatorCli implements Callable<Void> {
    /**
     * The pattern to match in the relocation rule passed to
     * {@code jar-relocator.}
     */
    @Parameters(
            index = "0",
            paramLabel = "PATTERN1",
            description = "Main pattern of class names to relocate"
    )
    private String pattern;

    /**
     * The pattern to relocated to in the relocation rule passed to
     * {@code jar-relocator}.
     */
    @Parameters(
            index = "1",
            paramLabel = "PATTERN2",
            description = "The pattern to relocate to"
    )
    private String relocatedPattern;

    /**
     * The collection of files for input JARs whose classes are to be relocated
     * by {@code jar-relocator}.
     */
    @Parameters(
            index = "2..*",
            arity = "1..*",
            paramLabel = "JAR",
            description = "The input JAR files with classes to relocate"
    )
    private Collection<File> inputFiles;

    /**
     * The format string used to determine an input JAR's corresponding output
     * JAR's file name.  The string must be able to be passed to the
     * {@link String#format(String, Object...)} method as the first argument
     * without causing any exceptions.  Exactly one argument is honored by the
     * format string, which is an input JAR's file name without the extension.
     */
    @Option(
            names = {"-o", "--output-format"},
            defaultValue = "%s-relocated.jar",
            paramLabel = "FORMAT",
            description = "Apply FORMAT to each input JAR's file name " +
                    "(without extension) to get its output JAR's file name " +
                    "(default: ${DEFAULT-VALUE})"
    )
    private String outputFileNameFormat;

    /**
     * A collection of patterns which the relocation rule passed to
     * {@code jar-relocator} should specifically include.
     */
    @Option(
            names = {"--include"},
            paramLabel = "PATTERN",
            description = "Include PATTERN in relocation"
    )
    private Collection<String> includes;

    /**
     * A collection of patterns which the relocation rule passed to
     * {@code jar-relocator} should specifically exclude.
     */
    @Option(
            names = {"--exclude"},
            paramLabel = "PATTERN",
            description = "Exclude PATTERN from relocation"
    )
    private Collection<String> excludes;

    /**
     * Whether more verbose program output has been requested via the
     * command-line arguments.
     */
    @Option(
            names = {"-v", "--verbose"},
            description = "Explain what is being done"
    )
    private boolean verboseRequested;

    /**
     * Allows only members in this class itself to construct new instances of
     * this class.
     */
    private JarRelocatorCli() {
    }

    /**
     * Calls {@code jar-relocator} using the arguments specified via the
     * command line to relocate classes in the input JARs.
     *
     * @return {@code null} always
     * @throws IOException if {@code jar-relocator} could not relocate the JARs
     */
    public Void call() throws IOException {
        // Prefer empty collection to null
        if (includes == null) {
            includes = Collections.emptySet();
        }
        if (excludes == null) {
            excludes = Collections.emptySet();
        }

        for (File inputFile : inputFiles) {
            String inputFileName = inputFile.getPath();
            String outputFileName = String.format(outputFileNameFormat,
                    inputFileName.substring(0, inputFileName.lastIndexOf(".")));
            if (verboseRequested) {
                System.out.println("'" + inputFileName + "' -> '" +
                        outputFileName + "'");
            }
            File outputFile = new File(outputFileName);

            Relocation rule = new Relocation(
                    pattern, relocatedPattern, includes, excludes);
            JarRelocator relocator = new JarRelocator(
                    inputFile, outputFile, Collections.singleton(rule));
            relocator.run();
        }
        return null;
    }

    /**
     * Runs this program with the specified command-line arguments.
     *
     * @param args the command-line arguments for this program
     */
    public static void main(String[] args) {
        System.exit(new CommandLine(new JarRelocatorCli()).execute(args));
    }
}
