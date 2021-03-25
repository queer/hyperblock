package gg.amy.hyperblock.bytecode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * @author amy
 * @since 3/22/21.
 */
public abstract class Injector implements ClassFileTransformer, Opcodes {
    private final String classToInject;

    protected Injector(final String classToInject) {
        System.out.println(">> agent: injector: will inject " + classToInject);
        this.classToInject = classToInject;
    }

    @SuppressWarnings("SameParameterValue")
    protected static String $(final Class<?> c) {
        return $(c.getName());
    }

    protected static String $(final String s) {
        return s.replace('.', '/');
    }

    protected static String $$(final Class<?> c) {
        return $$(c.getName());
    }

    protected static String $$(final String s) {
        return 'L' + $(s) + ';';
    }

    @Override
    public final byte[] transform(final ClassLoader classLoader, final String s,
                                  final Class<?> aClass, final ProtectionDomain protectionDomain, final byte[] bytes) {
        if(classToInject.equals(s)) {
            try {
                System.out.println(">> agent: injector: injecting: " + s);
                final ClassReader cr = new ClassReader(bytes);
                final ClassNode cn = new ClassNode();
                cr.accept(cn, 0);
                inject(cr, cn);
                System.out.println(">> agent: injector: finshed inject, validating");
                final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                cn.accept(cw);
                final byte[] cwBytes = cw.toByteArray();
                final var sw = new StringWriter();
                final var pw = new PrintWriter(sw);
                CheckClassAdapter.verify(new ClassReader(cwBytes), false, pw);
                System.out.println(">> agent: injector: verify output: " + sw);
                return cwBytes;
            } catch(final Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        } else {
            return null;
        }
    }

    protected abstract void inject(ClassReader cr, ClassNode cn);

    public String getClassToInject() {
        return classToInject;
    }
}
