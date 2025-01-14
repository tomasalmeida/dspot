package eu.stamp_project.utils.program;

import eu.stamp_project.utils.AmplificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 02/06/18
 */
public class ConstantsProperties {

    public static final InputConfigurationProperty PROJECT_ROOT_PATH =
            new InputConfigurationProperty(
                    "project",
                    "specify the path to the root of the project. " +
                            "This path can be either absolute (recommended) or relative to the working directory of the DSpot process. " +
                            "We consider as root of the project folder that contain the top-most parent in a multi-module project.",
                    null,
                    "project root"
            );

    public static final InputConfigurationProperty MODULE =
            new InputConfigurationProperty(
                    "targetModule",
                    "specify the module to be amplified. This value must be a relative path from the property " + PROJECT_ROOT_PATH.getName() + ". " +
                            "If your project is multi-module, you must use this property because DSpot works at module level.",
                    "",
                    "targeted module"
            );

    public static final InputConfigurationProperty SRC_CODE =
            new InputConfigurationProperty(
                    "src",
                    "specify the relative path from " +
                            PROJECT_ROOT_PATH.getName() + "/" + MODULE.getName() +
                            " of the folder that contain sources (.java).",
                    "src/main/java/",
                    "source folder"
            );

    public static final InputConfigurationProperty TEST_SRC_CODE =
            new InputConfigurationProperty(
                    "testSrc",
                    "specify the relative path from " +
                            PROJECT_ROOT_PATH.getName() + "/" + MODULE.getName() +
                            " of the folder that contain test sources (.java).",
                    "src/test/java/",
                    "test source folder"
            );

    public static final InputConfigurationProperty SRC_CLASSES =
            new InputConfigurationProperty(
                    "classes",
                    "specify the relative path from " +
                            PROJECT_ROOT_PATH.getName() + "/" + MODULE.getName() +
                            " of the folder that contain binaries of the source program (.class).",
                    "target/classes/",
                    "binaries folder"
            );

    public static final InputConfigurationProperty TEST_CLASSES =
            new InputConfigurationProperty(
                    "testClasses",
                    "specify the relative path from " +
                            PROJECT_ROOT_PATH.getName() + "/" + MODULE.getName() +
                            " of the folder that contain binaries of the test source program (.class).",
                    "target/test-classes/",
                    "test binaries folder"
            );

    public static final InputConfigurationProperty ADDITIONAL_CP_ELEMENTS =
            new InputConfigurationProperty(
                    "additionalClasspathElements",
                    "specify additional classpath elements. (e.g. a jar file) " +
                            "This value should be a list of relative paths from " +
                            PROJECT_ROOT_PATH.getName() + "/" + MODULE.getName() + ". " +
                            "Elements of the list must be separated by a comma \',\'.",
                    ""
            );

    public static final InputConfigurationProperty SYSTEM_PROPERTIES =
            new InputConfigurationProperty(
                    "systemProperties",
                    "specify system properties. " +
                            "This value should be a list of couple property;value, separated by a comma \',\'. " +
                            "For example, systemProperties=admin=toto,passwd=tata. This define two system properties."
                    ,
                    ""
            );

    public static final InputConfigurationProperty PATH_TO_SECOND_VERSION =
            new InputConfigurationProperty(
                    "pathToSecondVersion",
                    "when using the ChangeDetectorSelector" +
                            ", you must specify this property. " +
                            "This property should have for value the path to the root of " +
                            "the second version of the project. " +
                            "It is recommended to give an absolute path",
                    "",
                    "path to second version",
                    "folderPath"
            );

    public static final InputConfigurationProperty AUTOMATIC_BUILDER_NAME =
            new InputConfigurationProperty(
                    "automaticBuilderName",
                    "specify the type of automatic builder. " +
                            "This properties is redundant with the command line option --automatic-builder. " +
                            "It should have also the same value: (MavenBuilder | GradleBuilder). " +
                            "This property has the priority over the command line.",
                    ""
            );

