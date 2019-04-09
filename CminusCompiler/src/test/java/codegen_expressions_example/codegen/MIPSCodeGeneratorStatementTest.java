package codegen_expressions_example.codegen;

import codegen_expressions_example.syntax.*;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;

public class MIPSCodeGeneratorStatementTest {
    final Map<StructureName, LinkedHashMap<FieldName, Type>> TWO_INTS =
        new HashMap<StructureName, LinkedHashMap<FieldName, Type>>() {{
            put(new StructureName("TwoInts"), new LinkedHashMap<FieldName, Type>() {{
                put(new FieldName("x"), new IntType());
                put(new FieldName("y"), new IntType());
            }});
        }};

    final Map<StructureName, LinkedHashMap<FieldName, Type>> DOUBLE_TWO_INTS =
        new HashMap<StructureName, LinkedHashMap<FieldName, Type>>() {{
            final StructureName twoInts = new StructureName("TwoInts");
            put(twoInts, TWO_INTS.get(twoInts));
            put(new StructureName("FourInts"), new LinkedHashMap<FieldName, Type>() {{
                put(new FieldName("first"), new StructureType(twoInts));
                put(new FieldName("second"), new StructureType(twoInts));
            }});
        }};

    @Rule public TestName name = new TestName();
    
    // TODO: code duplication with MIPSCodeGeneratorTest
    public int parseOutput(final String[] spimOutput) {
        assert(spimOutput.length == 2);
        return Integer.parseInt(spimOutput[1]);
    } // parseOutput

    public void assertResult(final int expected, final Stmt stmt) throws IOException {
        assertResult(expected, stmt, new HashMap<StructureName, LinkedHashMap<FieldName, Type>>());
    }
        
    public void assertResult(final int expected,
                             final Stmt stmt,
                             final Map<StructureName, LinkedHashMap<FieldName, Type>> structDecs) throws IOException {
        boolean wantToSaveFile = true; // for debugging

        final File file = File.createTempFile(name.getMethodName(),
                                              ".asm",
                                              new File("testPrograms"));
        boolean testPassed = false;
        try {
            final MIPSCodeGenerator gen = new MIPSCodeGenerator(structDecs);
            gen.compileStatement(stmt);
            gen.writeCompleteFile(file, false);
            final String[] output = SPIMRunner.runFile(file);
            final int received = parseOutput(output);
            if (wantToSaveFile) {
                assertEquals("Expected: " + expected + " Received: " + received + " File: " +
                             file.getAbsolutePath(),
                             expected,
                             received);
            } else {
                assertEquals(expected, received);
            }
            testPassed = true;
        } finally {
            if (!wantToSaveFile || testPassed) {
                file.delete();
            }
        }
    } // assertResult

    public VariableDeclarationInitializationStmt vardec(final String variableName,
                                                        final Type type,
                                                        final Exp exp) {
        return new VariableDeclarationInitializationStmt(new VariableDeclaration(type,
                                                                                 new Variable(variableName)),
                                                         exp);
    }

    public static Stmt stmts(final Stmt... stmts) {
        assert(stmts.length > 0);
        Stmt result = stmts[stmts.length - 1];

        for (int index = stmts.length - 2; index >= 0; index--) {
            result = new SequenceStmt(stmts[index], result);
        }
        return result;
    }

    public static PrintStmt printVar(final String varName) {
        return new PrintStmt(new VariableExp(new Variable(varName)));
    }

    public static AssignmentStmt assign(final Lhs lhs, final Exp exp) {
        return new AssignmentStmt(lhs, exp);
    }
    
    public static AssignmentStmt assign(final String varName, final Exp exp) {
        return assign(new VariableLhs(new Variable(varName)), exp);
    }

    @Test
    public void testSingleIntVariableDeclaration() throws IOException {
        assertResult(1, stmts(vardec("x", new IntType(), new IntExp(1)),
                              printVar("x")));
    }

