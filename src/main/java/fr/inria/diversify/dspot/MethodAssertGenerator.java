package fr.inria.diversify.dspot;

import fr.inria.diversify.buildSystem.DiversifyClassLoader;
import fr.inria.diversify.compare.Compare;
import fr.inria.diversify.compare.ObjectLog;
import fr.inria.diversify.compare.Observation;
import fr.inria.diversify.runner.InputProgram;
import fr.inria.diversify.factories.DiversityCompiler;
import fr.inria.diversify.testRunner.JunitResult;
import fr.inria.diversify.testRunner.JunitRunner;
import fr.inria.diversify.util.Log;
import fr.inria.diversify.util.PrintClassUtils;
import org.apache.commons.io.FileUtils;
import org.junit.runner.notification.Failure;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Query;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User: Simon
 * Date: 22/10/15
 * Time: 10:06
 */
public class MethodAssertGenerator {
    protected ClassLoader assertGeneratorClassLoader;
    protected CtMethod test;
    protected CtType originalClass;
    protected DiversityCompiler compiler;
    protected InputProgram inputProgram;
    protected List<Integer> statementsIndexToAssert;



    public MethodAssertGenerator(CtType originalClass, InputProgram inputProgram, DiversityCompiler compiler, ClassLoader applicationClassLoader) throws IOException {
        this.originalClass = originalClass;
        this.compiler = compiler;
        this.assertGeneratorClassLoader = applicationClassLoader;

        this.inputProgram = inputProgram;
        statementsIndexToAssert = new ArrayList<>();
    }

    protected CtMethod generateAssert(CtMethod test) throws IOException, ClassNotFoundException {
        this.test = test;
        this.test = createTestWithoutAssert(new ArrayList<>(), false);
        this.test.setParent(test.getParent());
        for(int i = 0; i < Query.getElements(this.test, new TypeFilter(CtStatement.class)).size(); i++) {
            statementsIndexToAssert.add(i);
        }

        CtMethod newTest = generateAssert();
        if(newTest == null || !isCorrect(newTest)) {
            return null;
        }
        return newTest;
    }

    protected CtMethod  generateAssert(CtMethod test, List<Integer> statementsIndexToAssert) throws IOException, ClassNotFoundException {
        this.test = test;
        this.statementsIndexToAssert = statementsIndexToAssert;

        CtMethod newTest = generateAssert();
        if(newTest == null || !isCorrect(newTest)) {
            return null;
        }

//        JunitResult r1 = runSingleTest(newTest, assertGeneratorClassLoader);
//        JunitResult r2 = runSingleTest(newTest, DSpot.regressionClassLoader);
//
//        if(!equalResult(r1, r2)) {
//            try {
//                r1 = runSingleTest(newTest, assertGeneratorClassLoader);
//                r2 = runSingleTest(newTest, DSpot.regressionClassLoader);
//                Thread.sleep(200);
//                if(!equalResult(r1, r2)) {
//                    Log.info("");
//                    log.write("version: "+ version + ", " + test.getSignature()+ "\n");
//                    File file = new File(resultDir + "/assertGenerationTestSource/"+ version + "/" + System.currentTimeMillis() + "/");
//                    file.mkdirs();
//
//                    CtClass newClass = initTestClass();
//
//                    CtMethod cloneTest = getFactory().Core().clone(test);
//                    newClass.addMethod(cloneTest);
//                    PrintClassUtils.printJavaFile(file, newClass);
//                }
//            } catch (Throwable e) {}
//            log.flush();
//        }

        return newTest;
    }

//    protected boolean equalResult(JunitResult r1, JunitResult r2) {
//        return (r1 == null) == (r2 == null)
//                && r1 == null
//                || r1.getFailures().size() == r2.getFailures().size();
//    }