    public static final InputConfigurationProperty OUTPUT_DIRECTORY =
            new InputConfigurationProperty(
                    "outputDirectory",
                    "specify a path folder for the output.",
                    ""
            );

    public static final InputConfigurationProperty MAVEN_HOME =
            new InputConfigurationProperty(
                    "mavenHome",
                    "specify the maven home directory. " +
                            "This property is redundant with the command line option --maven-home. " +
                            "This property has the priority over the command line. " +
                            "If this property is not specified, nor the command line option --maven-home, " +
                            "DSpot will first look in both MAVEN_HOME and M2_HOME environment variables. " +
                            "If these variables are not set, DSpot will look for a maven home at default locations " +
                            "/usr/share/maven/, /usr/local/maven-3.3.9/ and /usr/share/maven3/.",
                    "",
                    "maven installation",
                    "maven.home"
            );

    public static final InputConfigurationProperty MAVEN_PRE_GOALS =
            new InputConfigurationProperty(
                    "mavenPreGoals",
                    "specify pre goals to run before executing test with maven." +
                            "This property will used as follow: the elements, separated by a comma," +
                            "must be valid maven goals and they will be placed just before the \"test\" goal, e.g." +
                            "maven.pre.goals=preGoal1,preGoal2 will give \"mvn preGoal1 preGoal2 test\"",
                    "",
                    "maven pre goals",
                    "maven.pre.goals"
                    );

    public static final InputConfigurationProperty DELTA_ASSERTS_FLOAT =
            new InputConfigurationProperty(
                    "delta",
                    "specify the delta value for the assertions of floating-point numbers. " +
                            "If DSpot generates assertions for float, " +
                            "it uses Assert.assertEquals(expected, actual, delta). " +
                            "This property specify the delta value.",
                    "0.1"
            );

    public static final InputConfigurationProperty PIT_FILTER_CLASSES_TO_KEEP =
            new InputConfigurationProperty(
                    "pitFilterClassesToKeep",
                    "specify the filter of classes to keep used by PIT. " +
                            "This allow you restrict the scope of the mutation done by PIT.",
                    "",
                    "",
                    "filter"
            );

    public static final InputConfigurationProperty PIT_VERSION =
            new InputConfigurationProperty(
                    "pitVersion",
                    "specify the version of PIT to use.",
                    "1.4.0"
            );

    public static final InputConfigurationProperty DESCARTES_VERSION =
            new InputConfigurationProperty(
                    "descartesVersion",
                    "specify the version of pit-descartes to use.",
                    "1.2.4"
            );

    public static final InputConfigurationProperty EXCLUDED_CLASSES =
            new InputConfigurationProperty(
                    "excludedClasses",
                    "specify the full qualified name of excluded test classes. " +
                            "Each qualified name must be separated by a comma \',\'. " +
                            "These classes won't be amplified, nor executed during the mutation analysis, " +
                            "if the PitMutantScoreSelector is used." +
                            "This property can be valued by a regex.",
                    ""
            );

    public static final InputConfigurationProperty EXCLUDED_TEST_CASES =
            new InputConfigurationProperty(
                    "excludedTestCases",
                    "specify the list of test cases to be excluded. " +
                            "Each is the name of a test case, separated by a comma \',\'.",
                    ""
            );

    public static final InputConfigurationProperty JVM_ARGS =
            new InputConfigurationProperty(
                    "jvmArgs",
                    "specify JVM args to use when executing the test, PIT or other java process. " +
                                "This arguments should be a list, separated by a comma \',\', "+
                                "e.g. jvmArgs=Xmx2048m,-Xms1024m',-Dis.admin.user=admin,-Dis.admin.passwd=$2pRSid#",
                    ""
            );

    public static final InputConfigurationProperty DESCARTES_MUTATORS =
            new InputConfigurationProperty(
                    "descartesMutators",
                    "specify the list of descartes mutators to be used separated by comma. Please refer to the descartes documentation for more details: https://github.com/STAMP-project/pitest-descartes",
                    ""
            );

    public static final InputConfigurationProperty CACHE_SIZE =
            new InputConfigurationProperty(
                    "cacheSize",
                    "specify the size of the memory cache in terms of the number of store entries",
                    "10000"
            );



