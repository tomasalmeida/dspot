package fr.inria.stamp.minimization;

import fr.inria.diversify.automaticbuilder.AutomaticBuilderFactory;
import fr.inria.diversify.dspot.support.DSpotCompiler;
import fr.inria.diversify.utils.AmplificationChecker;
import fr.inria.diversify.utils.AmplificationHelper;
import fr.inria.diversify.utils.DSpotUtils;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.diversify.utils.sosiefier.InputProgram;
import fr.inria.stamp.Main;
import fr.inria.stamp.test.launcher.TestLauncher;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 07/03/18
 */
public class ChangeMinimizer extends GeneralMinimizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeMinimizer.class);

    private CtType<?> testClass;

    private InputConfiguration configuration;

    private InputProgram program;

    private String pathToChangedVersionOfProgram;

    private Map<CtMethod<?>, Failure> failurePerAmplifiedTest;

    private String classpath;

    public ChangeMinimizer(CtType<?> testClass,
                           InputConfiguration configuration,
                           InputProgram program,
                           String pathToChangedVersionOfProgram,
                           Map<CtMethod<?>, Failure> failurePerAmplifiedTest) {
        this.testClass = testClass;
        this.configuration = configuration;
        this.program = program;
        this.pathToChangedVersionOfProgram = pathToChangedVersionOfProgram;
        this.failurePerAmplifiedTest = failurePerAmplifiedTest;
        this.classpath = AutomaticBuilderFactory
                .getAutomaticBuilder(this.configuration)
                .buildClasspath(this.program.getProgramDir())
                + AmplificationHelper.PATH_SEPARATOR + "target/dspot/dependencies/";
    }

    @Override
    public CtMethod<?> minimize(CtMethod<?> amplifiedTestToBeMinimized) {
        final CtMethod<?> generalMinimize = super.minimize(amplifiedTestToBeMinimized);
        final CtMethod<?> changeMinimize = generalMinimize.clone();
        final long time = System.currentTimeMillis();
        final Failure failureToKeep = this.failurePerAmplifiedTest.get(amplifiedTestToBeMinimized);
        final List<CtInvocation> assertions = changeMinimize.filterChildren(AmplificationHelper.ASSERTIONS_FILTER).list();
        LOGGER.info("Minimizing {} assertions.", assertions.size());
        assertions.forEach(invocation -> {
                    DSpotUtils.printProgress(assertions.indexOf(invocation), assertions.size());
                    tryToRemoveAssertion(changeMinimize,
                            invocation,
                            failureToKeep
                    );
                }
        );
        LOGGER.info("Reduce {}, {} statements to {} statements in {} ms.",
                amplifiedTestToBeMinimized.getSimpleName(),
                generalMinimize.getBody().getStatements().size(),
                changeMinimize.getBody().getStatements().size(),
                System.currentTimeMillis() - time
        );

        // now that we reduce the amplified test, we must update the stack trace
        updateStackTrace(amplifiedTestToBeMinimized, changeMinimize);

        return changeMinimize;
    }

    private void updateStackTrace(CtMethod<?> amplifiedTestToBeMinimized, CtMethod<?> changeMinimize) {
        CtType<?> clone = this.testClass.clone();
        // must compile
        if (!printAndCompile(clone, changeMinimize)) {
            throw new RuntimeException("The minimizer created an uncompilable test method.");
        }
        // must have (the same?) failure
        final Failure failure = TestLauncher.run(this.configuration,
                classpath +
                        AmplificationHelper.PATH_SEPARATOR +
                        this.pathToChangedVersionOfProgram + "/" + this.program.getClassesDir() +
                        AmplificationHelper.PATH_SEPARATOR +
                        this.pathToChangedVersionOfProgram + "/" + this.program.getTestClassesDir(),
                clone,
                Collections.singletonList(changeMinimize.getSimpleName()))
                .getFailingTests().get(0);
        failurePerAmplifiedTest.remove(amplifiedTestToBeMinimized);
        failurePerAmplifiedTest.put(changeMinimize, failure);
    }

    private void tryToRemoveAssertion(CtMethod<?> amplifiedTestToBeMinimized,
                                      CtInvocation<?> invocation,
                                      Failure failureToKeep) {
        final CtMethod<?> clone = amplifiedTestToBeMinimized.clone();
        clone.getBody().removeStatement(invocation);
        if (checkIfMinimizationIsOk(clone, failureToKeep)) {
            amplifiedTestToBeMinimized.getBody().removeStatement(invocation);
        }
    }

    private boolean checkIfMinimizationIsOk(CtMethod<?> amplifiedTestToBeMinimized, Failure failure) {
        CtType<?> clone = this.testClass.clone();
        // must compile
        if (!printAndCompile(clone, amplifiedTestToBeMinimized)) {
            return false;
        }
        // must have (the same?) failure
        final List<Failure> failingTests = TestLauncher.run(this.configuration,
                classpath +
                        AmplificationHelper.PATH_SEPARATOR +
                        this.pathToChangedVersionOfProgram + "/" + this.program.getClassesDir() +
                        AmplificationHelper.PATH_SEPARATOR +
                        this.pathToChangedVersionOfProgram + "/" + this.program.getTestClassesDir(),
                clone,
                Collections.singletonList(amplifiedTestToBeMinimized.getSimpleName()))
                .getFailingTests();
        return !failingTests.isEmpty() &&
                ((failingTests.get(0) == null && failure.getMessage() == null) ||
                        failingTests.get(0).getMessage() != null &&
                                failingTests.get(0).getMessage().equals(failure.getMessage())
                );
    }

    private boolean printAndCompile(CtType<?> clone, CtMethod<?> amplifiedTestToBeMinimized) {
        clone.setParent(this.testClass.getParent());
        Main.verbose = false;
        this.testClass.getMethods().stream()
                .filter(AmplificationChecker::isTest)
                .forEach(clone::removeMethod);
        clone.addMethod(amplifiedTestToBeMinimized);
        DSpotUtils.printCtTypeToGivenDirectory(clone, new File(DSpotCompiler.pathToTmpTestSources));
        final boolean compile = DSpotCompiler.compile(DSpotCompiler.pathToTmpTestSources,
                classpath + AmplificationHelper.PATH_SEPARATOR +
                        this.program.getProgramDir() + "/" + this.program.getClassesDir()
                        + AmplificationHelper.PATH_SEPARATOR +
                        this.program.getProgramDir() + "/" + this.program.getTestClassesDir(),
                new File(this.pathToChangedVersionOfProgram + "/" + this.program.getTestClassesDir()));
        Main.verbose = true;
        return compile;
    }
}