    @Test
    public void testDoubleIntVariableDeclarationGetFirst() throws IOException {
        assertResult(1, stmts(vardec("x", new IntType(), new IntExp(1)),
                              vardec("y", new IntType(), new IntExp(2)),
                              printVar("x")));
    }

    @Test
    public void testDoubleIntVariableDeclarationGetSecond() throws IOException {
        assertResult(2, stmts(vardec("x", new IntType(), new IntExp(1)),
                              vardec("y", new IntType(), new IntExp(2)),
                              printVar("y")));
    }

    @Test
    public void testSingleIntAssignment() throws IOException {
        assertResult(2, stmts(vardec("x", new IntType(), new IntExp(1)),
                              assign("x", new IntExp(2)),
                              printVar("x")));
    }

    @Test
    public void testTwoIntsAssignFirst() throws IOException {
        assertResult(3, stmts(vardec("x", new IntType(), new IntExp(1)),
                              vardec("y", new IntType(), new IntExp(2)),
                              assign("x", new IntExp(3)),
                              printVar("x")));
    }

    @Test
    public void testTwoIntsAssignSecond() throws IOException {
        assertResult(3, stmts(vardec("x", new IntType(), new IntExp(1)),
                              vardec("y", new IntType(), new IntExp(2)),
                              assign("y", new IntExp(3)),
                              printVar("y")));
    }