    /**
     * main method to generate the documentation. This method will output the documentation on the standard output, in markdown format.
     * @param args unused
     */
    public static void main(String[] args) {
        final List<InputConfigurationProperty> inputConfigurationProperties = new ArrayList<>();
        inputConfigurationProperties.add(PROJECT_ROOT_PATH);
        inputConfigurationProperties.add(MODULE);
        inputConfigurationProperties.add(SRC_CODE);
        inputConfigurationProperties.add(TEST_SRC_CODE);
        inputConfigurationProperties.add(SRC_CLASSES);
        inputConfigurationProperties.add(TEST_CLASSES);
        inputConfigurationProperties.add(ADDITIONAL_CP_ELEMENTS);
        inputConfigurationProperties.add(SYSTEM_PROPERTIES);
        inputConfigurationProperties.add(OUTPUT_DIRECTORY);
        inputConfigurationProperties.add(DELTA_ASSERTS_FLOAT);
        inputConfigurationProperties.add(EXCLUDED_CLASSES);
        inputConfigurationProperties.add(EXCLUDED_TEST_CASES);
        inputConfigurationProperties.add(MAVEN_HOME);
        inputConfigurationProperties.add(MAVEN_PRE_GOALS);
        inputConfigurationProperties.add(PATH_TO_SECOND_VERSION);
        inputConfigurationProperties.add(AUTOMATIC_BUILDER_NAME);
        inputConfigurationProperties.add(PIT_VERSION);
        inputConfigurationProperties.add(JVM_ARGS);
        inputConfigurationProperties.add(PIT_FILTER_CLASSES_TO_KEEP);
        inputConfigurationProperties.add(DESCARTES_VERSION);
        inputConfigurationProperties.add(DESCARTES_MUTATORS);
        final String output = "* Required properties" +
                AmplificationHelper.LINE_SEPARATOR +
                getRequiredProperties.apply(inputConfigurationProperties)
                        .map(InputConfigurationProperty::toString)
                        .collect(Collectors.joining(AmplificationHelper.LINE_SEPARATOR)) +
                AmplificationHelper.LINE_SEPARATOR +
                "* Optional properties" +
                AmplificationHelper.LINE_SEPARATOR +
                getOptionalProperties.apply(inputConfigurationProperties)
                        .map(InputConfigurationProperty::toString)
                        .map(s -> s.replaceAll("i\\.e\\.", "_i.e._"))
                        .map(s -> s.replaceAll("e\\.g\\.", "_e.g._"))
                        .map(wrapOptionWithQuote)
                        .collect(Collectors.joining(AmplificationHelper.LINE_SEPARATOR)) +
                AmplificationHelper.LINE_SEPARATOR +
                "You can find an example of properties file [here](https://github.com/STAMP-project/dspot/blob/master/dspot/src/test/resources/sample/sample.properties)).";
        System.out.println(output);
    }

    private final static Function<String, String> wrapOptionWithQuote = s -> {
        final String[] split = s.split(" ");
        List<String> wrappedSplit = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            if (split[i].startsWith("--")) {
                if (split[i].endsWith(".")) {
                    wrappedSplit.add("`" + split[i].substring(0, split[i].length() - 1) + "`.");
                } else {
                    wrappedSplit.add("`" + split[i] + "`");
                    if (i + 1 < split.length) {
                        wrappedSplit.add("`" + split[i+1] + "`");
                    }
                }
            } else {
                wrappedSplit.add(split[i]);
            }
        }
        return String.join(" ", wrappedSplit);
    };

    private final static Function<List<InputConfigurationProperty>, Stream<InputConfigurationProperty>> getRequiredProperties =
            properties -> properties.stream().filter(InputConfigurationProperty::isRequired);

    private final static Function<List<InputConfigurationProperty>, Stream<InputConfigurationProperty>> getOptionalProperties =
            properties -> properties.stream().filter(inputConfigurationProperty -> !inputConfigurationProperty.isRequired());
}