    protected CtMethod generateAssert() throws IOException, ClassNotFoundException {
        List<CtMethod> testsToRun = new ArrayList<>();
        CtType cl = initTestClass();

        CtMethod cloneTest = getFactory().Core().clone(test);
        cl.addMethod(cloneTest);
        testsToRun.add(cloneTest);

        CtMethod testWithoutAssert = createTestWithoutAssert(new ArrayList<>(), false);
        testsToRun.add(testWithoutAssert);
        cl.addMethod(testWithoutAssert);

        JunitResult result = runTests(testsToRun, assertGeneratorClassLoader);
        if(result == null || result.getTestRuns().size() != testsToRun.size()) {
            return null;
        }
        try {
            String testWithoutAssertName = test.getSimpleName() + "_withoutAssert";
            if(testFailed(testWithoutAssertName, result)) {
                return makeFailureTest(getFailure(testWithoutAssertName, result));
            } else if(!testFailed(test.getSimpleName(), result)) {
                if(!statementsIndexToAssert.isEmpty()) {
                    return buildNewAssert();
                }
            } else {
                removeFailAssert();
                if(!statementsIndexToAssert.isEmpty()) {
                    return buildNewAssert();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.debug("");
        }
        return null;
    }

    protected CtMethod makeFailureTest(Failure failure) {
        CtMethod testWithoutAssert = createTestWithoutAssert(new ArrayList<>(), false);
        testWithoutAssert.setSimpleName(test.getSimpleName());
        Factory factory = testWithoutAssert.getFactory();

        Throwable exception = failure.getException();
        if(exception instanceof  AssertionError)   {
            exception = exception.getCause();
        }
        Class exceptionClass;
        if(exception == null) {
            exceptionClass = Throwable.class;
        } else {
            exceptionClass = exception.getClass();
        }

        CtTry tryBlock = factory.Core().createTry();
        tryBlock.setBody(testWithoutAssert.getBody());
        String snippet = " junit.framework.TestCase.fail(\"" +test.getSimpleName()+" should have thrown " + exceptionClass.getSimpleName()+"\")";
        tryBlock.getBody().addStatement(factory.Code().createCodeSnippetStatement(snippet));

        CtCatch ctCatch = factory.Core().createCatch();
        CtTypeReference exceptionType = factory.Type().createReference(exceptionClass);
        ctCatch.setParameter(factory.Code().createCatchVariable(exceptionType, "eee"));

        ctCatch.setBody(factory.Core().createBlock());

        List<CtCatch> catchers = new ArrayList<>(1);
        catchers.add(ctCatch);
        tryBlock.setCatchers(catchers);

        CtBlock body = factory.Core().createBlock();
        body.addStatement(tryBlock);

        testWithoutAssert.setBody(body);
        return testWithoutAssert;
    }

    protected CtMethod buildNewAssert() throws IOException, ClassNotFoundException {
        CtType cl = initTestClass();
        List<CtMethod> testsToRun = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            CtMethod testWithLog = createTestWithLog();
            testWithLog.setSimpleName(testWithLog.getSimpleName() + i);
            cl.addMethod(testWithLog);
            testsToRun.add(testWithLog);
            cl.addMethod(testWithLog);
        }

        ObjectLog.reset();

        JunitResult result = runTests(testsToRun, assertGeneratorClassLoader);
        return buildTestWithAssert(ObjectLog.getObservations());
    }

    protected CtMethod buildTestWithAssert(Map<String, Observation> observations) {
        CtMethod testWithAssert = getFactory().Core().clone(test);

        List<CtStatement> statements = Query.getElements(testWithAssert, new TypeFilter(CtStatement.class));
        for(String id : observations.keySet()) {
           int line = Integer.parseInt(id.split("__")[1]);
            for(String snippet : observations.get(id).buildAssert()) {
                CtStatement assertStmt = getFactory().Code().createCodeSnippetStatement(snippet);
                try {
                    CtStatement stmt = statements.get(line);
                    if (stmt instanceof CtInvocation) {
                        String localVarSnippet = ((CtInvocation) stmt).getType().getQualifiedName()
                                + " o_" + id + " = "
                                + stmt.toString();
                        CtStatement localVarStmt = getFactory().Code().createCodeSnippetStatement(localVarSnippet);
                        stmt.replace(localVarStmt);
                        statements.set(line, localVarStmt);
                        localVarStmt.setParent(stmt.getParent());
                        localVarStmt.insertAfter(assertStmt);
                    } else {
                        stmt.insertAfter(assertStmt);
                    }

                } catch (Exception e) {
                    Log.debug("");
                }
            }
        }

        return testWithAssert;
    }

    protected boolean isCorrect(CtMethod test) throws IOException, ClassNotFoundException {
        JunitResult result = runSingleTest(test, assertGeneratorClassLoader);
        return result != null && result.getFailures().isEmpty();
    }

    protected void removeFailAssert() throws IOException, ClassNotFoundException {
        List<Integer> goodAssert = findGoodAssert();
        String testName = test.getSimpleName();
        test = createTestWithoutAssert(goodAssert, true);
        test.setSimpleName(testName);
    }

    protected List<Integer> findGoodAssert() throws IOException, ClassNotFoundException {
        int stmtIndex = 0;
        List<CtMethod> testsToRun = new ArrayList<>();
        List<Integer> assertIndex = new ArrayList<>();
        List<CtStatement> statements = Query.getElements(test, new TypeFilter(CtStatement.class));
        for(CtStatement statement : statements) {
            if (isAssert(statement)) {
                assertIndex.add(stmtIndex);
            }
            stmtIndex++;
        }

        CtType newClass = getFactory().Core().clone(originalClass);
        newClass.setParent(originalClass.getParent());
        for(int i = 0; i < assertIndex.size(); i++) {
            List<Integer> assertToKeep = new ArrayList<>();
            assertToKeep.add(assertIndex.get(i));
            CtMethod mth = createTestWithoutAssert(assertToKeep, false);
            mth.setSimpleName(mth.getSimpleName() + "_" + i);
            newClass.addMethod(mth);
            testsToRun.add(mth);
        }
        ObjectLog.reset();
        JunitResult result = runTests(testsToRun, assertGeneratorClassLoader);

        List<Integer> goodAssertIndex = new ArrayList<>();
        for(int i = 0; i < testsToRun.size(); i++) {
            if(!testFailed(testsToRun.get(i).getSimpleName(), result)) {
                goodAssertIndex.add(assertIndex.get(i));
            }
        }
        return goodAssertIndex;
    }

    protected Failure getFailure(String methodName, JunitResult result) {
        return result.getFailures().stream()
                .filter(failure -> methodName.equals(failure.getDescription().getMethodName()))
                .findAny()
                .orElse(null);
    }

    protected boolean testFailed(String methodName, JunitResult result) {
        return getFailure(methodName, result) != null;
    }

    protected JunitResult runTests(List<CtMethod> testsToRun, ClassLoader classLoader) throws ClassNotFoundException {
        DiversifyClassLoader diversifyClassLoader = new DiversifyClassLoader(classLoader, compiler.getBinaryOutputDirectory().getAbsolutePath());

        List<CtType> classesToCompile = testsToRun.stream()
                .map(mth -> mth.getDeclaringType())
                .distinct()
                .collect(Collectors.toList());

        boolean status = classesToCompile.stream()
                .allMatch(cl -> writeAndCompile(cl));

        if(!status) {
            return null;
        }

        List<String> ClassName = classesToCompile.stream()
                .map(cl -> cl.getQualifiedName())
                .distinct()
                .collect(Collectors.toList());
        diversifyClassLoader.setClassFilter(ClassName);
        JunitRunner junitRunner = new JunitRunner(inputProgram, diversifyClassLoader);
        return junitRunner.runTestClasses(ClassName, testsToRun.stream().map(test -> test.getSimpleName()).collect(Collectors.toList()));
    }

    protected JunitResult runSingleTest(CtMethod test, ClassLoader classLoader) throws ClassNotFoundException, IOException {
        List<CtMethod>testsToRun = new ArrayList<>();
        CtType newClass = initTestClass();

        CtMethod cloneTest = getFactory().Core().clone(test);
        newClass.addMethod(cloneTest);
        testsToRun.add(cloneTest);

        return runTests(testsToRun, classLoader);
    }

    //todo refactor
    protected boolean writeAndCompile(CtType cl) {
        try {
            FileUtils.cleanDirectory(compiler.getSourceOutputDirectory());
            FileUtils.cleanDirectory(compiler.getBinaryOutputDirectory());

            copyLoggerFile();
            PrintClassUtils.printJavaFile(compiler.getSourceOutputDirectory(), cl);

            return compiler.compileFileIn(compiler.getSourceOutputDirectory(), false);
        } catch (Exception e) {
            Log.warn("error during compilation", e);
            return false;
        }
    }

    protected CtType initTestClass() {
        CtType newClass = getFactory().Core().clone(originalClass);
        newClass.setParent(originalClass.getParent());

        return newClass;
    }

    protected CtMethod createTestWithLog() {
        CtMethod newTest = getFactory().Core().clone(test);
        newTest.setParent(test.getParent());
        newTest.setSimpleName(test.getSimpleName() + "_withlog");

        List<CtStatement> stmts = Query.getElements(newTest, new TypeFilter(CtStatement.class));
        for(int i = 0; i < stmts.size(); i++) {
            CtStatement stmt = stmts.get(i);
            if(statementsIndexToAssert.contains(i) && isStmtToLog(stmt)) {
                addLogStmt(stmt, test.getSimpleName() + "__" + i);
            }
        }
        return newTest;
    }

    protected boolean isStmtToLog(CtStatement statement) {
        if(!(statement.getParent() instanceof CtBlock)) {
            return false;
        }
        if(statement instanceof CtInvocation) {
            CtInvocation invocation = (CtInvocation) statement;
            String type = invocation.getType().toString();
            return !(type.equals("void") || type.equals("void"));
        }
        return statement instanceof CtVariableWrite
                || statement instanceof CtAssignment
                || statement instanceof CtLocalVariable;
    }

    protected void addLogStmt(CtStatement stmt, String id) {
        String snippet = "";
        CtStatement insertAfter = null;
        if(stmt instanceof CtVariableWrite) {
            CtVariableWrite varWrite = (CtVariableWrite) stmt;
            snippet = "fr.inria.diversify.compare.ObjectLog.log(" + varWrite.getVariable()
                    + ",\"" + varWrite.getVariable() + "\",\"" + id + "\")";
            insertAfter = stmt;
        }
        if(stmt instanceof CtLocalVariable) {
            CtLocalVariable localVar = (CtLocalVariable) stmt;
            snippet = "fr.inria.diversify.compare.ObjectLog.log(" + localVar.getSimpleName()
                    + ",\"" + localVar.getSimpleName() + "\",\"" + id + "\")";
            insertAfter = stmt;
        }
        if(stmt instanceof CtAssignment) {
            CtAssignment localVar = (CtAssignment) stmt;
            snippet = "fr.inria.diversify.compare.ObjectLog.log(" + localVar.getAssigned()
                    + ",\"" + localVar.getAssigned() + "\",\"" + id + "\")";
            insertAfter = stmt;
        }

        if(stmt instanceof CtInvocation) {
            CtInvocation invocation = (CtInvocation) stmt;
            String snippetStmt = "Object o_" + id + " = " + invocation.toString();
            CtStatement localVarSnippet = getFactory().Code().createCodeSnippetStatement(snippetStmt);
            stmt.replace(localVarSnippet);
            insertAfter = localVarSnippet;

            snippet = "fr.inria.diversify.compare.ObjectLog.log(o_" + id
                    + ",\"o_" + id + "\",\"" + id + "\")";
        }
        CtStatement logStmt = getFactory().Code().createCodeSnippetStatement(snippet);
        insertAfter.insertAfter(logStmt);
    }

    protected CtMethod createTestWithoutAssert(List<Integer> assertIndexToKeep, boolean updateStatementsIndexToAssert) {
        CtMethod newTest = getFactory().Core().clone(test);
        newTest.setParent(test.getParent());
        newTest.setSimpleName(test.getSimpleName() + "_withoutAssert");

        int stmtIndex = 0;
        List<CtStatement> statements = Query.getElements(newTest, new TypeFilter(CtStatement.class));
        for(CtStatement statement : statements){
            try {
                if (!assertIndexToKeep.contains(stmtIndex) && isAssert(statement)) {
                    CtBlock block = buildRemoveAssertBlock((CtInvocation) statement, stmtIndex);
                    if(updateStatementsIndexToAssert) {
                        updateStatementsIndexToAssert(stmtIndex, block.getStatements().size() - 1);
                    }
                    if(statement.getParent() instanceof CtCase) {
                        CtCase ctCase = (CtCase) statement.getParent();
                        int index = ctCase.getStatements().indexOf(statement);
                        ctCase.getStatements().add(index, block);
                        ctCase.getStatements().remove(statement);
                    } else {
                        statement.replace(block);
                    }
                }
                stmtIndex++;
            } catch (Exception e) {}
        }
        return newTest;
    }

    protected void updateStatementsIndexToAssert(int stmtIndex, int update) {
        if(update != 0) {
            List<Integer> newList = new ArrayList<>(statementsIndexToAssert.size());
            for (Integer index : statementsIndexToAssert) {
                if(index > stmtIndex) {
                    statementsIndexToAssert.add(index + update);
                } else {
                    newList.add(index);
                }
            }
            statementsIndexToAssert = newList;
        }
    }

    protected CtBlock buildRemoveAssertBlock(CtInvocation assertInvocation, int blockId) {
        CtBlock block = getFactory().Core().createBlock();

        int[] idx = { 0 };
        getNotLiteralArgs(assertInvocation).stream()
                .filter(arg -> !(arg instanceof CtVariableAccess))
                .map(arg -> buildVarStatement(arg, blockId + "_" + (idx[0]++)))
                .forEach(stmt -> block.addStatement(stmt));

        block.setParent(assertInvocation.getParent());
        return block;
    }

    protected List<CtExpression> getNotLiteralArgs(CtInvocation invocation) {
        List<CtExpression> args = invocation.getArguments();
        return args.stream()
                .filter(arg -> !(arg instanceof CtLiteral))
                .collect(Collectors.toList());
    }

    protected CtLocalVariable<Object> buildVarStatement(CtExpression arg, String id) {
        CtTypeReference<Object> objectType = getFactory().Core().createTypeReference();
        objectType.setSimpleName("Object");
        CtLocalVariable<Object> localVar = getFactory().Code().createLocalVariable(objectType, "o_" + id, arg);

        return localVar;
    }

    protected Factory getFactory() {
        return test.getFactory();
    }

    protected boolean isAssert(CtStatement statement) {
        if(statement instanceof CtInvocation) {
            CtInvocation invocation = (CtInvocation) statement;
            try {
                Class cl = invocation.getExecutable().getDeclaringType().getActualClass();
                String signature = invocation.getSignature();
                return (signature.contains("assertTrue")
                        || signature.contains("assertFalse")
                        || signature.contains("assertSame")
                        || signature.contains("assertEquals"));
//                    && isAssertInstance(cl);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    protected boolean isAssertInstance(Class cl) {
        if (cl.equals(org.junit.Assert.class) || cl.equals(junit.framework.Assert.class))
            return true;
        Class superCl = cl.getSuperclass();
        if(superCl != null) {
            return isAssertInstance(superCl);
        }
        return false;
    }

    protected void copyLoggerFile() throws IOException {
        String comparePackage = Compare.class.getPackage().getName().replace(".", "/");
        File srcDir = new File(System.getProperty("user.dir") + "/src/main/java/" + comparePackage);

        File destDir = new File(compiler.getSourceOutputDirectory() + "/" + comparePackage);
        FileUtils.forceMkdir(destDir);

        FileUtils.copyDirectory(srcDir, destDir);
    }
}