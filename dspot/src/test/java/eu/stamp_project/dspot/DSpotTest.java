package eu.stamp_project.dspot;

import eu.stamp_project.AbstractTest;
import eu.stamp_project.Utils;
import eu.stamp_project.dspot.amplifier.Amplifier;
import eu.stamp_project.dspot.amplifier.MethodDuplicationAmplifier;
import eu.stamp_project.dspot.selector.JacocoCoverageSelector;
import eu.stamp_project.dspot.selector.TakeAllSelector;
import eu.stamp_project.dspot.selector.TestSelector;
import eu.stamp_project.utils.options.BudgetizerEnum;
import eu.stamp_project.utils.program.InputConfiguration;
import eu.stamp_project.testrunner.EntryPoint;
import eu.stamp_project.utils.compilation.TestCompiler;
import org.junit.AfterClass;
import org.junit.Test;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 24/11/17
 */
public class DSpotTest extends AbstractTest {

    @Test
    public void testJVMArgsIsPassToEntryPoint() {

        /*
            test that the JVM args of InputConfiguration is well given to the JVMArgs of EntryPoint.
            This test is quite weak, we rely on the good usage of JVMArgs by the EntryPoint
         */

        assertEquals("-Xmx2048m,-Xms1024m,-Dis.admin.user=admin,-Dis.admin.passwd=$2pRSid#",InputConfiguration.get().getJVMArgs());
        assertEquals("-Xmx2048m -Xms1024m -Dis.admin.user=admin -Dis.admin.passwd=$2pRSid#", EntryPoint.JVMArgs);
    }

    @Test
    public void testUsingAmplifiedTestClassFromTheStart() throws Exception {

        /*
            Some project using the name in assertion, and thus we cannot use the original test class name during the
            process of the amplification. That is now, we rename, and use the renaming version from the start of the amplification.
         */

        final CtClass<?> testClass = Utils.findClass("fr.inria.preparation.MustBeRenamedFromStart");
        final DSpot dSpot = new DSpot(
                new TakeAllSelector()
        );
        final CtType amplifyTest = dSpot.amplifyTestClass("fr.inria.preparation.MustBeRenamedFromStart").get(0);
        assertTrue("should be empty", TestCompiler.compileAndRun(
                amplifyTest,
                Utils.getCompiler(),
                Collections.singletonList(Utils.findMethod("fr.inria.preparation.MustBeRenamedFromStart", "test")),
                InputConfiguration.get()
        ).getFailingTests().isEmpty());
    }

    @Test
    public void testExcludedClassesInPropertyFile() throws Exception {

        /*
            Usage of properties:
                - excludedClasses: list of full qualified name of test classes to be excluded (separated by comma ',')
                - excludedTestCases: list of name of test cases (methods) to be excluded (separated by comma ',')
         */

        final MockDSpot dSpot = new MockDSpot(1,
                Collections.singletonList(new MethodDuplicationAmplifier()),
                new JacocoCoverageSelector()
        );
        // the test class fr.inria.filter.passing.PassingTest has 3 methods, but only two are amplified
        assertEquals(3, Utils.findClass("fr.inria.filter.passing.PassingTest").getMethods().size());
        // the test class fr.inria.filter.failing.FailingTest match the regex, but it is excluded in the properties
        final List<CtType<?>> ctTypes = dSpot.amplifyTestClass("fr.inria.filter.*");
        assertEquals(1, ctTypes.size());
        // uses the mock to retrieve the number of method to be amplified
        assertEquals(2, dSpot.numberOfMethod);
    }

    private class MockDSpot extends DSpot {

        public int numberOfMethod = 0;

        public MockDSpot(int numberOfIterations, List<Amplifier> amplifiers, TestSelector testSelector) throws Exception {
            super(numberOfIterations, amplifiers, testSelector, BudgetizerEnum.RandomBudgetizer);
        }

        @Override
        protected List<CtMethod<?>> filterTestCases(List<CtMethod<?>> testMethods) {
            List<CtMethod<?>> filteredMethods = super.filterTestCases(testMethods);
            numberOfMethod = filteredMethods.size();
            return filteredMethods;
        }
    }

    @AfterClass
    public static void tearDown() {
        Utils.reset();
    }
}
