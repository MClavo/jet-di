package org.mclavo;

import java.util.stream.Collectors;

/**
 * Renders Java source code for generated {@code BeanDefinition} implementations.
 */
public final class DefinitionSourceRenderer {

    private static final String DEFINITION_TEMPLATE =
    """
    package %s;

    %s

    public final class %s implements BeanDefinition<%s> {

        private final ScopeProvider<%s> provider =
            ScopeProvider.singletonScope(beanProvider -> %s);

        @Override
        public %s apply(BeanProvider beanProvider) {
            return provider.apply(beanProvider);
        }

        @Override
        public BeanKey<%s> key() {
            return BeanKey.of(%s.class, %s);
        }
    }
        
    """;
    
    /**
     * Builds the full source code for a generated definition class.
     *
     * @param spec metadata used to render package, imports, class name and creation logic
     * @return Java source code for the generated definition class
     */
    public static String render(DefinitionSpec spec) {
        String importsBlock = spec.imports().stream()
            .sorted()
            .map(importName -> "import " + importName + ";")
            .collect(Collectors.joining("\n"));
        
        return DEFINITION_TEMPLATE.formatted(
            spec.packageName(),
            importsBlock,
            spec.simpleClassName(),
            spec.beanType(),
            spec.beanType(),
            spec.creationExpression(),
            spec.beanType(),
            spec.beanType(),
            spec.beanType(),
            spec.qualifierExpression()
        );
    }
}
