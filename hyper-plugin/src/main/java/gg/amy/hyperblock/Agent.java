package gg.amy.hyperblock;

import gg.amy.hyperblock.bytecode.Injector;
import gg.amy.hyperblock.bytecode.injectors.RegionFileCacheInjector;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 3/22/21.
 */
public final class Agent {
    private static final List<Injector> INJECTORS = List.of(
            new RegionFileCacheInjector()
    );

    private Agent() {
    }

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        System.out.println(">> agent: agentmain: start");
        System.out.println(">> agent: agentmain: can transform? " + inst.isRetransformClassesSupported());
        for(final var injector : INJECTORS) {
            inst.addTransformer(injector);
        }
        try {
            final var classes = INJECTORS.stream()
                    .map(Injector::getClassToInject)
                    .map(c -> {
                        try {
                            return Class.forName(c.replace('/', '.'));
                        } catch(final ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList())
                    .toArray(Class[]::new);

            System.out.println(">> agent: agentmain: asking to retransform: " + Arrays.toString(classes));
            inst.retransformClasses(classes);
        } catch(final UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
        System.out.println(">> agent: agentmain: finish");
    }
}
