package org.mclavo.generator;

import java.util.stream.Collectors;

public final class DefinitionSourceRenderer {

    private static String DefinitionTemplate = 
    """
    package %s.generated;

    %s

    public final class %s implements BeanDefinition<%s> {

        private final ScopeProvider<%s> provider =
            ScopeProvider.singletonScope(beanProvider -> %s);

        @Override
        public %s create(BeanProvider beanProvider) {
            return provider.apply(beanProvider);
        }

        @Override
        public BeanKey key() {
            return BeanKey.of(%s.class, %s);
        }
    }
        
    """;
    

    public static String render(DefinitionSpec spec) {
        String importsBlock = spec.imports().stream()
            .sorted()
            .map(importName -> "import " + importName + ";")
            .collect(Collectors.joining("\n"));
        
        return DefinitionTemplate.formatted(
            spec.packageName(),
            importsBlock,
            spec.simpleClassName(),
            spec.beanType(),
            spec.beanType(),
            spec.creationExpression(),
            spec.beanType(),
            spec.beanType(),
            spec.qualifierExpression()
        );

    }


}