    @Test
    public void testDeclareStructureGetFirst() throws IOException {
        final FieldAccessExp access = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                         new FieldName("x"));
        final StructureName structName = new StructureName("TwoInts");
        access.setExpStructure(structName);
        assertResult(1,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           new PrintStmt(access)),
                     TWO_INTS);
    }

    @Test
    public void testDeclareStructureGetSecond() throws IOException {
        final FieldAccessExp access = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                         new FieldName("y"));
        final StructureName structName = new StructureName("TwoInts");
        access.setExpStructure(structName);
        assertResult(2,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           new PrintStmt(access)),
                     TWO_INTS);
    }

    @Test
    public void testAssignSingleStructureGetFirst() throws IOException {
        final FieldAccessExp access = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                         new FieldName("x"));
        final StructureName structName = new StructureName("TwoInts");
        access.setExpStructure(structName);
        assertResult(3,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           assign("x",
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(3),
                                                           new IntExp(4)
                                                       })),
                           new PrintStmt(access)),
                     TWO_INTS);
    }
                                  
    @Test
    public void testAssignSingleStructureGetSecond() throws IOException {
        final FieldAccessExp access = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                         new FieldName("y"));
        final StructureName structName = new StructureName("TwoInts");
        access.setExpStructure(structName);
        assertResult(4,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           assign("x",
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(3),
                                                           new IntExp(4)
                                                       })),
                           new PrintStmt(access)),
                     TWO_INTS);
    }

    @Test
    public void testAssignStructureFieldFirst() throws IOException {
        final StructureName twoInts = new StructureName("TwoInts");

        final FieldAccessExp accessExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                            new FieldName("x"));
        accessExp.setExpStructure(twoInts);
        final FieldAccessLhs accessLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                            new FieldName("x"));
        accessLhs.setLhsStructure(twoInts);
        
        assertResult(3,
                     stmts(vardec("x",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           assign(accessLhs,new IntExp(3)),
                           new PrintStmt(accessExp)),
                     TWO_INTS);
    }

    @Test
    public void testAssignStructureFieldSecond() throws IOException {
        final StructureName twoInts = new StructureName("TwoInts");
        
        final FieldAccessExp accessExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                            new FieldName("y"));
        accessExp.setExpStructure(twoInts);
        final FieldAccessLhs accessLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                            new FieldName("y"));
        accessLhs.setLhsStructure(twoInts);
        
        assertResult(3,
                     stmts(vardec("x",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           assign(accessLhs, new IntExp(3)),
                           new PrintStmt(accessExp)),
                     TWO_INTS);
    }

    @Test
    public void testAssignNestedStructureFirst() throws IOException {
        final StructureName twoInts = new StructureName("TwoInts");
        final StructureName fourInts = new StructureName("FourInts");

        final FieldAccessExp accessFirst = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                              new FieldName("first"));
        accessFirst.setExpStructure(fourInts);

        final FieldAccessExp accessX = new FieldAccessExp(accessFirst,
                                                          new FieldName("x"));
        accessX.setExpStructure(twoInts);

        final FieldAccessLhs accessLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                            new FieldName("first"));
        accessLhs.setLhsStructure(fourInts);
        
        assertResult(5,
                     stmts(
                           // first = TwoInts(1, 2)
                           vardec("first",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           // second = TwoInts(3, 4)
                           vardec("second",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(3),
                                                           new IntExp(4)
                                                       })),
                           // x = FourInts(first, second)
                           vardec("x",
                                  new StructureType(fourInts),
                                  new MakeStructureExp(fourInts,
                                                       new Exp[] {
                                                           new VariableExp(new Variable("first")),
                                                           new VariableExp(new Variable("second"))
                                                       })),
                           // x.first = TwoInts(5, 6)
                           assign(accessLhs,
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(5),
                                                           new IntExp(6)
                                                       })),
                           // print(x.first.x)
                           new PrintStmt(accessX)),
                     DOUBLE_TWO_INTS);
    }

    @Test
    public void testAssignNestedStructureSecond() throws IOException {
        final StructureName twoInts = new StructureName("TwoInts");
        final StructureName fourInts = new StructureName("FourInts");

        final FieldAccessExp accessSecond = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                               new FieldName("second"));
        accessSecond.setExpStructure(fourInts);

        final FieldAccessExp accessX = new FieldAccessExp(accessSecond,
                                                          new FieldName("x"));
        accessX.setExpStructure(twoInts);

        final FieldAccessLhs accessLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                            new FieldName("second"));
        accessLhs.setLhsStructure(fourInts);
        
        assertResult(5,
                     stmts(
                           // first = TwoInts(1, 2)
                           vardec("first",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           // second = TwoInts(3, 4)
                           vardec("second",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(3),
                                                           new IntExp(4)
                                                       })),
                           // x = FourInts(first, second)
                           vardec("x",
                                  new StructureType(fourInts),
                                  new MakeStructureExp(fourInts,
                                                       new Exp[] {
                                                           new VariableExp(new Variable("first")),
                                                           new VariableExp(new Variable("second"))
                                                       })),
                           // x.second = TwoInts(5, 6)
                           assign(accessLhs,
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(5),
                                                           new IntExp(6)
                                                       })),
                           // print(x.second.x)
                           new PrintStmt(accessX)),
                     DOUBLE_TWO_INTS);
    }

    @Test
    public void testAssignIntThroughPointer() throws IOException {
        // int x = 5;
        // int* p = &x;
        // *p = 7;
        // print(x)

        final DereferenceLhs deref = new DereferenceLhs(new VariableLhs(new Variable("p")));
        deref.setTypeAfterDereference(new IntType());
        
        assertResult(7,
                     stmts(vardec("x",
                                  new IntType(),
                                  new IntExp(5)),
                           vardec("p",
                                  new PointerType(new IntType()),
                                  new AddressOfExp(new VariableLhs(new Variable("x")))),
                           assign(deref,
                                  new IntExp(7)),
                           printVar("x")));
    }

    @Test
    public void testAssignFirstFieldThroughPointer() throws IOException {
        // x = TwoInts(1, 2);
        // p = &x.x;
        // *p = 3;
        // print(x.x);

        final DereferenceLhs deref = new DereferenceLhs(new VariableLhs(new Variable("p")));
        deref.setTypeAfterDereference(new IntType());
        final StructureName structName = new StructureName("TwoInts");
        final FieldAccessExp accessExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                            new FieldName("x"));
        accessExp.setExpStructure(structName);
        final FieldAccessLhs accessLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                            new FieldName("x"));
        accessLhs.setLhsStructure(structName);

        assertResult(3,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           vardec("p",
                                  new PointerType(new IntType()),
                                  new AddressOfExp(accessLhs)),
                           assign(deref,
                                  new IntExp(3)),
                           new PrintStmt(accessExp)),
                     TWO_INTS);
    }

    @Test
    public void testAssignSecondFieldThroughPointer() throws IOException {
        // x = TwoInts(1, 2);
        // p = &x.y;
        // *p = 3;
        // print(x.y);

        final DereferenceLhs deref = new DereferenceLhs(new VariableLhs(new Variable("p")));
        deref.setTypeAfterDereference(new IntType());
        final StructureName structName = new StructureName("TwoInts");
        final FieldAccessExp accessExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                            new FieldName("y"));
        accessExp.setExpStructure(structName);
        final FieldAccessLhs accessLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                            new FieldName("y"));
        accessLhs.setLhsStructure(structName);

        assertResult(3,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           vardec("p",
                                  new PointerType(new IntType()),
                                  new AddressOfExp(accessLhs)),
                           assign(deref,
                                  new IntExp(3)),
                           new PrintStmt(accessExp)),
                     TWO_INTS);
    }

    @Test
    public void testAssignStructureThroughPointer() throws IOException {
        // x = TwoInts(1, 2)
        // p = &x
        // *p = TwoInts(3, 4)
        // print(x.x)

        final StructureName structName = new StructureName("TwoInts");
        final DereferenceLhs deref = new DereferenceLhs(new VariableLhs(new Variable("p")));
        deref.setTypeAfterDereference(new StructureType(structName));
        final FieldAccessExp accessExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                            new FieldName("x"));
        accessExp.setExpStructure(structName);

        assertResult(3,
                     stmts(vardec("x",
                                  new StructureType(structName),
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           vardec("p",
                                  new PointerType(new StructureType(structName)),
                                  new AddressOfExp(new VariableLhs(new Variable("x")))),
                           assign(deref,
                                  new MakeStructureExp(structName,
                                                       new Exp[] {
                                                           new IntExp(3),
                                                           new IntExp(4)
                                                       })),
                           new PrintStmt(accessExp)),
                     TWO_INTS);
    }

    @Test
    public void testAssignNestedStructureThroughPointerFirst() throws IOException {
        // x = FourInts(TwoInts(1, 2), TwoInts(3, 4));
        // p = &x.first;
        // *p = TwoInts(5, 6);
        // print(x.first.y)

        final StructureName twoInts = new StructureName("TwoInts");
        final StructureName fourInts = new StructureName("FourInts");
        final DereferenceLhs deref = new DereferenceLhs(new VariableLhs(new Variable("p")));
        deref.setTypeAfterDereference(new StructureType(twoInts));
        final FieldAccessExp accessFirstExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                                 new FieldName("first"));
        accessFirstExp.setExpStructure(fourInts);
        final FieldAccessExp accessYExp = new FieldAccessExp(accessFirstExp,
                                                             new FieldName("y"));
        accessYExp.setExpStructure(twoInts);
        final FieldAccessLhs accessFirstLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                                 new FieldName("first"));
        accessFirstLhs.setLhsStructure(fourInts);

        assertResult(6,
                     stmts(vardec("x",
                                  new StructureType(fourInts),
                                  new MakeStructureExp(fourInts,
                                                       new Exp[] {
                                                           new MakeStructureExp(twoInts,
                                                                                new Exp[] {
                                                                                    new IntExp(1),
                                                                                    new IntExp(2)
                                                                                }),
                                                           new MakeStructureExp(twoInts,
                                                                                new Exp[] {
                                                                                    new IntExp(3),
                                                                                    new IntExp(4)
                                                                                })
                                                       })),
                           vardec("p",
                                  new PointerType(new StructureType(twoInts)),
                                  new AddressOfExp(accessFirstLhs)),
                           assign(deref,
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(5),
                                                           new IntExp(6)
                                                       })),
                           new PrintStmt(accessYExp)),
                     DOUBLE_TWO_INTS);
    }

    @Test
    public void testAssignNestedStructureThroughPointerSecond() throws IOException {
        // x = FourInts(TwoInts(1, 2), TwoInts(3, 4));
        // p = &x.second;
        // *p = TwoInts(5, 6);
        // print(x.second.x)

        final StructureName twoInts = new StructureName("TwoInts");
        final StructureName fourInts = new StructureName("FourInts");
        final DereferenceLhs deref = new DereferenceLhs(new VariableLhs(new Variable("p")));
        deref.setTypeAfterDereference(new StructureType(twoInts));
        final FieldAccessExp accessSecondExp = new FieldAccessExp(new VariableExp(new Variable("x")),
                                                                  new FieldName("second"));
        accessSecondExp.setExpStructure(fourInts);
        final FieldAccessExp accessYExp = new FieldAccessExp(accessSecondExp,
                                                             new FieldName("x"));
        accessYExp.setExpStructure(twoInts);
        final FieldAccessLhs accessSecondLhs = new FieldAccessLhs(new VariableLhs(new Variable("x")),
                                                                  new FieldName("second"));
        accessSecondLhs.setLhsStructure(fourInts);

        assertResult(5,
                     stmts(vardec("x",
                                  new StructureType(fourInts),
                                  new MakeStructureExp(fourInts,
                                                       new Exp[] {
                                                           new MakeStructureExp(twoInts,
                                                                                new Exp[] {
                                                                                    new IntExp(1),
                                                                                    new IntExp(2)
                                                                                }),
                                                           new MakeStructureExp(twoInts,
                                                                                new Exp[] {
                                                                                    new IntExp(3),
                                                                                    new IntExp(4)
                                                                                })
                                                       })),
                           vardec("p",
                                  new PointerType(new StructureType(twoInts)),
                                  new AddressOfExp(accessSecondLhs)),
                           assign(deref,
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(5),
                                                           new IntExp(6)
                                                       })),
                           new PrintStmt(accessYExp)),
                     DOUBLE_TWO_INTS);
    }

    @Test
    public void testDereferenceInt() throws IOException {
        // x = 5;
        // p = &x;
        // print(*p);

        final DereferenceExp deref =
            new DereferenceExp(new VariableExp(new Variable("p")));
        deref.setExpType(new PointerType(new IntType()));

        assertResult(5,
                     stmts(vardec("x",
                                  new IntType(),
                                  new IntExp(5)),
                           vardec("p",
                                  new PointerType(new IntType()),
                                  new AddressOfExp(new VariableLhs(new Variable("x")))),
                           new PrintStmt(deref)));
    }

    @Test
    public void testDereferenceStructure() throws IOException {
        // x = TwoInts(1, 2);
        // p = &x;
        // print((*p).y);

        final DereferenceExp derefExp =
            new DereferenceExp(new VariableExp(new Variable("p")));
        derefExp.setExpType(new PointerType(new IntType()));

        final StructureName twoInts = new StructureName("TwoInts");
        final FieldAccessExp accessExp =
            new FieldAccessExp(derefExp, new FieldName("y"));
        accessExp.setExpStructure(twoInts);

        assertResult(2,
                     stmts(vardec("x",
                                  new StructureType(twoInts),
                                  new MakeStructureExp(twoInts,
                                                       new Exp[] {
                                                           new IntExp(1),
                                                           new IntExp(2)
                                                       })),
                           vardec("p",
                                  new PointerType(new StructureType(twoInts)),
                                  new AddressOfExp(new VariableLhs(new Variable("x")))),
                           new PrintStmt(accessExp)),
                     TWO_INTS);
    }
}
